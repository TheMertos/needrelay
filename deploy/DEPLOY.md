# Go live (emergency)

NeedRelay is meant to be stood up **fast** on one Linux VPS when a crisis starts. This folder is the whole production stack: TLS (Caddy), the app (UI + API), and Postgres. You do not need to build from source.

Public traffic is HTTPS on ports **80** and **443**. Postgres (`5432`) and the app (`8080`) stay on the host and must not be opened in the firewall.

Image: [`themertos/needrelay`](https://hub.docker.com/r/themertos/needrelay)

## You need

- A Linux VPS (Ubuntu or similar) with Docker Engine **and** the Compose plugin
- A domain name whose **A** record (and **AAAA** if you use IPv6) already points at this server
- About five minutes after DNS is live

Install Docker on Ubuntu if it is missing: https://docs.docker.com/engine/install/ubuntu/

Confirm:

```bash
docker compose version
```

## 1. DNS

Create an A record, for example `needrelay.example.com` → the VPS public IP. Wait until it resolves **before** you start Caddy, or Let's Encrypt will fail:

```bash
dig +short needrelay.example.com
```

## 2. Firewall

Allow SSH and HTTPS only. Do **not** open 8080 or 5432.

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

## 3. Configure

On the server:

```bash
git clone https://github.com/TheMertos/needrelay.git
cd needrelay/deploy
cp .env.example .env
nano .env
```

Set these before the first start (placeholders in `.env.example` are not safe):

| Variable | What to put |
| --- | --- |
| `DOMAIN` | Hostname only, e.g. `needrelay.example.com` |
| `CADDY_EMAIL` | Contact email for Let's Encrypt |
| `JWT_SECRET` | Long random string, at least 32 characters |
| `ADMIN_PASSWORD` | Password for the first admin account |
| `POSTGRES_PASSWORD` | Database password |
| `APP_PUBLIC_BASE_URL` | `https://` + the same host as `DOMAIN` |
| `CORS_ALLOWED_ORIGINS` | Same value as `APP_PUBLIC_BASE_URL` |

Optional: `RESEND_API_KEY` if invite and password-reset emails should be sent. Leave empty to skip mail.

Pin a release with `APP_IMAGE_TAG=v0.0.1`, or keep `latest`.

Generate a secret:

```bash
openssl rand -base64 48
```

## 4. Start

```bash
docker compose pull
docker compose up -d
docker compose ps
```

All three containers (`needrelay-postgres`, `needrelay-app`, `needrelay-caddy`) should be running. The app may take up to a minute on first boot (migrations).

## 5. Check and sign in

```bash
curl -fsS https://$DOMAIN/actuator/health/liveness
```

Open `https://your-domain` in a browser. Sign in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` from `.env`. **Change that password immediately.**

Then, under **Invites**, create tokens for other organizers. Registration is invite-only: `/register?invite=<token>`.

## 6. Update later

After a new image is published:

```bash
cd deploy
docker compose pull app
docker compose up -d app
```

## If it does not come up

| Symptom | What to do |
| --- | --- |
| Let's Encrypt / HTTPS fails | DNS is not pointing here yet, or ports 80/443 are closed. Fix DNS, wait, then `docker compose restart caddy`. |
| App never becomes healthy | `docker compose logs app`. Usual causes: wrong DB password, or 8080 already in use. |
| `docker compose pull` cannot fetch the image | The host needs outbound HTTPS to Docker Hub. |
| Site loads but login fails | Confirm you are on `https://` + `DOMAIN`, and that `CORS_ALLOWED_ORIGINS` matches. |

Logs:

```bash
docker compose logs -f
```
