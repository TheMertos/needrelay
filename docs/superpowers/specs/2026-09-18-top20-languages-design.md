# Top-20 UI languages

**Date:** 2026-09-18  
**Status:** Approved for implementation

## Goal

Ship full UI locale files for 20 languages (no Kurdish). English remains `fallbackLng`.

## Languages

en, de, ar, tr, fr, es, pt, ru, zh, ja, hi, id, it, nl, pl, uk, fa, ur, ko, vi

## Implementation

- One JSON file per code under `src/locales/`
- Register all in `i18n.ts`
- Language `Select` lists all 20 with native labels; persist via i18n (existing)
- Missing keys fall back to English
