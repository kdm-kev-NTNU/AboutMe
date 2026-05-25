# Pre-deploy checklist for Railway backend (migrations V1–V14, prod profile).
# Does not push to Railway — run backup/audit locally, then deploy via git/Railway dashboard.
#
# Usage:
#   .\scripts\railway-prod-deploy.ps1 -StagingDatabaseUrl $env:STAGING_DATABASE_URL
#   .\scripts\railway-prod-deploy.ps1 -SkipAudit   # backup only

param(
    [string] $StagingDatabaseUrl = $env:STAGING_DATABASE_URL,
    [string] $ProdDatabaseUrl = $env:DATABASE_URL,
    [switch] $SkipAudit,
    [switch] $SkipBackup
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent

Write-Host "=== Railway production deploy checklist ===" -ForegroundColor Cyan
Write-Host ""

Write-Host "Required Railway variables (see backend/railway.env.example):" -ForegroundColor Yellow
@(
    'SPRING_PROFILES_ACTIVE=prod',
    'PORTFOLIO_JWT_SECRET (>= 32 chars)',
    'AI_BUDGET_ANON_SALT (not portfolio-ai-budget)',
    'OPENAI_API_KEY + OPENAI_CHAT_ENABLED=true',
    'Linked Postgres (PGHOST/...) or strong SPRING_DATASOURCE_PASSWORD',
    'ADMIN_BOOTSTRAP_USERNAME + ADMIN_BOOTSTRAP_PASSWORD (first deploy only)'
) | ForEach-Object { Write-Host "  - $_" }

Write-Host ""
Write-Host "Local CI gate (optional):" -ForegroundColor Yellow
Write-Host "  cd backend; .\mvnw.cmd -B verify"
Write-Host ""

if (-not $SkipBackup) {
    if (-not $ProdDatabaseUrl) {
        Write-Warning "DATABASE_URL not set — skip backup or set DATABASE_URL before deploy."
    } else {
        Write-Host "Running pg_dump backup..." -ForegroundColor Yellow
        & (Join-Path $repoRoot 'scripts\db\backup-railway.ps1') -DatabaseUrl $ProdDatabaseUrl -OutputDir $repoRoot
    }
}

if (-not $SkipAudit) {
    if (-not $StagingDatabaseUrl) {
        Write-Warning "STAGING_DATABASE_URL not set — run audit manually:"
        Write-Host "  psql `"`$STAGING_DATABASE_URL`" -f scripts/db/audit_pre_migration.sql"
    } else {
        if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
            throw "psql not found. Install PostgreSQL client tools."
        }
        Write-Host "Running audit_pre_migration.sql on staging..." -ForegroundColor Yellow
        $audit = Join-Path $repoRoot 'scripts\db\audit_pre_migration.sql'
        & psql $StagingDatabaseUrl -f $audit
        if ($LASTEXITCODE -ne 0) { throw "audit_pre_migration.sql failed" }
    }
}

Write-Host ""
Write-Host "Deploy: push to Railway-connected branch (Flyway V1–V14 on startup)." -ForegroundColor Green
Write-Host "Post-deploy:" -ForegroundColor Green
Write-Host "  .\scripts\railway-post-deploy-verify.ps1 -BaseUrl https://<host> -AdminUsername ... -AdminPassword ..."
Write-Host ""
Write-Host "Rollback: V11+ contract migrations are not Flyway-reversible — restore pg_dump snapshot." -ForegroundColor Yellow
Write-Host "After first admin login: remove ADMIN_BOOTSTRAP_PASSWORD from Railway variables." -ForegroundColor Yellow
