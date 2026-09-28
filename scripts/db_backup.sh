#!/usr/bin/env bash
#
# Back up the ATU Cafeteria PostgreSQL database (compressed pg_dump).
#
# Usage:  scripts/db_backup.sh [output_file]
#         default output: backups/atu_cafeteria_<timestamp>.sql.gz
#
# Prefers the running Docker stack; falls back to a local PostgreSQL server
# using the DB_* values in backend/.env.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

TIMESTAMP="$(date +%F_%H%M)"
OUT="${1:-backups/atu_cafeteria_${TIMESTAMP}.sql.gz}"
mkdir -p "$(dirname "$OUT")"

if docker compose ps --services 2>/dev/null | grep -q '^db$'; then
  echo "▶ Backing up Docker database → $OUT"
  docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' | gzip > "$OUT"
else
  # shellcheck disable=SC1091
  [ -f backend/.env ] && set -a && . backend/.env && set +a
  echo "▶ Backing up local PostgreSQL ${DB_HOST:-127.0.0.1}:${DB_PORT:-5432}/${DB_DATABASE:-atu_cafeteria} → $OUT"
  PGPASSWORD="${DB_PASSWORD:-}" pg_dump \
    -h "${DB_HOST:-127.0.0.1}" \
    -p "${DB_PORT:-5432}" \
    -U "${DB_USERNAME:-postgres}" \
    "${DB_DATABASE:-atu_cafeteria}" | gzip > "$OUT"
fi

echo "✓ Backup written to $OUT  ($(du -h "$OUT" | cut -f1))"
echo "  Restore (Docker): docker compose exec -T db psql -U \$POSTGRES_USER \$POSTGRES_DB < <(gunzip -c '$OUT')"