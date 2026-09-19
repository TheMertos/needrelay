import {
  Alert,
  Button,
  Container,
  Group,
  Loader,
  Stack,
  Text,
  TextInput,
  Title,
  UnstyledButton,
} from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { publicApi } from '../api';
import type {
  DiscoveryNeedResponse,
  DiscoveryPointResponse,
} from '../api/generated/models';
import { NeedPriority } from '../api/generated/models';
import { NeedCategoryBadge } from '../components/NeedCategoryBadge';
import { NeedPriorityBadge } from '../components/NeedPriorityBadge';
import { NeedStatusBadge } from '../components/NeedStatusBadge';
import { PaginationBar } from '../components/PaginationBar';
import { LocationMap } from '../components/map/LocationMap';
import { SurfaceCard } from '../components/SurfaceCard';

const NEEDS_PAGE_SIZE = 20;

/**
 * Need row with the same quantity detail shown on the request detail page. Clicking it
 * navigates straight to that need's relief request public page.
 *
 * @param props.need discovery need
 * @returns clickable need card
 */
function DiscoveryNeedCard({ need }: { need: DiscoveryNeedResponse }) {
  const { t } = useTranslation();
  const remainingColor =
    need.priority === NeedPriority.CRITICAL ? 'var(--nr-signal)' : 'var(--nr-ink)';

  return (
    <UnstyledButton
      component={Link}
      to={`/r/${need.publicSlug}`}
      data-testid={`discovery-need-${need.id}`}
      style={{ textAlign: 'left', width: '100%' }}
    >
      <SurfaceCard accent="none" p="md">
        <Stack gap={4}>
          <Group justify="space-between" align="flex-start" wrap="wrap" gap="xs">
            <Text fw={700}>{need.title}</Text>
            <Group gap="xs">
              <NeedStatusBadge status={need.status} />
              <NeedPriorityBadge priority={need.priority} />
              <NeedCategoryBadge category={need.category} />
            </Group>
          </Group>
          <Text fz={22} fw={800} style={{ color: remainingColor }}>
            {need.remaining}{' '}
            <Text span fz="xs" fw={500} c="dimmed">
              {need.unit} {t('need.remaining').toLowerCase()}
            </Text>
          </Text>
          <Group gap="md">
            <Text size="xs" c="dimmed">
              {t('need.required')}: {need.quantityRequired} {need.unit}
            </Text>
            <Text size="xs" c="dimmed">
              {t('need.pending')}: {need.quantityPending} {need.unit}
            </Text>
            <Text size="xs" c="dimmed">
              {t('need.offered')}: {need.quantityOffered} {need.unit}
            </Text>
          </Group>
        </Stack>
      </SurfaceCard>
    </UnstyledButton>
  );
}

/**
 * Public help discovery: one map per ACTIVE help point + matching needs. Search and
 * pagination of the points list are both server-side, with a single pagination control
 * at the bottom of the page — individual operations don't get their own pagination.
 *
 * @returns discovery page
 */
export function DiscoveryPage() {
  const { t } = useTranslation();
  const [points, setPoints] = useState<DiscoveryPointResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [pointNeeds, setPointNeeds] = useState<Record<string, DiscoveryNeedResponse[]>>({});

  /**
   * Loads the current page of the public discovery feed for the active query.
   *
   * @returns void
   */
  async function loadDiscovery() {
    setLoading(true);
    setError(false);
    try {
      const data = await publicApi.getPublicDiscovery({
        q: query.trim() || undefined,
        page,
        size: pageSize,
      });
      setPoints(data.items);
      setTotalPages(Math.max(data.totalPages, 1));
      setTotalElements(data.totalElements);
      setPointNeeds({});
    } catch {
      setError(true);
      setPoints([]);
      setTotalElements(0);
      setPointNeeds({});
    } finally {
      setLoading(false);
    }
  }

  /**
   * Loads the first page (max 20) of a point's discoverable needs.
   *
   * @param pointId help point id
   * @returns void
   */
  async function loadPointNeeds(pointId: string) {
    try {
      const result = await publicApi.getDiscoveryPointNeeds(pointId, {
        page: 0,
        size: NEEDS_PAGE_SIZE,
      });
      setPointNeeds((prev) => ({ ...prev, [pointId]: result.items }));
    } catch {
      setPointNeeds((prev) => ({ ...prev, [pointId]: [] }));
    }
  }

  useEffect(() => {
    void loadDiscovery();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, page, pageSize]);

  useEffect(() => {
    setPage(0);
  }, [query, pageSize]);

  useEffect(() => {
    points.forEach((point) => void loadPointNeeds(point.id));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [points]);

  return (
    <Container size="lg" py="md">
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

        {!loading && !error && totalElements === 0 && !query.trim() ? (
          <Text c="dimmed">{t('discovery.emptyPoints')}</Text>
        ) : null}

        {!loading && !error && (totalElements > 0 || query.trim()) ? (
          <>
            <TextInput
              data-testid="discovery-search"
              placeholder={t('discovery.searchPlaceholder')}
              value={query}
              onChange={(event) => setQuery(event.currentTarget.value)}
            />

            {points.length === 0 ? (
              <Text c="dimmed">{t('discovery.emptyNeeds')}</Text>
            ) : (
              <Stack gap="lg">
                {points.map((point) => {
                  const list = pointNeeds[point.id] ?? [];
                  const needsLoaded = point.id in pointNeeds;

                  return (
                    <SurfaceCard
                      key={point.id}
                      accent="ink"
                      data-testid={`discovery-point-${point.id}`}
                    >
                      <Stack gap="md">
                        <Group justify="space-between" align="flex-start" wrap="wrap" gap="sm">
                          <div>
                            <Text fw={800} fz="lg">
                              {point.title}
                            </Text>
                            <Text size="sm" c="dimmed">
                              {point.locationLabel}
                            </Text>
                          </div>
                          <Button
                            data-testid={`discovery-open-details-${point.id}`}
                            component={Link}
                            to={`/r/${point.publicSlug}`}
                            variant="light"
                          >
                            {t('discovery.openDetails')}
                          </Button>
                        </Group>
                        <div data-testid={`discovery-map-${point.id}`}>
                          <LocationMap
                            mode="readonly"
                            lat={point.latitude}
                            lng={point.longitude}
                            height={240}
                          />
                        </div>
                        <Stack gap="sm">
                          {!needsLoaded ? (
                            <Group justify="center" py="sm">
                              <Loader size="sm" />
                            </Group>
                          ) : list.length === 0 ? (
                            <Text c="dimmed" size="sm">
                              {t('discovery.emptyNeeds')}
                            </Text>
                          ) : (
                            list.map((need) => (
                              <DiscoveryNeedCard key={need.id} need={need} />
                            ))
                          )}
                        </Stack>
                      </Stack>
                    </SurfaceCard>
                  );
                })}
              </Stack>
            )}

            <PaginationBar
              testIdPrefix="discovery-points"
              page={page}
              totalPages={totalPages}
              pageSize={pageSize}
              onPageChange={setPage}
              onPageSizeChange={setPageSize}
            />
          </>
        ) : null}
      </Stack>
    </Container>
  );
}
