import { Group, Pagination, Select, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';

const PAGE_SIZE_OPTIONS = [10, 20, 50];

/**
 * Shared pagination control: page controls plus a page-size dropdown. Always renders,
 * even at a single page, so the control's presence is consistent across lists.
 *
 * @param props.page zero-based current page
 * @param props.totalPages total page count (at least 1)
 * @param props.pageSize current page size
 * @param props.onPageChange called with the new zero-based page
 * @param props.onPageSizeChange called with the new page size
 * @param props.testIdPrefix prefix for data-testid attributes
 * @returns pagination bar
 */
export function PaginationBar({
  page,
  totalPages,
  pageSize,
  onPageChange,
  onPageSizeChange,
  testIdPrefix = 'pagination',
}: {
  page: number;
  totalPages: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
  testIdPrefix?: string;
}) {
  const { t } = useTranslation();
  const safeTotalPages = Math.max(totalPages, 1);

  return (
    <Group justify="center" align="center" gap="md" wrap="wrap">
      <Pagination
        data-testid={`${testIdPrefix}-pages`}
        total={safeTotalPages}
        value={Math.min(page, safeTotalPages - 1) + 1}
        onChange={(nextPage) => onPageChange(nextPage - 1)}
      />
      <Group gap={6} align="center" wrap="nowrap">
        <Text size="xs" c="dimmed">
          {t('common.perPage')}
        </Text>
        <Select
          data-testid={`${testIdPrefix}-size`}
          value={String(pageSize)}
          onChange={(value) => value && onPageSizeChange(Number(value))}
          data={PAGE_SIZE_OPTIONS.map((size) => ({ value: String(size), label: String(size) }))}
          allowDeselect={false}
          w={72}
          size="xs"
        />
      </Group>
    </Group>
  );
}
