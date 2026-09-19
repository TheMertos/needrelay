import { MantineProvider, Modal, Select } from '@mantine/core';
import { fireEvent, render, screen } from '@testing-library/react';
import { beforeAll, describe, expect, it } from 'vitest';
import { theme } from './theme';

/**
 * Mantine ScrollArea (used by Select dropdown) requires ResizeObserver in jsdom.
 */
beforeAll(() => {
  class ResizeObserverStub {
    /**
     * No-op observe for jsdom.
     * @returns {void}
     */
    observe(): void {
      return undefined;
    }

    /**
     * No-op unobserve for jsdom.
     * @returns {void}
     */
    unobserve(): void {
      return undefined;
    }

    /**
     * No-op disconnect for jsdom.
     * @returns {void}
     */
    disconnect(): void {
      return undefined;
    }
  }

  Object.defineProperty(globalThis, 'ResizeObserver', {
    writable: true,
    configurable: true,
    value: ResizeObserverStub,
  });

  // Combobox scrolls the active option into view; jsdom lacks Element.scrollIntoView.
  Element.prototype.scrollIntoView = () => undefined;
});

/**
 * Regression: Select dropdowns must open while nested in a Modal.
 * (Modal z-index was raised above Leaflet; combobox portals need a higher z-index.)
 */
describe('theme Select in Modal', () => {
  it('opens select options inside an open modal', async () => {
    render(
      <MantineProvider theme={theme}>
        <Modal opened onClose={() => undefined} title="Need">
          <Select
            label="Priority"
            data={[
              { value: 'HIGH', label: 'HIGH' },
              { value: 'LOW', label: 'LOW' },
            ]}
            defaultValue="HIGH"
          />
        </Modal>
      </MantineProvider>,
    );

    const trigger =
      screen.queryByRole('textbox', { name: 'Priority' }) ??
      screen.getByRole('combobox', { name: 'Priority' });
    fireEvent.click(trigger);
    expect(await screen.findByRole('option', { name: 'LOW' })).toBeInTheDocument();
  });
});
