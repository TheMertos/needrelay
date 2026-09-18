import { Alert, Anchor, Button, PasswordInput, Stack, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api';
import { setTokens } from '../api/client';
import { AuthPanel } from '../components/AuthPanel';
import { getApiErrorCode } from '../lib/apiError';

/**
 * Organizer login page.
 *
 * @returns login form
 */
export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [formError, setFormError] = useState<string | null>(null);
  const form = useForm({
    initialValues: { email: '', password: '' },
    validate: {
      email: (value) => (value.includes('@') ? null : 'Invalid email'),
      password: (value) => (value.length >= 8 ? null : 'Min 8 characters'),
    },
  });

  /**
   * Maps a login API failure to a user-facing message.
   *
   * @param error caught error
   * @returns i18n message
   */
  function messageForLoginError(error: unknown): string {
    const code = getApiErrorCode(error);
    if (code === 'INVALID_CREDENTIALS') {
      return t('auth.invalidCredentials');
    }
    if (code === 'ACCOUNT_DISABLED') {
      return t('auth.accountDisabled');
    }
    if (code === 'RATE_LIMITED') {
      return t('auth.rateLimited');
    }
    return t('common.error');
  }

  /**
   * Submits login credentials.
   *
   * @param values form values
   */
  async function handleSubmit(values: typeof form.values) {
    setFormError(null);
    try {
      const tokens = await authApi.login(values);
      setTokens(tokens.accessToken, tokens.refreshToken);
      navigate('/dashboard');
    } catch (error) {
      const message = messageForLoginError(error);
      setFormError(message);
      notifications.show({ color: 'red', message });
    }
  }

  return (
    <AuthPanel title={t('auth.loginTitle')}>
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack>
          {formError ? (
            <Alert color="red" variant="light" data-testid="login-error">
              {formError}
            </Alert>
          ) : null}
          <TextInput
            label={t('auth.email')}
            {...form.getInputProps('email')}
            onChange={(event) => {
              setFormError(null);
              form.getInputProps('email').onChange(event);
            }}
          />
          <PasswordInput
            label={t('auth.password')}
            {...form.getInputProps('password')}
            onChange={(event) => {
              setFormError(null);
              form.getInputProps('password').onChange(event);
            }}
          />
          <Button type="submit" color="red">
            {t('auth.submitLogin')}
          </Button>
          <Anchor component={Link} to="/forgot-password" size="sm">
            {t('auth.forgotLink')}
          </Anchor>
        </Stack>
      </form>
    </AuthPanel>
  );
}
