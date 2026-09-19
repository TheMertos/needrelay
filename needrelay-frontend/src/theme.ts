import { createTheme } from '@mantine/core';

/** Overlay stack: Leaflet ≤ ~1000, Modal/Drawer 2000, portaled Select/Popover above modals. */
const OVERLAY_Z_INDEX = 2000;
const PORTAL_Z_INDEX = 3000;

/**
 * Command-Center Mantine theme (ink primary, Sora, signal-friendly neutrals).
 * Compact controls (sm) and slightly tighter spacing on desktop and mobile.
 */
export const theme = createTheme({
  primaryColor: 'ink',
  fontFamily: 'Sora, Segoe UI, sans-serif',
  headings: {
    fontFamily: 'Sora, Segoe UI, sans-serif',
    fontWeight: '700',
  },
  defaultRadius: 'sm',
  spacing: {
    xs: '0.375rem',
    sm: '0.5rem',
    md: '0.75rem',
    lg: '1rem',
    xl: '1.25rem',
  },
  // Leaflet panes use z-index up to ~1000; keep overlays above maps.
  // Select/Combobox portals must sit above Modal/Drawer (same reason).
  components: {
    Button: {
      defaultProps: {
        size: 'sm',
      },
    },
    TextInput: {
      defaultProps: {
        size: 'sm',
      },
    },
    PasswordInput: {
      defaultProps: {
        size: 'sm',
      },
    },
    NumberInput: {
      defaultProps: {
        size: 'sm',
      },
    },
    Textarea: {
      defaultProps: {
        size: 'sm',
      },
    },
    Select: {
      defaultProps: {
        size: 'sm',
        comboboxProps: {
          zIndex: PORTAL_Z_INDEX,
        },
      },
    },
    Popover: {
      defaultProps: {
        zIndex: PORTAL_Z_INDEX,
      },
    },
    Menu: {
      defaultProps: {
        zIndex: PORTAL_Z_INDEX,
      },
    },
    Modal: {
      defaultProps: {
        zIndex: OVERLAY_Z_INDEX,
      },
    },
    Drawer: {
      defaultProps: {
        zIndex: OVERLAY_Z_INDEX,
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
