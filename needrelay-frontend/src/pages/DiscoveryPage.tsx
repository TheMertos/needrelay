import {
  Alert,
  Button,
  Drawer,
  Group,
  Loader,
  Stack,
  Text,
  TextInput,
  Title,
  UnstyledButton,
} from '@mantine/core';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { publicApi } from '../api';
import type {
  DiscoveryNeedResponse,
  DiscoveryPointResponse,
} from '../api/generated/models';
import { LocationMap } from '../components/map/LocationMap';
import { SurfaceCard } from '../components/SurfaceCard';
import { filterDiscoveryNeeds } from '../lib/discoveryFilter';
import { priorityRank } from '../lib/urgency';

/**
 * Public help discovery: one map per ACTIVE help point + matching needs.
 *
 * @returns discovery page
 */
export function DiscoveryPage() {
  const { t } = useTranslation();
  const [points, setPoints] = useState<DiscoveryPointResponse[]>([]);
  const [needs, setNeeds] = useState<DiscoveryNeedResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [query, setQuery] = useState('');
  const [drawerNeed, setDrawerNeed] = useState<DiscoveryNeedResponse | null>(null);

  /**
   * Loads the public discovery feed.
   *
   * @returns void
   */
  async function loadDiscovery() {
    setLoading(true);
    setError(false);
    try {
      const data = await publicApi.getPublicDiscovery();
      setPoints(data.points);
      setNeeds(data.needs);
    } catch {
      setError(true);
      setPoints([]);
      setNeeds([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDiscovery();
  }, []);

  const pointsWithNeeds = useMemo(() => {
    return points
      .map((point) => {
        const pointNeeds = filterDiscoveryNeeds(needs, {
          requestId: point.id,
          query,
        }).sort((a, b) => priorityRank(a.priority) - priorityRank(b.priority));
        return { point, needs: pointNeeds };
      })
      .filter((entry) => (query.trim() ? entry.needs.length > 0 : true));
  }, [points, needs, query]);

  return (
    <Stack gap="md" data-testid="discovery-page">
      <Title order={2} c="var(--nr-ink)">
        {t('discovery.title')}
      </Title>

      {error ? (
        <Alert color="red" title={t('discovery.loadError')}>
          <Button data-testid="discovery-retry" onClick={() => void loadDiscovery()}>
            {t('discovery.retry')}
          </Button>
        </Alert>
      ) : null}

      {loading ? (
        <Group justify="center" py="xl">
          <Loader />
        </Group>
      ) : null}

      {!loading && !error && points.length === 0 ? (
        <Text c="dimmed">{t('discovery.emptyPoints')}</Text>
      ) : null}

      {!loading && !error && points.length > 0 ? (
        <>
          <TextInput
            data-testid="discovery-search"
            placeholder={t('discovery.searchPlaceholder')}
            value={query}
            onChange={(event) => setQuery(event.currentTarget.value)}
          />

          {pointsWithNeeds.length === 0 ? (
            <Text c="dimmed">{t('discovery.emptyNeeds')}</Text>
          ) : (
            <Stack gap="lg">
              {pointsWithNeeds.map(({ point, needs: pointNeeds }) => (
                <SurfaceCard
                  key={point.id}
                  accent="ink"
                  data-testid={`discovery-point-${point.id}`}
                >
                  <Stack gap="md">
                    <div>
                      <Text fw={800} fz="lg">
                        {point.title}
                      </Text>
                      <Text size="sm" c="dimmed">
                        {point.locationLabel}
                      </Text>
                    </div>
                    <div data-testid={`discovery-map-${point.id}`}>
                      <LocationMap
                        mode="readonly"
                        lat={point.latitude}
                        lng={point.longitude}
                        height={240}
                      />
                    </div>
                    <Stack gap="sm">
                      {pointNeeds.length === 0 ? (
                        <Text c="dimmed" size="sm">
                          {t('discovery.emptyNeeds')}
                        </Text>
                      ) : (
                        pointNeeds.map((need) => (
                          <UnstyledButton
                            key={need.id}
                            data-testid={`discovery-need-${need.id}`}
                            onClick={() => setDrawerNeed(need)}
                            style={{ textAlign: 'left', width: '100%' }}
                          >
                            <SurfaceCard accent="none" p="md">
                              <Text fw={700}>{need.title}</Text>
                              <Text size="sm" c="dimmed">
                                {need.priority}
                              </Text>
                            </SurfaceCard>
                          </UnstyledButton>
                        ))
                      )}
                    </Stack>
                  </Stack>
                </SurfaceCard>
              ))}
            </Stack>
          )}
        </>
      ) : null}

      <Drawer
        opened={drawerNeed !== null}
        onClose={() => setDrawerNeed(null)}
        title={drawerNeed?.title}
        position="right"
      >
        {drawerNeed ? (
          <Stack gap="sm" data-testid="discovery-drawer">
            <Text size="sm">
              <Text span fw={600}>
                {t('discovery.priority')}:{' '}
              </Text>
              {drawerNeed.priority}
            </Text>
            <Text size="sm">
              <Text span fw={600}>
                {t('discovery.location')}:{' '}
              </Text>
              {drawerNeed.locationLabel}
            </Text>
            <Text size="sm">
              <Text span fw={600}>
                {t('discovery.org')}:{' '}
              </Text>
              {drawerNeed.organizationName}
            </Text>
            <Button
              data-testid="discovery-open-request"
              component={Link}
              to={`/r/${drawerNeed.publicSlug}`}
            >
              {t('discovery.openRequest')}
            </Button>
          </Stack>
        ) : null}
      </Drawer>
    </Stack>
  );
}
