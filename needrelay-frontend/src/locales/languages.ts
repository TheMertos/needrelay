/**
 * Supported UI languages for the language selector (native labels).
 */
export const UI_LANGUAGES = [
  { value: 'en', label: 'English' },
  { value: 'de', label: 'Deutsch' },
  { value: 'ar', label: 'العربية' },
  { value: 'tr', label: 'Türkçe' },
  { value: 'fr', label: 'Français' },
  { value: 'es', label: 'Español' },
  { value: 'pt', label: 'Português' },
  { value: 'ru', label: 'Русский' },
  { value: 'zh', label: '中文' },
  { value: 'ja', label: '日本語' },
  { value: 'hi', label: 'हिन्दी' },
  { value: 'id', label: 'Bahasa Indonesia' },
  { value: 'it', label: 'Italiano' },
  { value: 'nl', label: 'Nederlands' },
  { value: 'pl', label: 'Polski' },
  { value: 'uk', label: 'Українська' },
  { value: 'fa', label: 'فارسی' },
  { value: 'ur', label: 'اردو' },
  { value: 'ko', label: '한국어' },
  { value: 'vi', label: 'Tiếng Việt' },
] as const;

/** Language codes that use right-to-left document direction. */
export const RTL_LANGUAGES = new Set(['ar', 'fa', 'ur']);

/**
 * Resolves the active UI language code from i18n's language string.
 *
 * @param language - i18n language (may include region, e.g. en-US)
 * @returns matching UI language code or 'en'
 */
export function resolveUiLanguage(language: string): string {
  const base = language.split('-')[0]?.toLowerCase() ?? 'en';
  return UI_LANGUAGES.some((item) => item.value === base) ? base : 'en';
}
