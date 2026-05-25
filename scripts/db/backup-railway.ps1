# Full public-schema backup before Flyway V2+ deploy (Railway / prod).
# Usage (from repo root):
#   $env:DATABASE_URL = "postgresql://user:pass@host:port/db"
#   .\scripts\db\backup-railway.ps1
# Or pass -DatabaseUrl explicitly.

param(
    [string] $DatabaseUrl = $env:DATABASE_URL,
    [string] $OutputDir = "."
)

$ErrorActionPreference = 'Stop'

if (-not $DatabaseUrl) {
    throw "Set DATABASE_URL or pass -DatabaseUrl (postgresql://...)"
}

$stamp = Get-Date -Format 'yyyyMMdd'
$outFile = Join-Path $OutputDir "aboutme_backup_$stamp.dump"

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump not found. Install PostgreSQL client tools and ensure pg_dump is on PATH."
}

Write-Host "Backing up to $outFile ..."
& pg_dump $DatabaseUrl --schema=public -Fc -f $outFile
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed with exit code $LASTEXITCODE" }
Write-Host "Backup complete: $outFile"
