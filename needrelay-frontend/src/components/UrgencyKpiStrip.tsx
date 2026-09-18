import { SimpleGrid, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { SurfaceCard } from './SurfaceCard';
import './UrgencyKpiStrip.css';

/**
 * Dashboard KPI strip for critical, open, and covered need totals.
 *
 * @param props.critical critical need count
 * @param props.open open need count
 * @param props.covered covered need count
 * @returns KPI strip
 */
export function UrgencyKpiStrip({
  critical,
  open,
  covered,
}: {
  critical: number;
  open: number;
  covered: number;
}) {
  const { t } = useTranslation();

  return (
    <SimpleGrid
      cols={{ base: 1, sm: 3 }}
      spacing="md"
      data-testid="dashboard-kpis"
      className="nr-kpi-strip"
    >
      <SurfaceCard accent="signal" p="md" className="nr-kpi-item">
        <Text size="xs" c="dimmed" tt="uppercase">
          {t('dashboard.kpiCritical')}
        </Text>
        <Text data-testid="kpi-critical" fz={28} fw={800} c="red.7">
          {critical}
        </Text>
      </SurfaceCard>
      <SurfaceCard accent="warn" p="md" className="nr-kpi-item">
        <Text size="xs" c="dimmed" tt="uppercase">
          {t('dashboard.kpiOpen')}
        </Text>
        <Text data-testid="kpi-open" fz={28} fw={800} c="yellow.8">
          {open}
        </Text>
      </SurfaceCard>
      <SurfaceCard accent="ok" p="md" className="nr-kpi-item">
        <Text size="xs" c="dimmed" tt="uppercase">
          {t('dashboard.kpiCovered')}
        </Text>
        <Text data-testid="kpi-covered" fz={28} fw={800} c="green.7">
          {covered}
        </Text>
      </SurfaceCard>
    </SimpleGrid>
  );
}
