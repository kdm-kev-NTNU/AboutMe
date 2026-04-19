$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $Root 'backend')
try {
    & ./mvnw.cmd -B verify
} finally {
    Pop-Location
}
Set-Location (Join-Path $Root 'frontend\homepage')
npm ci
npm run type-check
npm run lint:ci
npm run test:unit:coverage
