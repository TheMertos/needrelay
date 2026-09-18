import { Button, PasswordInput, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api';
import { setTokens } from '../api/client';
import { AuthPanel } from '../components/AuthPanel';

/**
 * Invite-only organizer registration page.
 *
 * @returns register form
 */
export function RegisterPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const form = useForm({
    initialValues: {
      inviteToken: params.get('invite') ?? '',
      email: '',
      password: '',
      displayName: '',
    },
  });

  /**
   * Registers using an invite token.
   *
   * @param values form values
   */
  async function handleSubmit(values: typeof form.values) {
    try {
      const tokens = await authApi.register(values);
      setTokens(tokens.accessToken, tokens.refreshToken);
      navigate('/dashboard');
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  return (
    <AuthPanel title={t('auth.registerTitle')}>
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack>
          <TextInput label={t('auth.inviteToken')} {...form.getInputProps('inviteToken')} />
          <TextInput label={t('auth.displayName')} {...form.getInputProps('displayName')} />
          <TextInput label={t('auth.email')} {...form.getInputProps('email')} />
          <PasswordInput label={t('auth.password')} {...form.getInputProps('password')} />
          <Button type="submit" color="red">
            {t('auth.submitRegister')}
          </Button>
        </Stack>
      </form>
    </AuthPanel>
  );
}
