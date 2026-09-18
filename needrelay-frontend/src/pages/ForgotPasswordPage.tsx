import { Anchor, Button, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { authApi } from '../api';
import { AuthPanel } from '../components/AuthPanel';

/**
 * Requests a password-reset email via Resend.
 *
 * @returns forgot-password form
 */
export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const form = useForm({ initialValues: { email: '' } });

  /**
   * Submits the forgot-password request.
   *
   * @param values form values
   */
  async function handleSubmit(values: typeof form.values) {
    try {
      await authApi.forgotPassword(values);
      notifications.show({ color: 'green', message: t('auth.forgotSent') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  return (
    <AuthPanel title={t('auth.forgotTitle')}>
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack>
          <Text c="dimmed" size="sm">
            {t('auth.forgotHint')}
          </Text>
          <TextInput label={t('auth.email')} {...form.getInputProps('email')} />
          <Button type="submit" color="red">
            {t('auth.forgotSubmit')}
          </Button>
          <Anchor component={Link} to="/login" size="sm">
            {t('auth.backToLogin')}
          </Anchor>
        </Stack>
      </form>
    </AuthPanel>
  );
}
