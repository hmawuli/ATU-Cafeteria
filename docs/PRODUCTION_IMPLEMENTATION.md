# ATU Cafeteria — Production Implementation

This document records the production hardening enforced in the repository and the deployment controls that must be supplied by the hosting environment.

## Implemented in source control

- Flutter and Laravel automated tests run in CI.
- PHP dependency validation and `composer audit` run in CI.
- Flutter analysis/tests run before Android artifact creation.
- Flutter CI is pinned to the project's known Flutter 3.35.4 toolchain.
- Dependabot monitors Composer, Flutter, Gradle and GitHub Actions dependencies.
- A security policy prohibits committing secrets, production databases and signing keys.
- Android release signing supports a protected production keystore supplied through CI secrets.
- Production signing can be made mandatory with `ATU_REQUIRE_PROD_SIGNING=true`.
- The production release workflow refuses to build when the required signing material is missing.
- Paystack configuration is centralized in Laravel config so configuration caching works correctly.
- Paystack secret material remains server-side.
- Payment verification is tied to the authenticated account, initialized transaction and verified gateway amount.
- Wallet crediting is performed inside a database transaction with row locking and an already-successful guard.

## Required production environment controls

Configure these outside Git:

### Laravel

- `APP_ENV=production`
- `APP_DEBUG=false`
- a unique production `APP_KEY`
- a production `APP_URL` using HTTPS
- production database credentials
- restricted `CORS_ALLOWED_ORIGINS`
- production mail credentials if email verification/2FA is enabled
- `PAYSTACK_DEMO_MODE=false`
- `PAYSTACK_SECRET_KEY` only in the server secret store
- `PAYSTACK_PUBLIC_KEY` only where the client integration requires it

Run Laravel configuration/cache commands as part of deployment and never cache development secrets into source control.

### Android production release secrets

Configure GitHub Actions repository/environment secrets:

- `ATU_KEYSTORE_BASE64`
- `ATU_KEYSTORE_PASSWORD`
- `ATU_KEY_ALIAS`
- `ATU_KEY_PASSWORD`

The production workflow reconstructs the keystore only for the build and deletes the temporary file afterward.

## Production deployment sequence

1. Run the backend test suite.
2. Run Flutter analysis and tests.
3. Run dependency audits.
4. Build the signed Android APK and App Bundle using the protected production workflow.
5. Deploy Laravel with production environment variables.
6. Run database migrations using the deployment process.
7. Run the health endpoint and a small authenticated smoke test.
8. Confirm payment provider configuration and webhook/verification behavior in the production environment.
9. Confirm backups and restore procedures.
10. Monitor application and server logs after release.

## Still infrastructure-dependent

Source code cannot create or protect external infrastructure credentials. Before accepting real users, the deployment owner must still configure:

- a managed production database and backup policy
- HTTPS/TLS and domain configuration
- mail delivery
- Paystack production credentials and gateway configuration
- log retention/monitoring
- uptime monitoring
- Android signing secrets
- release distribution (Play Console or controlled APK distribution)
- a tested backup restore procedure

These are operational controls rather than values that should be hard-coded into the application repository.
