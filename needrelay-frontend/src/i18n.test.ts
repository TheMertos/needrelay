import { beforeEach, describe, expect, it } from 'vitest';
import i18n, { applySystemDefaultLanguage, markLanguageExplicit } from './i18n';

describe('applySystemDefaultLanguage', () => {
  beforeEach(() => {
    localStorage.clear();
    void i18n.changeLanguage('en');
  });

  it('applies the platform default when the visitor never chose a language', () => {
    applySystemDefaultLanguage('de');
    expect(i18n.language).toBe('de');
  });

  it('does not override a language the visitor explicitly picked', () => {
    markLanguageExplicit();
    void i18n.changeLanguage('fr');

    applySystemDefaultLanguage('de');

    expect(i18n.language).toBe('fr');
  });

  it('normalizes an unsupported or region-qualified default to a known UI language', () => {
    applySystemDefaultLanguage('ar-SA');
    expect(i18n.language).toBe('ar');
  });
});
