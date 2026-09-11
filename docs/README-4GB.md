# ATU Cafeteria — Web + Mobile + Admin (4GB Development Profile)

## Architecture
Web and Flutter mobile clients should use the same Laravel API and production database.
Do not use 10.0.2.2 in production; that address is only useful for an Android emulator talking
to a server on the development machine.

## 4GB laptop guidance
- Prefer VS Code instead of running Android Studio and an emulator simultaneously.
- Test Flutter on a physical Android phone where possible.
- Run one heavy service at a time.
- Keep node_modules, vendor, .dart_tool, build and .gradle excluded from VS Code search/watching.

## Important
This package preserves the source application. Before production deployment, verify all authentication,
authorization, database migrations, payment integrations, and API endpoints in a staging environment.
Never commit real passwords, API keys, payment-card information, or production secrets.
