import { Badge } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { NeedStatus } from '../api/generated/models';

const STATUS_COLOR: Record<NeedStatus, string> = {
  OPEN: 'blue',
  PARTIALLY_COVERED: 'yellow',
  COVERED: 'teal',
  CLOSED: 'gray',
};

/**
 * Renders a need coverage status badge.
 *
 * @param props status value
 * @returns Mantine badge
 */
export function NeedStatusBadge({ status }: { status: NeedStatus }) {
  const { t } = useTranslation();
  return (
    <Badge color={STATUS_COLOR[status]} variant="light">
      {t(`need.status.${status}`)}
    </Badge>
  );
}
