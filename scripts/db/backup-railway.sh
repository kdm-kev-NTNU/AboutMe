#!/usr/bin/env bash
# Full public-schema backup before Flyway V2+ deploy (Railway / prod).
# Usage: DATABASE_URL=postgresql://... ./scripts/db/backup-railway.sh [output_dir]

set -euo pipefail

DATABASE_URL="${DATABASE_URL:-}"
OUTPUT_DIR="${1:-.}"

if [[ -z "$DATABASE_URL" ]]; then
  echo "Set DATABASE_URL (postgresql://user:pass@host:port/db)" >&2
  exit 1
fi

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "pg_dump not found. Install PostgreSQL client tools." >&2
  exit 1
fi

stamp="$(date +%Y%m%d)"
out="${OUTPUT_DIR}/aboutme_backup_${stamp}.dump"
echo "Backing up to ${out} ..."
pg_dump "$DATABASE_URL" --schema=public -Fc -f "$out"
echo "Backup complete: ${out}"
