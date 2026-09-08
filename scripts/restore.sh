#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <path-to-dump-file>"
  exit 1
fi

DUMP_FILE="$1"
CONTAINER="${POSTGRES_CONTAINER:-share_money_backend-postgres-1}"
DB_USER="${DB_USERNAME:-share_money}"
DB_NAME="${DB_NAME:-share_money}"

if [ ! -f "$DUMP_FILE" ]; then
  echo "Dump file not found: $DUMP_FILE"
  exit 1
fi

docker cp "$DUMP_FILE" "$CONTAINER:/tmp/restore.dump"
docker exec "$CONTAINER" pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists /tmp/restore.dump
docker exec "$CONTAINER" rm -f /tmp/restore.dump

echo "Restore complete from $DUMP_FILE"
