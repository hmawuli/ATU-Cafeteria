#!/usr/bin/env bash
#
# Security deploy gate — fails when critical hygiene is broken.
# Run as part of CI / deploy pipelines before promoting an environment.
#
#   bash scripts/security_gate.sh
set -euo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/backend"

echo "◆ Config gate (production-hardened keys, no debug)…"
php artisan config:check --fail-on-prod

echo "◆ Security audit (no known dev PINs, admins 2FA-in-production)…"
php artisan security:audit --fail-on-critical

echo "✓ Security gate passed."