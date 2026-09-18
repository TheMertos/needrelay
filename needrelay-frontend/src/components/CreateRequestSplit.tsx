import { Box, SimpleGrid } from '@mantine/core';
import type { ReactNode } from 'react';

/**
 * Create-request layout: map beside form; mobile stacks map above form.
 *
 * @param props.map map picker node
 * @param props.form form fields node
 * @returns layout wrapper
 */
export function CreateRequestSplit({ map, form }: { map: ReactNode; form: ReactNode }) {
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" data-testid="create-request-split">
      <Box data-testid="create-request-map">{map}</Box>
      <Box data-testid="create-request-form">{form}</Box>
    </SimpleGrid>
  );
}
