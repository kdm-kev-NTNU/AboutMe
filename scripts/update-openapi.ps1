# Refresh frontend/homepage/openapi/openapi.json and regenerate the Orval client.
# Run from repo root: .\scripts\update-openapi.ps1

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host 'Patching openapi.json with extension paths...'
node (Join-Path $RepoRoot 'scripts\patch-openapi-extensions.mjs')

Write-Host 'Running OpenAPI contract test (optional export with -Dopenapi.export=true)...'
Push-Location (Join-Path $RepoRoot 'backend')
& .\mvnw.cmd -q test '-Dtest=SecurityConfigIT#openApiSnapshotIncludesEndpointsAndOptionalExport' '-Dopenapi.export=true'
Pop-Location

Write-Host 'Regenerating Orval client...'
Push-Location (Join-Path $RepoRoot 'frontend\homepage')
npm run api:generate
Pop-Location

Write-Host 'Done. Commit openapi/openapi.json and src/api/generated/portfolio.ts if changed.'
