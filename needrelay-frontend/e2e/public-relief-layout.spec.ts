import { expect, test } from '@playwright/test';

test('public relief stacks map above needs on mobile when slug is set', async ({
  page,
}) => {
  const slug = process.env.E2E_PUBLIC_SLUG;
  test.skip(!slug, 'Set E2E_PUBLIC_SLUG to a seeded public relief slug');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(`/r/${slug}`);
  const map = page.getByTestId('public-relief-map');
  const needs = page.getByTestId('public-relief-needs');
  await expect(map).toBeVisible();
  await expect(needs).toBeVisible();
  const mapBox = await map.boundingBox();
  const needsBox = await needs.boundingBox();
  expect(mapBox && needsBox && mapBox.y < needsBox.y).toBeTruthy();
});
