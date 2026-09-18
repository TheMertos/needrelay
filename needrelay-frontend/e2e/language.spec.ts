import { expect, test } from '@playwright/test';

test.describe('language switcher', () => {
  test('switches UI language and document direction', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Find where help is needed' })).toBeVisible();

    const language = page.getByRole('combobox', { name: 'Language' });
    await language.click();
    await page.getByRole('option', { name: 'Deutsch' }).click();
    await expect(page.getByRole('heading', { name: 'Hilfe finden, wo sie gebraucht wird' })).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'de');
    await expect(page.locator('html')).toHaveAttribute('dir', 'ltr');

    await language.click();
    await page.getByRole('option', { name: 'العربية' }).click();
    await expect(
      page.getByRole('heading', { name: 'اعثر على أماكن الحاجة إلى المساعدة' }),
    ).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'ar');
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
  });
});
