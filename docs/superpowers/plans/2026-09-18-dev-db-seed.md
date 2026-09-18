# Dev DB Seed Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Yarn-managed Node script under `needrelay-db/seed/` that wipe+seeds local Postgres with broad demo data for discovery and organizer flows.

**Architecture:** Standalone ESM package using `pg` for SQL and `bcryptjs` for Spring-compatible BCrypt password hashes. Hardcoded fixed UUIDs/slugs in `seed.mjs`. No backend involvement.

**Tech Stack:** Node 22+, Yarn 4, `pg`, `bcryptjs`, Postgres 16 (docker-compose)

**Spec:** `docs/superpowers/specs/2026-09-18-dev-db-seed-design.md`

## Global Constraints

- Yarn 4 only (`nodeLinker: node-modules`); no npm / package-lock.
- English only in code/comments/README.
- Manual run only — never auto-seed on backend start.
- Wipe destroys all app data in target DB; Liquibase tables untouched.
- Demo passwords only (≥8 chars for login UI): `Admin123!` / `Org1234!` (documented in README).
- If `git status` fails (no repo), skip commit steps.
- JSDoc on every new function in `seed.mjs`.

---

## File structure (locked)

| Path | Responsibility |
|------|----------------|
| `needrelay-db/seed/package.json` | scripts + deps |
| `needrelay-db/seed/.yarnrc.yml` | `nodeLinker: node-modules` |
| `needrelay-db/seed/seed.mjs` | wipe + insert + console summary |
| `needrelay-db/seed/README.md` | how to run + credentials + warning |
| `needrelay-db/seed/.gitignore` | `node_modules`, `.yarn/cache` if present |

---

### Task 1: Yarn package scaffold

**Files:**
- Create: `needrelay-db/seed/package.json`
- Create: `needrelay-db/seed/.yarnrc.yml`
- Create: `needrelay-db/seed/.gitignore`
- Create: `needrelay-db/seed/README.md` (stub; finalize in Task 2)

**Interfaces:**
- Produces: `yarn seed` → runs `node seed.mjs`

- [ ] **Step 1: Write `package.json`**

```json
{
  "name": "needrelay-db-seed",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "seed": "node seed.mjs"
  },
  "dependencies": {
    "bcryptjs": "^3.0.2",
    "pg": "^8.16.0"
  },
  "packageManager": "yarn@4.14.0"
}
```

Copy the exact `packageManager` string from `needrelay-frontend/package.json` (currently `yarn@4.14.0+sha512...`) so Corepack resolves the same Yarn.

- [ ] **Step 2: Write `.yarnrc.yml`**

```yaml
nodeLinker: node-modules
```

- [ ] **Step 3: Write `.gitignore`**

```
node_modules
.yarn/install-state.gz
```

- [ ] **Step 4: Stub README**

```markdown
# NeedRelay DB seed

**WARNING:** `yarn seed` deletes all application data in the target database, then inserts demo rows.

## Setup

Postgres must be running (`docker compose up -d` from `needrelay-db/`).

```bash
cd needrelay-db/seed
yarn install
yarn seed
```

(Credentials and connection env vars filled in Task 2.)
```

- [ ] **Step 5: Install deps**

```bash
cd needrelay-db/seed
yarn install
```

Expected: `node_modules/pg` and `node_modules/bcryptjs` present; yarn.lock created.

- [ ] **Step 6: Commit** (skip if no git)

```bash
git add needrelay-db/seed/package.json needrelay-db/seed/.yarnrc.yml needrelay-db/seed/.gitignore needrelay-db/seed/README.md needrelay-db/seed/yarn.lock
git commit -m "chore: scaffold Yarn seed package for local Postgres"
```

---

### Task 2: Implement wipe + seed + verify

**Files:**
- Create: `needrelay-db/seed/seed.mjs`
- Modify: `needrelay-db/seed/README.md` (full docs)

**Interfaces:**
- Consumes: env `DATABASE_HOST` (default `localhost`), `DATABASE_PORT` (`5432`), `DATABASE_NAME` (`needrelay`), `DATABASE_USER` (`needrelay`), `DATABASE_PASSWORD` (`needrelay`)
- Produces: wiped DB + seeded rows; stdout summary

- [ ] **Step 1: Implement `seed.mjs`**

Use fixed UUIDs (v4-style literals are fine). Hash passwords with `bcryptjs.hashSync(password, 10)` (compatible with Spring `BCryptPasswordEncoder`).

```js
import bcrypt from 'bcryptjs';
import pg from 'pg';

const { Client } = pg;

const IDS = {
  admin: '11111111-1111-1111-1111-111111111101',
  org: '11111111-1111-1111-1111-111111111102',
  contact1: '22222222-2222-2222-2222-222222222201',
  contact2: '22222222-2222-2222-2222-222222222202',
  activeA: '33333333-3333-3333-3333-333333333301',
  activeB: '33333333-3333-3333-3333-333333333302',
  archived: '33333333-3333-3333-3333-333333333303',
  needOpenA: '44444444-4444-4444-4444-444444444401',
  needPartialA: '44444444-4444-4444-4444-444444444402',
  needCoveredA: '44444444-4444-4444-4444-444444444403',
  needClosedA: '44444444-4444-4444-4444-444444444404',
  needOpenB: '44444444-4444-4444-4444-444444444405',
  needArchived: '44444444-4444-4444-4444-444444444406',
  offer: '55555555-5555-5555-5555-555555555501',
};

/**
 * Builds a Postgres client from env overrides or defaults.
 *
 * @returns {import('pg').Client}
 */
function createClient() {
  return new Client({
    host: process.env.DATABASE_HOST ?? 'localhost',
    port: Number(process.env.DATABASE_PORT ?? 5432),
    database: process.env.DATABASE_NAME ?? 'needrelay',
    user: process.env.DATABASE_USER ?? 'needrelay',
    password: process.env.DATABASE_PASSWORD ?? 'needrelay',
  });
}

/**
 * Deletes all application rows in FK-safe order.
 *
 * @param {import('pg').Client} client open client
 * @returns {Promise<void>}
 */
async function wipe(client) {
  const tables = [
    'offers',
    'relief_request_comments',
    'needs',
    'relief_requests',
    'organizer_contacts',
    'refresh_tokens',
    'password_reset_tokens',
    'invites',
    'organizers',
  ];
  for (const table of tables) {
    await client.query(`DELETE FROM ${table}`);
  }
}

/**
 * Inserts demo organizers, contacts, requests, needs, and one offer.
 *
 * @param {import('pg').Client} client open client
 * @returns {Promise<void>}
 */
async function seed(client) {
  const now = new Date().toISOString();
  const adminHash = bcrypt.hashSync('Admin123!', 10);
  const orgHash = bcrypt.hashSync('Org1234!', 10);

  await client.query(
    `INSERT INTO organizers (id, email, password_hash, display_name, description, role, active, created_at)
     VALUES ($1,$2,$3,$4,$5,'ADMIN',true,$6)`,
    [
      IDS.admin,
      'admin@needrelay.local',
      adminHash,
      'NeedRelay Admin',
      'Seeded admin account for local development.',
      now,
    ],
  );

  await client.query(
    `INSERT INTO organizers (id, email, password_hash, display_name, description, role, active, created_at)
     VALUES ($1,$2,$3,$4,$5,'ORGANIZER',true,$6)`,
    [
      IDS.org,
      'org@needrelay.local',
      orgHash,
      'Aleppo Relief Org',
      'Community organizers coordinating water and shelter needs.',
      now,
    ],
  );

  await client.query(
    `INSERT INTO organizer_contacts
      (id, organizer_id, name, role, phone, email, note, sort_order, created_at)
     VALUES
      ($1,$2,'Sara Khalil','Coordinator','+963-11-0000001','sara@example.local','Primary contact',0,$3),
      ($4,$2,'Omar Haddad','Logistics','+963-11-0000002','omar@example.local',NULL,1,$3)`,
    [IDS.contact1, IDS.org, now, IDS.contact2],
  );

  await client.query(
    `INSERT INTO relief_requests
      (id, organizer_id, title, description, location_label, latitude, longitude, public_slug, status, created_at, updated_at)
     VALUES
      ($1,$2,'Aleppo North Camp','Urgent water and shelter.','Aleppo North',36.2021,37.1343,'aleppo-north','ACTIVE',$3,$3),
      ($4,$2,'Damascus Hub','Blankets and food staging.','Damascus Center',33.5138,36.2765,'damascus-hub','ACTIVE',$3,$3),
      ($5,$2,'Old Site (archived)','No longer active.','Homs',34.7268,36.7234,'homs-archived','ARCHIVED',$3,$3)`,
    [IDS.activeA, IDS.org, now, IDS.activeB, IDS.archived],
  );

  await client.query(
    `INSERT INTO needs
      (id, relief_request_id, title, description, category, quantity_required, quantity_offered, unit, priority, status, version, created_at, updated_at)
     VALUES
      ($1,$2,'Water tanks',NULL,'WATER',100,0,'L','CRITICAL','OPEN',0,$8,$8),
      ($3,$2,'Tarps',NULL,'SHELTER',50,20,'pcs','HIGH','PARTIALLY_COVERED',0,$8,$8),
      ($4,$2,'Blankets (covered)',NULL,'CLOTHING',30,30,'pcs','NORMAL','COVERED',0,$8,$8),
      ($5,$2,'Old request (closed)',NULL,'OTHER',1,0,'x','LOW','CLOSED',0,$8,$8),
      ($6,$7,'Rice bags',NULL,'FOOD',200,0,'kg','HIGH','OPEN',0,$8,$8),
      ($9,$10,'Should hide from discovery',NULL,'FOOD',5,0,'kg','NORMAL','OPEN',0,$8,$8)`,
    [
      IDS.needOpenA,
      IDS.activeA,
      IDS.needPartialA,
      IDS.needCoveredA,
      IDS.needClosedA,
      IDS.needOpenB,
      IDS.activeB,
      now,
      IDS.needArchived,
      IDS.archived,
    ],
  );

  await client.query(
    `INSERT INTO offers (id, need_id, provider_name, quantity, contact, note, created_at)
     VALUES ($1,$2,'Local Donor',10,'donor@example.local','Partial water delivery',$3)`,
    [IDS.offer, IDS.needOpenA, now],
  );
}

/**
 * Entry point: wipe then seed, print summary.
 *
 * @returns {Promise<void>}
 */
async function main() {
  const client = createClient();
  await client.connect();
  try {
    await client.query('BEGIN');
    await wipe(client);
    await seed(client);
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    await client.end();
  }

  console.log('Seed complete (wipe + insert).');
  console.log('Admin:     admin@needrelay.local / Admin123!');
  console.log('Organizer: org@needrelay.local / Org1234!');
  console.log('Public:    /r/aleppo-north  /r/damascus-hub');
  console.log('Archived (hidden from discovery): /r/homs-archived');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
```

Confirm `offers` column names against `001-initial-schema.yaml` (`provider_name`, `quantity`, `contact`, `note`, `created_at`, `need_id`). Adjust INSERT if schema differs.

Also confirm `organizers` has no `updated_at` — schema only has `created_at` for organizers.

- [ ] **Step 2: Finalize README** with env vars, credentials table, wipe warning, discovery expectations (ACTIVE open/partial visible; archived/covered/closed not in discovery list).

- [ ] **Step 3: Run seed**

```bash
cd needrelay-db
docker compose up -d
cd seed
yarn seed
```

Expected: stdout with credentials; exit 0.

If backend is running, optionally:

```bash
curl -s http://localhost:8080/api/public/discovery
```

Expected JSON: 2 points (`aleppo-north`, `damascus-hub`); needs titles include Water tanks / Tarps / Rice bags; exclude archived/closed/covered.

- [ ] **Step 4: Commit** (skip if no git)

```bash
git add needrelay-db/seed/seed.mjs needrelay-db/seed/README.md
git commit -m "feat: add wipe-and-seed script for local Postgres demo data"
```

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| Yarn 4 + node-modules | 1 |
| Manual `yarn seed` | 1–2 |
| Wipe order | 2 |
| ADMIN + ORGANIZER + contacts | 2 |
| 2 ACTIVE + 1 ARCHIVED | 2 |
| OPEN/PARTIAL/COVERED/CLOSED + offer | 2 |
| Env overrides | 2 |
| README warning + credentials | 2 |

## Self-review notes

- No unit test harness for the script (verify by running against docker).
- If `offers` INSERT fails, re-read schema column list before inventing columns.
- Do not wipe `databasechangelog` / `databasechangeloglock`.
