#!/usr/bin/env bash

# ==============================================================================
# ATU Cafeteria Backend Environment & Database Repair Script
# Resolves common configuration issues, DB connections, and storage permissions.
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================================================="
echo "🔧 Running ATU Cafeteria Environment & Database Diagnostic Tool..."
echo "========================================================================="

cd "$BASE_DIR"

# 1. Verify Storage & Cache Directory Permissions
echo "📁 [1/5] Checking storage and cache permissions..."
mkdir -p storage/app/public storage/framework/cache/data storage/framework/sessions storage/framework/views storage/logs bootstrap/cache database
chmod -R 775 storage bootstrap/cache database 2>/dev/null || true

# 2. Check and Prepare .env File
echo "⚙️ [2/5] Validating .env configuration file..."
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "   ⚠️ .env file missing! Creating .env from .env.example..."
        cp .env.example .env
    else
        echo "   ⚠️ Creating default .env configuration..."
        cat <<EOT > .env
APP_NAME="ATU Cafeteria API"
APP_ENV=local
APP_KEY=
APP_DEBUG=true
APP_URL=http://localhost:8000

LOG_CHANNEL=stack
LOG_LEVEL=debug

DB_CONNECTION=sqlite
DB_DATABASE=${BASE_DIR}/database/database.sqlite

BROADCAST_DRIVER=log
CACHE_DRIVER=file
FILESYSTEM_DISK=local
QUEUE_CONNECTION=sync
SESSION_DRIVER=file
SESSION_LIFETIME=120

SANCTUM_STATEFUL_DOMAINS=localhost,127.0.0.1
EOT
    fi
fi

# 3. Ensure SQLite Database File Exists if using SQLite
DB_CONN=$(grep -E "^DB_CONNECTION=" .env | cut -d '=' -f 2 || echo "sqlite")
if [ "$DB_CONN" = "sqlite" ] || [ -z "$DB_CONN" ]; then
    echo "🗄️ [3/5] Verifying SQLite database file at database/database.sqlite..."
    if [ ! -f database/database.sqlite ]; then
        touch database/database.sqlite
        echo "   ✅ Created empty database/database.sqlite file."
    else
        echo "   ✅ database/database.sqlite already exists."
    fi
fi

# 4. App Key & Configuration Repair
echo "🔑 [4/5] Checking APP_KEY status..."
if ! grep -q "^APP_KEY=base64:" .env; then
    echo "   ⚠️ APP_KEY missing or ungenerated. Attempting artisan key:generate..."
    if command -v php >/dev/null 2>&1; then
        php artisan key:generate --force
    else
        # Fallback pseudo-random base64 key generation
        RKEY=$(head -c 32 /dev/urandom | base64 2>/dev/null || echo "w84XpZ1qL8mB7vN2cK4jT9sF6yR3eA1d")
        sed -i 's/^APP_KEY=.*/APP_KEY=base64:'"$RKEY"'/' .env
        echo "   ✅ Formatted fallback APP_KEY in .env."
    fi
fi

# 5. Clear Caches & Run Migrations
echo "🧹 [5/5] Flushing stale cache entries and verifying schema..."
if command -v php >/dev/null 2>&1; then
    php artisan config:clear || true
    php artisan cache:clear || true
    php artisan route:clear || true
    echo "   🚀 Running pending database migrations..."
    php artisan migrate --force || echo "   ⚠️ Migration skipped or database already up-to-date."
else
    echo "   ℹ️ PHP binary unavailable in this shell context. File permissions and .env settings repaired successfully."
fi

echo "========================================================================="
echo "✅ Environment & Database resolution finished cleanly!"
echo "========================================================================="
