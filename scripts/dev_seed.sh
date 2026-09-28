#!/usr/bin/env bash
#
# Seed the LOCAL development database with the restaurant demo catalog.
#
# Development-only data is kept out of DatabaseSeeder on purpose; this script
# is the single, documented way to load it. It refuses to run when the app is
# configured for production so demo data can never be created accidentally.
#
# Usage:  scripts/dev_seed.sh
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/backend"

if [ "${APP_ENV:-}" = "production" ] \
  || [ "${APP_ENV:-}" = "prod" ]; then
  echo "✗ Refusing to seed development data with APP_ENV=${APP_ENV}." >&2
  exit 1
fi

echo "==> Running migrations..."
php artisan migrate --force

echo "==> Seeding menu categories and vendors..."
php artisan db:seed --class=MenuCategoryAndVendorSeeder --force

echo "==> Seeding development restaurant catalog..."
php artisan db:seed --class=DevelopmentRestaurantCatalogSeeder --force

echo "✓ Development vendors and restaurant catalog seeded."
echo "  Vendor logins (PIN hash of 'vendor123'):"
echo "    testvendor1  Campus Delight"
echo "    testvendor2  Quick Bites"