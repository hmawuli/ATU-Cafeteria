#!/usr/bin/env sh
#
# Container entrypoint for the ATU Cafeteria API.
#
# - Generates APP_KEY on first boot when it is unset (set it explicitly in
#   production deployments).
# - Runs database migrations when MIGRATE_ON_START is true (default true —
#   set MIGRATE_ON_START=false when you want to control migrations yourself).
# - Ensures Laravel writable paths belong to the PHP-FPM user.
set -e

BOLD_CYAN='\033[1;36m'
NC='\033[0m'

if [ -z "${APP_KEY:-}" ]; then
    echo "${BOLD_CYAN}⚙  Generating APP_KEY …${NC}"
    php artisan key:generate --force
fi

if [ "${MIGRATE_ON_START:-true}" = "true" ]; then
    echo "${BOLD_CYAN}⚙  Running database migrations …${NC}"
    php artisan migrate --force --no-interaction
fi

chown -R www-data:www-data storage bootstrap/cache

exec "$@"