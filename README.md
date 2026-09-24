# ATU Cafeteria Management System

Production-oriented Flutter + Laravel implementation for Accra Technical University cafeteria operations.

## Technology

| Layer | Technology |
|---|---|
| Mobile frontend | Flutter / Dart |
| State management | Provider |
| Local cache | SQLite / sqflite |
| Backend | Laravel 11 / PHP 8.2+ |
| API | REST / JSON |
| Authentication | Laravel-issued bearer token |
| Payments | Paystack integration |
| Analytics | Laravel services + native Flutter charts |

## Repository structure

```text
ATU-Cafeteria/
├── frontend/       # Flutter application — active client
├── backend/        # Laravel REST API
├── scripts/        # lightweight setup/utility scripts
├── docs/           # architecture, deployment and operations documentation
└── .vscode/        # lightweight editor settings
```

## Development philosophy

The project is intentionally optimized for a machine with 4 GB RAM. Android Studio and an Android emulator are not required. VS Code, the Laravel server and a physical Android phone are recommended for low-resource development.

The Laravel API is the production source of truth. Flutter SQLite is used for local resilience and offline reads.

## Quick start

```bash
./scripts/setup_4gb_linux.sh
```

Then start Laravel and Flutter using `docs/PRODUCTION_RUNBOOK.md`.

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
   The app's default `http://127.0.0.1:8001` then reaches your computer over USB.

2. **Same Wi-Fi** — set the address once in `lib/core/config/app_config.dart` and start the backend with `scripts/serve_backend.sh`.

3. **Android emulator** — pass the emulator loopback alias at run time:
   ```bash
   flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8001
   ```

Precedence: `--dart-define` → `staticApiHost` → `127.0.0.1:8001`.
Android development builds also need cleartext HTTP when using a local HTTP API.

## Quality gates

Before committing:

```bash
cd backend && php artisan test
cd backend && ./vendor/bin/pint --test
cd frontend && flutter analyze
cd frontend && flutter test
cd frontend && flutter build apk --release
cd frontend && flutter build appbundle --release
```

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
