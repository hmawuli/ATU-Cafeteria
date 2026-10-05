# 🍽️ ATU Cafeteria Management System

> Production-ready **Flutter + Laravel + PostgreSQL** platform for Accra
> Technical University cafeteria operations.

![CI](https://github.com/hmawuli/ATU-Cafeteria/actions/workflows/flutter.yml/badge.svg)
[![OpenAPI](https://img.shields.io/badge/API-OpenAPI%203.0-6b21a8)](https://github.com/hmawuli/ATU-Cafeteria)
[![PostgreSQL](https://img.shields.io/badge/database-PostgreSQL-336791)](https://github.com/hmawuli/ATU-Cafeteria)

Production-oriented Flutter + Laravel implementation for Accra Technical University cafeteria operations.

## Technology

| Layer | Technology |
|---|---|
| Mobile frontend | Flutter / Dart |
| State management | Provider |
| Local cache | SQLite / sqflite |
| Primary database | PostgreSQL |
| Backend | Laravel 11 / PHP 8.2+ |
| API | REST / JSON |
| Authentication | Laravel-issued bearer token |
| Payments | Paystack integration |
| Analytics | Laravel services + native Flutter charts |

## Repository structure

```text
ATU-Cafeteria/
├── frontend/          # Flutter application — active client
├── backend/           # Laravel REST API (+ Docker image for self-hosting)
├── scripts/           # setup/utility scripts
├── docs/              # architecture, deployment, standards and operations docs
├── .githooks/         # versioned git hooks (pre-commit / pre-push)
├── Makefile           # developer task runner (make help)
├── docker-compose.yml # self-hosted backend stack (Laravel + PostgreSQL + Nginx)
├── CHANGELOG.md       # release history
└── .vscode/           # lightweight editor settings
```

## Key documents

| Purpose | Document |
|---|---|
| Payment gateway setup (wallet + Paystack, demo↔live) | `docs/PAYMENTS_SETUP.md` |
| Student data rights (export & erasure) | `docs/DATA_RIGHTS.md` |
| UI design system & token rules | `docs/DESIGN_SYSTEM.md` |
| One-page system state & owners (handover) | `docs/HANDOVER.md` |
| Industrial-standard readiness matrix | `docs/INDUSTRIAL_READINESS.md` |
| Stand-out features (open-now board, QR menus, scheduling, badges, streaks, receipts) | `docs/STANDOUT_FEATURES.md` |
| How data flows between pages & the API (conventions) | `docs/DATA_FLOW.md` |
| OpenAPI contract + generated Dart client (`make api-gen`) | `docs/openapi.json`, `frontend/lib/generated/atu_api.dart` |
| Local PostgreSQL setup & credentials | `docs/LOCAL_POSTGRES.md` |
| API conventions (envelope, errors, idempotency, rate limits) | `docs/API_STANDARDS.md` |
| Interactive API docs (served live) | `/api/docs` on the running backend |
| Deploy to a Linux VPS (Nginx + PHP-FPM + PostgreSQL) | `docs/DEPLOY_VPS.md` |
| Containerised deployment (incl. queue worker + scheduler) | `docker-compose.yml` + `make docker-up` |
| Logging, health checks, monitoring, crash reporting | `docs/OBSERVABILITY.md` |
| Performance baselines & budgets | `docs/PERFORMANCE_BUDGET.md` |
| Accessibility audit & offline-order design | `docs/MOBILE_POLISH.md` |
| Manual release test pass | `docs/UAT_CHECKLIST.md` |
| Staff & admin operations guide | `docs/STAFF_QUICKSTART.md` |
| Rollout/release checklist (A–F) | `docs/DEPLOYMENT_CHECKLIST.md` |
| Release & versioning process | `RELEASING.md` |
| Contribution guide | `CONTRIBUTING.md` |

## Development philosophy

The project is intentionally optimized for a machine with 4 GB RAM. Android Studio and an Android emulator are not required. VS Code, the Laravel server and a physical Android phone are recommended for low-resource development.

The Laravel API is the production source of truth. Flutter SQLite is used for local resilience and offline reads.

## Quick start

Two supported paths — the `Makefile` (recommended) or the raw setup script.

**Option A — `make` (recommended):**

```bash
make setup          # git hooks + PHP/Flutter dependencies + .env files
make seed-dev       # migrate + seed development vendors and restaurant catalog
make serve          # Laravel on 0.0.0.0:8000 (reachable by a phone)
```

Then run the Flutter app from `frontend/` (`flutter run`).

**Option B — setup script:**

```bash
./scripts/setup_4gb_linux.sh
```

Then start Laravel (`make serve` or `scripts/serve_backend.sh`) and seed the
development catalog (`make seed-dev`). See `docs/PRODUCTION_RUNBOOK.md` for
the detailed runbook and `docs/SETUP_SMOOTHLY.md` for the 4 GB RAM workflow.

### Developer workflow

- `make help` lists every task; `make test`, `make lint`, `make analyze`,
  `make format` and `make check` cover the quality gates.
- `make setup` installs versioned git hooks (`.githooks/`): a fast
  pre-commit check on staged files and a full pre-push health gate.
  See `CONTRIBUTING.md`.
- Development data is seeded on demand via `make seed-dev` — it is never part
  of `DatabaseSeeder` and refuses to run against a production environment.

## API endpoint configuration

Do not hard-code a deployment URL in source code. Supply it at runtime/build time:

```bash
flutter run --dart-define=API_BASE_URL=https://your-api.example.com/api/
```

### Running on a physical phone

`127.0.0.1` inside the app means **the phone itself**, never your computer. Pick one mode:

1. **USB (recommended, zero configuration)** — tunnel over the cable:
   ```bash
   scripts/serve_backend.sh
   scripts/connect_phone.sh
   flutter run
   ```
   The app's default `http://127.0.0.1:8000` then reaches your computer over USB.

2. **Same Wi-Fi** — set the address once in `lib/core/config/app_config.dart` and start the backend with `scripts/serve_backend.sh`.

3. **Android emulator** — pass the emulator loopback alias at run time:
   ```bash
   flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8000
   ```

Precedence: `--dart-define` → `staticApiHost` → `127.0.0.1:8000`.
Android development builds also need cleartext HTTP when using a local HTTP API.

## Quality gates

Before committing or pushing:

```bash
make lint        # Pint style + Flutter analyze
make test        # backend + frontend test suites
make check       # full CI-style health gate (includes lint + tests)
```

The raw commands (`cd backend && php artisan test`, `flutter analyze`,
`flutter test`, `./vendor/bin/pint --test`) are equivalent to the `make`
targets above. Android release builds (`apk` / `appbundle --release`) are
built in CI by GitHub Actions — see `.github/workflows/flutter.yml`.

Versioned pre-commit/pre-push hooks (`.githooks/`) run the fast checks on
commit and the full gate on push; `make setup` installs them.

## Production standard

See `docs/PRODUCTION_STANDARD.md` for security, testing, deployment and architecture standards. The active application frontend is Flutter and the backend is Laravel 11 REST API with Sanctum.

Production Android releases are built through GitHub Actions using protected signing credentials. The Android App Bundle (`.aab`) is the primary artifact for Google Play distribution; the signed APK is retained for direct distribution and verification.

## Standout Customer Experience

The customer app is designed as a modern restaurant platform rather than a student portal.

- Personalized **For You** recommendations based on actual order history.
- **Live deals** sourced from active promotion campaigns.
- **Real vendor ratings** with review counts; no fabricated ratings.
- **One-tap Order Again** from order history.
- Live order tracking with queue position and estimated wait support.
- Secure digital collection passes and pickup verification.
- Saved addresses, device/session controls and customer support.
- Wallet, loyalty points, secure online payments and transactional refunds.
- Server-side pricing, promotion validation and idempotent checkout.
- Lightweight Flutter + Laravel architecture suitable for constrained development hardware.

## Smart Cafeteria Capabilities

The platform includes smart queue estimation, personalized food recommendations, vendor demand forecasting, food-waste analytics, QR collection passes, an administrative command center, security-alert review, real-time order events and audit-backed financial governance. See `docs/SMART_FEATURES.md` for the API contracts and design decisions.

### Authentication security
- Single API login endpoint with Laravel Sanctum
- Server-side RBAC and permission enforcement
- Rate-limited authentication
- Secure Flutter token storage (`flutter_secure_storage`)
- Session restoration through `/api/me`
- Password reset with expiring single-use verification codes
- Optional administrator 2FA with email verification codes
- Existing sessions revoked after password reset

### Payment security
- Paystack secret material remains server-side
- Payment verification is performed by Laravel
- Wallet crediting uses transactional safeguards and duplicate-success protection
- Production environments keep payment demo mode disabled
