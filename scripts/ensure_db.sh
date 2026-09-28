#!/usr/bin/env bash
#
# Ensure the local PostgreSQL database (from backend/.env) exists, or ping it.
#
# backend/.env is the single source of truth for the local connection. This
# script reads the same DB_* values the application uses, so "credentials" and
# behaviour can never drift.
#
# Usage:
#   scripts/ensure_db.sh            create the database if missing
#   scripts/ensure_db.sh --ping     verify the connection only
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT/backend/.env"
PING_ONLY=0
[ "${1:-}" = "--ping" ] && PING_ONLY=1

if [ ! -f "$ENV_FILE" ]; then
  echo "✗ backend/.env missing — run: cp backend/.env.example backend/.env"
  exit 1
fi

get() { grep -E "^DB_${1}=" "$ENV_FILE" | tail -1 | cut -d= -f2- | tr -d ' "'; }

DB_CONNECTION="$(get CONNECTION)"
DB_HOST="$(get HOST)";    DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="$(get PORT)";    DB_PORT="${DB_PORT:-5432}"
DB_DATABASE="$(get DATABASE)"
DB_USERNAME="$(get USERNAME)"
DB_PASSWORD="$(get PASSWORD)"

if [ "$DB_CONNECTION" != "pgsql" ]; then
  echo "! DB_CONNECTION is '${DB_CONNECTION}' — PostgreSQL is not the configured driver."
  exit 1
fi
if [ -z "$DB_DATABASE" ] || [ -z "$DB_USERNAME" ]; then
  echo "✗ DB_DATABASE / DB_USERNAME are empty in backend/.env"
  exit 1
fi
if ! command -v psql >/dev/null 2>&1; then
  echo "✗ PostgreSQL client (psql) not installed — install postgresql-client."
  exit 1
fi

export PGPASSWORD="$DB_PASSWORD"

ping() {
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_DATABASE" \
    -tAc "SELECT 'connected to ' || current_database() || ' as ' || current_user;"
}

if [ "$PING_ONLY" -eq 1 ]; then
  if ping >/dev/null 2>&1; then
    echo "✓ PostgreSQL connection OK: $(ping)"
  else
    echo "✗ Cannot connect to ${DB_HOST}:${DB_PORT}/${DB_DATABASE} as ${DB_USERNAME}."
    exit 1
  fi
  exit 0
fi

if ping >/dev/null 2>&1; then
  echo "✓ PostgreSQL database '${DB_DATABASE}' already exists and is reachable."
  exit 0
fi

# Database missing — try to create it (user may already exist).
echo "▶ Creating PostgreSQL database '${DB_DATABASE}' (owner ${DB_USERNAME})..."
if createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -O "$DB_USERNAME" "$DB_DATABASE" 2>/dev/null \
   || sudo -u postgres createdb -h "$DB_HOST" -p "$DB_PORT" -O "$DB_USERNAME" "$DB_DATABASE" 2>/dev/null
then
  echo "✓ Database '${DB_DATABASE}' created."
else
  echo "✗ Could not create '${DB_DATABASE}'. Do it manually, e.g.:"
  echo "    sudo -u postgres createdb -O ${DB_USERNAME} ${DB_DATABASE}"
  exit 1
fi