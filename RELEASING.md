# Releasing ATU Cafeteria

How versions are chosen, recorded and shipped.

## Version scheme

The single source of truth for the application version is the Flutter app in
`frontend/pubspec.yaml`:

```yaml
version: 1.2.0+3
```

- `1.2.0` — **semantic version**:
  - `MAJOR` for incompatible API/client changes;
  - `MINOR` for backward-compatible features;
  - `PATCH` for backward-compatible fixes.
- `+3` — build number, incremented for every release (required by Android).

The backend tracks the same release cadence; its version is documented in the
OpenAPI `info.version` (`backend/.../SwaggerController.php`) and the release
notes below.

## Before a release

1. All gates green:
   ```bash
   make lint && make test && make check
   ```
2. `CHANGELOG.md` is up to date: move `Unreleased` items into a new `[x.y.z]`
   section with the release date.
3. `frontend/pubspec.yaml` version and build number bumped.
4. OpenAPI `info.version` updated to match if the API contract changed.
5. Deployment docs (`docs/DEPLOY_VPS.md`, `docs/PRODUCTION_RUNBOOK.md`)
   reflect any new environment variables or migrations.

## Cutting the release

1. Commit the version bump and changelog (e.g.
   `release: 1.2.0+3`).
2. Tag the commit:
   ```bash
   git tag -a v1.2.0+3 -m "Release 1.2.0+3"
   git push origin main --tags
   ```
3. Trigger `.github/workflows/production-release.yml` (workflow_dispatch)
   with the version label. It builds the signed App Bundle + APK; artifacts
   are available from the workflow run.
4. Publish to Google Play from the `.aab`; the signed APK is retained for
   direct distribution.

## After the release

- Deploy the backend (Docker `docker compose up -d --build` or the VPS
  runbook), running `php artisan migrate --force`.
- Update the backend environment for any new vars documented in the
  runbook/standards.
- Verify `/api/health`, login, catalogue, ordering and wallet flows against
  the deployed API.
- Bump the build number in `pubspec.yaml` for the next iteration.

## Hotfix process

For a critical fix on the current release: commit the fix on `main`, bump
`PATCH`, update the changelog under `[x.y.z]`, tag and re-run the release
workflow. Never ship from feature branches.