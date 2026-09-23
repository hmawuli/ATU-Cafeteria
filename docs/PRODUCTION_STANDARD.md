# ATU Cafeteria — Production Engineering Standard

ATU Cafeteria is maintained as a production application. This document defines the baseline for changes to the system.

## Application boundaries

- Flutter is the mobile client.
- Laravel is the API and business-logic backend.
- No defense/demo-only runtime modes, mock production flows, or hard-coded school-defense shortcuts belong in production code.

## Quality gates

Every change should preserve:

1. Flutter static analysis and automated tests.
2. Laravel automated tests and database migrations.
3. PHP formatting with Laravel Pint.
4. Dependency auditing where supported by CI.
5. Successful release APK and Android App Bundle builds.
6. Production signing only through protected CI secrets.

## Security baseline

- Never commit production credentials, private keys, signing keystores, access tokens, or real `.env` files.
- Keep secrets in the deployment environment or GitHub Actions secrets.
- Validate all client input on the API.
- Enforce authentication and authorization server-side.
- Use least-privilege access for users and services.
- Do not expose internal exceptions, stack traces, credentials, or database details to clients.
- Apply appropriate rate limiting to authentication and other abuse-sensitive endpoints.

## Release baseline

Production releases must be reproducible from the repository and must use the protected production signing configuration. Validation builds may use non-production signing, but they must never be presented as production releases.

## Performance baseline

The application should remain practical to develop and test on a 4 GB RAM workstation. Prefer Flutter and Laravel native capabilities and lightweight dependencies over introducing heavy infrastructure without a clear production requirement.

## Change discipline

Changes should be small, reviewable, tested, and documented when they alter architecture, security, deployment, or operational behavior.
