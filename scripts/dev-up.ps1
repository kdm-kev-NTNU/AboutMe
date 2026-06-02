# Start AboutMe Docker dev stack and wait until API + Vite are ready.
param(
    [switch]$FromRailway,
    [switch]$Recreate,
    [int]$FrontendPort = 5174
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$syncParams = @{}
if ($FromRailway) { $syncParams['FromRailway'] = $true }
& "$PSScriptRoot\sync-local-env.ps1" @syncParams

$otherOn5173 = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalAddress -in '127.0.0.1', '0.0.0.0', '::' }
if ($otherOn5173) {
    Write-Host ''
    Write-Host 'NOTE: Port 5173 is used by another app on your machine.' -ForegroundColor Yellow
    Write-Host "AboutMe Docker frontend is on http://127.0.0.1:$FrontendPort" -ForegroundColor Yellow
    Write-Host ''
}

$composeArgs = @('--profile', 'dev', 'up', '-d')
if ($Recreate) { $composeArgs += '--force-recreate' }
docker compose @composeArgs

$apiUrl = 'http://127.0.0.1:8080/actuator/health'
$appUrl = "http://127.0.0.1:$FrontendPort"
$deadline = (Get-Date).AddMinutes(5)

Write-Host 'Waiting for backend...'
do {
    try {
        $r = Invoke-WebRequest -Uri $apiUrl -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { break }
    } catch { }
    Start-Sleep -Seconds 2
    if ((Get-Date) -gt $deadline) { Write-Error "Backend not ready at $apiUrl" }
} while ($true)

Write-Host 'Waiting for frontend...'
do {
    try {
        $r = Invoke-WebRequest -Uri $appUrl -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { break }
    } catch { }
    Start-Sleep -Seconds 2
    if ((Get-Date) -gt $deadline) { Write-Error "Frontend not ready at $appUrl" }
} while ($true)

Write-Host ''
Write-Host 'AboutMe dev stack is ready:' -ForegroundColor Green
Write-Host "  App:           $appUrl"
Write-Host "  Chat:          $appUrl/chat"
Write-Host "  Voice:         $appUrl/voice"
Write-Host "  Privacy:       $appUrl/privacy-policy"
Write-Host "  Accessibility: $appUrl/accessibility"
Write-Host '  API:           http://127.0.0.1:8080'
Write-Host '  Swagger:       http://127.0.0.1:8080/swagger-ui/index.html'
