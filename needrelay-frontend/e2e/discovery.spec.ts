import { expect, test } from '@playwright/test';

const CAMP_A = '11111111-1111-1111-1111-111111111111';
const CAMP_B = '22222222-2222-2222-2222-222222222222';

const points = [
  {
    id: CAMP_A,
    title: 'Camp A',
    locationLabel: 'Aleppo North',
    latitude: 36.2,
    longitude: 37.1,
    publicSlug: 'camp-a',
  },
  {
    id: CAMP_B,
    title: 'Camp B',
    locationLabel: 'Damascus',
    latitude: 33.5,
    longitude: 36.3,
    publicSlug: 'camp-b',
  },
];

const needs = [
  {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    title: 'Water tanks',
    category: 'WATER',
    quantityRequired: 200,
    quantityOffered: 80,
    quantityPending: 0,
    remaining: 120,
    unit: 'L',
    priority: 'CRITICAL',
    status: 'OPEN',
    requestId: CAMP_A,
    publicSlug: 'camp-a',
    locationLabel: 'Aleppo North',
    organizationName: 'Aid Org',
  },
  {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    title: 'Blankets',
    category: 'SUPPLIES',
    quantityRequired: 50,
    quantityOffered: 10,
    quantityPending: 0,
    remaining: 40,
    unit: 'pcs',
    priority: 'HIGH',
    status: 'OPEN',
    requestId: CAMP_B,
    publicSlug: 'camp-b',
    locationLabel: 'Damascus',
    organizationName: 'Other Org',
  },
];

const pointNeedsById: Record<string, typeof needs> = {
  [CAMP_A]: [needs[0]],
  [CAMP_B]: [needs[1]],
};

test.beforeEach(async ({ page }) => {
  await page.route('**/api/public/discovery?*', async (route) => {
    const url = new URL(route.request().url());
    const q = (url.searchParams.get('q') ?? '').toLowerCase();
    const pageParam = Number(url.searchParams.get('page') ?? '0');
    const sizeParam = Number(url.searchParams.get('size') ?? '20');
    const matches = points.filter((point) => {
      if (!q) {
        return true;
      }
      const needTitles = (pointNeedsById[point.id] ?? []).map((n) => n.title.toLowerCase());
      return (
        point.title.toLowerCase().includes(q) ||
        point.locationLabel.toLowerCase().includes(q) ||
        needTitles.some((title) => title.includes(q))
      );
    });
    const start = pageParam * sizeParam;
    const items = matches.slice(start, start + sizeParam);
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items,
        page: pageParam,
        size: sizeParam,
        totalElements: matches.length,
        totalPages: Math.max(Math.ceil(matches.length / sizeParam), 1),
      }),
    });
  });

  await page.route('**/api/public/discovery/points/*/needs*', async (route) => {
    const url = new URL(route.request().url());
    const pointId = url.pathname.split('/').at(-2) ?? '';
    const items = pointNeedsById[pointId] ?? [];
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items,
        page: 0,
        size: 20,
        totalElements: items.length,
        totalPages: 1,
      }),
    });
  });
});

test('discovery shows per-point maps and needs', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByTestId('discovery-page')).toBeVisible();
  await expect(page.getByTestId(`discovery-map-${CAMP_A}`)).toBeVisible();
  await expect(page.getByTestId(`discovery-map-${CAMP_B}`)).toBeVisible();
  await expect(page.getByText('Water tanks')).toBeVisible();
  await expect(page.getByText('Blankets')).toBeVisible();

  await page.getByTestId('discovery-search').fill('aleppo');
  await expect(page.getByText('Water tanks')).toBeVisible();
  await expect(page.getByText('Blankets')).toHaveCount(0);
  await expect(page.getByTestId(`discovery-map-${CAMP_B}`)).toHaveCount(0);

  await expect(page.getByTestId(`discovery-open-details-${CAMP_A}`)).toHaveAttribute(
    'href',
    '/r/camp-a',
  );
  await expect(page.getByTestId(`discovery-open-details-${CAMP_B}`)).toHaveCount(0);

  await page.getByTestId('discovery-search').fill('');
  await expect(page.getByTestId(`discovery-open-details-${CAMP_B}`)).toHaveAttribute(
    'href',
    '/r/camp-b',
  );

  const landingWidth = await page.getByTestId('discovery-page').evaluate((el) => {
    const container = el.closest('.mantine-Container-root');
    return container ? getComputedStyle(container).maxWidth : '';
  });
  expect(landingWidth).toBeTruthy();
});

test('discovery need card shows quantity detail like the request page', async ({ page }) => {
  await page.goto('/');
  const card = page.getByTestId('discovery-need-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
  await expect(card).toBeVisible();
  await expect(card).toContainText('120');
  await expect(card).toContainText('Required');
  await expect(card).toContainText('Received');
});

test('discovery open details navigates to public relief page', async ({ page }) => {
  await page.goto('/');
  await page.getByTestId(`discovery-open-details-${CAMP_A}`).click();
  await expect(page).toHaveURL(/\/r\/camp-a$/);
});

test('clicking a need card navigates directly to the public relief page', async ({ page }) => {
  await page.goto('/');
  await page.getByTestId('discovery-need-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa').click();
  await expect(page).toHaveURL(/\/r\/camp-a$/);
});
