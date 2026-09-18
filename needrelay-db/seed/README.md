# NeedRelay DB seed

**WARNING:** `yarn seed` deletes all application data in the target database, then inserts demo rows. Liquibase changelog tables are not touched.

## Setup

Postgres must be running:

```bash
cd needrelay-db
docker compose up -d
```

Install and seed (Yarn 4 only):

```bash
cd needrelay-db/seed
yarn install
yarn seed
```

## Connection

| Variable | Default |
| --- | --- |
| `DATABASE_HOST` | `localhost` |
| `DATABASE_PORT` | `5432` |
| `DATABASE_NAME` | `needrelay` |
| `DATABASE_USER` | `needrelay` |
| `DATABASE_PASSWORD` | `needrelay` |

## Demo credentials

| Role | Email | Password |
| --- | --- | --- |
| ADMIN | `admin@needrelay.local` | `Admin123!` |
| ORGANIZER | `org@needrelay.local` | `Org1234!` |

Public slugs: `/r/aleppo-north`, `/r/damascus-hub`  
Archived (hidden from discovery): `/r/homs-archived`

## Discovery expectations

After seed, `GET /api/public/discovery` should list **2** ACTIVE points and needs that are `OPEN` or `PARTIALLY_COVERED` only (Water tanks + Tarps on Aleppo, Rice bags on Damascus). Covered, closed, and archived-request needs must not appear. Water tanks has a sample offer and is `PARTIALLY_COVERED`.
