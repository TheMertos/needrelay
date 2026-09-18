# Organization description + contacts

**Date:** 2026-09-18  
**Status:** Approved for implementation (approach 1)

## Goal

Organizers maintain an organization **description** and a list of **contacts** (name, role, phone, email, optional note) on `/account`. The same data appears on the **public relief page** for that organizer’s requests.

## Data

- `organizers.description` — nullable text
- Table `organizer_contacts`: id, organizer_id, name, role, phone, email, note (nullable), sort_order, created_at

## API

- Extend profile: `description` on `OrganizerResponse` / `UpdateProfileRequest`
- CRUD: `GET/POST /api/me/contacts`, `PUT/DELETE /api/me/contacts/{id}`
- Public: `PublicReliefRequestResponse` includes `organizationName`, `organizationDescription`, `contacts[]`

## UI

- Account: description textarea + contact list add/edit/delete (`SurfaceCard`)
- Public: org block with description + contact cards
