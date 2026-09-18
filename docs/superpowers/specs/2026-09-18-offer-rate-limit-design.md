# Public offer rate limit

**Date:** 2026-09-18  
**Status:** Approved for implementation  

## Goal

Reduce spam/abuse on anonymous public offer creation without blocking legitimate helpers.

## Locked decisions

| Topic | Choice |
| --- | --- |
| Endpoint | `POST /api/public/needs/{needId}/offers` only |
| Key | Client IP (`ClientIpResolver`) |
| Default limit | **10 attempts / 10 minutes** |
| Config | `needrelay.offer-rate-limit.max-attempts` / `window-seconds` |
| On exceed | HTTP **429**, code `OFFER_RATE_LIMITED` |
| Storage | In-memory sliding window (same approach as login) |

## Implementation notes

- Prefer extracting a small reusable sliding-window helper used by login + offers, **or** a dedicated `OfferRateLimiter` mirroring `LoginRateLimiter` (YAGNI: dedicated class is fine if extraction is noisy).
- Wire check in `PublicController` before `offerService.createPublicOffer`.
- Unit test for limiter; optional controller test for 429.

## Non-goals

- Per-need-id keys, CAPTCHA, Redis, relief-request create limits (separate if needed later).
