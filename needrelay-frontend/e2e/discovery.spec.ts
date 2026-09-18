import { expect, test } from '@playwright/test';

const payload = {
  points: [
    {
      id: '11111111-1111-1111-1111-111111111111',
      title: 'Camp A',
      locationLabel: 'Aleppo North',
      latitude: 36.2,
      longitude: 37.1,
      publicSlug: 'camp-a',
    },
    {
      id: '22222222-2222-2222-2222-222222222222',
      title: 'Camp B',
      locationLabel: 'Damascus',
      latitude: 33.5,
      longitude: 36.3,
      publicSlug: 'camp-b',
    },
  ],
  needs: [
    {
      id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      title: 'Water tanks',
      priority: 'CRITICAL',
      status: 'OPEN',
      requestId: '11111111-1111-1111-1111-111111111111',
      publicSlug: 'camp-a',
      locationLabel: 'Aleppo North',
      organizationName: 'Aid Org',
    },
    {
      id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
      title: 'Blankets',
      priority: 'HIGH',
      status: 'OPEN',
      requestId: '22222222-2222-2222-2222-222222222222',
      publicSlug: 'camp-b',
      locationLabel: 'Damascus',
      organizationName: 'Other Org',
    },
  ],
};

test.beforeEach(async ({ page }) => {
  await page.route('**/api/public/discovery', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(payload),
    });
  });
});

test('discovery shows per-point maps and needs; text and drawer work', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('discovery-page')).toBeVisible();
  await expect(
    page.getByTestId('discovery-map-11111111-1111-1111-1111-111111111111'),
  ).toBeVisible();
  await expect(
    page.getByTestId('discovery-map-22222222-2222-2222-2222-222222222222'),
  ).toBeVisible();
  await expect(page.getByText('Water tanks')).toBeVisible();
  await expect(page.getByText('Blankets')).toBeVisible();

  await page.getByTestId('discovery-search').fill('aleppo');
  await expect(page.getByText('Water tanks')).toBeVisible();
  await expect(page.getByText('Blankets')).toHaveCount(0);
  await expect(
    page.getByTestId('discovery-map-22222222-2222-2222-2222-222222222222'),
  ).toHaveCount(0);

  await page.getByText('Water tanks').click();
  await expect(page.getByTestId('discovery-open-request')).toBeVisible();
  await expect(page.getByTestId('discovery-open-request')).toHaveAttribute(
    'href',
    '/r/camp-a',
  );
});
