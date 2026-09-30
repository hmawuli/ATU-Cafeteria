#!/usr/bin/env bash
#
# Stand up the ATU Cafeteria backend on THIS machine WITHOUT Docker — the
# "deploy now" path for hosts without a Docker daemon (like this laptop).
#
#   - ensures PostgreSQL from backend/.env
#   - migrates
#   - seeds development data (refuses on staging/production)
#   - serves on 0.0.0.0:8000 and smoke-tests it
#
# For a full container deployment on a Docker host use scripts/deploy_docker.sh.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "◆ Ensuring PostgreSQL database…"
scripts/ensure_db.sh

(cd backend && php artisan migrate --force --no-interaction)

if [ "${APP_ENV:-local}" = "local" ] || [ "${APP_ENV:-local}" = "testing" ]; then
  echo "◆ Seeding development data (locked to non-production environments)…"
  (cd backend && php artisan db:seed --class=MenuCategoryAndVendorSeeder --force --no-interaction)
  (cd backend && php artisan db:seed --class=DevelopmentRestaurantCatalogSeeder --force --no-interaction)
  (cd backend && php artisan db:seed --class=DevelopmentAdminSeeder --force --no-interaction)
else
  echo "◆ APP_ENV=${APP_ENV:-local} — skipping development seeders."
fi

# Start serving (foreground) and confirm it's healthy.
scripts/serve_backend.sh 8000 &

sleep 3
scripts/smoke_test.sh "http://127.0.0.1:8000"

echo
echo "✅ ATU Cafeteria backend is LIVE at http://0.0.0.0:8000"
echo "   Interactive docs: http://localhost:8000/api/docs"
wait