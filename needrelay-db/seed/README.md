# NeedRelay DB seed

**WARNING:** Seed deletes all application data in the target database, then inserts demo rows. Liquibase changelog tables are not touched. Wait until the app has started once so migrations exist.

## Setup

Against a running Postgres (Yarn 4):

```bash
cd needrelay-db
docker compose up -d
cd seed
yarn install
yarn seed
```

Against the published-image stack (on demand; Postgres is `localhost:5433`):

```bash
cd local-test
docker compose up -d
docker compose --profile seed run --rm seed
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

After seed, `GET /api/public/discovery` should list **2** ACTIVE points and needs that are `OPEN` or `PARTIALLY_COVERED` only (Water tanks + Tarps on Aleppo, Rice bags on Damascus). Covered, closed, and archived-request needs must not appear. Water tanks has a pending sample offer and stays `OPEN`; Tarps is `PARTIALLY_COVERED`.
