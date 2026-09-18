import { Container, Stack, Title } from '@mantine/core';
import type { ReactNode } from 'react';
import { SurfaceCard } from './SurfaceCard';

/**
 * Shared frame for auth and simple settings forms.
 *
 * @param props.title page title
 * @param props.children form content
 * @returns framed panel
 */
export function AuthPanel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Container size="xs" py="xl">
      <SurfaceCard accent="signal">
        <Stack>
          <Title order={2}>{title}</Title>
          {children}
        </Stack>
      </SurfaceCard>
    </Container>
  );
}
