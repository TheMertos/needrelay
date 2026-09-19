import { expect, test } from '@playwright/test';

test.describe('mobile shell', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/public/discovery*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 1 }),
      });
    });
  });

  test('burger opens drawer with login', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile', 'mobile project only');

    await page.goto('/');
    await expect(page.getByTestId('discovery-page')).toBeVisible();
    await expect(page.getByTestId('nav-burger')).toBeVisible();
    await page.getByTestId('nav-burger').click();
    await expect(page.getByTestId('nav-drawer')).toBeVisible();
    await page.getByTestId('nav-drawer').getByRole('link', { name: 'Log in' }).click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('invites page shows card list on mobile', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile', 'mobile project only');

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
    await page.route('**/api/invites', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
              token: 'invite-token-1',
              email: null,
              expiresAt: new Date(Date.now() + 86400000).toISOString(),
              usedAt: null,
              createdAt: new Date().toISOString(),
            },
          ]),
        });
        return;
      }
      await route.fallback();
    });

    await page.addInitScript(() => {
      localStorage.setItem('needrelay.accessToken', 'test-access');
      localStorage.setItem('needrelay.refreshToken', 'test-refresh');
    });

    await page.goto('/invites');
    await expect(page.getByTestId('mobile-card-list')).toBeVisible();
    await expect(page.getByTestId('desktop-table')).toBeHidden();
    await expect(page.getByTestId('mobile-card-list').getByText(/invite-token-1/)).toBeVisible();
  });
});
