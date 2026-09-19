import { expect, test } from '@playwright/test';

test('public relief stacks organization info above the map on mobile when slug is set', async ({
  page,
}) => {
  const slug = process.env.E2E_PUBLIC_SLUG;
  test.skip(!slug, 'Set E2E_PUBLIC_SLUG to a seeded public relief slug');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/r/${slug}`);
  const left = page.getByTestId('public-relief-left');
  const right = page.getByTestId('public-relief-right');
  await expect(left).toBeVisible();
  await expect(right).toBeVisible();
  const leftBox = await left.boundingBox();
  const rightBox = await right.boundingBox();
  expect(leftBox && rightBox && leftBox.y < rightBox.y).toBeTruthy();
});
