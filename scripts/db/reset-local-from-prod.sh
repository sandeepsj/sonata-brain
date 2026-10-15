#!/usr/bin/env bash
# Recreate a local database as an exact copy of a production dump.
#
# Usage: scripts/db/reset-local-from-prod.sh [target-db] [dump-file]
#   target-db  default: sonata-local
#   dump-file  default: newest ~/sonata-dumps/*.dump
#
# Take a fresh dump first with scripts/db/dump-prod.sh.
set -euo pipefail

TARGET_DB="${1:-sonata-local}"
DUMP="${2:-$(ls -t "$HOME"/sonata-dumps/*.dump 2>/dev/null | head -1)}"
PGHOST="${PGHOST:-localhost}"
PGUSER="${PGUSER:-sandeepsj}"
export PGPASSWORD="${PGPASSWORD:-sonata123}"

if [[ -z "$DUMP" || ! -f "$DUMP" ]]; then
  echo "No dump found. Run scripts/db/dump-prod.sh first." >&2
  exit 1
fi
case "$PGHOST" in
  localhost|127.0.0.1|/*) ;;
  *) echo "Refusing: PGHOST '$PGHOST' is not a local server." >&2; exit 1 ;;
esac

echo "Resetting '$TARGET_DB' from $DUMP"
psql -h "$PGHOST" -U "$PGUSER" -d postgres -v ON_ERROR_STOP=1 -q \
  -c "DROP DATABASE IF EXISTS \"$TARGET_DB\" WITH (FORCE)" \
  -c "CREATE DATABASE \"$TARGET_DB\""
pg_restore -h "$PGHOST" -U "$PGUSER" -d "$TARGET_DB" --no-owner --no-acl --exit-on-error "$DUMP"

psql -h "$PGHOST" -U "$PGUSER" -d "$TARGET_DB" -At -c "
  SELECT format('%-20s %s', relname, n_live_tup) FROM pg_stat_user_tables ORDER BY relname" 2>/dev/null || true
echo "Done."
