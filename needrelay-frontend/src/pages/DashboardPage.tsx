import { Button, Container, Group, Stack, Text, Title } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { reliefRequestsApi } from '../api';
import type { ReliefRequestSummaryResponse } from '../api/generated/models';
import { RequestUrgencyRow } from '../components/RequestUrgencyRow';
import { UrgencyKpiStrip } from '../components/UrgencyKpiStrip';
import { sortSummariesByUrgency, sumSummaryCounts } from '../lib/urgency';

/**
 * Organizer dashboard listing owned relief requests.
 *
 * @returns dashboard page
 */
export function DashboardPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<ReliefRequestSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void reliefRequestsApi
      .listReliefRequests()
      .then(setItems)
      .finally(() => setLoading(false));
  }, []);

  const sorted = sortSummariesByUrgency(items);
  const totals = sumSummaryCounts(items);

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
          {sorted.length === 0 ? (
            <Stack gap="sm">
              <Text c="dimmed">{t('dashboard.empty')}</Text>
              <Button component={Link} to="/requests/new" w="fit-content">
                {t('nav.createRequest')}
              </Button>
            </Stack>
          ) : (
            <Stack gap="md">
              {sorted.map((item) => (
                <RequestUrgencyRow key={item.id} item={item} />
              ))}
            </Stack>
          )}
        </Stack>
      )}
    </Container>
  );
}
