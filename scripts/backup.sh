#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
CONTAINER="${POSTGRES_CONTAINER:-share_money_backend-postgres-1}"
DB_USER="${DB_USERNAME:-share_money}"
DB_NAME="${DB_NAME:-share_money}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

mkdir -p "$BACKUP_DIR"
OUT_FILE="$BACKUP_DIR/share_money_${TIMESTAMP}.dump"

docker exec "$CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" -F c -f "/tmp/backup.dump"
docker cp "$CONTAINER:/tmp/backup.dump" "$OUT_FILE"
docker exec "$CONTAINER" rm -f /tmp/backup.dump

echo "Backup written to $OUT_FILE"

find "$BACKUP_DIR" -name "share_money_*.dump" -mtime "+$RETENTION_DAYS" -print -delete
