import { Button, PasswordInput, Stack } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api';
import { AuthPanel } from '../components/AuthPanel';

/**
 * Completes password reset using the emailed token.
 *
 * @returns reset-password form
 */
export function ResetPasswordPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const form = useForm({
    initialValues: {
      token: params.get('token') ?? '',
      newPassword: '',
    },
  });

  /**
   * Submits the new password with the reset token.
   *
   * @param values form values
   */
  async function handleSubmit(values: typeof form.values) {
    try {
      await authApi.resetPassword(values);
      notifications.show({ color: 'green', message: t('auth.resetSuccess') });
      navigate('/login');
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  return (
    <AuthPanel title={t('auth.resetTitle')}>
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack>
          <PasswordInput
            label={t('auth.newPassword')}
            {...form.getInputProps('newPassword')}
          />
          <Button type="submit" color="red">
            {t('auth.resetSubmit')}
          </Button>
        </Stack>
      </form>
    </AuthPanel>
  );
}
