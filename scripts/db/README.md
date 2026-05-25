# Database migration scripts

## Pre-migration audit (run on staging before V2+)

```bash
psql "$DATABASE_URL" -f scripts/db/audit_pre_migration.sql
```

## Full backup (Railway / prod)

```bash
pg_dump "$DATABASE_URL" --schema=public -Fc -f aboutme_backup_$(date +%Y%m%d).dump
```

## Backfill (after Flyway V2–V9, idempotent)

```bash
psql "$DATABASE_URL" -f scripts/db/backfill_all.sql
psql "$DATABASE_URL" -f scripts/db/validate_constraints.sql
```

## Staging from Railway

Use repo-root `scripts/sync-from-railway.sh` for `vector_store` only. For full schema+data clone:

```bash
pg_dump "$RAILWAY_DATABASE_URL" --schema=public -Fc -f railway_full.dump
pg_restore -d "$STAGING_DATABASE_URL" --clean --if-exists railway_full.dump
```
