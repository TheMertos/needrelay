# Emergency production deploy

Bring NeedRelay online on a single Linux VPS with Docker. All application traffic goes through Caddy on ports 80/443. Postgres and the app listen on the host loopback/LAN ports but must **not** be opened in the firewall.

## Architecture

- **Caddy** — TLS (Let's Encrypt) + reverse proxy to `127.0.0.1:8080`
- **App** — `themertos/needrelay` (UI + API in one image)
- **Postgres** — `127.0.0.1:5432`
- All services use `network_mode: host` and `restart: always`

## Prerequisites

- Ubuntu (or similar) VPS with a public IPv4/IPv6
- Docker Engine + Docker Compose plugin
- A DNS name pointing at the server (A/AAAA)

## 1. DNS

Create an **A** (and optionally **AAAA**) record for your domain, for example `needrelay.example.com` → server IP. Wait until it resolves before starting Caddy so Let's Encrypt can succeed.

## 2. Firewall (UFW)

Allow only SSH and HTTP(S). Do **not** open 5432 or 8080.

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

## 3. Configure environment

```bash
cd deploy
cp .env.example .env
nano .env   # set DOMAIN, CADDY_EMAIL, JWT_SECRET, admin password, DB password
```

Set at least:

| Variable | Purpose |
| --- | --- |
| `DOMAIN` | Public hostname (no scheme), e.g. `needrelay.example.com` |
| `CADDY_EMAIL` | ACME contact email for Let's Encrypt |
| `JWT_SECRET` | Long random string (≥ 32 characters) |
| `ADMIN_PASSWORD` | Bootstrap admin password (change after first login) |
| `POSTGRES_PASSWORD` | Database password |
| `APP_PUBLIC_BASE_URL` | `https://your.domain` (must match `DOMAIN`) |
| `CORS_ALLOWED_ORIGINS` | Same origin as `APP_PUBLIC_BASE_URL` |

## 4. Start

```bash
cd deploy
docker compose pull
docker compose up -d
docker compose ps
```

## 5. Verify

```bash
curl -fsS https://$DOMAIN/actuator/health/liveness
# Open https://$DOMAIN in a browser and sign in with the admin credentials from .env
```

## 6. Update to a new image

After CI publishes a new `themertos/needrelay` tag or `latest`:

```bash
cd deploy
docker compose pull app
docker compose up -d app
```

## GitHub Actions → Docker Hub

The workflow `.github/workflows/docker-publish.yml` builds the root `Dockerfile` and pushes:

- `themertos/needrelay:latest` on pushes to `main`
- `themertos/needrelay:<version>` (and `latest`) on tags `v*`

Repository secrets required:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN` (Docker Hub access token)

## Notes

- Host networking means Compose `ports:` mappings are unused; processes bind host ports directly.
- Keep `.env` off git; only `.env.example` is committed.
- For a local all-in-one build without Caddy, use the repo-root `docker-compose.app.yml` instead of this folder.
