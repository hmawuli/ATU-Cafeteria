#!/usr/bin/env bash
#
# Start the Laravel backend reachable from a physical phone on the same
# Wi-Fi network (binds 0.0.0.0 instead of 127.0.0.1).
#
# Usage:  scripts/serve_backend.sh [PORT]      (default port: 8001)
#
# After starting, run the app on a phone. The app reads the address once from
# lib/core/config/app_config.dart -> staticApiHost (set it to the LAN IP shown
# below). No need to retype the IP for every launch.
set -euo pipefail

PORT="${1:-8001}"
BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../backend" && pwd)"

LAN_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"

# Also advertise the USB path: with the phone connected via cable, this makes
# the phone's own 127.0.0.1:$PORT forward to this machine over USB, so the IP
# never needs to change at all.
echo "◆ Backend will be reachable at:"
echo "    http://127.0.0.1:${PORT}        (this machine)"
if [ -n "${LAN_IP}" ]; then
  echo "    http://${LAN_IP}:${PORT}       (phone, same Wi-Fi — set this in app_config.dart -> staticApiHost)"
fi
echo "◆ USB alternative: run  scripts/connect_phone.sh  (adb reverse, no IP needed)"
echo "◆ Starting Laravel on 0.0.0.0:${PORT}..."
cd "${BACKEND_DIR}"
exec php artisan serve --host=0.0.0.0 --port="${PORT}"