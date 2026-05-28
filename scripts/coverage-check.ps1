# Local coverage parity with CI gates (backend JaCoCo + frontend Vitest thresholds).
# Usage from repo root: .\scripts\coverage-check.ps1
# Optional: -SkipBackend or -SkipFrontend

param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot

function Write-Step($msg) {
    Write-Host "`n==> $msg" -ForegroundColor Cyan
}

if (-not $SkipBackend) {
    Write-Step 'Backend: mvn verify (tests + JaCoCo check)'
    Push-Location (Join-Path $Root 'backend')
    try {
        if (Test-Path '.\mvnw.cmd') {
            & .\mvnw.cmd -B verify
        } else {
            & ./mvnw -B verify
        }
        if ($LASTEXITCODE -ne 0) { throw "Backend verify failed with exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
    $jacocoHtml = Join-Path $Root 'backend\target\site\jacoco\index.html'
    if (Test-Path $jacocoHtml) {
        Write-Host "JaCoCo report: $jacocoHtml" -ForegroundColor Green
    }
}

if (-not $SkipFrontend) {
    Write-Step 'Frontend: unit tests with coverage thresholds'
    Push-Location (Join-Path $Root 'frontend\homepage')
    try {
        if (-not (Test-Path 'node_modules')) {
            $env:CYPRESS_INSTALL_BINARY = '0'
            npm ci
        }
        npm run test:unit:coverage
        if ($LASTEXITCODE -ne 0) { throw "Frontend coverage failed with exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
    $vitestHtml = Join-Path $Root 'frontend\homepage\coverage\index.html'
    if (Test-Path $vitestHtml) {
        Write-Host "Vitest coverage report: $vitestHtml" -ForegroundColor Green
    }
}

Write-Host "`nCoverage check passed (same gates as CI)." -ForegroundColor Green
