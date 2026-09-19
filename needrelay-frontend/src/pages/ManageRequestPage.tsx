import {
  Button,
  Code,
  Container,
  Group,
  Modal,
  NumberInput,
  Select,
  Stack,
  Text,
  TextInput,
  Textarea,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconArrowLeft } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { commentsApi, contactsApi, needsApi, reliefRequestsApi } from '../api';
import type {
  CommentResponse,
  ListOffersParams,
  NeedResponse,
  OfferResponse,
  OrganizerContactResponse,
  ReliefRequestResponse,
} from '../api/generated/models';
import {
  NeedCategory,
  NeedPriority,
  OfferStatus,
  ProviderType,
  ReliefRequestResponseStatus,
} from '../api/generated/models';
import { ConfirmModal } from '../components/ConfirmModal';
import { OfferInbox } from '../components/OfferInbox';
import { SurfaceCard } from '../components/SurfaceCard';
import { LocationMap } from '../components/map/LocationMap';
import { sortNeedsByUrgency } from '../lib/urgency';

type PendingConfirm =
  | { kind: 'closeNeed'; needId: string }
  | { kind: 'markComing'; offer: OfferResponse }
  | { kind: 'cancelOffer'; offer: OfferResponse }
  | { kind: 'deleteComment'; commentId: string }
  | { kind: 'deleteContact'; contactId: string }
  | { kind: 'toggleStatus' };

export type OfferFilters = Pick<
  ListOffersParams,
  'status' | 'providerType' | 'q' | 'minQuantity' | 'maxQuantity' | 'minDistanceKm' | 'maxDistanceKm'
>;

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
  const [offersRefreshKey, setOffersRefreshKey] = useState(0);
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [needOpen, setNeedOpen] = useState(false);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [contactsModalOpen, setContactsModalOpen] = useState(false);
  const [requestContacts, setRequestContacts] = useState<OrganizerContactResponse[]>([]);
  const [contactFormOpen, setContactFormOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<OrganizerContactResponse | null>(null);
  const [editingNeed, setEditingNeed] = useState<NeedResponse | null>(null);
  const [editingOffer, setEditingOffer] = useState<OfferResponse | null>(null);
  const [receivingOffer, setReceivingOffer] = useState<OfferResponse | null>(null);
  const [statusOffer, setStatusOffer] = useState<OfferResponse | null>(null);
  const [statusSelection, setStatusSelection] = useState<OfferStatus | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [pendingConfirm, setPendingConfirm] = useState<PendingConfirm | null>(null);
  const [confirming, setConfirming] = useState(false);
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
      status: ReliefRequestResponseStatus.ACTIVE as ReliefRequestResponseStatus,
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
      providerType: ProviderType.ORGANIZATION as ProviderType,
      quantity: 1,
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      note: '',
    },
  });
  const commentForm = useForm({ initialValues: { body: '' } });
  const contactForm = useForm({
    initialValues: { name: '', role: '', phone: '', email: '', note: '' },
  });

  /**
   * Reloads request, needs, and comments. Offer tables fetch their own data
   * independently inside {@link OfferInbox}.
   *
   * @returns void
   */
  async function reload() {
    const [req, needList, commentList, contactList] = await Promise.all([
      reliefRequestsApi.getReliefRequest(requestId),
      needsApi.listNeeds(requestId),
      commentsApi.listComments(requestId),
      contactsApi.listRequestContacts(requestId),
    ]);
    setRequest(req);
    setNeeds(needList);
    setComments(commentList);
    setRequestContacts(contactList);
    locationForm.setValues({
      title: req.title,
      description: req.description,
      locationLabel: req.locationLabel,
      latitude: req.latitude,
      longitude: req.longitude,
      status: req.status,
    });
  }

  /**
   * Manual refresh with loading state and error toast.
   *
   * @returns void
   */
  async function handleRefresh() {
    setRefreshing(true);
    try {
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    } finally {
      setRefreshing(false);
    }
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, [requestId]);

  /**
   * Runs the pending confirm action, then closes the modal.
   *
   * @returns void
   */
  async function runPendingConfirm() {
    if (!pendingConfirm) {
      return;
    }
    setConfirming(true);
    try {
      if (pendingConfirm.kind === 'closeNeed') {
        await closeNeed(pendingConfirm.needId);
      } else if (pendingConfirm.kind === 'markComing') {
        await markComing(pendingConfirm.offer);
      } else if (pendingConfirm.kind === 'cancelOffer') {
        await cancelOffer(pendingConfirm.offer);
      } else if (pendingConfirm.kind === 'deleteComment') {
        await removeComment(pendingConfirm.commentId);
      } else if (pendingConfirm.kind === 'deleteContact') {
        await removeContact(pendingConfirm.contactId);
      } else {
        await toggleRequestStatus();
      }
      setPendingConfirm(null);
    } finally {
      setConfirming(false);
    }
  }

  /**
   * Body text for the current pending confirm action.
   *
   * @returns confirmation body or empty string
   */
  function pendingConfirmBody(): string {
    if (!pendingConfirm) {
      return '';
    }
    if (pendingConfirm.kind === 'closeNeed') {
      return t('request.confirmCloseNeed');
    }
    if (pendingConfirm.kind === 'markComing') {
      return t('offer.confirmMarkComing');
    }
    if (pendingConfirm.kind === 'cancelOffer') {
      return t('request.confirmCancelOffer');
    }
    if (pendingConfirm.kind === 'deleteComment') {
      return t('request.confirmDeleteComment');
    }
    if (pendingConfirm.kind === 'deleteContact') {
      return t('account.confirmDeleteContact');
    }
    return request?.status === ReliefRequestResponseStatus.ACTIVE
      ? t('request.confirmDeactivate')
      : t('request.confirmReactivate');
  }

  /**
   * Toggles the relief request between ACTIVE and ARCHIVED. Archiving stops new public
   * offers and hides the public page's details (only the deactivated notice remains).
   *
   * @returns void
   */
  async function toggleRequestStatus() {
    if (!request) {
      return;
    }
    const nextStatus =
      request.status === ReliefRequestResponseStatus.ACTIVE
        ? ReliefRequestResponseStatus.ARCHIVED
        : ReliefRequestResponseStatus.ACTIVE;
    try {
      await reliefRequestsApi.updateReliefRequest(requestId, {
        title: request.title,
        description: request.description,
        locationLabel: request.locationLabel,
        latitude: request.latitude,
        longitude: request.longitude,
        status: nextStatus,
      });
      notifications.show({
        color: 'green',
        message:
          nextStatus === ReliefRequestResponseStatus.ARCHIVED
            ? t('request.deactivated')
            : t('request.reactivated'),
      });
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Saves location and request metadata.
   *
   * @param values location form
   */
  async function saveLocation(values: typeof locationForm.values) {
    try {
      await reliefRequestsApi.updateReliefRequest(requestId, values);
      notifications.show({ color: 'green', message: t('request.locationSaved') });
      setDetailsModalOpen(false);
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
      providerType: offer.providerType,
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
      setOffersRefreshKey((key) => key + 1);
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
      setOffersRefreshKey((key) => key + 1);
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
    setStatusOffer(null);
    setReceivingOffer(offer);
    receiveForm.setValues({ quantityReceived: offer.quantity });
  }

  /**
   * Closes the status modal and opens a separate confirm modal to mark the offer coming.
   *
   * @param offer offer to mark coming
   * @returns void
   */
  function requestMarkComing(offer: OfferResponse) {
    setStatusOffer(null);
    setPendingConfirm({ kind: 'markComing', offer });
  }

  /**
   * Closes the status modal and opens a separate confirm modal to cancel the offer.
   *
   * @param offer offer to cancel
   * @returns void
   */
  function requestCancelOffer(offer: OfferResponse) {
    setStatusOffer(null);
    setPendingConfirm({ kind: 'cancelOffer', offer });
  }

  /**
   * Opens the status modal for an offer, seeded with its current status.
   *
   * @param offer offer to change status for
   * @returns void
   */
  function openStatusModal(offer: OfferResponse) {
    setStatusOffer(offer);
    setStatusSelection(offer.status);
  }

  /**
   * Whether picking `target` from the status dropdown is a real, supported transition
   * from `current`. Offers can only move forward (pending → coming/received/cancelled,
   * coming → received/cancelled); reverting to pending is never allowed.
   *
   * @param current offer's current status
   * @param target dropdown selection
   * @returns whether the transition is applicable
   */
  function isValidStatusTransition(current: OfferStatus, target: OfferStatus | null): boolean {
    if (!target || target === current || target === OfferStatus.PENDING) {
      return false;
    }
    if (current === OfferStatus.PENDING) {
      return true;
    }
    if (current === OfferStatus.COMING) {
      return target === OfferStatus.RECEIVED || target === OfferStatus.CANCELLED;
    }
    return false;
  }

  /**
   * Applies the selected status from the status modal dropdown, routing to the
   * matching separate confirmation step.
   *
   * @returns void
   */
  function applyStatusSelection() {
    if (!statusOffer || !isValidStatusTransition(statusOffer.status, statusSelection)) {
      return;
    }
    if (statusSelection === OfferStatus.COMING) {
      requestMarkComing(statusOffer);
    } else if (statusSelection === OfferStatus.RECEIVED) {
      openReceive(statusOffer);
    } else if (statusSelection === OfferStatus.CANCELLED) {
      requestCancelOffer(statusOffer);
    }
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
      setOffersRefreshKey((key) => key + 1);
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
      setOffersRefreshKey((key) => key + 1);
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

  /**
   * Opens the create-contact form for this relief request.
   *
   * @returns void
   */
  function openCreateContact() {
    setEditingContact(null);
    contactForm.reset();
    setContactFormOpen(true);
  }

  /**
   * Opens the edit-contact form seeded with an existing request contact.
   *
   * @param contact contact to edit
   * @returns void
   */
  function openEditContact(contact: OrganizerContactResponse) {
    setEditingContact(contact);
    contactForm.setValues({
      name: contact.name,
      role: contact.role,
      phone: contact.phone,
      email: contact.email,
      note: contact.note ?? '',
    });
    setContactFormOpen(true);
  }

  /**
   * Creates or updates a contact specific to this relief request.
   *
   * @param values contact form
   * @returns void
   */
  async function saveContact(values: typeof contactForm.values) {
    const payload = {
      name: values.name,
      role: values.role,
      phone: values.phone,
      email: values.email,
      note: values.note.trim() || null,
    };
    try {
      if (editingContact) {
        await contactsApi.updateRequestContact(requestId, editingContact.id, payload);
      } else {
        await contactsApi.createRequestContact(requestId, payload);
      }
      setContactFormOpen(false);
      contactForm.reset();
      setEditingContact(null);
      await reload();
      notifications.show({ color: 'green', message: t('account.contactSaved') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Deletes a contact specific to this relief request.
   *
   * @param contactId contact id
   * @returns void
   */
  async function removeContact(contactId: string) {
    try {
      await contactsApi.deleteRequestContact(requestId, contactId);
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
        <Button
          component={Link}
          to="/dashboard"
          variant="default"
          size="sm"
          w="fit-content"
          leftSection={<IconArrowLeft size={16} />}
        >
          {t('request.backToDashboard')}
        </Button>
        <Group justify="space-between">
          <div>
            <Title order={2}>{request.title}</Title>
            <Text c="dimmed">{request.locationLabel}</Text>
          </div>
          <Group gap="sm">
            <Button
              data-testid="manage-refresh"
              variant="default"
              loading={refreshing}
              onClick={() => void handleRefresh()}
            >
              {t('common.refresh')}
            </Button>
            <Button
              data-testid="manage-edit-details"
              variant="default"
              onClick={() => setDetailsModalOpen(true)}
            >
              {t('request.editDetails')}
            </Button>
            <Button
              data-testid="manage-share"
              variant="light"
              onClick={() => setShareModalOpen(true)}
            >
              {t('request.share')}
            </Button>
            <Button
              data-testid="manage-contacts"
              variant="default"
              onClick={() => setContactsModalOpen(true)}
            >
              {t('request.contacts')}
            </Button>
            <Button
              data-testid="manage-toggle-status"
              variant="outline"
              color={request.status === ReliefRequestResponseStatus.ACTIVE ? 'red' : 'green'}
              onClick={() => setPendingConfirm({ kind: 'toggleStatus' })}
            >
              {request.status === ReliefRequestResponseStatus.ACTIVE
                ? t('request.deactivate')
                : t('request.reactivate')}
            </Button>
          </Group>
        </Group>
        {request.status === ReliefRequestResponseStatus.ARCHIVED ? (
          <Text c="red" fw={600} size="sm">
            {t('request.deactivatedNotice')}
          </Text>
        ) : null}

        <Stack gap="md">
          <LocationMap
            mode="readonly"
            lat={request.latitude}
            lng={request.longitude}
            height={240}
          />
          <OfferInbox
            needs={sortedNeeds}
            requestId={requestId}
            refreshToken={offersRefreshKey}
            onAddNeed={() => setNeedOpen(true)}
            onEditNeed={openEditNeed}
            onCloseNeed={(needId) => setPendingConfirm({ kind: 'closeNeed', needId })}
            onEdit={openEditOffer}
            onOpenStatus={openStatusModal}
          />
        </Stack>

        <SurfaceCard accent="ink" data-testid="comments-section">
          <Stack gap="lg">
            <div>
              <Title order={4}>{t('comments.title')}</Title>
              <Text size="sm" c="dimmed">
                {t('comments.hint')}
              </Text>
            </div>
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
                        onClick={() =>
                          setPendingConfirm({ kind: 'deleteComment', commentId: comment.id })
                        }
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
        </SurfaceCard>
      </Stack>

      <Modal
        opened={detailsModalOpen}
        onClose={() => setDetailsModalOpen(false)}
        title={t('request.detailsMeta')}
      >
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
      </Modal>

      <Modal
        opened={shareModalOpen}
        onClose={() => setShareModalOpen(false)}
        title={t('request.share')}
      >
        <Stack gap="sm">
          <Code block data-testid="share-public-link">
            {publicPath}
          </Code>
          <Group justify="flex-end">
            <Button
              data-testid="share-copy-link"
              onClick={() => {
                void navigator.clipboard.writeText(publicPath).then(() => {
                  notifications.show({ color: 'green', message: t('request.linkCopied') });
                });
              }}
            >
              {t('request.copyLink')}
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={contactsModalOpen}
        onClose={() => setContactsModalOpen(false)}
        title={t('request.contacts')}
      >
        <Stack gap="md">
          <Group justify="space-between">
            <Text size="sm" c="dimmed">
              {t('request.contactsHint')}
            </Text>
            <Button size="xs" onClick={openCreateContact}>
              {t('account.addContact')}
            </Button>
          </Group>
          {requestContacts.length === 0 ? (
            <Text c="dimmed">{t('account.contactsEmpty')}</Text>
          ) : (
            <Stack gap="sm">
              {requestContacts.map((contact) => (
                <SurfaceCard key={contact.id} p="sm" accent="none">
                  <Group justify="space-between" align="flex-start">
                    <div>
                      <Text fw={700}>{contact.name}</Text>
                      <Text size="sm">{contact.role}</Text>
                      <Text size="sm">
                        <bdi dir="ltr">
                          {contact.phone} · {contact.email}
                        </bdi>
                      </Text>
                      {contact.note ? (
                        <Text size="xs" c="dimmed" mt={4}>
                          {contact.note}
                        </Text>
                      ) : null}
                    </div>
                    <Group gap="xs">
                      <Button size="xs" variant="light" onClick={() => openEditContact(contact)}>
                        {t('account.editContact')}
                      </Button>
                      <Button
                        size="xs"
                        color="red"
                        variant="outline"
                        onClick={() => setPendingConfirm({ kind: 'deleteContact', contactId: contact.id })}
                      >
                        {t('common.delete')}
                      </Button>
                    </Group>
                  </Group>
                </SurfaceCard>
              ))}
            </Stack>
          )}
        </Stack>
      </Modal>

      <Modal
        opened={contactFormOpen}
        onClose={() => setContactFormOpen(false)}
        title={editingContact ? t('account.editContact') : t('account.addContact')}
      >
        <form onSubmit={contactForm.onSubmit(saveContact)}>
          <Stack>
            <TextInput label={t('account.contactName')} required {...contactForm.getInputProps('name')} />
            <TextInput label={t('account.contactRole')} required {...contactForm.getInputProps('role')} />
            <TextInput label={t('account.contactPhone')} required {...contactForm.getInputProps('phone')} />
            <TextInput label={t('account.contactEmail')} required {...contactForm.getInputProps('email')} />
            <Textarea label={t('account.contactNote')} {...contactForm.getInputProps('note')} />
            <Button type="submit">{t('request.save')}</Button>
          </Stack>
        </form>
      </Modal>

      <Modal opened={needOpen} onClose={() => setNeedOpen(false)} title={t('request.addNeed')}>
        <form onSubmit={needForm.onSubmit(addNeed)}>
          <Stack>
            <TextInput label={t('request.title')} required {...needForm.getInputProps('title')} />
            <Textarea label={t('request.description')} {...needForm.getInputProps('description')} />
            <Select
              label={t('need.category')}
              data={Object.values(NeedCategory).map((value) => ({
                value,
                label: t(`need.categoryValues.${value}`),
              }))}
              {...needForm.getInputProps('category')}
            />
            <Select
              label={t('need.priority')}
              data={Object.values(NeedPriority).map((value) => ({
                value,
                label: t(`need.priorityValues.${value}`),
              }))}
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
              data={Object.values(NeedCategory).map((value) => ({
                value,
                label: t(`need.categoryValues.${value}`),
              }))}
              {...editForm.getInputProps('category')}
            />
            <Select
              label={t('need.priority')}
              data={Object.values(NeedPriority).map((value) => ({
                value,
                label: t(`need.priorityValues.${value}`),
              }))}
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
            <Select
              label={t('offer.providerType')}
              data={[
                { value: ProviderType.PERSON, label: t('offer.providerTypeValues.PERSON') },
                { value: ProviderType.ORGANIZATION, label: t('offer.providerTypeValues.ORGANIZATION') },
              ]}
              allowDeselect={false}
              {...offerForm.getInputProps('providerType')}
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

      <Modal
        opened={Boolean(statusOffer)}
        onClose={() => setStatusOffer(null)}
        title={t('offer.changeStatus')}
      >
        {statusOffer ? (
          <Stack gap="sm">
            <Text size="sm">{statusOffer.providerName}</Text>
            <Select
              label={t('offer.statusLabel')}
              data={Object.values(OfferStatus).map((value) => ({
                value,
                label: t(`offer.status.${value}`),
                disabled: !isValidStatusTransition(statusOffer.status, value),
              }))}
              value={statusSelection}
              onChange={(value) => setStatusSelection(value as OfferStatus)}
              allowDeselect={false}
            />
            <Button
              disabled={!isValidStatusTransition(statusOffer.status, statusSelection)}
              onClick={applyStatusSelection}
            >
              {t('common.confirm')}
            </Button>
          </Stack>
        ) : null}
      </Modal>

      <ConfirmModal
        opened={Boolean(pendingConfirm)}
        onClose={() => setPendingConfirm(null)}
        title={t('common.confirmTitle')}
        body={pendingConfirmBody()}
        confirming={confirming}
        onConfirm={() => void runPendingConfirm()}
      />
    </Container>
  );
}
