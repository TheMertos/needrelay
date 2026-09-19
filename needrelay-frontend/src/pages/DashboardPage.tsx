import { Button, Container, Group, Stack, Tabs, Text, TextInput, Title } from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { reliefRequestsApi } from '../api';
import {
  ReliefRequestSummaryResponseStatus,
  type ReliefRequestSummaryResponse,
} from '../api/generated/models';
import { RequestUrgencyRow } from '../components/RequestUrgencyRow';
import { UrgencyKpiStrip } from '../components/UrgencyKpiStrip';
import { sortSummariesByUrgency, sumSummaryCounts } from '../lib/urgency';

/**
 * Organizer dashboard listing owned relief requests, split into active/deactivated tabs.
 *
 * @returns dashboard page
 */
export function DashboardPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<ReliefRequestSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<string>('active');
  const [search, setSearch] = useState('');

  useEffect(() => {
    void reliefRequestsApi
      .listReliefRequests()
      .then(setItems)
      .finally(() => setLoading(false));
  }, []);

  const active = useMemo(
    () => items.filter((item) => item.status === ReliefRequestSummaryResponseStatus.ACTIVE),
    [items],
  );
  const deactivated = useMemo(
    () => items.filter((item) => item.status === ReliefRequestSummaryResponseStatus.ARCHIVED),
    [items],
  );

  const totals = sumSummaryCounts(active);

  const visible = tab === 'active' ? active : deactivated;
  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    const base = query
      ? visible.filter(
          (item) =>
            item.title.toLowerCase().includes(query) ||
            item.locationLabel.toLowerCase().includes(query),
        )
      : visible;
    return sortSummariesByUrgency(base);
  }, [visible, search]);

  return (
    <Container size="lg" py="md">
      <Group justify="space-between" mb="lg">
        <Title order={2}>{t('dashboard.title')}</Title>
        <Button component={Link} to="/requests/new">
          {t('nav.createRequest')}
        </Button>
      </Group>
      {loading ? (
        <Text>{t('common.loading')}</Text>
      ) : (
        <Stack gap="lg">
          <UrgencyKpiStrip
            critical={totals.critical}
            open={totals.open}
            covered={totals.covered}
          />
          {items.length === 0 ? (
            <Stack gap="sm">
              <Text c="dimmed">{t('dashboard.empty')}</Text>
              <Button component={Link} to="/requests/new" w="fit-content">
                {t('nav.createRequest')}
              </Button>
            </Stack>
          ) : (
            <Tabs value={tab} onChange={(value) => setTab(value ?? 'active')}>
              <Tabs.List>
                <Tabs.Tab value="active" data-testid="dashboard-tab-active">
                  {t('dashboard.tabActive')} ({active.length})
                </Tabs.Tab>
                <Tabs.Tab value="deactivated" data-testid="dashboard-tab-deactivated">
                  {t('dashboard.tabDeactivated')} ({deactivated.length})
                </Tabs.Tab>
              </Tabs.List>
              <Stack gap="md" mt="md">
                <TextInput
                  placeholder={t('dashboard.searchPlaceholder')}
                  leftSection={<IconSearch size={16} />}
                  value={search}
                  onChange={(event) => setSearch(event.currentTarget.value)}
                  maw={360}
                />
                {filtered.length === 0 ? (
                  <Text c="dimmed">
                    {tab === 'active' ? t('dashboard.emptyActive') : t('dashboard.emptyDeactivated')}
                  </Text>
                ) : (
                  <Stack gap="md">
                    {filtered.map((item) => (
                      <RequestUrgencyRow key={item.id} item={item} />
                    ))}
                  </Stack>
                )}
              </Stack>
            </Tabs>
          )}
        </Stack>
      )}
    </Container>
  );
}
