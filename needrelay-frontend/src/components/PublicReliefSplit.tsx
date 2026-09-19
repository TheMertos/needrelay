import { Box, SimpleGrid } from '@mantine/core';
import type { ReactNode } from 'react';
import './PublicReliefSplit.css';

/**
 * Responsive two-column public layout; on mobile the left content stacks above the
 * right content. Whichever side holds the map should stay sticky on wider screens
 * so it stays visible while scrolling — pick it with `stickySide`.
 *
 * @param props.left left column on desktop, top on mobile
 * @param props.right right column on desktop, bottom on mobile
 * @param props.stickySide which column stays sticky on wider screens (default 'left')
 * @returns layout wrapper
 */
export function PublicReliefSplit({
  left,
  right,
  stickySide = 'left',
}: {
  left: ReactNode;
  right: ReactNode;
  stickySide?: 'left' | 'right';
}) {
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" data-testid="public-relief-split">
      <Box
        data-testid="public-relief-left"
        className={stickySide === 'left' ? 'nr-public-relief-map' : undefined}
      >
        {left}
      </Box>
      <Box
        data-testid="public-relief-right"
        className={stickySide === 'right' ? 'nr-public-relief-map' : undefined}
      >
        {right}
      </Box>
    </SimpleGrid>
  );
}
