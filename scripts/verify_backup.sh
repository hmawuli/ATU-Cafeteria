#!/usr/bin/env bash
# Validate a PostgreSQL plain-SQL backup without changing a database.
# Usage: scripts/verify_backup.sh backups/atu_cafeteria_YYYY-MM-DD_HHMM.sql.gz
set -euo pipefail

BACKUP_PATH="${1:?Usage: scripts/verify_backup.sh <backup.sql.gz>}"

if [ ! -f "$BACKUP_PATH" ]; then
  echo "Backup not found: $BACKUP_PATH" >&2
  exit 1
fi

gzip -t "$BACKUP_PATH"

TEMP_SQL="$(mktemp)"
trap 'rm -f "$TEMP_SQL"' EXIT
gunzip -c "$BACKUP_PATH" > "$TEMP_SQL"

if ! head -n 25 "$TEMP_SQL" | grep -q 'PostgreSQL database dump'; then
  echo "Backup is not a PostgreSQL plain-SQL dump: $BACKUP_PATH" >&2
  exit 1
fi

if ! grep -q '^CREATE TABLE' "$TEMP_SQL"; then
  echo "Backup does not contain a database schema: $BACKUP_PATH" >&2
  exit 1
fi

echo "Backup archive and SQL structure verified: $BACKUP_PATH"
echo "Run scripts/db_restore_drill.sh against a dedicated drill database monthly."
