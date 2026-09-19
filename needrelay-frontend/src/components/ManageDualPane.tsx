import { Box, Stack } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Manage-request stacked layout: needs above, offer inbox below.
 *
 * @param props.left needs and map section
 * @param props.right offer inbox section
 * @returns layout wrapper
 */
export function ManageDualPane({ left, right }: { left: ReactNode; right: ReactNode }) {
  return (
    <Stack gap="md" data-testid="manage-dual-pane">
      <Box data-testid="manage-left">{left}</Box>
      <Box data-testid="manage-right">{right}</Box>
    </Stack>
  );
}
