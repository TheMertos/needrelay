import { Button, Group, Modal, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';

/**
 * Shared confirmation dialog for destructive actions.
 *
 * @param props.opened whether the modal is open
 * @param props.onClose close without confirming
 * @param props.title modal title
 * @param props.body confirmation message
 * @param props.confirmLabel confirm button label
 * @param props.confirming loading state on confirm
 * @param props.onConfirm confirm handler
 * @returns confirm modal
 */
export function ConfirmModal({
  opened,
  onClose,
  title,
  body,
  confirmLabel,
  confirming = false,
  onConfirm,
}: {
  opened: boolean;
  onClose: () => void;
  title: string;
  body: string;
  confirmLabel?: string;
  confirming?: boolean;
  onConfirm: () => void | Promise<void>;
}) {
  const { t } = useTranslation();

  return (
    <Modal opened={opened} onClose={onClose} title={title}>
      <Text mb="md">{body}</Text>
      <Group justify="flex-end">
        <Button variant="default" onClick={onClose} disabled={confirming}>
          {t('common.cancel')}
        </Button>
        <Button
          data-testid="confirm-modal-confirm"
          color="red"
          loading={confirming}
          onClick={() => void onConfirm()}
        >
          {confirmLabel ?? t('common.confirm')}
        </Button>
      </Group>
    </Modal>
  );
}
