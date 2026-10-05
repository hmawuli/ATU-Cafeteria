# System Handover — ATU Cafeteria

One page so any maintainer knows what this is, how to run it, and what to
do before it serves real students.

## What this system is

A campus-wide **cashless food-ordering platform**: Flutter app (students +
vendor terminals + admin), Laravel 11 REST API, PostgreSQL as the source of
truth. One wallet, audited payments, real-time order flow, offline resilience.

## Current status (verified)

- **Backend:** 103 feature/unit tests on **SQLite and PostgreSQL**, Pint clean.
- **Frontend:** 60 tests, `flutter analyze` clean, contract check green.
- **E2E:** a single HTTP journey test (register → login → order → vendor →
  pickup) passes on both databases.
- **Deploy path:** `scripts/deploy_docker.sh` (one command) + `docs/DEPLOY_VPS.md`.
- **Running now (local):** the API serves `http://0.0.0.0:8000` — docs at
  `/api/docs`, live stall board at `/stalls`, health at `/api/health`.

## How to run it (daily)

```bash
make seed-dev     # migrate + seed dev data into local PostgreSQL
make serve        # API on 0.0.0.0:8000
make smoke        # verify health + catalogue
cd frontend && flutter run -d <device>      # the app
make check        # full CI-style gate
```

## Operational accounts (created locally)

| Role | Username | PIN | Note |
|---|---|---|---|
| SUPER_ADMIN | `headadmin` | `Hd@2026!Cafeteria` | Change/disable before public |
| Vendor | `campusdelight` | `CD#K1tchen2026` | Campus Delight |
| Vendor | `quickbites` | `QB#Snacks2026` | Quick Bites |

> The older demo accounts (`superadmin`, `admin`, `testvendor1/2`) exist only
> for local demos — `php artisan security:audit --fail-on-critical` flags them
> as expected.

## Who does what

| Area | Owner | Reference |
|---|---|---|
| Code, builds, CI | maintainer (repo `hmawuli/ATU-Cafeteria`) | `CONTRIBUTING.md`, `RELEASING.md` |
| Servers & deployment | maintainer + any host admin | `DEPLOYMENT_CHECKLIST.md` |
| Day-to-day ops (vendors, payments, audits) | appointed admin | `STAFF_QUICKSTART.md` |
| Support & UAT | campus admin + maintainer | `UAT_CHECKLIST.md` |

## Before going public (non-negotiable)

1. Deploy on a Docker host with the compose plugin: `APP_ENV=production bash scripts/deploy_docker.sh`.
2. Real credentials → `make release-build`: the 4 `ATU_FIREBASE_*` values,
   a Paystack production secret (`PAYSTACK_DEMO_MODE=false`), a Play keystore.
3. Real people: `vendor:create` each stall, `admin:create --super --enable-2fa`
   for the real admin; load the real menu/prices; brief staff.
4. Prove on a phone: `docs/UAT_CHECKLIST.md` + `flutter test integration_test`.
5. Operate: uptime monitor on `/api/health`, daily `pg_dump` with a tested
   restore, an admin reviewing `audit_logs` daily.

## Where everything lives

`docs/` (API standards, data flow, observability, performance budget, local
PostgreSQL, standout features, mobile polish, disaster/convention docs),
`scripts/` (deploy, seed, smoke, load, backup, hooks), `backend/` + `frontend/`
(the two applications), `.github/workflows/` (CI + signed release).

## Failed builds / incidents

- Start with `make smoke` and `docker compose logs app|nginx|db`.
- Queue backlog: `docker compose logs worker`; `failed_jobs` table.
- Security concerns: `php artisan security:audit`, then `audit_logs`.
- See `docs/OBSERVABILITY.md` runbooks.