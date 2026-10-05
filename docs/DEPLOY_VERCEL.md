# Deploying the API to Vercel (container)

Vercel runs the Laravel API as a **container image function**: it detects
[`Dockerfile.vercel`](../Dockerfile.vercel) at the repository root, builds the
image, and routes HTTP traffic to it on `$PORT` (default `80`). Nginx and
PHP-FPM run together inside that one image.

> This guide is a **deliberate compromise**. Vercel is designed for stateless,
> request-scoped workloads. The queue worker, scheduler and database backups
> that `docker-compose.yml` provides do **not** run on Vercel. See
> [Limitations](#limitations-and-what-runs-where) before choosing this path.
>
> Hosting the **Flutter web client** on Vercel (as static files) is covered
> separately in [`DEPLOY_VERCEL_WEB.md`](DEPLOY_VERCEL_WEB.md).

## Architecture on Vercel

```text
Flutter app ──HTTPS──▶ Vercel container function (Nginx + PHP-FPM + Laravel)
                             │
                             └──▶ Managed PostgreSQL (Neon / Supabase / …)
```

- The container filesystem is ephemeral, so Laravel writes its runtime storage
  to `/tmp` (`LARAVEL_STORAGE_PATH`). Nothing there survives a cold start, which
  is why the shared cache lives in PostgreSQL rather than on disk.
- `TRUSTED_PROXIES=*` makes Laravel read Vercel's `X-Forwarded-Proto` so the
  `EnsureSecureTransport` middleware sees HTTPS and `APP_URL` links are correct.

## Prerequisites

1. A **GitHub-connected Vercel project** for `hmawuli/ATU-Cafeteria`.
2. A **managed PostgreSQL** database (e.g. [Neon](https://neon.tech)). Vercel does
   not host PostgreSQL.
3. The backend running locally so you can generate a key and run migrations.

## 1. Create the database

Create a project on Neon (or Supabase). Copy the connection values:

```text
host      = ep-xxx.<region>.aws.neon.tech   # use the -pooler host for serverless
port      = 5432
database  = atu_cafeteria
username  = <user>
password  = <password>
sslmode   = require
```

## 2. Generate the application key

The key must be stable across instances, so generate it once locally and store
it in Vercel — never let the container generate it per-instance:

```bash
cd backend
php artisan key:generate --show      # prints base64:…
```

## 3. Import the project on Vercel

Use your existing import link (or **Add New → Project → Import Git Repository**):

- **Framework Preset:** `Other` (Vercel auto-detects `Dockerfile.vercel`).
- **Root Directory:** leave it as the **repository root**. Do **not** set it to
  `backend` — the Dockerfile and build context are at the root by design.
- **Build & Output Settings:** leave defaults.

## 4. Set environment variables

Add these under **Project → Settings → Environment Variables** (Production,
and Preview if you want preview deploys to work):

| Variable | Value |
|---|---|
| `APP_NAME` | `ATU Cafeteria` |
| `APP_ENV` | `production` |
| `APP_KEY` | `base64:…` from step 2 |
| `APP_DEBUG` | `false` |
| `APP_URL` | `https://atu-cafeteria-backend.vercel.app` |
| `APP_TIMEZONE` | `Africa/Accra` |
| `LOG_CHANNEL` | `stderr` |
| `LOG_LEVEL` | `warning` |
| `DB_CONNECTION` | `pgsql` |
| `DB_HOST` | Neon host (prefer the `-pooler` host) |
| `DB_PORT` | `5432` |
| `DB_DATABASE` | `atu_cafeteria` |
| `DB_USERNAME` | Neon user |
| `DB_PASSWORD` | Neon password |
| `DB_SSLMODE` | `require` |
| `CACHE_STORE` | `database` (shared cache — see the warning below) |
| `SESSION_DRIVER` | `array` |
| `QUEUE_CONNECTION` | `sync` |
| `FILESYSTEM_DISK` | `local` |
| `LARAVEL_STORAGE_PATH` | `/tmp/atu-storage` |
| `TRUSTED_PROXIES` | `*` |
| `CORS_ALLOWED_ORIGINS` | your Flutter web origin, e.g. `https://atu-cafeteria.vercel.app` |
| `PAYSTACK_SECRET_KEY` | live or test secret |
| `PAYSTACK_PUBLIC_KEY` | matching public key |
| `PAYSTACK_BASE_URL` | `https://api.paystack.co` |
| `PAYSTACK_DEMO_MODE` | `false` for real payments |
| `MAIL_MAILER`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_ENCRYPTION`, `MAIL_FROM_ADDRESS` | your SMTP provider |

Do **not** set `MIGRATE_ON_START` on a normal deploy — run migrations once,
manually (next step), so multiple cold starts cannot race each other.

> **Cache must be shared (`CACHE_STORE=database`), not `array` or `file`.**
> Authentication rate limiting (`throttle:auth`), payment throttling
> (`throttle:payments`) and the idempotency middleware for orders and payments
> all use `Cache::get` / `Cache::lock`. Those must be visible to every worker
> and every instance; an `array` store is per-process and would let concurrent
> requests double-charge or bypass login throttling. The `cache` and
> `cache_locks` tables are created by the migration in step 5. A managed Redis
> (e.g. Upstash) works too if you add `predis/predis`; the database store needs
> no extra dependency.

## 5. Run migrations (once, from your machine)

Migrations are schema changes and must not run on every container boot. Point
the local Artisan CLI at the managed database:

```bash
cd backend
DB_CONNECTION=pgsql \
DB_HOST=<neon-host> DB_PORT=5432 \
DB_DATABASE=atu_cafeteria DB_USERNAME=<user> DB_PASSWORD=<password> DB_SSLMODE=require \
APP_KEY='base64:…' \
php artisan migrate --force --no-interaction
```

Create the first administrator the same way:

```bash
DB_CONNECTION=pgsql DB_HOST=<neon-host> DB_PORT=5432 \
DB_DATABASE=atu_cafeteria DB_USERNAME=<user> DB_PASSWORD=<password> DB_SSLMODE=require \
APP_KEY='base64:…' \
php artisan admin:create headadmin --pin '<strong-pin>' --super --enable-2fa
```

## 6. Deploy and verify

Push to `main` (or click **Redeploy**). Then:

```bash
curl -fsS https://atu-cafeteria-backend.vercel.app/api/health
curl -fsS https://atu-cafeteria-backend.vercel.app/api/catalog/food-items
```

Both should return JSON with `"status":"UP"` / a catalogue payload.

## Will it hold up?

For a single-campus workload (hundreds to a few thousand students), the API
itself handles fine: each instance runs up to 10 PHP-FPM workers and Vercel adds
instances as traffic grows. Keep an eye on these when you scale:

- **Shared-state correctness** — the reason `CACHE_STORE` must be `database`
  (or Redis). Without it, rate limiting and idempotency break. This is the one
  thing that is not "just performance".
- **Cold starts** — an idle instance is reclaimed after ~5 minutes; the next
  request pays Nginx + PHP-FPM + Laravel + (Neon) startup. A periodic health
  ping keeps one instance warm if the latency matters.
- **Database connections** — every instance opens a small pool. Use Neon's
  pooled (`-pooler`) host when concurrency rises. If you ever see
  *"prepared statement already exists"*, use Neon's direct host or disable
  server-side prepares.
- **Request duration** — the container function is capped at `maxDuration`
  (default 300s). Heavy exports or report endpoints must finish inside it.
- **Region** — pick the Vercel region closest to your users (Europe is nearest
  to Accra; `iad1` adds ~100ms+ round-trips).

## Limitations and what runs where

| Capability | On Vercel | Recommended replacement |
|---|---|---|
| HTTP API | ✅ Runs | — |
| Queue worker (`queue:work`) | ❌ No always-on process | `QUEUE_CONNECTION=sync` today (all notifications are synchronous); add Vercel Queues or a small worker on Railway/Fly if you introduce queued jobs |
| Scheduler (`cleanup:database`, `database:backup`) | ❌ No `schedule:work` | Vercel Cron calling a guarded route, or an external cron host |
| Database backups | ❌ No `pg_dump` + ephemeral disk | Use Neon's built-in PITR/branching, or run `scripts/db_backup.sh` on a worker host |
| Persistent uploads | ❌ Ephemeral `/tmp` | Add object storage (S3/Blob) before storing files |
| Logs | ✅ stdout/stderr in Vercel logs | — |
| Cold starts | ⚠️ ~after 5 min idle | Manageable; Neon has its own cold start |

Also note: **Secure Compute and Static IPs are not supported** with custom
container images, and function requests are capped at the function's
`maxDuration` (default 300s).

## Updating a deployment

Any push to the connected branch rebuilds the image and rolls out a new
version. If a release includes migrations, run step 5 **before or immediately
after** the deploy — never rely on `MIGRATE_ON_START` for normal releases.

## Security checklist for this deployment

- `APP_KEY` is set and stable; `APP_DEBUG=false`.
- `PAYSTACK_DEMO_MODE=false` with a live secret once you go live.
- `CORS_ALLOWED_ORIGINS` lists only your real frontend origins.
- Run the security gate against the production database before launch:
  ```bash
  DB_CONNECTION=pgsql DB_HOST=<host> DB_PORT=5432 DB_DATABASE=atu_cafeteria \
  DB_USERNAME=<user> DB_PASSWORD=<password> DB_SSLMODE=require APP_KEY='base64:…' \
  php artisan security:audit --fail-on-critical
  ```
