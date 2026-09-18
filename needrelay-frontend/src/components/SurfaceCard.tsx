import { Paper, type PaperProps } from '@mantine/core';
import type { ReactNode } from 'react';

type Accent = 'signal' | 'warn' | 'ok' | 'ink' | 'none';

const ACCENT: Record<Accent, string | undefined> = {
  signal: 'var(--nr-signal)',
  warn: 'var(--nr-warn)',
  ok: 'var(--nr-ok)',
  ink: 'var(--nr-ink)',
  none: undefined,
};

/**
 * Light framed surface matching AuthPanel (border + optional top accent).
 *
 * @param props.children content
 * @param props.accent optional top border accent
 * @param props remaining Mantine Paper props
 * @returns bordered paper card
 */
export function SurfaceCard({
  children,
  accent = 'none',
  style,
  ...paperProps
}: {
  children: ReactNode;
  accent?: Accent;
} & PaperProps) {
  const top = ACCENT[accent];
  return (
    <Paper
      withBorder
      radius="sm"
      p="lg"
      bg="var(--nr-card)"
      style={{
        borderColor: 'var(--nr-border)',
        ...(top ? { borderTop: `3px solid ${top}` } : null),
        ...style,
      }}
      {...paperProps}
    >
      {children}
    </Paper>
  );
}
