# Offer lifecycle: PENDING → COMING → RECEIVED

**Date:** 2026-09-18  
**Status:** Approved for implementation

## Goal

Public offers no longer fulfill a need immediately. Helpers submit an **expected** quantity as **PENDING**. Organizers move offers to **COMING**, then **RECEIVED** with the **actual** quantity (may be higher or lower). Public shows pending (expected in flight) separately; **remaining** uses only received amounts and never goes below 0.

## Offer states

| Status | Who | Effect on need totals |
| --- | --- | --- |
| `PENDING` | set on create | counts toward **pending** only |
| `COMING` | organizer | still **pending** (expected qty) |
| `RECEIVED` | organizer + actual qty | counts toward **received / offered coverage** |

## Data

- `offers.status` — `PENDING` \| `COMING` \| `RECEIVED` (NOT NULL, default PENDING)
- `offers.quantity` — expected quantity from helper (unchanged meaning)
- `offers.quantity_received` — nullable; set when RECEIVED
- `needs.quantity_offered` — sum of **received** quantities only (rename in API display as “received/offered toward need”; keep column name for migration simplicity)
- Drop or relax DB check that forbids `quantity_offered > quantity_required` so surplus receipts are allowed
- `Need.remaining()` / API `remaining` = `max(0, quantityRequired − quantityOffered)`
- API adds `quantityPending` on need responses = sum of `quantity` for offers in PENDING+COMING on that need

## API

- `POST /api/public/needs/{id}/offers` — creates PENDING; does **not** bump `quantity_offered`
- Organizer: `POST /api/relief-requests/{id}/offers/{offerId}/coming` (PENDING → COMING)
- Organizer: `POST /api/relief-requests/{id}/offers/{offerId}/received` body `{ quantityReceived }` (COMING or PENDING → RECEIVED); recomputes need totals from all RECEIVED offers
- Existing update/delete: adjust so only RECEIVED offers affect `quantity_offered`; deleting RECEIVED subtracts `quantity_received`

## UI

- Public need row: Required, **Pending** (expected), **Received/Offered**, **Remaining** (≥ 0)
- Manage Offer inbox: status badge; actions **Mark coming**, **Mark received** (modal for actual qty)
- Helper still enters expected quantity on create

## Out of scope

- Helper editing after submit
- Reject/cancel status (organizer can delete for now)
- Notifying helpers of status changes
