import { describe, expect, it } from 'vitest';
import { resolveUiLanguage, RTL_LANGUAGES, UI_LANGUAGES } from './languages';

describe('resolveUiLanguage', () => {
  it('returns base code for known languages', () => {
    expect(resolveUiLanguage('de')).toBe('de');
    expect(resolveUiLanguage('ar-SA')).toBe('ar');
    expect(resolveUiLanguage('zh-CN')).toBe('zh');
  });

  it('falls back to en for unknown codes', () => {
    expect(resolveUiLanguage('xx')).toBe('en');
    expect(resolveUiLanguage('')).toBe('en');
  });

  it('lists 20 UI languages including RTL set', () => {
    expect(UI_LANGUAGES).toHaveLength(20);
    expect(RTL_LANGUAGES.has('ar')).toBe(true);
    expect(RTL_LANGUAGES.has('fa')).toBe(true);
    expect(RTL_LANGUAGES.has('ur')).toBe(true);
    expect(RTL_LANGUAGES.has('en')).toBe(false);
  });
});
