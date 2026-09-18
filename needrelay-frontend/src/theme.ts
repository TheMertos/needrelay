import { createTheme } from '@mantine/core';

/**
 * Command-Center Mantine theme (ink primary, Sora, signal-friendly neutrals).
 * Default control sizes aim for ≥44px touch targets on mobile.
 */
export const theme = createTheme({
  primaryColor: 'ink',
  fontFamily: 'Sora, Segoe UI, sans-serif',
  headings: {
    fontFamily: 'Sora, Segoe UI, sans-serif',
    fontWeight: '700',
  },
  defaultRadius: 'sm',
  // Leaflet panes use z-index up to ~1000; keep overlays above maps.
  components: {
    Button: {
      defaultProps: {
        size: 'md',
      },
    },
    TextInput: {
      defaultProps: {
        size: 'md',
      },
    },
    PasswordInput: {
      defaultProps: {
        size: 'md',
      },
    },
    Select: {
      defaultProps: {
        size: 'md',
      },
    },
    Modal: {
      defaultProps: {
        zIndex: 2000,
      },
    },
    Drawer: {
      defaultProps: {
        zIndex: 2000,
      },
    },
  },
  colors: {
    ink: [
      '#f1f5f9',
      '#e2e8f0',
      '#cbd5e1',
      '#94a3b8',
      '#64748b',
      '#475569',
      '#334155',
      '#1e293b',
      '#0f172a',
      '#0b1220',
    ],
  },
  primaryShade: 9,
});
