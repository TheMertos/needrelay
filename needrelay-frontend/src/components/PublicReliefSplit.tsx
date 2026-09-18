import { Box, SimpleGrid } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Responsive public layout: map + needs; on mobile map stacks above needs.
 *
 * @param props.map map node
 * @param props.list needs list node
 * @returns layout wrapper
 */
export function PublicReliefSplit({ map, list }: { map: ReactNode; list: ReactNode }) {
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" data-testid="public-relief-split">
      <Box data-testid="public-relief-map">{map}</Box>
      <Box data-testid="public-relief-needs">{list}</Box>
    </SimpleGrid>
  );
}
