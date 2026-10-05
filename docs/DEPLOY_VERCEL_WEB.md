# Deploying the Flutter web client to Vercel

Vercel can host the **Flutter web build as a static site**. Flutter compiles to
HTML/JS/WASM, and Vercel serves that directory from its global CDN — the same
place the Laravel API already runs (see [`DEPLOY_VERCEL.md`](DEPLOY_VERCEL.md)).

```text
Student browser ──HTTPS──▶ Vercel static site (Flutter web: HTML/JS/WASM)
        │
        └──XHR/HTTPS──▶ Vercel container function (Laravel API)
                              └──▶ Managed PostgreSQL (Neon / Supabase / …)
```

> **Vercel's build image does not include the Flutter SDK.** This guide builds
> the bundle locally (or in CI) and uploads the finished `build/web` directory,
> so Vercel never needs a Flutter toolchain. Drag-and-drop deploys work too.

## `API_BASE_URL` — pass the origin, not `/api`

The Flutter `ApiClient` appends `/api/...` to its configured base itself. Pass
the backend **origin** (scheme + host), exactly like the in-app *Server base
URL* field:

```text
✅  https://atu-cafeteria-backend.vercel.app
❌  https://atu-cafeteria-backend.vercel.app/api/
```

The API then resolves to `https://atu-cafeteria-backend.vercel.app/api/health`,
`/api/catalog/menu-items`, and so on.

## Is the client web-ready?

The mobile app is the primary target, so a few native-only pieces are handled
for the browser. These are already implemented in the codebase:

| Concern | Native (Android/iOS) | Web |
|---|---|---|
| Local cache | `sqflite` (platform SQLite) | `sqflite_common_ffi_web` (SQLite compiled to WASM, persisted in IndexedDB) |
| HTTP calls | `package:http` / `dart:io` | `package:http` (all API calls now go through `ApiClient`) |
| Live order SSE stream | `dart:io HttpClient` stream | Falls back to the in-app status simulator (`kIsWeb` guard) |
| Push notifications / Crashlytics | Firebase native plugins | No-ops (`kIsWeb` guards); configure web push later if needed |
| Session/token storage | OS keychain | `flutter_secure_storage` web (WebCrypto + localStorage) |

The SQLite WASM binaries are committed under `frontend/web/`:

- `sqlite3.wasm`
- `sqflite_sw.js`

Regenerate them after upgrading `sqflite_common_ffi_web`:

```bash
cd frontend
dart run sqflite_common_ffi_web:setup --force
```

## Prerequisites

1. The Laravel API is deployed and healthy
   (e.g. `https://atu-cafeteria-backend.vercel.app/api/health`).
2. The API's `CORS_ALLOWED_ORIGINS` includes the Flutter web origin, e.g.
   `https://atu-cafeteria.vercel.app`.
3. Node.js (`npx`) locally, or a Vercel account for drag-and-drop.
4. A Vercel token for non-interactive/CI deploys (`VERCEL_TOKEN`), or run
   `npx vercel login` once.

## Build

The API base URL is injected at build time — never hard-code it:

```bash
cd frontend
flutter build web --release \
  --dart-define=API_BASE_URL=https://atu-cafeteria-backend.vercel.app
```

Release builds reject non-HTTPS endpoints, so use `https://…`.

## Deploy

### One command (recommended)

```bash
API_BASE_URL=https://atu-cafeteria-backend.vercel.app \
VERCEL_TOKEN=xxxx \
  scripts/deploy_frontend_vercel.sh
```

The script resolves the Flutter SDK, builds, checks the API health endpoint,
and runs `vercel deploy frontend/build/web --prod`.

Or from the `Makefile`:

```bash
make deploy-web API_BASE_URL=https://atu-cafeteria-backend.vercel.app
```

(`VERCEL_TOKEN` is read from the environment if set.)

### Manual CLI

```bash
cd frontend
flutter build web --release --dart-define=API_BASE_URL=https://atu-cafeteria-backend.vercel.app
npx vercel deploy build/web --prod
```

The first run asks you to link or create a Vercel project. Name it
`atu-cafeteria` so the production URL is `https://atu-cafeteria.vercel.app`.

### Drag-and-drop

Open <https://vercel.com/drop> and drop the `frontend/build/web` folder.

## Verify

1. Open the production URL and wait for the splash screen → login.
2. Register or sign in, open the menu — the catalogue comes from the API.
3. Confirm no CORS errors in the browser console
   (`CORS_ALLOWED_ORIGINS` on the API).
4. Place a test order and confirm it appears in the vendor dashboard.

## Updating the deployment

Rebuild and re-run the deploy command above. Flutter's generated service worker
(`flutter_service_worker.js`) handles cache-busting for returning visitors.

## Continuous deployment from GitHub

Add a repository secret `VERCEL_TOKEN`, then push to `main`. A ready-to-enable
workflow lives at `.github/workflows/deploy-web.yml`; it builds the client and
publishes it with the same script. Optionally set the repository variable
`API_BASE_URL` (the backend origin); it defaults to the Vercel API URL.

## Limitations and what works where

| Capability | On Vercel static web |
|---|---|
| Catalogue, auth, orders, wallet, vendor dashboards | ✅ Works |
| Offline order queue + local cache | ✅ IndexedDB-backed SQLite |
| Live order tracking SSE stream | ⚠️ Falls back to the in-app simulator; polling still refreshes status |
| Push notifications (FCM) | ⚠️ Client no-op until web push is configured |
| Crash reporting (Crashlytics) | ⚠️ Disabled on web |
| Deep links | Hash routing (`/#/route`) by default — no server rewrites needed |

## Security checklist for this deployment

- `API_BASE_URL` uses HTTPS (release builds enforce it).
- `CORS_ALLOWED_ORIGINS` lists only the real Flutter web origins.
- `VERCEL_TOKEN` is a scoped token stored as a CI secret — never committed.
- The SQLite WASM binaries are pinned to the `sqflite_common_ffi_web` version in
  `frontend/pubspec.yaml`.
