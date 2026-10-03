#!/usr/bin/env bash
# Logical backup of the local Docker Compose PostgreSQL database.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p database/backups
FILE="database/backups/shopsphere-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose exec -T postgres pg_dump -U "${DB_USERNAME:-shopsphere}" -d "${DB_NAME:-shopsphere}" --clean --if-exists | gzip > "$FILE"
echo "Backup written to $FILE"
