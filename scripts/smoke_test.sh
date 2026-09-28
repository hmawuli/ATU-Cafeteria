#!/usr/bin/env bash
#
# Smoke-test a running ATU Cafeteria backend.
#
# Usage:  scripts/smoke_test.sh [BASE_URL]      (default: http://127.0.0.1:8000)
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8000}"
fail=0

check() {
  local name="$1" url="$2"
  if curl -fsS -o /dev/null --max-time 10 "$url" 2>/dev/null; then
    echo "✓ $name ($url)"
  else
    echo "✗ $name ($url)"
    fail=1
  fi
}

echo "▶ Smoke-testing $BASE_URL"
check "Laravel health"   "$BASE_URL/health"
check "API health"       "$BASE_URL/api/health"
check "OpenAPI spec"     "$BASE_URL/api/docs/openapi.json"

# Catalogue must be a public 200 with a JSON menu_items list.
if curl -fsS --max-time 10 "$BASE_URL/api/catalog/menu-items" 2>/dev/null | php -r '$d=json_decode(stream_get_contents(STDIN),true); exit(isset($d["success"]) && is_array($d["menu_items"] ?? null) ? 0 : 1);'; then
  echo "✓ Catalogue envelope (/api/catalog/menu-items)"
else
  echo "✗ Catalogue envelope (/api/catalog/menu-items)"
  fail=1
fi

if [ "$fail" -eq 0 ]; then
  echo "✅ Smoke test passed."
else
  echo "❌ Smoke test failed — inspect the endpoint errors above."
  exit 1
fi