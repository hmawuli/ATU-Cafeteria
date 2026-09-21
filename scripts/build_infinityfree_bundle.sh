#!/usr/bin/env bash
# =============================================================================
#  build_infinityfree_bundle.sh
#  Builds a ready-to-upload InfinityFree bundle from the backend/ source.
#
#  InfinityFree has no SSH / Composer / terminal, so the app must be built
#  here (dependencies installed for PHP 8.2, .env produced, files arranged so
#  the Laravel front controller lives in htdocs/ and the rest in laravel-app/).
#
#  Required environment variables:
#    APP_KEY       Laravel application key (base64:...)
#    DB_HOST       InfinityFree MySQL host, e.g. sql303.infinityfree.com
#    DB_DATABASE   e.g. if0_42973998_atu_cafeteria
#    DB_USERNAME   e.g. if0_42973998
#    DB_PASSWORD   database password
#  Optional:
#    APP_URL       defaults to https://atucafeteria.free.nf
#
#  Usage:  bash scripts/build_infinityfree_bundle.sh [output_dir]
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"
OUT_DIR="${1:-$REPO_ROOT/dist}"
APP_URL="${APP_URL:-https://atucafeteria.free.nf}"

require() { [ -n "${!1:-}" ] || { echo "❌ Missing required env var: $1" >&2; exit 1; }; }
require APP_KEY; require DB_HOST; require DB_DATABASE; require DB_USERNAME; require DB_PASSWORD

STAGE="$(mktemp -d)"
WEBROOT="$STAGE/webroot"
APP_DEST="$WEBROOT/laravel-app"
mkdir -p "$APP_DEST"

echo "▶ Staging backend source…"
( cd "$BACKEND_DIR" && tar \
    --exclude='.git' --exclude='tests' --exclude='.env' --exclude='.env.*' \
    --exclude='database/database.sqlite' --exclude='*.result.cache' \
    --exclude='node_modules' -cf - . ) | tar -xf - -C "$APP_DEST"

# Development-only files that must not ship to shared hosting.
rm -rf "$APP_DEST"/{phpunit.xml,railway.json,scripts,public,storage/framework/cache/data/*} 2>/dev/null || true
rm -rf "$APP_DEST"/database/{migrations,factories,seeders} 2>/dev/null || true
rm -f  "$APP_DEST"/*.md 2>/dev/null || true
mkdir -p "$APP_DEST"/{storage/framework/cache/data,storage/framework/sessions,storage/framework/views,storage/logs,bootstrap/cache}

echo "▶ Installing production dependencies for PHP 8.2…"
if command -v composer >/dev/null 2>&1; then
  ( cd "$APP_DEST" && composer config platform.php 8.2.0 \
      && composer install --no-dev --no-interaction --prefer-dist --optimize-autoloader --no-progress )
else
  echo "❌ composer not found — install Composer, or copy a pre-built vendor/ into backend/." >&2
  exit 1
fi

echo "▶ Writing .env…"
cat > "$APP_DEST/.env" <<ENV
APP_NAME="ATU Cafeteria API"
APP_ENV=production
APP_KEY=${APP_KEY}
APP_DEBUG=false
APP_URL=${APP_URL}
APP_TIMEZONE=Africa/Accra

LOG_CHANNEL=stack
LOG_LEVEL=error

DB_CONNECTION=mysql
DB_HOST=${DB_HOST}
DB_PORT=3306
DB_DATABASE=${DB_DATABASE}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}

BROADCAST_CONNECTION=log
CACHE_STORE=file
FILESYSTEM_DISK=local
QUEUE_CONNECTION=sync
SESSION_DRIVER=file
SESSION_LIFETIME=120

PAYSTACK_SECRET_KEY=
PAYSTACK_PUBLIC_KEY=

CORS_ALLOWED_ORIGINS=${APP_URL}
CORS_SUPPORTS_CREDENTIALS=false
SANCTUM_TOKEN_EXPIRATION=1440
STUDENT_SESSION_TIMEOUT_SECONDS=900

AUTH_CODE_EXPIRATION_MINUTES=10
MAIL_MAILER=log
MAIL_FROM_ADDRESS=noreply@atu.edu.gh
MAIL_FROM_NAME="ATU Cafeteria"
ENV

# Lock down the application folder so .env / source cannot be fetched over HTTP.
cat > "$APP_DEST/.htaccess" <<'HTACCESS'
<IfModule mod_authz_core.c>
    Require all denied
</IfModule>
<IfModule !mod_authz_core.c>
    Order allow,deny
    Deny from all
</IfModule>
HTACCESS

echo "▶ Writing front controller + rewrite rules…"
cat > "$WEBROOT/index.php" <<'PHP'
<?php

use Illuminate\Http\Request;

define('LARAVEL_START', microtime(true));

if (file_exists($maintenance = __DIR__.'/laravel-app/storage/framework/maintenance.php')) {
    require $maintenance;
}

require __DIR__.'/laravel-app/vendor/autoload.php';

(require_once __DIR__.'/laravel-app/bootstrap/app.php')
    ->handleRequest(Request::capture());
PHP

cat > "$WEBROOT/.htaccess" <<'HTACCESS'
<IfModule mod_rewrite.c>
    <IfModule mod_negotiation.c>
        Options -MultiViews -Indexes
    </IfModule>

    RewriteEngine On

    RewriteCond %{HTTP:Authorization} .
    RewriteRule .* - [E=HTTP_AUTHORIZATION:%{HTTP:Authorization}]

    RewriteCond %{REQUEST_FILENAME} !-d
    RewriteCond %{REQUEST_URI} (.+)/$
    RewriteRule ^ %1 [L,R=301]

    RewriteCond %{REQUEST_FILENAME} !-d
    RewriteCond %{REQUEST_FILENAME} !-f
    RewriteRule ^ index.php [L]
</IfModule>
HTACCESS

echo "▶ Packaging…"
mkdir -p "$OUT_DIR"
rm -f "$OUT_DIR/atu_cafeteria_webroot.zip"
( cd "$WEBROOT" && zip -rq "$OUT_DIR/atu_cafeteria_webroot.zip" . )

rm -rf "$STAGE"
echo "✅ Bundle ready: $OUT_DIR/atu_cafeteria_webroot.zip"
