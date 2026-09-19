import {
  Badge,
  Button,
  Group,
  Loader,
  Modal,
  NumberInput,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { reliefRequestsApi } from '../api';
import type { ListOffersSort, NeedResponse, OfferResponse } from '../api/generated/models';
import { NeedPriority, NeedStatus, OfferStatus, ProviderType } from '../api/generated/models';
import { normalizeOfferPage } from '../lib/normalizeOfferPage';
import type { OfferFilters } from '../pages/ManageRequestPage';
import { NeedStatusBadge } from './NeedStatusBadge';
import { PaginationBar } from './PaginationBar';
import { SurfaceCard } from './SurfaceCard';
import './OfferInbox.css';

const SORTABLE_COLUMNS = [
  'providerName',
  'providerType',
  'email',
  'quantity',
  'distanceKm',
  'status',
] as const;
type SortableColumn = (typeof SORTABLE_COLUMNS)[number];
const DEFAULT_SORT = 'createdAt,desc';
const DEFAULT_PAGE_SIZE = 20;

interface NeedOfferState {
  items: OfferResponse[];
  page: number;
  pageSize: number;
  sort: string;
  filters: OfferFilters;
  totalPages: number;
  totalElements: number;
  loading: boolean;
}

/**
 * Fresh per-need offer state: page 0, default sort, no filters.
 *
 * @returns default state
 */
function defaultNeedState(): NeedOfferState {
  return {
    items: [],
    page: 0,
    pageSize: DEFAULT_PAGE_SIZE,
    sort: DEFAULT_SORT,
    filters: {},
    totalPages: 1,
    totalElements: 0,
    loading: true,
  };
}

/**
 * Needs with nested public offers; each need's table fetches, filters, sorts, and
 * paginates independently against the backend via the `needId` offer filter.
 *
 * @param props.needs needs for this request (display order)
 * @param props.requestId relief request id, scopes every offer fetch
 * @param props.refreshToken bump to refetch every visible need's offers in place
 * @param props.onEditNeed open need edit
 * @param props.onCloseNeed request close-need confirm
 * @param props.onEdit open offer edit
 * @param props.onOpenStatus open the status-change modal for PENDING/COMING offers
 * @param props.onAddNeed open add-need modal
 * @returns grouped inbox
 */
export function OfferInbox({
  needs,
  requestId,
  refreshToken,
  onEditNeed,
  onCloseNeed,
  onEdit,
  onOpenStatus,
  onAddNeed,
}: {
  needs: NeedResponse[];
  requestId: string;
  refreshToken: number;
  onEditNeed: (need: NeedResponse) => void;
  onCloseNeed: (needId: string) => void;
  onEdit: (offer: OfferResponse) => void;
  onOpenStatus: (offer: OfferResponse) => void;
  onAddNeed: () => void;
}) {
  const { t } = useTranslation();
  const needList = needs ?? [];

  const [needState, setNeedState] = useState<Record<string, NeedOfferState>>({});
  const [filterModalNeedId, setFilterModalNeedId] = useState<string | null>(null);
  const [draft, setDraft] = useState<OfferFilters>({});

  /**
   * Fetches one need's offers with the given page/size/sort/filters and stores the
   * result (or an empty page on error) under that need's state entry.
   *
   * @param needId need to fetch offers for
   * @param params page/size/sort/filters to apply
   * @returns void
   */
  async function fetchNeedOffers(
    needId: string,
    params: Pick<NeedOfferState, 'page' | 'pageSize' | 'sort' | 'filters'>,
  ) {
    setNeedState((prev) => ({
      ...prev,
      [needId]: { ...(prev[needId] ?? defaultNeedState()), ...params, loading: true },
    }));
    try {
      const result = normalizeOfferPage(
        await reliefRequestsApi.listOffers(requestId, {
          needId,
          page: params.page,
          size: params.pageSize,
          sort: params.sort as ListOffersSort,
          status: params.filters.status,
          providerType: params.filters.providerType,
          q: params.filters.q,
          minQuantity: params.filters.minQuantity,
          maxQuantity: params.filters.maxQuantity,
          minDistanceKm: params.filters.minDistanceKm,
          maxDistanceKm: params.filters.maxDistanceKm,
        }),
        params.page,
        params.pageSize,
      );
      setNeedState((prev) => ({
        ...prev,
        [needId]: {
          ...(prev[needId] ?? defaultNeedState()),
          ...params,
          items: result.items,
          totalPages: result.totalPages,
          totalElements: result.totalElements,
          loading: false,
        },
      }));
    } catch {
      setNeedState((prev) => ({
        ...prev,
        [needId]: {
          ...(prev[needId] ?? defaultNeedState()),
          ...params,
          items: [],
          totalPages: 1,
          totalElements: 0,
          loading: false,
        },
      }));
    }
  }

  useEffect(() => {
    needList.forEach((need) => {
      const existing = needState[need.id];
      void fetchNeedOffers(need.id, {
        page: existing?.page ?? 0,
        pageSize: existing?.pageSize ?? DEFAULT_PAGE_SIZE,
        sort: existing?.sort ?? DEFAULT_SORT,
        filters: existing?.filters ?? {},
      });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [needList.map((need) => need.id).join(','), refreshToken]);

  /**
   * Opens the filter modal for one need, seeded with that need's current filters.
   *
   * @param needId need to filter
   * @returns void
   */
  function openFiltersModal(needId: string) {
    setDraft(needState[needId]?.filters ?? {});
    setFilterModalNeedId(needId);
  }

  /**
   * Commits the draft filters to the open need (refetching from page 0) and closes
   * the modal.
   *
   * @returns void
   */
  function applyDraftFilters() {
    if (filterModalNeedId) {
      const current = needState[filterModalNeedId] ?? defaultNeedState();
      void fetchNeedOffers(filterModalNeedId, {
        page: 0,
        pageSize: current.pageSize,
        sort: current.sort,
        filters: draft,
      });
    }
    setFilterModalNeedId(null);
  }

  /**
   * Clears the open need's filters (refetching from page 0) and closes the modal.
   *
   * @returns void
   */
  function clearFilters() {
    if (filterModalNeedId) {
      const current = needState[filterModalNeedId] ?? defaultNeedState();
      void fetchNeedOffers(filterModalNeedId, {
        page: 0,
        pageSize: current.pageSize,
        sort: current.sort,
        filters: {},
      });
    }
    setDraft({});
    setFilterModalNeedId(null);
  }

  /**
   * Toggles one need's sort for a column: unsorted → asc → desc → back to default,
   * refetching from page 0.
   *
   * @param needId need whose table is being sorted
   * @param column sortable field
   * @returns void
   */
  function toggleSort(needId: string, column: SortableColumn) {
    const current = needState[needId] ?? defaultNeedState();
    const [activeProperty, direction] = current.sort.split(',');
    let next: string;
    if (activeProperty !== column) {
      next = `${column},asc`;
    } else if (direction === 'asc') {
      next = `${column},desc`;
    } else {
      next = DEFAULT_SORT;
    }
    void fetchNeedOffers(needId, {
      page: 0,
      pageSize: current.pageSize,
      sort: next,
      filters: current.filters,
    });
  }

  /**
   * Changes one need's current page.
   *
   * @param needId need whose table is being paged
   * @param nextPage 0-based page index
   * @returns void
   */
  function changeNeedPage(needId: string, nextPage: number) {
    const current = needState[needId] ?? defaultNeedState();
    void fetchNeedOffers(needId, {
      page: nextPage,
      pageSize: current.pageSize,
      sort: current.sort,
      filters: current.filters,
    });
  }

  /**
   * Changes one need's page size, refetching from page 0.
   *
   * @param needId need whose table's page size is changing
   * @param size new page size
   * @returns void
   */
  function changeNeedPageSize(needId: string, size: number) {
    const current = needState[needId] ?? defaultNeedState();
    void fetchNeedOffers(needId, { page: 0, pageSize: size, sort: current.sort, filters: current.filters });
  }

  /**
   * Renders a sort indicator arrow for the active column of one need's table.
   *
   * @param needId need whose table is being sorted
   * @param column sortable field
   * @returns arrow suffix or empty string
   */
  function sortIndicator(needId: string, column: SortableColumn): string {
    const sort = needState[needId]?.sort;
    if (!sort) {
      return '';
    }
    const [activeProperty, direction] = sort.split(',');
    if (activeProperty !== column) {
      return '';
    }
    return direction === 'asc' ? ' ▲' : ' ▼';
  }

  /**
   * Clickable, sortable column header scoped to one need's table.
   *
   * @param needId need whose table is being sorted
   * @param column sortable field
   * @param label header text
   * @returns header cell
   */
  function sortableHeader(needId: string, column: SortableColumn, label: string) {
    return (
      <Table.Th
        onClick={() => toggleSort(needId, column)}
        style={{ cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap' }}
      >
        {label}
        {sortIndicator(needId, column)}
      </Table.Th>
    );
  }

  /**
   * Renders action buttons for one offer.
   *
   * @param offer offer row
   * @returns action controls
   */
  function offerActions(offer: OfferResponse) {
    return (
      <>
        {offer.status === OfferStatus.PENDING || offer.status === OfferStatus.COMING ? (
          <Button size="compact-xs" onClick={() => onOpenStatus(offer)}>
            {t('offer.changeStatus')}
          </Button>
        ) : null}
        {offer.status !== OfferStatus.RECEIVED ? (
          <Button size="compact-xs" variant="light" onClick={() => onEdit(offer)}>
            {t('request.editOffer')}
          </Button>
        ) : null}
      </>
    );
  }

  /**
   * Renders one offer table row under a need.
   *
   * @param offer offer
   * @returns table row
   */
  function offerRow(offer: OfferResponse) {
    return (
      <Table.Tr
        key={offer.id}
        className="nr-offer-row"
        data-testid={`offer-under-need-${offer.needId}-${offer.id}`}
      >
        <Table.Td>
          <Text size="sm" fw={700}>
            {offer.providerName}
          </Text>
          <Text size="xs" c="dimmed">
            {offer.firstName} {offer.lastName}
          </Text>
          {offer.note ? (
            <Text size="xs" c="dimmed" lineClamp={1}>
              {offer.note}
            </Text>
          ) : null}
        </Table.Td>
        <Table.Td miw={110} style={{ whiteSpace: 'nowrap' }}>
          <Badge size="sm" variant="outline" w="auto" style={{ maxWidth: 'none', width: 'auto' }}>
            {t(`offer.providerTypeValues.${offer.providerType}`)}
          </Badge>
        </Table.Td>
        <Table.Td>
          <Text size="xs">
            <bdi dir="ltr">
              {offer.phone} · {offer.email}
            </bdi>
          </Text>
        </Table.Td>
        <Table.Td>
          <Text size="sm">× {offer.quantity}</Text>
          {offer.status === OfferStatus.RECEIVED && offer.quantityReceived != null ? (
            <Text size="xs" c="dimmed">
              {t('offer.received')}: × {offer.quantityReceived}
            </Text>
          ) : null}
        </Table.Td>
        <Table.Td data-testid={`offer-distance-${offer.id}`}>
          <Text size="sm">
            {offer.distanceKm != null ? t('offer.distanceKm', { km: offer.distanceKm }) : '–'}
          </Text>
        </Table.Td>
        <Table.Td miw={120} style={{ whiteSpace: 'nowrap' }}>
          <Badge size="sm" variant="light" w="auto" style={{ maxWidth: 'none', width: 'auto' }}>
            {t(`offer.status.${offer.status}`)}
          </Badge>
        </Table.Td>
        <Table.Td>
          <Group gap={4} wrap="wrap">
            {offerActions(offer)}
          </Group>
        </Table.Td>
      </Table.Tr>
    );
  }

  return (
    <SurfaceCard data-testid="offer-inbox" accent="signal">
      <Group justify="space-between" mb="xs" wrap="wrap" gap="sm">
        <Title order={4}>{t('request.needsWithOffers')}</Title>
        <Button onClick={onAddNeed}>{t('request.addNeed')}</Button>
      </Group>
      <Text size="sm" c="dimmed" mb="md">
        {t('request.offersHint')}
      </Text>

      <Modal
        opened={filterModalNeedId !== null}
        onClose={() => setFilterModalNeedId(null)}
        title={t('common.filters')}
      >
        <Stack gap="sm" data-testid="offer-filters-modal">
          <TextInput
            data-testid="offer-filter-q"
            label={t('common.search')}
            placeholder={t('offer.searchPlaceholder')}
            value={draft.q ?? ''}
            onChange={(event) => setDraft({ ...draft, q: event.currentTarget.value || undefined })}
          />
          <Select
            data-testid="offer-filter-status"
            label={t('offer.statusLabel')}
            value={draft.status ?? null}
            onChange={(value) => setDraft({ ...draft, status: (value as OfferStatus) ?? undefined })}
            clearable
            placeholder={t('common.all')}
            data={Object.values(OfferStatus).map((value) => ({
              value,
              label: t(`offer.status.${value}`),
            }))}
          />
          <Select
            data-testid="offer-filter-type"
            label={t('offer.providerType')}
            value={draft.providerType ?? null}
            onChange={(value) =>
              setDraft({ ...draft, providerType: (value as ProviderType) ?? undefined })
            }
            clearable
            placeholder={t('common.all')}
            data={Object.values(ProviderType).map((value) => ({
              value,
              label: t(`offer.providerTypeValues.${value}`),
            }))}
          />
          <Group grow>
            <NumberInput
              data-testid="offer-filter-min-quantity"
              label={`${t('offer.quantity')} ${t('common.min')}`}
              value={draft.minQuantity ?? ''}
              onChange={(value) =>
                setDraft({ ...draft, minQuantity: value === '' ? undefined : Number(value) })
              }
              min={0}
            />
            <NumberInput
              data-testid="offer-filter-max-quantity"
              label={`${t('offer.quantity')} ${t('common.max')}`}
              value={draft.maxQuantity ?? ''}
              onChange={(value) =>
                setDraft({ ...draft, maxQuantity: value === '' ? undefined : Number(value) })
              }
              min={0}
            />
          </Group>
          <Group grow>
            <NumberInput
              data-testid="offer-filter-min-distance"
              label={`${t('offer.distance')} ${t('common.min')}`}
              value={draft.minDistanceKm ?? ''}
              onChange={(value) =>
                setDraft({ ...draft, minDistanceKm: value === '' ? undefined : Number(value) })
              }
              min={0}
            />
            <NumberInput
              data-testid="offer-filter-max-distance"
              label={`${t('offer.distance')} ${t('common.max')}`}
              value={draft.maxDistanceKm ?? ''}
              onChange={(value) =>
                setDraft({ ...draft, maxDistanceKm: value === '' ? undefined : Number(value) })
              }
              min={0}
            />
          </Group>
          <Group justify="space-between" mt="sm">
            <Button
              data-testid="offer-filter-clear"
              variant="subtle"
              color="gray"
              onClick={clearFilters}
            >
              {t('common.clearFilters')}
            </Button>
            <Button data-testid="offer-filter-apply" onClick={applyDraftFilters}>
              {t('common.confirm')}
            </Button>
          </Group>
        </Stack>
      </Modal>

      {needList.length === 0 ? (
        <Text size="sm" c="dimmed">
          {t('discovery.emptyNeeds')}
        </Text>
      ) : (
        <Stack gap="sm" data-testid="needs-offer-groups">
          {needList.map((need) => {
            const state = needState[need.id] ?? defaultNeedState();
            const activeFilterCount = Object.values(state.filters).filter(
              (v) => v != null && v !== '',
            ).length;

            return (
              <SurfaceCard key={need.id} p="sm" accent="ink" data-testid={`need-group-${need.id}`}>
                <Stack gap="xs">
                  <Group justify="space-between" align="flex-start" wrap="wrap" gap="sm">
                    <Group gap="xs" align="baseline">
                      <Text fw={700}>{need.title}</Text>
                      <NeedStatusBadge status={need.status} />
                      <Text
                        size="sm"
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
                      <Text size="xs" c="dimmed">
                        {t(`need.priorityValues.${need.priority}`)}
                      </Text>
                    </Group>
                    {need.status !== NeedStatus.CLOSED ? (
                      <Group gap={4}>
                        <Button size="compact-xs" variant="light" onClick={() => onEditNeed(need)}>
                          {t('request.editNeed')}
                        </Button>
                        <Button
                          size="compact-xs"
                          variant="outline"
                          color="gray"
                          onClick={() => onCloseNeed(need.id)}
                        >
                          {t('request.closeNeed')}
                        </Button>
                      </Group>
                    ) : null}
                  </Group>

                  <Group>
                    <Button
                      data-testid={`offer-filters-open-${need.id}`}
                      variant="light"
                      size="compact-sm"
                      onClick={() => openFiltersModal(need.id)}
                    >
                      {t('common.filters')}
                      {activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}
                    </Button>
                  </Group>

                  {state.loading ? (
                    <Group justify="center" py="sm">
                      <Loader size="sm" />
                    </Group>
                  ) : state.items.length === 0 ? (
                    <Text size="sm" c="dimmed">
                      {t('request.offersEmpty')}
                    </Text>
                  ) : (
                    <>
                      <Table.ScrollContainer minWidth={720}>
                        <Table
                          verticalSpacing={4}
                          horizontalSpacing="sm"
                          withTableBorder
                          withColumnBorders
                          striped
                          highlightOnHover
                          data-testid={`offers-table-${need.id}`}
                        >
                          <Table.Thead>
                            <Table.Tr>
                              {sortableHeader(need.id, 'providerName', t('offer.providerName'))}
                              {sortableHeader(need.id, 'providerType', t('offer.providerType'))}
                              {sortableHeader(need.id, 'email', t('offer.contact'))}
                              {sortableHeader(need.id, 'quantity', t('offer.quantity'))}
                              {sortableHeader(need.id, 'distanceKm', t('offer.distance'))}
                              {sortableHeader(need.id, 'status', t('offer.statusLabel'))}
                              <Table.Th>{t('offer.actions')}</Table.Th>
                            </Table.Tr>
                          </Table.Thead>
                          <Table.Tbody>{state.items.map((offer) => offerRow(offer))}</Table.Tbody>
                        </Table>
                      </Table.ScrollContainer>
                      <PaginationBar
                        testIdPrefix={`offer-page-${need.id}`}
                        page={state.page}
                        totalPages={state.totalPages}
                        pageSize={state.pageSize}
                        onPageChange={(nextPage) => changeNeedPage(need.id, nextPage)}
                        onPageSizeChange={(size) => changeNeedPageSize(need.id, size)}
                      />
                    </>
                  )}
                </Stack>
              </SurfaceCard>
            );
          })}
        </Stack>
      )}
    </SurfaceCard>
  );
}
