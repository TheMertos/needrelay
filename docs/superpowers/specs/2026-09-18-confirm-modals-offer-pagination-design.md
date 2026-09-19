# Confirm modals + paginated offer inbox

**Date:** 2026-09-18  
**Status:** Approved  

## Goals

1. Confirm modal before destructive actions (delete, cancel, close need, ban, delete contact).
2. Manage layout: public offers **under** needs (single column).
3. Organizer offer list: server pagination + sort (scale for large volume).

## Locked decisions

| Topic | Choice |
| --- | --- |
| Confirm UI | Shared `ConfirmModal` (Cancel + confirm) |
| Layout | Stack: needs then offers |
| Pagination | `page` (0-based), `size` default **25** |
| Sort | `createdAt,desc` (default), `createdAt,asc`, `status,asc`, `quantity,desc` |
| API shape | `{ items, page, size, totalElements, totalPages }` |

## API

`GET /api/relief-requests/{requestId}/offers?page=0&size=25&sort=createdAt,desc`

## Non-goals

- Discovery/public map pagination (separate later)
- Infinite scroll
- Client-only paging of full offer lists
