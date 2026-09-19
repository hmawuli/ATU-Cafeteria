#!/usr/bin/env bash
#
# Connect a physical Android phone to the local Laravel backend WITHOUT ever
# changing the IP address. Uses `adb reverse` to tunnel the phone's own
# loopback port to this machine over USB.
#
# Usage:  scripts/connect_phone.sh [PORT]      (default port: 8001)
#
# After this runs, the app as configured (127.0.0.1:8001) works on the phone
# exactly as it does on web/desktop — no dart-define, no LAN IP.
set -euo pipefail

PORT="${1:-8001}"

if ! command -v adb >/dev/null 2>&1; then
  echo "✗ adb not found. Install it (e.g. 'sudo apt install adb') or use the"
  echo "  Wi-Fi mode instead: set staticApiHost in app_config.dart and start"
  echo "  the backend with scripts/serve_backend.sh."
  exit 1
fi

DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [ "${DEVICE_COUNT}" -eq 0 ]; then
  echo "✗ No Android device detected. Connect your phone with USB debugging"
  echo "  enabled and try again."
  exit 1
fi

echo "◆ Applying adb reverse tcp:${PORT} tcp:${PORT} on ${DEVICE_COUNT} device(s)..."
adb reverse "tcp:${PORT}" "tcp:${PORT}"
echo "◆ Done. Your phone's 127.0.0.1:${PORT} now forwards to this machine."
echo "  Run: flutter run"
echo "  (Re-run this script after unplugging/reconnecting the phone.)"
adb reverse --list