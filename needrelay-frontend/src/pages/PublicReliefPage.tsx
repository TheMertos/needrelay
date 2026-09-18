import {
  Button,
  Container,
  Group,
  Modal,
  NumberInput,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { publicApi } from '../api';
import type { NeedResponse, PublicReliefRequestResponse } from '../api/generated/models';
import { NeedOfferRow } from '../components/NeedOfferRow';
import { PublicReliefSplit } from '../components/PublicReliefSplit';
import { SurfaceCard } from '../components/SurfaceCard';
import { LocationMap } from '../components/map/LocationMap';
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
  const form = useForm({
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
   * Submits an anonymous offer for the selected need.
   *
   * @param values offer form values
   */
  async function submitOffer(values: typeof form.values) {
    if (!selected) {
      return;
    }
    try {
      await publicApi.createPublicOffer(selected.id, {
        providerName: values.providerName,
        quantity: values.quantity,
        firstName: values.firstName,
        lastName: values.lastName,
        phone: values.phone,
        email: values.email,
        note: values.note || null,
      });
      notifications.show({ color: 'green', message: t('offer.success') });
      setSelected(null);
      form.reset();
      await load();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  if (!data) {
    return (
      <Container py="xl">
        <Text>{t('common.loading')}</Text>
      </Container>
    );
  }

  const sorted = sortNeedsByUrgency(data.needs);

  return (
    <Container size="lg" py="md">
      <Stack gap="lg">
        <div>
          <Title order={1}>{data.title}</Title>
          <Text c="dimmed" mt="xs">
            {data.locationLabel}
          </Text>
          <Text mt="md">{data.description}</Text>
        </div>

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
                        {contact.phone} · {contact.email}
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

        <PublicReliefSplit
          map={
            <LocationMap mode="readonly" lat={data.latitude} lng={data.longitude} height={320} />
          }
          list={
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
          }
        />
      </Stack>

      <Modal
        opened={Boolean(selected)}
        onClose={() => setSelected(null)}
        title={t('offer.title')}
      >
        <form onSubmit={form.onSubmit(submitOffer)}>
          <Stack>
            <TextInput
              label={t('offer.providerName')}
              required
              {...form.getInputProps('providerName')}
            />
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
            <Group justify="flex-end">
              <Button variant="default" onClick={() => setSelected(null)}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" color="red">
                {t('offer.submit')}
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>
    </Container>
  );
}
