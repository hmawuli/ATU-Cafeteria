#!/usr/bin/env bash
# Restore a backup into a dedicated non-production database and run migrations.
# This intentionally refuses the configured application database.
#
# Usage:
#   DB_RESTORE_DRILL_DATABASE=atu_restore_drill \
#     scripts/db_restore_drill.sh backups/atu_cafeteria_YYYY-MM-DD_HHMM.sql.gz
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BACKUP_PATH="${1:?Usage: DB_RESTORE_DRILL_DATABASE=<safe-db> scripts/db_restore_drill.sh <backup.sql.gz>}"
DRILL_DATABASE="${DB_RESTORE_DRILL_DATABASE:-}"

if [ -z "$DRILL_DATABASE" ]; then
  echo "Set DB_RESTORE_DRILL_DATABASE to a dedicated non-production database." >&2
  exit 1
fi

if [ ! -f "$BACKUP_PATH" ]; then
  echo "Backup not found: $BACKUP_PATH" >&2
  exit 1
fi

# shellcheck disable=SC1091
[ -f backend/.env ] && set -a && . backend/.env && set +a
LIVE_DATABASE="${DB_DATABASE:-atu_cafeteria}"
if [ "$DRILL_DATABASE" = "$LIVE_DATABASE" ]; then
  echo "Refusing to restore into the configured application database." >&2
  exit 1
fi

case "$DRILL_DATABASE" in
  *restore*|*drill*|*test*) ;;
  *)
    echo "Refusing database '$DRILL_DATABASE': use a name containing restore, drill, or test." >&2
    exit 1
    ;;
esac

"$ROOT/scripts/verify_backup.sh" "$BACKUP_PATH"

export PGPASSWORD="${DB_PASSWORD:-}"
PGHOST="${DB_HOST:-127.0.0.1}"
PGPORT="${DB_PORT:-5432}"
PGUSER="${DB_USERNAME:-postgres}"

echo "Restoring into dedicated drill database: $DRILL_DATABASE"
dropdb --if-exists -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" "$DRILL_DATABASE"
createdb -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" "$DRILL_DATABASE"
gunzip -c "$BACKUP_PATH" | psql -v ON_ERROR_STOP=1 -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" "$DRILL_DATABASE"

DB_CONNECTION=pgsql DB_HOST="$PGHOST" DB_PORT="$PGPORT" \
DB_DATABASE="$DRILL_DATABASE" DB_USERNAME="$PGUSER" DB_PASSWORD="$PGPASSWORD" \
  php backend/artisan migrate:status --no-interaction >/dev/null

echo "Restore drill succeeded for $DRILL_DATABASE. Record the duration and result in the go-live log."
