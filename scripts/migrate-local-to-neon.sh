#!/usr/bin/env bash
set -euo pipefail

# One-off: copy the local 'shmy' schema (schema + data) straight into a Neon
# database for SIT, instead of going through the legacy Excel import.
# WARNING: this DROPS and recreates the 'shmy' schema on the Neon target
# (--clean --if-exists) before restoring — only run this against a Neon
# database you're fine wiping.

LOCAL_HOST="${LOCAL_DB_HOST:-localhost}"
LOCAL_PORT="${LOCAL_DB_PORT:-5432}"
LOCAL_DB="${LOCAL_DB_NAME:-share_money}"
LOCAL_USER="${LOCAL_DB_USER:-postgres}"

if [ -z "${NEON_CONNECTION_STRING:-}" ]; then
  echo "Set NEON_CONNECTION_STRING first, e.g.:"
  echo "  export NEON_CONNECTION_STRING='postgresql://user:password@ep-xxx.neon.tech/dbname?sslmode=require'"
  exit 1
fi

DUMP_FILE="$(mktemp -t share_money_XXXXXX.dump)"
trap 'rm -f "$DUMP_FILE"' EXIT

echo "Dumping schema 'shmy' from local $LOCAL_DB@$LOCAL_HOST:$LOCAL_PORT ..."
pg_dump -h "$LOCAL_HOST" -p "$LOCAL_PORT" -U "$LOCAL_USER" -d "$LOCAL_DB" \
  -n shmy -F c -f "$DUMP_FILE"

echo "Restoring into Neon ..."
pg_restore -d "$NEON_CONNECTION_STRING" --clean --if-exists --no-owner --no-privileges "$DUMP_FILE"

echo "Done. Local 'shmy' schema (including flyway_schema_history) has been copied into Neon."
