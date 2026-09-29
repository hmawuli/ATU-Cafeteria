# Security Policy

## Supported deployments

Security fixes are prioritized for the current `main` branch and the latest production release.

## Reporting a vulnerability

Do not publish credentials, payment secrets, tokens, personal data, or exploit details in a public GitHub issue.

Report suspected vulnerabilities privately to the project maintainer and include:

- affected component and version/commit
- reproducible steps
- security impact
- relevant logs with secrets and personal data removed
- a suggested mitigation, if known

## Production security requirements

Before deployment:

- `APP_ENV=production`
- `APP_DEBUG=false`
- HTTPS is enabled
- production secrets exist only in the deployment secret store
- Paystack secret keys remain server-side
- database backups are configured and restore-tested
- authentication and authorization tests pass
- release APKs and App Bundles use protected production signing credentials
- dependency audits and automated tests pass

### Administrator & bootstrap hardening

- Create admins with `php artisan admin:create <username> --pin <strong-pin>
  [--super] [--enable-2fa]`. In production this command requires a PIN of at
  least 10 characters with letters and numbers, and enables 2FA by default.
- Run `php artisan security:audit --fail-on-critical` before every deployment:
  it flags any admin still holding a known development PIN (or missing 2FA)
  and fails the run when a critical finding exists.
- The development seeders (`MenuCategoryAndVendorSeeder`,
  `DevelopmentRestaurantCatalogSeeder`, `DevelopmentAdminSeeder`) throw unless
  the environment is non-production — they can never run accidentally live.
- Known development PINs (`atuAdmin123`, `admin123`, `vendor123`, …) exist only
  to bootstrap local demos; rotate/remove them before anything public.

Never commit `.env`, keystores, private keys, access tokens, payment secrets, or production database dumps.
