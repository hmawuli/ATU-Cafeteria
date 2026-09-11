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
