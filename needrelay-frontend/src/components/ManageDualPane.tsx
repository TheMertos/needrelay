import { Box, SimpleGrid } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Manage-request dual pane: left triage, right offer inbox.
 *
 * @param props.left needs and map column
 * @param props.right offer inbox column
 * @returns layout wrapper
 */
export function ManageDualPane({ left, right }: { left: ReactNode; right: ReactNode }) {
  return (
    <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md" data-testid="manage-dual-pane">
      <Box data-testid="manage-left">{left}</Box>
      <Box data-testid="manage-right">{right}</Box>
    </SimpleGrid>
  );
}
