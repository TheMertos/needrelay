import {
  Button,
  Container,
  NumberInput,
  Select,
  Stack,
  Switch,
  TextInput,
  Textarea,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { needsApi, reliefRequestsApi } from '../api';
import { NeedCategory, NeedPriority } from '../api/generated/models';
import { CreateRequestSplit } from '../components/CreateRequestSplit';
import { SurfaceCard } from '../components/SurfaceCard';
import { LocationMap } from '../components/map/LocationMap';

/**
 * Creates a new relief request with map + form split and optional first need.
 *
 * @returns create form page
 */
export function NewRequestPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const form = useForm({
    initialValues: {
      title: '',
      description: '',
      locationLabel: '',
      latitude: 36.2023,
      longitude: 36.1613,
      firstNeedEnabled: false,
      firstNeedTitle: '',
      firstNeedQuantity: 1,
      firstNeedUnit: 'units',
      firstNeedPriority: NeedPriority.NORMAL,
      firstNeedCategory: NeedCategory.SUPPLIES,
    },
  });

  /**
   * Persists a new relief request and optional first need.
   *
   * @param values form values
   */
  async function handleSubmit(values: typeof form.values) {
    try {
      const created = await reliefRequestsApi.createReliefRequest({
        title: values.title,
        description: values.description,
        locationLabel: values.locationLabel,
        latitude: values.latitude,
        longitude: values.longitude,
      });
      if (values.firstNeedEnabled && values.firstNeedTitle.trim()) {
        await needsApi.createNeed(created.id, {
          title: values.firstNeedTitle.trim(),
          description: null,
          category: values.firstNeedCategory,
          quantityRequired: values.firstNeedQuantity,
          unit: values.firstNeedUnit,
          priority: values.firstNeedPriority,
        });
      }
      navigate(`/requests/${created.id}`);
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  return (
    <Container size="lg" py="md">
      <Title order={2} mb="md">
        {t('nav.createRequest')}
      </Title>
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <CreateRequestSplit
          map={
            <SurfaceCard accent="ink">
              <Stack>
                <LocationMap
                  mode="picker"
                  lat={form.values.latitude}
                  lng={form.values.longitude}
                  onChange={(lat, lng) => {
                    form.setFieldValue('latitude', lat);
                    form.setFieldValue('longitude', lng);
                  }}
                />
                <NumberInput
                  label="Latitude"
                  decimalScale={6}
                  {...form.getInputProps('latitude')}
                />
                <NumberInput
                  label="Longitude"
                  decimalScale={6}
                  {...form.getInputProps('longitude')}
                />
              </Stack>
            </SurfaceCard>
          }
          form={
            <SurfaceCard accent="signal">
              <Stack>
                <TextInput label={t('request.title')} required {...form.getInputProps('title')} />
                <Textarea
                  label={t('request.description')}
                  required
                  minRows={3}
                  {...form.getInputProps('description')}
                />
                <TextInput
                  label={t('request.locationLabel')}
                  required
                  {...form.getInputProps('locationLabel')}
                />
                <Switch
                  label={t('request.addFirstNeed')}
                  {...form.getInputProps('firstNeedEnabled', { type: 'checkbox' })}
                />
                {form.values.firstNeedEnabled ? (
                  <Stack gap="sm">
                    <TextInput
                      label={t('request.firstNeedTitle')}
                      {...form.getInputProps('firstNeedTitle')}
                    />
                    <Select
                      label={t('need.category')}
                      data={Object.values(NeedCategory)}
                      {...form.getInputProps('firstNeedCategory')}
                    />
                    <Select
                      label={t('need.priority')}
                      data={Object.values(NeedPriority)}
                      {...form.getInputProps('firstNeedPriority')}
                    />
                    <NumberInput
                      label={t('need.required')}
                      min={0.0001}
                      {...form.getInputProps('firstNeedQuantity')}
                    />
                    <TextInput label={t('need.unit')} {...form.getInputProps('firstNeedUnit')} />
                  </Stack>
                ) : null}
                <Button type="submit" color="ink">
                  {t('request.save')}
                </Button>
              </Stack>
            </SurfaceCard>
          }
        />
      </form>
    </Container>
  );
}
