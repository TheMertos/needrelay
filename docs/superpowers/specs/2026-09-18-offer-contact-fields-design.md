# Offer contact: first name, last name, phone, email

**Date:** 2026-09-18  
**Status:** Approved for implementation (approach 1)

## Goal

Public “I can provide this” and organizer offer edit require **first name, last name, phone, and email** so organizers can reach the offerer. Replace unstructured `contact` with dedicated columns.

## Data

Liquibase `005-offer-contact-fields.yaml`:

- Drop `offers.contact`
- Add NOT NULL: `first_name` (varchar 100), `last_name` (varchar 100), `phone` (varchar 80), `email` (varchar 320)

## API

- `CreateOfferRequest` / `UpdateOfferRequest` / `OfferResponse`: `firstName`, `lastName`, `phone`, `email` (no `contact`)
- Validation: all `@NotBlank`; email `@Email` + max 320

## UI

- Public offer modal + manage offer edit: four required fields
- Offer inbox: show name + phone + email
- i18n keys for the new labels
- Seed script + OpenAPI/Orval updated

## Out of scope

- Optional fields; keeping legacy `contact` column
