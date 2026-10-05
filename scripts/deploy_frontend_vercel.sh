#!/usr/bin/env bash
#
# One-command deploy of the Flutter web client to Vercel (static hosting).
#
# Vercel's build image does not ship the Flutter SDK, so this script builds the
# web bundle locally and uploads the finished `build/web` directory. Vercel then
# serves it as static files — no Flutter toolchain required on their side.
#
# Usage:
#   API_BASE_URL=https://atu-cafeteria-backend.vercel.app \
#     scripts/deploy_frontend_vercel.sh
#
# API_BASE_URL is the backend ORIGIN (scheme + host), not the /api path: the
# Flutter ApiClient appends /api/... to it. See docs/DEPLOY_VERCEL_WEB.md.
#
# Authentication (pick one):
#   VERCEL_TOKEN=xxxx scripts/deploy_frontend_vercel.sh
#   # or run `npx vercel login` once and let the CLI use the stored session
#
# Requirements before running:
#   - The backend is deployed and reachable at API_BASE_URL.
#   - Its CORS_ALLOWED_ORIGINS includes the Flutter web origin.
#   - API_BASE_URL uses HTTPS (release builds reject plain HTTP).
#
# See docs/DEPLOY_VERCEL_WEB.md.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

log() { echo "◆ $*"; }
fail() { echo "✗ $*" >&2; exit 1; }

FLUTTER="${FLUTTER:-$(scripts/flutter.sh)}"
API_BASE_URL="${API_BASE_URL:-https://atu-cafeteria-backend.vercel.app}"

case "$API_BASE_URL" in
  https://*) ;;
  *) fail "API_BASE_URL must be HTTPS for a release build (got: $API_BASE_URL)." ;;
esac

command -v npx >/dev/null 2>&1 || fail "npx (Node.js) is required to run the Vercel CLI."

log "Flutter: $FLUTTER"
log "API_BASE_URL: $API_BASE_URL"

log "Fetching dependencies…"
(cd frontend && "$FLUTTER" pub get)

log "Building Flutter web (release)…"
(cd frontend && "$FLUTTER" build web --release --dart-define=API_BASE_URL="$API_BASE_URL")

[ -f frontend/build/web/index.html ] || fail "Build did not produce frontend/build/web/index.html."
[ -f frontend/build/web/sqlite3.wasm ] || fail "web/sqlite3.wasm missing — run 'dart run sqflite_common_ffi_web:setup' in frontend/."

# Fail fast if the deployed backend is unreachable so we do not ship a client
# pointing at a dead API. Health is a public endpoint.
log "Checking backend health…"
curl -fsS -o /dev/null --max-time 15 "${API_BASE_URL%/}/api/health" \
  || log "⚠ /api/health did not answer 200 — deploying anyway (check the API URL later)."

TOKEN_ARGS=()
if [ -n "${VERCEL_TOKEN:-}" ]; then
  TOKEN_ARGS=(--token "$VERCEL_TOKEN")
fi

log "Deploying frontend/build/web to Vercel (production)…"
cd frontend
npx --yes vercel@latest deploy build/web --prod --yes "${TOKEN_ARGS[@]}" "$@"

echo
echo "✅ Flutter web deployed. Open the production URL printed above."
echo "   First run may prompt to link/create the Vercel project for this directory."
