#!/usr/bin/env bash
#
# Contract check: every API path the Flutter app calls must have a matching
# backend route (and vice-versa is acceptable). Protects the frontend<->backend
# seam so the "smooth data flow" cannot silently drift.
#
# Usage:  scripts/check_api_contract.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "▶ Checking backend ↔ frontend API contract…"

if [ ! -d backend/vendor ]; then
  echo "✗ backend/vendor missing — run: cd backend && composer install"
  exit 1
fi

# 1. Backend routes (paths under /api).
backend_paths="$( (cd backend && php artisan route:list --except-vendor 2>/dev/null) \
  | grep -E "^\s+(GET|HEAD|POST|PUT|PATCH|DELETE)" \
  | awk '{print $2}' \
  | grep -E "^api/" \
  | sed 's#^api/##' \
  | sort -u || true )"

if [ -z "${backend_paths}" ]; then
  echo "✗ Could not list backend routes (is the backend bootable?)."
  exit 1
fi

# 2. Frontend calls (literal /api/... strings in Dart sources).
frontend_calls="$( \
  grep -rhoE "/api/[A-Za-z0-9_\$\{\}/.\?-]+" frontend/lib --include='*.dart' \
  | sed "s/[\"'()]//g" \
  | sed 's#^/api/##' \
  | grep -vE "^\\$|^\\.\\.\\.|^\\?|^\$" \
  | grep -vE "^\\$|^\{|^\\.\\.\\." \
  | sort -u || true )"

# 3. Normalize dynamic segments: ${x}, $x and {x} all become {}.
normalize() {
  sed -E 's/\$\{?[A-Za-z0-9_.]+}?/\{\}/g; s/\{[^}]*\}/\{\}/g; s/\?.*$//'
}

missing=0
for path in ${frontend_calls}; do
  [ -z "${path}" ] && continue
  normalized="$(printf '%s' "${path}" | normalize)"
  if ! printf '%s\n' "${backend_paths}" | normalize | grep -qxF "${normalized}"; then
    echo "  ✗ frontend calls /api/${path} — no matching backend route"
    missing=1
  fi
done

if [ "${missing}" -eq 0 ]; then
  echo "✓ All frontend API calls have a matching backend route."
else
  echo "✗ Contract drift detected — add the missing backend routes or fix the client."
  exit 1
fi