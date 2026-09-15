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
touch database/database.sqlite
composer install --prefer-dist --optimize-autoloader
php artisan key:generate --force

echo
echo "Setup complete."
echo "Backend: cd backend && php artisan serve"
echo "Frontend: cd frontend && flutter run"
echo "For 4 GB RAM, use a physical Android phone instead of an emulator."
