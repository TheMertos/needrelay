import {
  Anchor,
  AppShell,
  Burger,
  Button,
  Drawer,
  Group,
  Modal,
  Select,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { authApi } from '../api';
import { clearTokens, getAccessToken, getRefreshToken } from '../api/client';
import { OrganizerResponseRole } from '../api/generated/models';
import { resolveUiLanguage, RTL_LANGUAGES, UI_LANGUAGES } from '../locales/languages';

/**
 * Application shell with navigation for all screens.
 *
 * @returns layout wrapping routed pages
 */
export function AppLayout() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const loggedIn = Boolean(getAccessToken());
  const [isAdmin, setIsAdmin] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [navOpened, { toggle: toggleNav, close: closeNav }] = useDisclosure(false);
  const uiLanguage = resolveUiLanguage(i18n.language);

  useEffect(() => {
    if (!loggedIn) {
      setIsAdmin(false);
      return;
    }
    void authApi
      .me()
      .then((me) => setIsAdmin(me.role === OrganizerResponseRole.ADMIN))
      .catch(() => setIsAdmin(false));
  }, [loggedIn]);

  useEffect(() => {
    document.documentElement.lang = uiLanguage;
    document.documentElement.dir = RTL_LANGUAGES.has(uiLanguage) ? 'rtl' : 'ltr';
  }, [uiLanguage]);

  /**
   * Clears tokens and returns to home after confirm (revokes refresh when possible).
   *
   * @returns void
   */
  async function confirmLogout() {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      try {
        await authApi.logout({ refreshToken });
      } catch {
        // best-effort revoke
      }
    }
    clearTokens();
    setLogoutOpen(false);
    closeNav();
    navigate('/');
  }

  /**
   * Closes the mobile nav drawer then opens the logout confirm modal.
   *
   * @returns void
   */
  function openLogoutFromNav() {
    closeNav();
    setLogoutOpen(true);
  }

  const languageSelect = (
    <Select
      aria-label="Language"
      data={[...UI_LANGUAGES]}
      value={uiLanguage}
      onChange={(value) => {
        if (value) {
          void i18n.changeLanguage(value);
        }
      }}
      w={{ base: 120, sm: 160 }}
      searchable
      allowDeselect={false}
    />
  );

  const desktopNav = loggedIn ? (
    <>
      <Button component={Link} to="/dashboard" variant="subtle" color="ink">
        {t('nav.ops')}
      </Button>
      <Button component={Link} to="/requests/new" color="ink">
        {t('nav.createRequest')}
      </Button>
      <Button component={Link} to="/invites" variant="subtle" color="ink">
        {t('nav.invites')}
      </Button>
      <Button component={Link} to="/account" variant="subtle" color="ink">
        {t('nav.account')}
      </Button>
      {isAdmin ? (
        <Button component={Link} to="/admin/organizers" variant="subtle" color="ink">
          {t('nav.admin')}
        </Button>
      ) : null}
      <Button
        data-testid="nav-logout"
        variant="light"
        color="gray"
        onClick={() => setLogoutOpen(true)}
      >
        {t('nav.logout')}
      </Button>
    </>
  ) : (
    <Button component={Link} to="/login" color="ink">
      {t('nav.login')}
    </Button>
  );

  const drawerNav = (
    <Stack gap="sm" data-testid="nav-drawer">
      {loggedIn ? (
        <>
          <Button component={Link} to="/dashboard" variant="subtle" color="ink" fullWidth onClick={closeNav}>
            {t('nav.ops')}
          </Button>
          <Button component={Link} to="/requests/new" color="ink" fullWidth onClick={closeNav}>
            {t('nav.createRequest')}
          </Button>
          <Button component={Link} to="/invites" variant="subtle" color="ink" fullWidth onClick={closeNav}>
            {t('nav.invites')}
          </Button>
          <Button component={Link} to="/account" variant="subtle" color="ink" fullWidth onClick={closeNav}>
            {t('nav.account')}
          </Button>
          {isAdmin ? (
            <Button
              component={Link}
              to="/admin/organizers"
              variant="subtle"
              color="ink"
              fullWidth
              onClick={closeNav}
            >
              {t('nav.admin')}
            </Button>
          ) : null}
          <Button variant="light" color="gray" fullWidth onClick={openLogoutFromNav}>
            {t('nav.logout')}
          </Button>
        </>
      ) : (
        <Button component={Link} to="/login" color="ink" fullWidth onClick={closeNav}>
          {t('nav.login')}
        </Button>
      )}
    </Stack>
  );

  return (
    <AppShell header={{ height: 64 }} padding="md">
      <AppShell.Header
        style={{
          background: 'var(--nr-card)',
          borderBottom: '1px solid var(--nr-border)',
        }}
      >
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              data-testid="nav-burger"
              opened={navOpened}
              onClick={toggleNav}
              hiddenFrom="sm"
              size="sm"
              aria-label="Open navigation"
            />
            <Anchor component={Link} to="/" underline="never" c="var(--nr-ink)" onClick={closeNav}>
              <Title order={3} c="var(--nr-ink)">
                {t('app.name')}
              </Title>
            </Anchor>
          </Group>
          <Group gap="sm" wrap="nowrap">
            {languageSelect}
            <Group gap="sm" visibleFrom="sm" wrap="nowrap">
              {desktopNav}
            </Group>
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>

      <Drawer
        opened={navOpened}
        onClose={closeNav}
        title={t('app.name')}
        padding="md"
        size="xs"
        hiddenFrom="sm"
      >
        {drawerNav}
      </Drawer>

      <Modal
        opened={logoutOpen}
        onClose={() => setLogoutOpen(false)}
        title={t('nav.logoutConfirmTitle')}
      >
        <Text mb="md">{t('nav.logoutConfirmBody')}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={() => setLogoutOpen(false)}>
            {t('common.cancel')}
          </Button>
          <Button data-testid="logout-confirm" color="red" onClick={() => void confirmLogout()}>
            {t('nav.logout')}
          </Button>
        </Group>
      </Modal>
    </AppShell>
  );
}
