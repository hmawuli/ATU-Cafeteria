# Deployment & Release Checklist

Everything required to take the repository from "development green" to
"standing in production". Run through **A → F** once, then reuse **C–F** for
every release.

**Companions:** `docs/STAFF_QUICKSTART.md` (how venue staff and admins run it),
`docs/UAT_CHECKLIST.md` (manual QA), `docs/PERFORMANCE_BUDGET.md` (baselines),
`RELEASING.md` (versioning).

## A. Environment (only you can provide)

- [ ] A production server (VPS or any Docker host) with 2 GB+ RAM, ports 80/443 open.
- [ ] DNS `A` record pointed at the server (e.g. `api.atu.edu.gh`).
- [ ] Firebase project values `ATU_FIREBASE_API_KEY / APP_ID / MESSAGING_SENDER_ID / PROJECT_ID`
      (same four values activate both FCM and Crashlytics).
- [ ] Paystack **production** secret key (keep `PAYSTACK_DEMO_MODE=false`).
- [ ] Google Play upload key + keystore for the signed release workflow.
- [ ] An uptime monitor account (UptimeRobot/Pingdom) — free tier is fine.

## B. Backend deploy (choose one)

### Docker (recommended)
```bash
cp backend/.env.example backend/.env      # fill APP_KEY null? generate, DB_*, Paystack, Firebase
export APP_ENV=production
bash scripts/deploy_docker.sh             # build → up → healthcheck → migrate → smoke → security:audit → backup
```

### VPS (manual)
Follow `docs/DEPLOY_VPS.md` end-to-end (Nginx + PHP-FPM + PostgreSQL, TLS,
queue worker, scheduler cron, `pg_dump` backups), then `make smoke`.

## C. Verify the deployed API

- [ ] `curl https://<host>/health` → `"status":"Healthy"`.
- [ ] `curl https://<host>/api/health` → healthy.
- [ ] `https://<host>/api/docs` renders Swagger UI.
- [ ] `scripts/load_test.sh https://<host>/api/catalog/menu-items 50 500` — 0×5xx,
      within `docs/PERFORMANCE_BUDGET.md`.
- [ ] Uptime monitor created against `/api/health` (1-minute interval).

## D. Mobile build with Firebase

```bash
cd frontend
flutter build apk --release \
  --dart-define=API_BASE_URL=https://<host>/api/ \
  --dart-define=ATU_FIREBASE_API_KEY=... \
  --dart-define=ATU_FIREBASE_APP_ID=... \
  --dart-define=ATU_FIREBASE_MESSAGING_SENDER_ID=... \
  --dart-define=ATU_FIREBASE_PROJECT_ID=...
```

## E. User Acceptance Testing (on a physical phone)

Run `docs/UAT_CHECKLIST.md` against the deployed API. At minimum:

- [ ] Register → login → order → vendor READY → pickup verify (the whole journey).
- [ ] Wallet top-up with a real Paystack reference credits the balance.
- [ ] Offline: compose an order with no network → it shows as queued and delivers
      after reconnect with **no double charge** (`orders` has one row, idempotency key).
- [ ] Crashlytics shows no crashes from the release build.
- [ ] Vendor dashboard loads metrics, daily-revenue and recharts data.

### Payments (sandbox first, then live)

- [ ] Paystack **test mode** end-to-end: initialize → redirect → verify → wallet credit.
- [ ] Duplicate webhook/success protection (no double credit).
- [ ] Refund flow restores the wallet.
- [ ] Only then flip `PAYSTACK_DEMO_MODE=false` with the production secret.

### Real data & people onboarding

- [ ] Create real vendor accounts via Admin → Vendors (`POST /api/admin/vendors`).
- [ ] Load the actual menu/prices/categories; delete/ignore demo seed data.
- [ ] Brief vendor staff using `docs/STAFF_QUICKSTART.md`.
- [ ] Appointed admin reviews `audit_logs` daily.

## F. Release (per `RELEASING.md`)

- [ ] CHANGELOG up to date; bump `frontend/pubspec.yaml` version + build number.
- [ ] Tag `vX.Y.Z+Build`, run `.github/workflows/production-release.yml`.
- [ ] Upload the `.aab` to Google Play; keep the signed APK for direct distribution.
- [ ] After deploy: `pg_dump` daily backup confirmed, `failed_jobs` empty,
      scheduler producing audit rows.