import { Button, Group, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { NeedResponse } from '../api/generated/models';
import { NeedPriority, NeedStatus } from '../api/generated/models';
import { NeedCategoryBadge } from './NeedCategoryBadge';
import { NeedPriorityBadge } from './NeedPriorityBadge';
import { NeedStatusBadge } from './NeedStatusBadge';
import { SurfaceCard } from './SurfaceCard';
import './NeedOfferRow.css';

/**
 * Public need row with emphasized remaining quantity and offer CTA.
 *
 * @param props.need need to display
 * @param props.onOffer callback when user offers help
 * @returns need row
 */
export function NeedOfferRow({
  need,
  onOffer,
}: {
  need: NeedResponse;
  onOffer: (need: NeedResponse) => void;
}) {
  const { t } = useTranslation();
  const accent =
    need.priority === NeedPriority.CRITICAL
      ? 'signal'
      : need.priority === NeedPriority.HIGH
        ? 'warn'
        : 'ink';
  const remainingColor =
    need.priority === NeedPriority.CRITICAL ? 'var(--nr-signal)' : 'var(--nr-ink)';
  const disabled =
    need.status === NeedStatus.COVERED || need.status === NeedStatus.CLOSED;

  return (
    <SurfaceCard
      mb="sm"
      accent={accent}
      className={need.priority === NeedPriority.CRITICAL ? 'nr-need-critical' : undefined}
    >
      <Stack gap="xs">
        <Group justify="space-between" align="flex-start">
          <div>
            <Title order={4}>{need.title}</Title>
            <Group gap="xs" mt={4}>
              <NeedStatusBadge status={need.status} />
              <NeedPriorityBadge priority={need.priority} />
              <NeedCategoryBadge category={need.category} />
            </Group>
          </div>
          <Button
            data-testid={`need-offer-${need.id}`}
            disabled={disabled}
            color="red"
            onClick={() => onOffer(need)}
          >
            {t('need.provide')}
          </Button>
        </Group>
        <Text fz={28} fw={800} style={{ color: remainingColor }}>
          {need.remaining}{' '}
          <Text span fz="sm" fw={500} c="dimmed">
            {need.unit} {t('need.remaining').toLowerCase()}
          </Text>
        </Text>
        <Group gap="lg">
          <Text size="sm">
            {t('need.required')}: {need.quantityRequired} {need.unit}
          </Text>
          <Text size="sm">
            {t('need.pending')}: {need.quantityPending} {need.unit}
          </Text>
          <Text size="sm">
            {t('need.offered')}: {need.quantityOffered} {need.unit}
          </Text>
        </Group>
      </Stack>
    </SurfaceCard>
  );
}
