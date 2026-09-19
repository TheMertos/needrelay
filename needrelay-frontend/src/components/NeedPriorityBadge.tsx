import { Badge } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { NeedPriority } from '../api/generated/models';

/**
 * Renders a translated need priority badge.
 *
 * @param props priority value
 * @returns Mantine badge
 */
export function NeedPriorityBadge({ priority }: { priority: NeedPriority }) {
  const { t } = useTranslation();
  return <Badge variant="outline">{t(`need.priorityValues.${priority}`)}</Badge>;
}
