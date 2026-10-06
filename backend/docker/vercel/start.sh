#!/usr/bin/env sh
#
# Container entrypoint for the ATU Cafeteria API on Vercel.
#
# Vercel routes HTTP traffic to $PORT (default 80) and gives each instance an
# ephemeral filesystem. Nginx and PHP-FPM are started in this single image, and
# Laravel's writable storage is redirected to /tmp by bootstrap/app.php when
# LARAVEL_STORAGE_PATH is set below.
set -e

PORT="${PORT:-80}"
export LARAVEL_STORAGE_PATH="${LARAVEL_STORAGE_PATH:-/tmp/atu-storage}"

# Recreate Laravel's writable storage tree on every cold start. The container
# starts as root but PHP-FPM runs as www-data, so the tree must be owned by
# www-data or Laravel cannot write its caches/logs (every request 500s).
mkdir -p \
    "${LARAVEL_STORAGE_PATH}/app" \
    "${LARAVEL_STORAGE_PATH}/framework/cache/data" \
    "${LARAVEL_STORAGE_PATH}/framework/sessions" \
    "${LARAVEL_STORAGE_PATH}/framework/views" \
    "${LARAVEL_STORAGE_PATH}/logs"

chown -R www-data:www-data "${LARAVEL_STORAGE_PATH}"

# Optional: run migrations on boot. Off by default because several instances can
# cold-start at once; prefer running migrations once from your machine.
if [ "${MIGRATE_ON_START:-false}" = "true" ]; then
    echo "Running database migrations…"
    php artisan migrate --force --no-interaction
fi

# Render the Nginx config with the port Vercel actually routes to.
sed "s/__PORT__/${PORT}/g" /etc/nginx/atu-nginx.conf.template > /tmp/atu-nginx.conf

# Start PHP-FPM in the background, then keep Nginx in the foreground as PID 1.
php-fpm -D
exec nginx -c /tmp/atu-nginx.conf -g 'daemon off;'
