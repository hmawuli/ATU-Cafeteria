#!/usr/bin/env bash
#
# One-command production deploy of the ATU Cafeteria backend (Docker Compose).
#
# Run this ON the target host (Docker + Docker Compose plugin installed):
#   APP_ENV=production bash scripts/deploy_docker.sh
#
# Requirements before running:
#   - backend/.env filled (APP_KEY, DB_*, PAYSTACK_*, CORS_ALLOWED_ORIGINS)
#   - backend/.env NOT pointing at development PINs (security:audit gate)
#
# This will STOP when a step fails so a half-deployed stack is never left
# looking healthy.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

log() { echo "◆ $*"; }
fail() { echo "✗ $*" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || fail "docker not installed."
docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin missing (install 'docker-compose-plugin')."
[ -f backend/.env ] || fail "backend/.env missing — cp backend/.env.example backend/.env and set secrets."

port() { grep -E "^APP_PORT=" backend/.env | head -1 | cut -d= -f2- | tr -d ' "'; }
APP_PORT="${APP_PORT:-$(port)}"
BASE_URL="${1:-http://127.0.0.1:${APP_PORT:-8000}}"

log "Build & start stack (app, worker, scheduler, nginx, postgres)…"
docker compose build --quiet
docker compose up -d

log "Wait for health (up to 60s)…"
for _ in $(seq 1 20); do
  if curl -fsS -o /dev/null --max-time 5 "${BASE_URL}/health" 2>/dev/null; then
    break
  fi
  sleep 3
done
curl -fsS -o /dev/null --max-time 5 "${BASE_URL}/health" || fail "API did not become healthy at ${BASE_URL}."

log "Migrations (explicit, entrypoint also guards on boot)…"
docker compose exec -T app php artisan migrate --force --no-interaction

log "Configuration check…"
docker compose exec -T app php artisan config:check --fail-on-prod

log "Smoke test…"
scripts/smoke_test.sh "${BASE_URL}"

log "Security gate (no admins on known dev PINs / 2FA missing)…"
docker compose exec -T app php artisan security:audit --fail-on-critical

log "Backup check…"
scripts/db_backup.sh

echo
echo "✅ Deployed and healthy at ${BASE_URL}"
echo "   Next: docs/DEPLOYMENT_CHECKLIST.md → D (app build) and E (UAT)."