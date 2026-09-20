# Local test (published image)

Runs Postgres and the [`themertos/needrelay`](https://hub.docker.com/r/themertos/needrelay) image on your machine. Demo seed is optional.

Host ports: app **8081**, Postgres **5433** (so this stack can run next to local Spring on 8080 and `needrelay-db` on 5432).

## Start

```bash
cd local-test
docker compose pull
docker compose up -d
```

Open [http://localhost:8081](http://localhost:8081).

Empty database (no seed): `admin@needrelay.local` / `ChangeMeAdmin123!`.

Pin a version with `APP_IMAGE_TAG` in `.env` (copy from `.env.example`), e.g. `v0.0.1`.

## Optional seed

Wipes application tables (not Liquibase) and inserts demo rows. Run after the app is healthy so migrations exist:

```bash
docker compose --profile seed run --rm seed
```

After seed:

| Role | Email | Password |
| --- | --- | --- |
| ADMIN | `admin@needrelay.local` | `Admin123!` |
| ORGANIZER | `org@needrelay.local` | `Org1234!` |

Public pages: `/r/aleppo-north`, `/r/damascus-hub`. Archived (hidden from discovery): `/r/homs-archived`.

You can run seed again whenever you want a clean demo dataset.

## Stop

```bash
docker compose down
```

Add `-v` to drop the local Postgres volume.
