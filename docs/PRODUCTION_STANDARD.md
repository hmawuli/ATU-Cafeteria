# ATU Cafeteria — Production Standard

## Architecture

Flutter is the only application frontend. Laravel 11 is the API/backend. The client communicates through REST endpoints protected by Laravel Sanctum. SQLite is recommended for lightweight local development; PostgreSQL is recommended for production.

## Security
- Sanctum is the single API authentication mechanism.
- Public registration creates STUDENT or VENDOR accounts only.
- Role middleware enforces authorization server-side.
- PINs are hashed with Laravel Hash; legacy SHA-256 client hashes are rehashed on successful login.
- CORS is allow-list based rather than wildcard by default.
- Token expiration and student inactivity timeout are configurable through environment variables.
- Payment secrets remain server-side.

## Application layers
- `frontend/lib/core`: configuration, networking, theme and cross-cutting concerns.
- `frontend/lib/data`: local/remote data access and repositories.
- `frontend/lib/domain`: domain models/entities.
- `frontend/lib/presentation`: screens, widgets and state.
- `backend/app/Http`: controllers, requests, resources and middleware.
- `backend/app/Services`: business integrations and reusable domain services.

## Reliability
Use transactions for financial/order operations, validate all request data server-side, return consistent error shapes, log important security/audit events, and keep local caching separate from authoritative server state.

## Testing
Run `./scripts/check_project.sh`. On a development machine with Flutter and Composer installed it runs PHP syntax checks, Flutter analysis/tests and Laravel tests.

## Deployment
Set `APP_ENV=production`, `APP_DEBUG=false`, a strong `APP_KEY`, production database credentials, restricted CORS origins, payment secrets only in the server environment, and configure backups/log retention.

## Authentication Security Standard

The production authentication flow uses one Laravel Sanctum API login, server-side account-status checks, rate limiting, secure device token storage in Flutter, revocable sessions, inactivity timeout, password reset codes, and optional administrator two-factor verification. The Flutter client never chooses or asserts its own role. Laravel determines the role and enforces permissions.

Administrator 2FA uses a short-lived, single-use verification code delivered through the configured mail channel. Enable it from **Admin Dashboard → Security**. In local development, `MAIL_MAILER=log` writes the message to Laravel logs; configure a real SMTP/provider in production.

Password reset never reveals whether a username exists. Reset codes expire after ten minutes, are single-use, are limited to five verification attempts, and all existing Sanctum tokens are revoked after a successful password change.


## Authentication security
- Flutter sends credentials only over HTTPS in production; credential hashing is performed by Laravel.
- Legacy SHA-256 client credentials are accepted only for migration and upgraded after successful authentication.
- Tokens are stored using Flutter Secure Storage.
- Login, 2FA, password reset, email verification and logout events are audited.
- Admin 2FA uses single-use, expiring verification codes.
- Password reset revokes all existing Sanctum tokens.
