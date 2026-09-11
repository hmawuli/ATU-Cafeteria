#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
fail=0
check(){ if "$@" >/dev/null 2>&1; then echo "✓ $1"; else echo "✗ $1"; fail=1; fi; }
command -v php >/dev/null && echo "✓ PHP available" || { echo "✗ PHP missing"; fail=1; }
command -v flutter >/dev/null && echo "✓ Flutter available" || echo "! Flutter not installed in this environment"
php -l backend/artisan >/dev/null && echo "✓ Laravel bootstrap syntax" || fail=1
find backend/app backend/routes backend/config backend/database -name '*.php' -print0 | xargs -0 -n1 php -l >/dev/null && echo "✓ PHP syntax" || fail=1
if command -v flutter >/dev/null; then (cd frontend && flutter analyze); (cd frontend && flutter test); fi
if [ -d backend/vendor ]; then (cd backend && php artisan test); else echo "! backend/vendor missing - run composer install"; fi
if grep -R "web_app\|jetpack\|compose" -n README.md docs frontend/lib backend/routes >/dev/null 2>&1; then echo "! legacy frontend references detected"; else echo "✓ no legacy frontend references"; fi
exit "$fail"
