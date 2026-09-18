import { Badge, Button, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import type { ReliefRequestSummaryResponse } from '../api/generated/models';
import { SurfaceCard } from './SurfaceCard';

/**
 * Dashboard row for a relief request with urgency badges.
 *
 * @param props.item summary item
 * @returns request row
 */
export function RequestUrgencyRow({ item }: { item: ReliefRequestSummaryResponse }) {
  const { t } = useTranslation();
  const accent =
    item.criticalNeeds > 0 ? 'signal' : item.openNeeds > 0 ? 'warn' : 'ink';

  return (
    <SurfaceCard data-testid={`request-row-${item.id}`} accent={accent}>
      <Stack gap="sm">
        <div>
          <Title order={4}>{item.title}</Title>
          <Text size="sm" c="dimmed">
            {item.locationLabel}
          </Text>
        </div>
        <Group gap="xs">
          <Badge color="red" variant="light">
            {t('dashboard.criticalNeeds')}: {item.criticalNeeds}
          </Badge>
          <Badge color="yellow" variant="light">
            {t('dashboard.openNeeds')}: {item.openNeeds}
          </Badge>
          <Badge color="green" variant="light">
            {t('dashboard.coveredNeeds')}: {item.coveredNeeds}
          </Badge>
        </Group>
        <Group>
          <Button component={Link} to={`/requests/${item.id}`} size="sm" color="ink">
            {t('dashboard.manage')}
          </Button>
          <Button component={Link} to={`/r/${item.publicSlug}`} variant="light" size="sm">
            {t('dashboard.publicLink')}
          </Button>
        </Group>
      </Stack>
    </SurfaceCard>
  );
}
