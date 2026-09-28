#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$ROOT/frontend"
BACKEND="$ROOT/backend"

command -v flutter >/dev/null 2>&1 || { echo "Flutter is not installed or not on PATH."; exit 1; }
command -v php >/dev/null 2>&1 || { echo "PHP is not installed or not on PATH."; exit 1; }
command -v composer >/dev/null 2>&1 || { echo "Composer is not installed or not on PATH."; exit 1; }

echo "==> Preparing Flutter frontend"
cd "$FRONTEND"
flutter create . --platforms=android --no-pub
mkdir -p android
cat > android/gradle.properties <<'PROPS'
# ATU Cafeteria — 4 GB RAM Linux profile
org.gradle.jvmargs=-Xmx768m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8
org.gradle.parallel=false
org.gradle.workers.max=1
org.gradle.daemon=false
org.gradle.caching=true
org.gradle.configuration-cache=true
kotlin.compiler.execution.strategy=in-process
kotlin.code.style=official
android.useAndroidX=true
android.nonTransitiveRClass=true
PROPS
flutter pub get

echo "==> Preparing Laravel backend"
cd "$BACKEND"
[ -f .env ] || cp .env.example .env
mkdir -p database bootstrap/cache storage/framework/cache storage/framework/sessions storage/framework/views storage/logs

# PostgreSQL is the primary database. Offer a helpful hint if a local server
# is available; otherwise the user creates the database on their own machine.
if grep -q '^DB_CONNECTION=pgsql' .env; then
  DB_NAME="$(grep -E '^DB_DATABASE=' .env | head -1 | cut -d= -f2 | tr -d ' \"')"
  DB_USER="$(grep -E '^DB_USERNAME=' .env | head -1 | cut -d= -f2 | tr -d ' \"')"
  if command -v psql >/dev/null 2>&1; then
    if ! psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>/dev/null | grep -q 1; then
      echo "==> Creating PostgreSQL database '$DB_NAME' (owner: ${DB_USER:-current user})"
      createdb -O "$DB_USER" "$DB_NAME" 2>/dev/null || sudo -u postgres createdb -O "$DB_USER" "$DB_NAME" 2>/dev/null || \
        echo "! Could not auto-create the database. Create '$DB_NAME' in PostgreSQL manually."
    fi
  else
    echo "! PostgreSQL client not found. Ensure a PostgreSQL server is running and create the '$DB_NAME' database."
  fi
else
  # SQLite fallback for machines without PostgreSQL.
  touch database/database.sqlite
fi

composer install --prefer-dist --optimize-autoloader
php artisan key:generate --force

echo "==> Installing git hooks"
cd "$ROOT"
git config core.hooksPath .githooks

echo
echo "Setup complete."
echo "Backend: make serve   (or: cd backend && php artisan serve)"
echo "Seed dev data: make seed-dev"
echo "Frontend: cd frontend && flutter run"
echo "For 4 GB RAM, use a physical Android phone instead of an emulator."
