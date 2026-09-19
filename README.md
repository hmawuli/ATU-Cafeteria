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
├── frontend/       # Flutter application — the only active UI
├── backend/        # Laravel REST API
├── scripts/        # low-resource setup/utility scripts
├── docs/           # architecture, deployment and defense material
└── .vscode/        # lightweight editor settings
```

## Development philosophy

The project is intentionally optimized for a machine with 4 GB RAM. Android Studio and an Android emulator are not required. VS Code, the Laravel server and a physical Android phone are the recommended development setup.

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

### Running on a physical phone (no more changing the IP)

`127.0.0.1` inside the app means **the phone itself**, never your computer. Pick one mode:

1. **USB (recommended, zero configuration)** — tunnel over the cable:
   ```bash
   scripts/serve_backend.sh        # starts Laravel on 0.0.0.0:8001
   scripts/connect_phone.sh        # adb reverse tcp:8001 tcp:8001
   flutter run
   ```
   The app's default `http://127.0.0.1:8001` then reaches your computer over USB.
   Works regardless of Wi-Fi changes; re-run `connect_phone.sh` after unplugging.

2. **Same Wi-Fi** — set the address once in `lib/core/config/app_config.dart`:
   ```dart
   static const String staticApiHost = '192.168.1.50'; // your `hostname -I` IP
   ```
   Start the backend with `scripts/serve_backend.sh` (binds `0.0.0.0` so the phone
   can reach it). To make the laptop's IP truly fixed, pin it on your router
   (DHCP reservation) or run: `sudo nmcli connection modify <wifi> ipv4.method manual ipv4.addresses 192.168.1.50/24 ipv4.gateway 192.168.1.1` and reconnect.

3. **Android emulator** — pass the emulator loopback alias at run time:
   ```bash
   flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8001
   ```

Precedence: `--dart-define` → `staticApiHost` → `127.0.0.1:8001`.
Android dev builds also need cleartext HTTP, enabled in
`frontend/android/app/src/main/AndroidManifest.xml` (`usesCleartextTraffic`).

## Quality gates

Before committing:

```bash
cd backend && php artisan test
cd backend && ./vendor/bin/pint --test
cd frontend && flutter analyze
```

See `docs/ARCHITECTURE.md` for the full architectural boundary.


## Production-standard architecture
See `docs/PRODUCTION_STANDARD.md` for security, testing, deployment and architecture standards. The active application frontend is Flutter and the backend is Laravel 11 REST API with Sanctum.

## Smart Cafeteria Capabilities

The platform now includes smart queue estimation, personalized food recommendations, vendor demand forecasting, food-waste analytics, QR collection passes, an administrative command center, security-alert review, real-time order events and audit-backed financial governance. See `docs/SMART_FEATURES.md` for the API contracts and design decisions.

### Authentication security
- Single API login endpoint with Laravel Sanctum
- Server-side RBAC and permission enforcement
- Rate-limited authentication
- Secure Flutter token storage (`flutter_secure_storage`)
- Session restoration through `/api/me`
- Password reset with expiring single-use verification codes
- Optional administrator 2FA with email verification codes
- Existing sessions revoked after password reset

### Authentication security
The mobile client sends the credential over HTTPS to Laravel, where it is hashed and verified. Legacy SHA-256 client credentials are supported only for migration and are upgraded after successful login. Tokens are stored with Flutter Secure Storage. Admin 2FA, password reset, optional email verification, rate limiting, session expiry and security audit events are included.
