#!/usr/bin/env bash
# Dump the Neon production database (read-only) to ~/sonata-dumps/.
# Uses DATABASE_URL_UNPOOLED from .env.local (written by `npx neon link`).
# Dumps contain student personal data: never commit them.
set -euo pipefail

cd "$(dirname "$0")/../.."
set -a; . ./.env.local; set +a
case "$DATABASE_URL_UNPOOLED" in *-pooler*) echo "Need the direct (unpooled) URL." >&2; exit 1 ;; esac

mkdir -p "$HOME/sonata-dumps"
OUT="$HOME/sonata-dumps/sonata-prod-$(date +%Y%m%d-%H%M).dump"
pg_dump "$DATABASE_URL_UNPOOLED" --format=custom --no-owner --no-acl -f "$OUT"
chmod 600 "$OUT"
echo "$OUT"
