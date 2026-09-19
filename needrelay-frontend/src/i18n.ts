import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import ar from './locales/ar.json';
import de from './locales/de.json';
import en from './locales/en.json';
import es from './locales/es.json';
import fa from './locales/fa.json';
import fr from './locales/fr.json';
import hi from './locales/hi.json';
import id from './locales/id.json';
import it from './locales/it.json';
import ja from './locales/ja.json';
import ko from './locales/ko.json';
import { resolveUiLanguage } from './locales/languages';
import nl from './locales/nl.json';
import pl from './locales/pl.json';
import pt from './locales/pt.json';
import ru from './locales/ru.json';
import tr from './locales/tr.json';
import uk from './locales/uk.json';
import ur from './locales/ur.json';
import vi from './locales/vi.json';
import zh from './locales/zh.json';

const LANG_STORAGE_KEY = 'nr-ui-lang';
const LANG_EXPLICIT_KEY = 'nr-ui-lang-explicit';

/**
 * Reads the persisted UI language from localStorage (browser only).
 *
 * @returns stored language code or null
 */
function readStoredLanguage(): string | null {
  try {
    return localStorage.getItem(LANG_STORAGE_KEY);
  } catch {
    return null;
  }
}

/**
 * Persists the active UI language for the next visit.
 *
 * @param language - i18n language string
 * @returns void
 */
function persistLanguage(language: string): void {
  try {
    localStorage.setItem(LANG_STORAGE_KEY, resolveUiLanguage(language));
  } catch {
    // ignore quota / private mode
  }
}

/**
 * Marks that the visitor picked a language themselves, so the platform's default
 * language no longer overrides their choice on future visits.
 *
 * @returns void
 */
export function markLanguageExplicit(): void {
  try {
    localStorage.setItem(LANG_EXPLICIT_KEY, '1');
  } catch {
    // ignore quota / private mode
  }
}

/**
 * Whether the visitor has explicitly picked a language before.
 *
 * @returns true when a manual choice was recorded
 */
function isLanguageExplicit(): boolean {
  try {
    return localStorage.getItem(LANG_EXPLICIT_KEY) === '1';
  } catch {
    return false;
  }
}

/**
 * Applies the platform's default language, unless the visitor already picked one
 * themselves. Safe to call on every app boot — it only ever affects visitors who have
 * never touched the language switcher.
 *
 * @param defaultLanguage - platform default language code
 * @returns void
 */
export function applySystemDefaultLanguage(defaultLanguage: string): void {
  if (isLanguageExplicit()) {
    return;
  }
  const resolved = resolveUiLanguage(defaultLanguage);
  if (resolved !== resolveUiLanguage(i18n.language)) {
    void i18n.changeLanguage(resolved);
  }
}

const initialLng = resolveUiLanguage(readStoredLanguage() ?? 'en');

/**
 * Initializes i18n with all supported UI locales (English fallback).
 */
void i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    de: { translation: de },
    ar: { translation: ar },
    tr: { translation: tr },
    fr: { translation: fr },
    es: { translation: es },
    pt: { translation: pt },
    ru: { translation: ru },
    zh: { translation: zh },
    ja: { translation: ja },
    hi: { translation: hi },
    id: { translation: id },
    it: { translation: it },
    nl: { translation: nl },
    pl: { translation: pl },
    uk: { translation: uk },
    fa: { translation: fa },
    ur: { translation: ur },
    ko: { translation: ko },
    vi: { translation: vi },
  },
  lng: initialLng,
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});

i18n.on('languageChanged', persistLanguage);

export default i18n;
