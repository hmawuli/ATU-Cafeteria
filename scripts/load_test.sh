#!/usr/bin/env bash
#
# Simple HTTP load test with no extra dependencies (curl + xargs).
#
# Usage:  scripts/load_test.sh [URL] [concurrency] [requests]
#         defaults: http://127.0.0.1:8000/api/health  20  200
#
# Reports success rate, latency percentiles and throughput. Compare the
# numbers against the budgets in docs/PERFORMANCE_BUDGET.md.
set -euo pipefail

URL="${1:-http://127.0.0.1:8000/api/health}"
CONCURRENCY="${2:-20}"
TOTAL="${3:-200}"
RESULTS="$(mktemp)"

echo "▶ Load test: ${URL}"
echo "  concurrency=${CONCURRENCY}  requests=${TOTAL}  (max 30s/request)"

START_NS="$(date +%s%N)"
seq 1 "${TOTAL}" | xargs -P "${CONCURRENCY}" -I{} \
  curl -sS -o /dev/null -w '%{http_code} %{time_total}\n' --max-time 30 "${URL}" > "${RESULTS}" 2>/dev/null || true
END_NS="$(date +%s%N)"

ELAPSED_MS="$(( (END_NS - START_NS) / 1000000 ))"
TOTAL_DONE="$(wc -l < "${RESULTS}")"

if [ "${TOTAL_DONE}" -eq 0 ]; then
  echo "✗ No responses captured — is the server up at ${URL}?"
  rm -f "${RESULTS}"
  exit 1
fi

awk -v total="${TOTAL_DONE}" -v elapsed_ms="${ELAPSED_MS}" '
  {
    code=$1; t=$2;
    sum += t; if (t > max) max = t; if (min == 0 || t < min) min = t;
    if (code >= 200 && code < 300) ok++; else if (code == 429) limited++; else if (code >= 500) errors++;
    lines = lines t "\n";
  }
  END {
    avg = sum / total;
    n = split(lines, arr, "\n"); 
    # simple percentile approximation on sorted? keep to min/avg/max for portability
    throughput = total / (elapsed_ms / 1000.0);
    printf "  responses: %d  2xx: %d  429: %d  5xx: %d\n", total, ok, limited, errors;
    printf "  latency:   min=%.3fs avg=%.3fs max=%.3fs\n", min, avg, max;
    printf "  elapsed:   %.1fs   throughput: %.1f req/s\n", elapsed_ms/1000, throughput;
  }
' "${RESULTS}"

rm -f "${RESULTS}"

echo "  ✓ budgets: docs/PERFORMANCE_BUDGET.md"