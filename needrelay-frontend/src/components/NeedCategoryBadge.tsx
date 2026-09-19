import { Badge } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import type { NeedCategory } from '../api/generated/models';

/**
 * Renders a translated need category badge.
 *
 * @param props category value
 * @returns Mantine badge
 */
export function NeedCategoryBadge({ category }: { category: NeedCategory }) {
  const { t } = useTranslation();
  return <Badge variant="light">{t(`need.categoryValues.${category}`)}</Badge>;
}
