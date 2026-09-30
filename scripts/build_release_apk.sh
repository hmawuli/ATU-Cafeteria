#!/usr/bin/env bash
#
# Build a signed-ready release APK/AppBundle with the production API + Firebase
# configuration. All values come from the environment (never hard-coded):
#
#   export API_BASE_URL=https://api.example.com/api
#   export ATU_FIREBASE_API_KEY=...
#   export ATU_FIREBASE_APP_ID=...
#   export ATU_FIREBASE_MESSAGING_SENDER_ID=...
#   export ATU_FIREBASE_PROJECT_ID=...
#   bash scripts/build_release_apk.sh         # APK
#   bash scripts/build_release_apk.sh appbundle   # .aab
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FLUTTER="$(bash "$ROOT/scripts/flutter.sh")"

require() { [ -n "${!1:-}" ] || { echo "✗ Missing required env var: $1" >&2; exit 1; }; }
require API_BASE_URL
require ATU_FIREBASE_API_KEY
require ATU_FIREBASE_APP_ID
require ATU_FIREBASE_MESSAGING_SENDER_ID
require ATU_FIREBASE_PROJECT_ID

case "${API_BASE_URL}" in
  https://*) ;;
  *) echo "✗ API_BASE_URL must start with https:// (release builds enforce HTTPS)." >&2; exit 1 ;;
esac

TARGET="${1:-apk}"
case "${TARGET}" in
  apk) TARGET_FLAGS="apk --release";;
  appbundle) TARGET_FLAGS="appbundle --release";;
  *) echo "✗ Target must be 'apk' or 'appbundle'." >&2; exit 1 ;;
esac

cd "$ROOT/frontend"
"$FLUTTER" build $TARGET_FLAGS \
  --dart-define=API_BASE_URL="${API_BASE_URL}" \
  --dart-define=ATU_FIREBASE_API_KEY="${ATU_FIREBASE_API_KEY}" \
  --dart-define=ATU_FIREBASE_APP_ID="${ATU_FIREBASE_APP_ID}" \
  --dart-define=ATU_FIREBASE_MESSAGING_SENDER_ID="${ATU_FIREBASE_MESSAGING_SENDER_ID}" \
  --dart-define=ATU_FIREBASE_PROJECT_ID="${ATU_FIREBASE_PROJECT_ID}"

echo "✅ Release build complete: build/app/outputs/${TARGET}/"