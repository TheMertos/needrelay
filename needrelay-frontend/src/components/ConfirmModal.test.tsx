import { MantineProvider } from '@mantine/core';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import i18n from '../i18n';
import { theme } from '../theme';
import { ConfirmModal } from './ConfirmModal';

describe('ConfirmModal', () => {
  it('calls onConfirm when confirm is clicked', async () => {
    await i18n.changeLanguage('en');
    const onConfirm = vi.fn();
    const onClose = vi.fn();

    render(
      <MantineProvider theme={theme}>
        <ConfirmModal
          opened
          onClose={onClose}
          title="Confirm delete"
          body="Delete this item?"
          onConfirm={onConfirm}
        />
      </MantineProvider>,
    );

    expect(screen.getByText('Delete this item?')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('confirm-modal-confirm'));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });
});
