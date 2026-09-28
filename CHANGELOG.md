# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows the versioning policy described in
[RELEASING.md](RELEASING.md) (app version lives in `frontend/pubspec.yaml`).

## [Unreleased]

### Added
- **Primary database switched to PostgreSQL** (local, Docker and VPS
  deployment). SQLite remains only for automated tests (`phpunit.xml`) and
  the Flutter offline cache. MySQL-specific JSON lookups were replaced with
  a DB-portable duplicate-email check, and `audit_logs.user_id` became
  nullable so pre-auth order audits no longer violate Postgres constraints.
- Docker self-hosting stack for the backend API (`backend/Dockerfile`,
  `docker-compose.yml`) with PostgreSQL, Nginx and health checks.
- `docs/DEPLOY_VPS.md` — bare-metal Linux VPS deployment guide (Nginx +
  PHP-FPM + PostgreSQL, HTTPS, queue worker, backups).
- `docs/API_STANDARDS.md` — response envelope, idempotency, rate-limit and
  error conventions for the REST API.
- Root `Makefile` with `make setup|serve|connect|seed-dev|test|lint|check`
  and `docker-*` targets.
- Versioned git hooks (`.githooks/`) — pre-commit checks and pre-push gate.
- `scripts/dev_seed.sh` (guarded) for loading development vendors and the
  restaurant catalog.
- Development restaurant catalog seeder
  (`DevelopmentRestaurantCatalogSeeder`).
- OpenAPI contract updated to match the production routes (auth, catalog,
  orders, vendor, wallet, system).
- Contribution and release documentation (`CONTRIBUTING.md`,
  `RELEASING.md`), issue/PR templates and `CODEOWNERS`.
- Docker stack now runs the queue worker and Laravel scheduler
  (`docker compose up`); `make db-backup` and `make smoke` operational
  helpers.
- `docs/OBSERVABILITY.md` (health checks, logging, monitoring, Crashlytics)
  and `docs/UAT_CHECKLIST.md` (manual release test pass).
- Flutter Crashlytics integration (`CrashReporting`), enabled via the same
  `ATU_FIREBASE_*` dart-defines used by FCM.
- End-to-end customer journey test (`CustomerJourneyEndToEndTest`): register →
  login → catalogue → order → vendor READY → student poll → pickup verify,
  over the real HTTP API on SQLite and PostgreSQL.
- Direct `CafeteriaProvider` catalogue tests via an injectable HTTP client
  (with a `fetchRemoteFoodItems()` seam); `make load` load-test script and
  `docs/PERFORMANCE_BUDGET.md`; `docs/MOBILE_POLISH.md` (accessibility audit
  checklist + offline order-queueing design).

### Changed
- **One typed API layer in Flutter**: `CafeteriaProvider` now takes an
  `ApiClient`; auth/account, `/me`, catalogue, vendor-menu, order-read and
  pickup flows all go through it (token passing, idempotency keys, JSON error
  mapping, HTTPS enforcement).
- **API contract gaps fixed** (found by the new contract check):
  vendor food-item create/update/delete now target `/api/vendor/menu-items`;
  legacy order sync targets `/api/customer/orders`; audit reads use
  `/api/admin/audit-logs`; audit/feedback remote writes removed (server owns
  them) — every URL the app calls now has a matching backend route.
- **Pagination**: `/api/catalog/menu-items` and `/api/wallet` accept optional
  `page`/`per_page` (with a `pagination` meta block, full list when omitted);
  OpenAPI and API_STANDARDS updated.
- **Checked contract in CI**: `make contract` / a CI step run a route↔app
  audit so the frontend<->backend seam cannot silently drift again.
- **Offline order queue**: `pending_orders` SQLite table + `PendingOrderQueue`
  service (idempotency-keyed, flush reconciles 2xx/4xx/5xx) wired into the
  provider; 5 unit tests.
- **Typed client finished**: login/2FA and Paystack initialize/verify now run
  through `ApiClient` (`rawRequest` keeps status/header semantics for auth);
  SSE order-tracking streaming remains the documented exception.
- **Branding**: generated Android launcher icons + brand asset; README banner
  and badges; `docs/DEPLOYMENT_CHECKLIST.md` (A–F rollout/release plan).

### Changed
- Local backend port standardised on Laravel default `8000` (loopback,
  USB `adb reverse`, emulator and Wi-Fi modes).
- Customer catalogue sync now uses `GET /api/catalog/menu-items` (server-side
  menu items with vendor, stock and availability) instead of the legacy
  `food-items` bare-array endpoint.
- Unauthenticated requests to `/api/*` now return a JSON `401`
  (`{"message":"Unauthenticated."}`) instead of a redirect.
- Whole backend reformatted with Laravel Pint to satisfy the CI style gate.

### Removed
- Netlify deployment artifacts (`netlify.toml`, legacy `index.html` /
  `web_app/` demo pages, `DEPLOY_TO_NETLIFY.md`).
- Railway deployment artifacts (`railway.json`, `DEPLOYMENT_RAILWAY.md`).
- InfinityFree deployment artifacts (deploy workflow, bundle script, guide)
  and the Flutter client's InfinityFree browser-challenge workaround —
  self-hosted deployment now targets PostgreSQL on Docker or a Linux VPS.

### Fixed
- API 401 handling for mobile clients (machine-readable JSON).
- Catalog response parsing tolerates `menu_items`, `data` and bare-list
  envelopes.

## [1.1.0] — 2026-09

### Added
- Restaurant platform customer experience: vendor catalogue, For You
  recommendations, live deals, vendor ratings, order tracking with queue
  position, collection passes, saved addresses, wallet and loyalty points.
- Vendor restaurant operations: production menu management with stock,
  inventory movements, demand forecasting, kiosk sales, promotions,
  payouts and an integrated operations dashboard.
- Production cart checkout: idempotent multi-vendor checkout, split-venue
  order allocations, refunds, promotion redemption and loyalty allocation.
- Real-time order events, QR collection passes, admin command center and
  audit-backed financial governance.

## [1.0.0] — 2026-06

### Added
- Initial production release of the ATU Cafeteria platform:
  Flutter client with local SQLite resilience, Laravel 11 REST API with
  Sanctum authentication, RBAC, digital wallet, Paystack payments,
  real-time order events and admin/audit workflows.
- GitHub Actions CI (backend tests + Pint, Flutter analyze/test/build) and
  signed production release workflow.
- Security, authentication, RBAC and production engineering documentation.