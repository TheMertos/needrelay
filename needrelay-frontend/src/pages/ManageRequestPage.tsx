import {
  Accordion,
  Button,
  Code,
  Container,
  Group,
  Modal,
  NumberInput,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { commentsApi, needsApi, reliefRequestsApi } from '../api';
import type {
  CommentResponse,
  NeedResponse,
  OfferResponse,
  ReliefRequestResponse,
} from '../api/generated/models';
import { NeedCategory, NeedPriority, NeedStatus } from '../api/generated/models';
import { ManageDualPane } from '../components/ManageDualPane';
import { NeedStatusBadge } from '../components/NeedStatusBadge';
import { OfferInbox } from '../components/OfferInbox';
import { SurfaceCard } from '../components/SurfaceCard';
import { LocationMap } from '../components/map/LocationMap';
import { sortNeedsByUrgency } from '../lib/urgency';

/**
 * Organizer manage view: dual-pane triage (needs+map | offers) with secondary notes.
 *
 * @returns manage page
 */
export function ManageRequestPage() {
  const { requestId = '' } = useParams();
  const { t } = useTranslation();
  const [request, setRequest] = useState<ReliefRequestResponse | null>(null);
  const [needs, setNeeds] = useState<NeedResponse[]>([]);
  const [offers, setOffers] = useState<OfferResponse[]>([]);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [needOpen, setNeedOpen] = useState(false);
  const [editingNeed, setEditingNeed] = useState<NeedResponse | null>(null);
  const [editingOffer, setEditingOffer] = useState<OfferResponse | null>(null);
  const [receivingOffer, setReceivingOffer] = useState<OfferResponse | null>(null);
  const receiveForm = useForm({
    initialValues: { quantityReceived: 1 },
  });

  const locationForm = useForm({
    initialValues: {
      title: '',
      description: '',
      locationLabel: '',
      latitude: 0,
      longitude: 0,
    },
  });
  const needForm = useForm({
    initialValues: {
      title: '',
      description: '',
      category: NeedCategory.SUPPLIES,
      quantityRequired: 1,
      unit: 'units',
      priority: NeedPriority.NORMAL,
    },
  });
  const editForm = useForm<{
    title: string;
    description: string;
    category: NeedCategory;
    quantityRequired: number;
    unit: string;
    priority: NeedPriority;
  }>({
    initialValues: {
      title: '',
      description: '',
      category: NeedCategory.SUPPLIES,
      quantityRequired: 1,
      unit: 'units',
      priority: NeedPriority.NORMAL,
    },
  });
  const offerForm = useForm({
    initialValues: {
      providerName: '',
      quantity: 1,
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      note: '',
    },
  });
  const commentForm = useForm({ initialValues: { body: '' } });

  /**
   * Reloads request, needs, offers, and comments.
   */
  async function reload() {
    const [req, needList, offerList, commentList] = await Promise.all([
      reliefRequestsApi.getReliefRequest(requestId),
      needsApi.listNeeds(requestId),
      reliefRequestsApi.listOffers(requestId),
      commentsApi.listComments(requestId),
    ]);
    setRequest(req);
    setNeeds(needList);
    setOffers(offerList);
    setComments(commentList);
    locationForm.setValues({
      title: req.title,
      description: req.description,
      locationLabel: req.locationLabel,
      latitude: req.latitude,
      longitude: req.longitude,
    });
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, [requestId]);

  /**
   * Saves location and request metadata.
   *
   * @param values location form
   */
  async function saveLocation(values: typeof locationForm.values) {
    try {
      await reliefRequestsApi.updateReliefRequest(requestId, values);
      notifications.show({ color: 'green', message: t('request.locationSaved') });
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Creates a need on the current request.
   *
   * @param values need form
   */
  async function addNeed(values: typeof needForm.values) {
    try {
      await needsApi.createNeed(requestId, {
        ...values,
        description: values.description || null,
      });
      setNeedOpen(false);
      needForm.reset();
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Opens the edit modal for an existing need.
   *
   * @param need need to edit
   */
  function openEditNeed(need: NeedResponse) {
    setEditingNeed(need);
    editForm.setValues({
      title: need.title,
      description: need.description ?? '',
      category: need.category,
      quantityRequired: need.quantityRequired,
      unit: need.unit,
      priority: need.priority,
    });
  }

  /**
   * Saves need edits.
   *
   * @param values edit form
   */
  async function saveNeedEdit(values: typeof editForm.values) {
    if (!editingNeed) {
      return;
    }
    try {
      await needsApi.updateNeed(requestId, editingNeed.id, {
        ...values,
        description: values.description || null,
      });
      setEditingNeed(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Closes a need.
   *
   * @param needId need id
   */
  async function closeNeed(needId: string) {
    try {
      await needsApi.closeNeed(requestId, needId);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Opens offer edit modal.
   *
   * @param offer offer to edit
   */
  function openEditOffer(offer: OfferResponse) {
    setEditingOffer(offer);
    offerForm.setValues({
      providerName: offer.providerName,
      quantity: offer.quantity,
      firstName: offer.firstName,
      lastName: offer.lastName,
      phone: offer.phone,
      email: offer.email,
      note: offer.note ?? '',
    });
  }

  /**
   * Saves organizer edits to a public offer.
   *
   * @param values offer form
   */
  async function saveOfferEdit(values: typeof offerForm.values) {
    if (!editingOffer) {
      return;
    }
    try {
      await reliefRequestsApi.updateOffer(requestId, editingOffer.id, {
        ...values,
        note: values.note || null,
      });
      setEditingOffer(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Marks a pending offer as coming.
   *
   * @param offer offer to update
   * @returns void
   */
  async function markComing(offer: OfferResponse) {
    try {
      await reliefRequestsApi.markOfferComing(requestId, offer.id);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Opens receive modal with expected quantity as default.
   *
   * @param offer offer to receive
   * @returns void
   */
  function openReceive(offer: OfferResponse) {
    setReceivingOffer(offer);
    receiveForm.setValues({ quantityReceived: offer.quantity });
  }

  /**
   * Confirms received quantity for an offer.
   *
   * @param values receive form
   * @returns void
   */
  async function confirmReceived(values: typeof receiveForm.values) {
    if (!receivingOffer) {
      return;
    }
    try {
      await reliefRequestsApi.markOfferReceived(requestId, receivingOffer.id, {
        quantityReceived: values.quantityReceived,
      });
      setReceivingOffer(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Cancels a pending or coming offer.
   *
   * @param offer offer to cancel
   * @returns void
   */
  async function cancelOffer(offer: OfferResponse) {
    try {
      await reliefRequestsApi.cancelOffer(requestId, offer.id);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Deletes an offer and restores remaining quantity.
   *
   * @param offerId offer id
   */
  async function removeOffer(offerId: string) {
    try {
      await reliefRequestsApi.deleteOffer(requestId, offerId);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Posts an internal organizer note/comment.
   *
   * @param values comment form
   */
  async function addComment(values: typeof commentForm.values) {
    try {
      await commentsApi.createComment(requestId, values);
      commentForm.reset();
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Deletes a comment.
   *
   * @param commentId comment id
   */
  async function removeComment(commentId: string) {
    try {
      await commentsApi.deleteComment(requestId, commentId);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  if (!request) {
    return (
      <Container py="xl">
        <Text>{t('common.loading')}</Text>
      </Container>
    );
  }

  const publicPath = `${window.location.origin}/r/${request.publicSlug}`;
  const minRequired = editingNeed?.quantityOffered ?? 0.0001;
  const sortedNeeds = sortNeedsByUrgency(needs);

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        <Group justify="space-between">
          <div>
            <Title order={2}>{request.title}</Title>
            <Text c="dimmed">{request.locationLabel}</Text>
          </div>
          <Button component={Link} to={`/r/${request.publicSlug}`} variant="light">
            {t('request.share')}
          </Button>
        </Group>
        <Code block>{publicPath}</Code>

        <ManageDualPane
          left={
            <SurfaceCard accent="ink">
            <Stack gap="md">
              <Group justify="space-between">
                <Title order={3}>{t('request.needs')}</Title>
                <Button onClick={() => setNeedOpen(true)}>{t('request.addNeed')}</Button>
              </Group>

              <Stack gap="sm" hiddenFrom="sm" data-testid="mobile-card-list">
                {sortedNeeds.map((need) => (
                  <SurfaceCard key={need.id} p="md" accent="ink">
                    <Stack gap="sm">
                      <Stack gap={2}>
                        <Text fw={600}>{need.title}</Text>
                        <NeedStatusBadge status={need.status} />
                      </Stack>
                      <Text
                        fw={800}
                        style={{
                          color:
                            need.priority === NeedPriority.CRITICAL
                              ? 'var(--nr-signal)'
                              : 'var(--nr-ink)',
                        }}
                      >
                        {need.remaining} {need.unit}
                      </Text>
                      <Text size="sm">{need.priority}</Text>
                      {need.status !== NeedStatus.CLOSED ? (
                        <Stack gap="xs">
                          <Button size="sm" variant="light" onClick={() => openEditNeed(need)}>
                            {t('request.editNeed')}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            color="gray"
                            onClick={() => void closeNeed(need.id)}
                          >
                            {t('request.closeNeed')}
                          </Button>
                        </Stack>
                      ) : null}
                    </Stack>
                  </SurfaceCard>
                ))}
              </Stack>

              <Table visibleFrom="sm" data-testid="desktop-table">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{t('request.title')}</Table.Th>
                    <Table.Th>{t('need.remaining')}</Table.Th>
                    <Table.Th>{t('need.priority')}</Table.Th>
                    <Table.Th />
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {sortedNeeds.map((need) => (
                    <Table.Tr key={need.id}>
                      <Table.Td>
                        <Stack gap={2}>
                          <Text fw={600}>{need.title}</Text>
                          <NeedStatusBadge status={need.status} />
                        </Stack>
                      </Table.Td>
                      <Table.Td>
                        <Text
                          fw={800}
                          style={{
                            color:
                              need.priority === NeedPriority.CRITICAL
                                ? 'var(--nr-signal)'
                                : 'var(--nr-ink)',
                          }}
                        >
                          {need.remaining} {need.unit}
                        </Text>
                      </Table.Td>
                      <Table.Td>{need.priority}</Table.Td>
                      <Table.Td>
                        <Group gap="xs" justify="flex-end" wrap="nowrap">
                          {need.status !== NeedStatus.CLOSED ? (
                            <>
                              <Button
                                size="sm"
                                variant="light"
                                onClick={() => openEditNeed(need)}
                              >
                                {t('request.editNeed')}
                              </Button>
                              <Button
                                size="sm"
                                variant="outline"
                                color="gray"
                                onClick={() => void closeNeed(need.id)}
                              >
                                {t('request.closeNeed')}
                              </Button>
                            </>
                          ) : null}
                        </Group>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
              <LocationMap
                mode="readonly"
                lat={request.latitude}
                lng={request.longitude}
                height={240}
              />
            </Stack>
            </SurfaceCard>
          }
          right={
            <OfferInbox
              offers={offers}
              onEdit={openEditOffer}
              onMarkComing={(offer) => void markComing(offer)}
              onMarkReceived={openReceive}
              onCancel={(offer) => void cancelOffer(offer)}
              onDelete={(id) => void removeOffer(id)}
            />
          }
        />

        <Accordion variant="contained">
          <Accordion.Item value="meta">
            <Accordion.Control>{t('request.detailsMeta')}</Accordion.Control>
            <Accordion.Panel>
              <Stack gap="lg">
                <form onSubmit={locationForm.onSubmit(saveLocation)}>
                  <Stack>
                    <Text size="sm" c="dimmed">
                      {t('request.locationHint')}
                    </Text>
                    <TextInput
                      label={t('request.title')}
                      required
                      {...locationForm.getInputProps('title')}
                    />
                    <Textarea
                      label={t('request.description')}
                      minRows={2}
                      {...locationForm.getInputProps('description')}
                    />
                    <TextInput
                      label={t('request.locationLabel')}
                      required
                      {...locationForm.getInputProps('locationLabel')}
                    />
                    <LocationMap
                      mode="picker"
                      lat={locationForm.values.latitude}
                      lng={locationForm.values.longitude}
                      onChange={(lat, lng) => {
                        locationForm.setFieldValue('latitude', lat);
                        locationForm.setFieldValue('longitude', lng);
                      }}
                    />
                    <Group>
                      <NumberInput
                        label="Latitude"
                        decimalScale={6}
                        {...locationForm.getInputProps('latitude')}
                      />
                      <NumberInput
                        label="Longitude"
                        decimalScale={6}
                        {...locationForm.getInputProps('longitude')}
                      />
                    </Group>
                    <Button type="submit" w="fit-content">
                      {t('request.saveLocation')}
                    </Button>
                  </Stack>
                </form>

                <Title order={4}>{t('comments.title')}</Title>
                <Text size="sm" c="dimmed">
                  {t('comments.hint')}
                </Text>
                <Stack gap="sm">
                  {comments.map((comment) => (
                    <Stack
                      key={comment.id}
                      gap={4}
                      p="sm"
                      style={{ borderBottom: '1px solid var(--mantine-color-gray-3)' }}
                    >
                      <Group justify="space-between">
                        <Text fw={600} size="sm">
                          {comment.authorDisplayName}
                        </Text>
                        <Group gap="xs">
                          <Text size="xs" c="dimmed">
                            {new Date(comment.createdAt).toLocaleString()}
                          </Text>
                          <Button
                            size="compact-xs"
                            variant="subtle"
                            color="red"
                            onClick={() => void removeComment(comment.id)}
                          >
                            {t('common.delete')}
                          </Button>
                        </Group>
                      </Group>
                      <Text size="sm">{comment.body}</Text>
                    </Stack>
                  ))}
                </Stack>
                <form onSubmit={commentForm.onSubmit(addComment)}>
                  <Stack>
                    <Textarea
                      label={t('comments.new')}
                      minRows={2}
                      {...commentForm.getInputProps('body')}
                    />
                    <Button type="submit" w="fit-content">
                      {t('comments.post')}
                    </Button>
                  </Stack>
                </form>
              </Stack>
            </Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      </Stack>

      <Modal opened={needOpen} onClose={() => setNeedOpen(false)} title={t('request.addNeed')}>
        <form onSubmit={needForm.onSubmit(addNeed)}>
          <Stack>
            <TextInput label={t('request.title')} required {...needForm.getInputProps('title')} />
            <Textarea label={t('request.description')} {...needForm.getInputProps('description')} />
            <Select
              label={t('need.category')}
              data={Object.values(NeedCategory)}
              {...needForm.getInputProps('category')}
            />
            <Select
              label={t('need.priority')}
              data={Object.values(NeedPriority)}
              {...needForm.getInputProps('priority')}
            />
            <NumberInput
              label={t('need.required')}
              min={0.0001}
              {...needForm.getInputProps('quantityRequired')}
            />
            <TextInput label={t('need.unit')} {...needForm.getInputProps('unit')} />
            <Button type="submit">{t('request.save')}</Button>
          </Stack>
        </form>
      </Modal>

      <Modal
        opened={Boolean(editingNeed)}
        onClose={() => setEditingNeed(null)}
        title={t('request.editNeed')}
      >
        <form onSubmit={editForm.onSubmit(saveNeedEdit)}>
          <Stack>
            <TextInput label={t('request.title')} required {...editForm.getInputProps('title')} />
            <Textarea label={t('request.description')} {...editForm.getInputProps('description')} />
            <Select
              label={t('need.category')}
              data={Object.values(NeedCategory)}
              {...editForm.getInputProps('category')}
            />
            <Select
              label={t('need.priority')}
              data={Object.values(NeedPriority)}
              {...editForm.getInputProps('priority')}
            />
            <NumberInput
              label={t('need.required')}
              description={t('request.requiredMinHint', {
                offered: editingNeed?.quantityOffered ?? 0,
                unit: editingNeed?.unit ?? '',
              })}
              min={minRequired}
              {...editForm.getInputProps('quantityRequired')}
            />
            <TextInput label={t('need.unit')} {...editForm.getInputProps('unit')} />
            <Button type="submit">{t('request.save')}</Button>
          </Stack>
        </form>
      </Modal>

      <Modal
        opened={Boolean(editingOffer)}
        onClose={() => setEditingOffer(null)}
        title={t('request.editOffer')}
      >
        <form onSubmit={offerForm.onSubmit(saveOfferEdit)}>
          <Stack>
            <TextInput
              label={t('offer.providerName')}
              required
              {...offerForm.getInputProps('providerName')}
            />
            <NumberInput
              label={t('offer.quantity')}
              min={0.0001}
              {...offerForm.getInputProps('quantity')}
            />
            <TextInput label={t('offer.firstName')} required {...offerForm.getInputProps('firstName')} />
            <TextInput label={t('offer.lastName')} required {...offerForm.getInputProps('lastName')} />
            <TextInput label={t('offer.phone')} required {...offerForm.getInputProps('phone')} />
            <TextInput label={t('offer.email')} type="email" required {...offerForm.getInputProps('email')} />
            <Textarea label={t('offer.note')} {...offerForm.getInputProps('note')} />
            <Button type="submit">{t('request.save')}</Button>
          </Stack>
        </form>
      </Modal>

      <Modal
        opened={Boolean(receivingOffer)}
        onClose={() => setReceivingOffer(null)}
        title={t('offer.markReceived')}
      >
        <form onSubmit={receiveForm.onSubmit(confirmReceived)}>
          <Stack>
            <NumberInput
              label={t('offer.quantityReceived')}
              min={0.0001}
              required
              {...receiveForm.getInputProps('quantityReceived')}
            />
            <Button type="submit" color="red">
              {t('offer.markReceived')}
            </Button>
          </Stack>
        </form>
      </Modal>
    </Container>
  );
}
