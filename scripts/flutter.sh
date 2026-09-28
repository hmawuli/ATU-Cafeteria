#!/usr/bin/env bash
#
# Resolve a working `flutter` binary. Prints the command to use.
#
# Prefers `flutter` on PATH when it actually works. If the PATH binary is
# missing or broken (for example a snap install that cannot report its
# version), common SDK locations are probed as a fallback.
#
# Usage:  FLUTTER="$(scripts/flutter.sh)"
set -euo pipefail

probe() { "$@" --version >/dev/null 2>&1; }

if command -v flutter >/dev/null 2>&1 && probe flutter; then
  echo "flutter"
  exit 0
fi

for candidate in \
  "${HOME}/flutter/bin/flutter" \
  "${HOME}/sdks/flutter/bin/flutter" \
  "/opt/flutter/bin/flutter" \
  "/usr/local/flutter/bin/flutter"; do
  if [ -x "${candidate}" ] && probe "${candidate}"; then
    echo "${candidate}"
    exit 0
  fi
done

if command -v flutter >/dev/null 2>&1; then
  echo "flutter" # broken on PATH — let the real error surface downstream
else
  echo "✗ Could not find a working Flutter SDK (checked PATH and common locations)." >&2
  exit 1
fi