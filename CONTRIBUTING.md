# Contributing to ATU Cafeteria

Welcome! These guidelines keep the project healthy for a small team working
on constrained hardware, while still meeting production standards.

## Repository layout

| Path | What it is |
|---|---|
| `frontend/` | Flutter application (active client) |
| `backend/` | Laravel 11 REST API |
| `scripts/` | Setup, serving and development utilities |
| `docs/` | Architecture, security, deployment and runbook docs |
| `.github/workflows/` | CI/CD (tests, static analysis, release builds) |

## First-time setup

From the project root:

```bash
make setup          # git hooks + PHP & Flutter dependencies + .env files
make seed-dev       # migrate + seed development vendors and restaurant catalog
make serve          # Laravel on 0.0.0.0:8000 (reachable by a phone)
```

Then run the Flutter app from `frontend/` (`flutter run`). See
`docs/SETUP_SMOOTHLY.md` and `docs/PRODUCTION_RUNBOOK.md` for the 4 GB RAM
workflow (physical phone over USB is recommended over an emulator).

## Everyday commands

```bash
make serve      # start Laravel on 0.0.0.0:8000
make connect    # adb reverse for a USB-connected phone
make test       # full test suite (Laravel + Flutter)
make analyze    # static analysis (Flutter)
make lint       # style gates (Pint + flutter analyze)
make format     # apply formatters (Pint, dart format)
make check      # full CI-style health gate
```

## Hooks

`make setup` installs versioned git hooks via `core.hooksPath` (see
`.githooks/`):

- `pre-commit` — fast checks on staged files: PHP lint, Pint, and
  `flutter analyze` when Dart files are staged.
- `pre-push` — runs the full `scripts/check_project.sh` gate.

Because that gate takes a few minutes, `make setup` also enables SSH keepalives
(`core.sshCommand` with `ServerAliveInterval`) for SSH remotes. Without it the
connection git opens before the hook can be dropped while tests run, and git
fails with `SIGPIPE` (exit 141) right after the hook reports success.

Temporarily bypass with `git commit --no-verify` / `git push --no-verify`.

## Quality gates (must be green before pushing)

```bash
cd backend && composer validate --strict
cd backend && php artisan test
cd backend && ./vendor/bin/pint --test
cd frontend && flutter analyze
cd frontend && flutter test
```

The GitHub Actions workflow (`flutter.yml`) runs all of these on every push
and pull request to `main`.

## Development-data rules

`DatabaseSeeder` is intentionally empty so production never receives demo
data. Development data lives in dedicated seeders only:

- `MenuCategoryAndVendorSeeder` — campus categories and vendor accounts
  (`testvendor1` Campus Delight, `testvendor2` Quick Bites).
- `DevelopmentRestaurantCatalogSeeder` — restaurant menu with stock.

Load them with `make seed-dev` (or `scripts/dev_seed.sh`). Both are guarded
against `APP_ENV=production`.

## Conventions

- **PHP:** follow Laravel Pint defaults (`./vendor/bin/pint`).
- **Dart:** follow the repo's `analysis_options.yaml` (`flutter_lints`);
  keep `flutter analyze` clean.
- **Backend API:** REST/JSON with Laravel Sanctum. Handlers in
  `backend/app/Http/Controllers/Api/`.
- **Config, not credentials:** never commit secrets or deploy URLs. The API
  base URL is injected at runtime (`--dart-define=API_BASE_URL=...`).
- **Commits:** small, focused commits with clear messages referencing the
  area changed (e.g. `vendor menu CRUD`, `production checkout`).

## Where to look for deeper context

- `docs/ARCHITECTURE.md` — system design.
- `docs/PRODUCTION_STANDARD.md` — security, testing, deployment standard.
- `docs/RBAC.md` — roles and permissions.
- `README.md` — quick start, API configuration, customer experience.