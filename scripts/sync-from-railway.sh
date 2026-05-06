#!/usr/bin/env bash
# Sync public.vector_store from Railway Postgres to local Docker Postgres (pg_dump + psql).
# Requires: pg_dump, psql (PostgreSQL client tools) on PATH.
#
# Remote (Railway) — set one of:
#   RAILWAY_PGHOST, RAILWAY_PGPORT, RAILWAY_PGDATABASE, RAILWAY_PGUSER, RAILWAY_PGPASSWORD
# or legacy:
#   PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD  (only if not using local defaults below)
#
# Local (docker-compose db) — defaults match repo docker-compose.yml:
#   LOCAL_PGHOST=localhost LOCAL_PGPORT=5432 LOCAL_PGDATABASE=aboutme
#   LOCAL_PGUSER=postgres LOCAL_PGPASSWORD=postgres
#
# Usage:
#   ./scripts/sync-from-railway.sh --clean    # TRUNCATE local vector_store, then import
#   ./scripts/sync-from-railway.sh --merge    # import only (pg_dump may still emit INSERTs; use backend API for pure merge)

set -euo pipefail

CLEAN=false
MERGE=false
for arg in "$@"; do
  case "$arg" in
    --clean) CLEAN=true ;;
    --merge) MERGE=true ;;
    -h|--help)
      echo "Usage: $0 --clean | --merge"
      echo "  --clean  TRUNCATE public.vector_store on local DB before restore"
      echo "  --merge  Restore without truncating (overlapping keys may error unless dump uses ON CONFLICT)"
      exit 0
      ;;
  esac
done

if [[ "$CLEAN" == false && "$MERGE" == false ]]; then
  echo "Specify --clean or --merge" >&2
  exit 1
fi
if [[ "$CLEAN" == true && "$MERGE" == true ]]; then
  echo "Use only one of --clean or --merge" >&2
  exit 2
fi

RH="${RAILWAY_PGHOST:-${PGHOST:-}}"
RPORT="${RAILWAY_PGPORT:-${PGPORT:-5432}}"
RDB="${RAILWAY_PGDATABASE:-${PGDATABASE:-}}"
RUSER="${RAILWAY_PGUSER:-${PGUSER:-}}"
RPASS="${RAILWAY_PGPASSWORD:-${PGPASSWORD:-}}"

if [[ -z "$RH" || -z "$RDB" || -z "$RUSER" ]]; then
  echo "Set remote credentials, e.g. RAILWAY_PGHOST, RAILWAY_PGDATABASE, RAILWAY_PGUSER (and RAILWAY_PGPASSWORD)." >&2
  exit 3
fi

LH="${LOCAL_PGHOST:-localhost}"
LPORT="${LOCAL_PGPORT:-5432}"
LDB="${LOCAL_PGDATABASE:-aboutme}"
LUSER="${LOCAL_PGUSER:-postgres}"
LPASS="${LOCAL_PGPASSWORD:-postgres}"

TMP="$(mktemp -t vector_store_dump.XXXXXX.sql)"
cleanup() { rm -f "$TMP"; }
trap cleanup EXIT

export PGPASSWORD="$RPASS"
pg_dump -h "$RH" -p "$RPORT" -U "$RUSER" -d "$RDB" \
  --schema=public --table=public.vector_store \
  --data-only --no-owner --no-privileges \
  -f "$TMP"

export PGPASSWORD="$LPASS"
if [[ "$CLEAN" == true ]]; then
  psql -h "$LH" -p "$LPORT" -U "$LUSER" -d "$LDB" -v ON_ERROR_STOP=1 \
    -c "TRUNCATE TABLE public.vector_store;"
fi

psql -h "$LH" -p "$LPORT" -U "$LUSER" -d "$LDB" -v ON_ERROR_STOP=1 -f "$TMP"

echo "Done: vector_store synced from $RH to $LH"
