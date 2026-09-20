# Production

One VPS, Docker Compose, TLS via Caddy. Traffic: `80/443` → Caddy → app `:8080` → Postgres `:5432`. Do not expose 8080 or 5432.

**Requirements:** Linux host, Docker Compose plugin, DNS A/AAAA for your domain.

```bash
cd deploy
cp .env.example .env   # set DOMAIN, CADDY_EMAIL, JWT_SECRET, ADMIN_PASSWORD, POSTGRES_PASSWORD
docker compose pull
docker compose up -d
```

`APP_PUBLIC_BASE_URL` and `CORS_ALLOWED_ORIGINS` must be `https://<DOMAIN>`. All variables are listed in `.env.example`.

Firewall: allow `22`, `80`, and `443` only.

Health check: `curl -fsS https://<DOMAIN>/actuator/health/liveness`

Image [`themertos/needrelay`](https://hub.docker.com/r/themertos/needrelay): `latest`, or pin with `APP_IMAGE_TAG=v0.0.1`.

```bash
cd deploy
docker compose pull app && docker compose up -d app
```
