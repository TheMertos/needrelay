# OSS emergency deploy design

**Date:** 2026-09-18  
**Status:** Approved for implementation  

## Goal

Prepare NeedRelay as a public GitHub repository that can be brought online quickly in an emergency: MIT license, Docker Hub image publish via CI, and a host-network production stack behind Caddy with minimal firewall exposure.

## Locked decisions

| Topic | Choice |
| --- | --- |
| License | MIT (Copyright Mert Yagci and contributors) |
| Docker image | `themertos/needrelay` |
| Domain config | `DOMAIN` environment variable (optional `www` → apex) |
| Layout | Separate `deploy/` for production; keep root `docker-compose.app.yml` for local/dev |
| Networking | `network_mode: host` for Caddy, app, Postgres |
| Restart | `restart: always` |
| Firewall | UFW allow only TCP 22, 80, 443 |
| Docs language | English |
| Server auto-deploy | Out of scope (image pull on the host) |
| Release script | `.\scripts\release.ps1` with no parameters (auto patch bump) |

## Architecture

```text
Internet --:80/:443--> Caddy --127.0.0.1:8080--> App --127.0.0.1:5432--> Postgres
UFW: 22, 80, 443 only (5432 and 8080 not published to WAN)
```

## Components

### Open source

- Root `LICENSE` (MIT)
- README: license, structure, emergency deploy link, screenshots section, releasing note

### `deploy/`

- `docker-compose.yml` — caddy, app (`themertos/needrelay`), postgres; host network; always restart
- `Caddyfile` — TLS for `{$DOMAIN}`, reverse proxy to `127.0.0.1:8080`
- `.env.example` — domain, secrets, DB, public URLs
- `DEPLOY.md` — DNS, UFW, env, compose up, verify, update

### CI

- `.github/workflows/docker-publish.yml`
- Push `main` → `themertos/needrelay:latest`
- Tag `v*` → `themertos/needrelay:<version>` (+ latest)
- Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`
- Gate: backend unit tests before image build

### Release script

- `scripts/release.ps1`: `git add -A`, commit `Release vX.Y.Z`, annotated tag, push branch + tag
- Version: latest `v*` semver patch + 1; if none, `v0.1.0`

## Non-goals

- Creating Docker Hub / GitHub secrets for the operator
- Real screenshot assets (placeholders only)
- CONTRIBUTING / CODE_OF_CONDUCT / SECURITY.md (v1)
- Major/minor bump flags on the release script
- Application code changes for networking
