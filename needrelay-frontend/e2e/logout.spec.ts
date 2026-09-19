import { expect, test } from '@playwright/test';

test('logout asks for confirmation and revokes refresh token', async ({ page }) => {
  const logoutBodies: unknown[] = [];

  await page.route('**/api/auth/logout', async (route) => {
    logoutBodies.push(route.request().postDataJSON());
    await route.fulfill({ status: 204 });
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '11111111-1111-1111-1111-111111111101',
        email: 'org@needrelay.local',
        displayName: 'Org',
        description: null,
        role: 'ORGANIZER',
        active: true,
        createdAt: new Date().toISOString(),
      }),
    });
  });
  await page.route('**/api/public/discovery*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 1 }),
    });
  });

  await page.addInitScript(() => {
    localStorage.setItem('needrelay.accessToken', 'test-access');
    localStorage.setItem('needrelay.refreshToken', 'test-refresh');
  });

  await page.goto('/');

  /**
   * Opens logout from desktop header or mobile drawer.
   *
   * @returns void
   */
  async function openLogout() {
    const burger = page.getByTestId('nav-burger');
    if (await burger.isVisible()) {
      await burger.click();
      await page.getByTestId('nav-drawer').getByRole('button', { name: 'Log out' }).click();
      return;
    }
    await page.getByTestId('nav-logout').click();
  }

  await openLogout();
  await expect(page.getByText('Are you sure you want to log out?')).toBeVisible();

  await page.getByRole('button', { name: 'Cancel' }).click();

  await openLogout();
  await page.getByTestId('logout-confirm').click();

  const burger = page.getByTestId('nav-burger');
  if (await burger.isVisible()) {
    await burger.click();
    await expect(page.getByTestId('nav-drawer').getByRole('link', { name: 'Log in' })).toBeVisible();
  } else {
    await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();
  }
  expect(logoutBodies).toEqual([{ refreshToken: 'test-refresh' }]);
});
