import {
  Button,
  Container,
  Group,
  Input,
  Modal,
  NumberInput,
  SegmentedControl,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { IconArrowLeft, IconNavigation } from '@tabler/icons-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';
import { publicApi } from '../api';
import type { NeedResponse, PublicReliefRequestResponse } from '../api/generated/models';
import {
  NeedPriority,
  NeedStatus,
  ProviderType,
  PublicReliefRequestResponseStatus,
} from '../api/generated/models';
import { NeedOfferRow } from '../components/NeedOfferRow';
import { PublicReliefSplit } from '../components/PublicReliefSplit';
import { SurfaceCard } from '../components/SurfaceCard';
import { UrgencyKpiStrip } from '../components/UrgencyKpiStrip';
import { LocationMap } from '../components/map/LocationMap';
import { getCurrentPositionSafe, type GeoCoordinates } from '../lib/geolocation';
import { sortNeedsByUrgency } from '../lib/urgency';

/**
 * Public relief page — map + remaining needs, offer without account.
 *
 * @returns public relief page
 */
export function PublicReliefPage() {
  const { slug = '' } = useParams();
  const { t } = useTranslation();
  const [data, setData] = useState<PublicReliefRequestResponse | null>(null);
  const [selected, setSelected] = useState<NeedResponse | null>(null);
  const [geoPosition, setGeoPosition] = useState<GeoCoordinates | null>(null);
  const [geoState, setGeoState] = useState<'idle' | 'requesting' | 'granted' | 'denied'>('idle');
  const form = useForm({
    initialValues: {
      providerName: '',
      providerType: ProviderType.PERSON,
      quantity: 1,
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      note: '',
    },
  });

  /**
   * Loads the public relief request by slug.
   */
  async function load() {
    const response = await publicApi.getPublicReliefRequest(slug);
    setData(response);
  }

  useEffect(() => {
    void load().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, [slug]);

  /**
   * Requests the browser's current location; required before an offer can be sent
   * so organizers can see the offerer's distance. Only the computed distance is
   * ever stored — never the exact coordinates.
   *
   * @returns void
   */
  async function requestLocation() {
    setGeoState('requesting');
    const position = await getCurrentPositionSafe();
    if (position) {
      setGeoPosition(position);
      setGeoState('granted');
    } else {
      setGeoPosition(null);
      setGeoState('denied');
    }
  }

  useEffect(() => {
    if (selected && geoState === 'idle') {
      void requestLocation();
    }
  }, [selected, geoState]);

  /**
   * Submits an anonymous offer for the selected need. Requires a shared location.
   *
   * @param values offer form values
   */
  async function submitOffer(values: typeof form.values) {
    if (!selected || !geoPosition) {
      return;
    }
    try {
      await publicApi.createPublicOffer(selected.id, {
        providerName: values.providerName,
        providerType: values.providerType,
        quantity: values.quantity,
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone,
        email: values.email,
        note: values.note || null,
        latitude: geoPosition.latitude,
        longitude: geoPosition.longitude,
      });
      notifications.show({ color: 'green', message: t('offer.success') });
      closeOfferModal();
      form.reset();
      await load();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Closes the offer modal and resets geolocation state for the next attempt.
   *
   * @returns void
   */
  function closeOfferModal() {
    setSelected(null);
    setGeoPosition(null);
    setGeoState('idle');
  }

  if (!data) {
    return (
      <Container py="xl">
        <Text>{t('common.loading')}</Text>
      </Container>
    );
  }

  if (data.status === PublicReliefRequestResponseStatus.ARCHIVED) {
    return (
      <Container size="sm" py="xl">
        <Stack gap="md" align="flex-start">
          <Button
            component={Link}
            to="/"
            variant="default"
            size="sm"
            leftSection={<IconArrowLeft size={16} />}
          >
            {t('publicPage.backToList')}
          </Button>
          <Title order={2}>{data.title}</Title>
          <Text c="dimmed">{t('publicPage.deactivated')}</Text>
        </Stack>
      </Container>
    );
  }

  const sorted = sortNeedsByUrgency(data.needs);
  const critical = data.needs.filter(
    (n) =>
      n.priority === NeedPriority.CRITICAL &&
      n.status !== NeedStatus.COVERED &&
      n.status !== NeedStatus.CLOSED,
  ).length;
  const open = data.needs.filter(
    (n) => n.status === NeedStatus.OPEN || n.status === NeedStatus.PARTIALLY_COVERED,
  ).length;
  const covered = data.needs.filter((n) => n.status === NeedStatus.COVERED).length;

  return (
    <Container size="lg" py="md">
      <Stack gap="lg">
        <Button
          component={Link}
          to="/"
          variant="default"
          size="sm"
          w="fit-content"
          leftSection={<IconArrowLeft size={16} />}
        >
          {t('publicPage.backToList')}
        </Button>
        <div>
          <Title order={1}>{data.title}</Title>
          <Text c="dimmed" mt="xs">
            {data.locationLabel}
          </Text>
          <Text mt="md">{data.description}</Text>
        </div>

        <UrgencyKpiStrip critical={critical} open={open} covered={covered} />

        <PublicReliefSplit
          stickySide="right"
          left={
            <SurfaceCard accent="ink" data-testid="public-organization">
              <Stack gap="sm">
                <Title order={3}>{t('publicPage.organization')}</Title>
                <Text fw={700}>{data.organizationName}</Text>
                {data.organizationDescription ? (
                  <Text>{data.organizationDescription}</Text>
                ) : null}
                {data.contacts.length > 0 ? (
                  <>
                    <Title order={4} mt="sm">
                      {t('publicPage.contacts')}
                    </Title>
                    <Stack gap="sm">
                      {data.contacts.map((contact) => (
                        <SurfaceCard key={contact.id} p="md" accent="none">
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
                        </SurfaceCard>
                      ))}
                    </Stack>
                  </>
                ) : null}
              </Stack>
            </SurfaceCard>
          }
          right={
            <Stack gap="xs">
              <LocationMap mode="readonly" lat={data.latitude} lng={data.longitude} height={320} />
              <Button
                component="a"
                href={`https://www.google.com/maps/dir/?api=1&destination=${data.latitude},${data.longitude}`}
                target="_blank"
                rel="noopener noreferrer"
                variant="light"
                fullWidth
                leftSection={<IconNavigation size={16} />}
              >
                {t('publicPage.getDirections')}
              </Button>
            </Stack>
          }
        />

        <Stack gap="xs">
          <Title order={3}>{t('request.needs')}</Title>
          <Stack gap="xs">
            {sorted.map((need) => (
              <NeedOfferRow
                key={need.id}
                need={need}
                onOffer={(n) => {
                  setSelected(n);
                  form.setFieldValue('quantity', Math.min(1, n.remaining));
                }}
              />
            ))}
          </Stack>
        </Stack>
      </Stack>

      <Modal opened={Boolean(selected)} onClose={closeOfferModal} title={t('offer.title')}>
        <form onSubmit={form.onSubmit(submitOffer)}>
          <Stack>
            <TextInput
              label={t('offer.providerName')}
              required
              {...form.getInputProps('providerName')}
            />
            <Input.Wrapper label={t('offer.providerType')} required>
              <SegmentedControl
                fullWidth
                data={[
                  { value: ProviderType.PERSON, label: t('offer.providerTypeValues.PERSON') },
                  { value: ProviderType.ORGANIZATION, label: t('offer.providerTypeValues.ORGANIZATION') },
                ]}
                {...form.getInputProps('providerType')}
              />
            </Input.Wrapper>
            <NumberInput
              label={t('offer.quantity')}
              min={0.0001}
              max={selected?.remaining}
              required
              {...form.getInputProps('quantity')}
            />
            <TextInput label={t('offer.firstName')} required {...form.getInputProps('firstName')} />
            <TextInput label={t('offer.lastName')} required {...form.getInputProps('lastName')} />
            <TextInput label={t('offer.phone')} required {...form.getInputProps('phone')} />
            <TextInput label={t('offer.email')} type="email" required {...form.getInputProps('email')} />
            <Textarea label={t('offer.note')} {...form.getInputProps('note')} />
            <Text size="xs" c="dimmed">
              {t('offer.locationHint')}
            </Text>
            {geoState === 'requesting' ? (
              <Text size="xs" c="dimmed">
                {t('offer.locationRequesting')}
              </Text>
            ) : null}
            {geoState === 'granted' ? (
              <Text size="xs" c="green">
                {t('offer.locationGranted')}
              </Text>
            ) : null}
            {geoState === 'denied' ? (
              <Stack gap={4}>
                <Text size="xs" c="red">
                  {t('offer.locationDenied')}
                </Text>
                <Button
                  size="xs"
                  variant="light"
                  w="fit-content"
                  onClick={() => void requestLocation()}
                >
                  {t('offer.retryLocation')}
                </Button>
              </Stack>
            ) : null}
            <Group justify="flex-end">
              <Button variant="default" onClick={closeOfferModal}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" color="red" disabled={geoState !== 'granted'}>
                {t('offer.submit')}
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>
    </Container>
  );
}
