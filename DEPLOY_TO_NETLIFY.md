# Netlify Deployment Guide

> **Note:** the active client of ATU Cafeteria is the **Flutter** application
> (see `README.md`). The `web_app/` folder in this repository is a legacy
> static placeholder and is **not** the production frontend. Production
> Android releases are built and signed through GitHub Actions
> (`.github/workflows/flutter.yml` and `production-release.yml`).

This guide is only relevant if you explicitly want to host a static web
preview of the Flutter app.

## Hosting the Flutter web build on Netlify

1. Build the Flutter web application:

   ```bash
   cd frontend
   flutter build web --release
   ```

2. Drag and drop the generated `frontend/build/web` folder onto
   <https://app.netlify.com/drop>, or connect the repository and use:
   - Build command: `cd frontend && flutter build web --release`
   - Publish directory: `frontend/build/web`

The SPA redirect rules in `netlify.toml` route unknown paths back to
`index.html`, so deep links work on direct load.

## Authoritative deployment documentation

- `docs/PRODUCTION_RUNBOOK.md` — local production-style run.
- `docs/DEPLOY_INFINITYFREE.md` — Laravel backend deployment.
- `docs/FCM_PRODUCTION_SETUP.md` — Firebase Cloud Messaging setup.
- `README.md` — API endpoint configuration for deployed hosts.