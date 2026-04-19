#Requires -Version 5.1
<#
.SYNOPSIS
  Starts Docker infra (MySQL, ChromaDB, Phoenix), then opens backend and frontend dev servers in new windows.
.DESCRIPTION
  Run from anywhere: pwsh -File .\scripts\dev.ps1
  Expects a .env file at the repository root (copy from .env.example). Backend runs with repo root as cwd
  so Spring loads .env at the repo root as documented in application.yaml.
  Only starts db, chromadb, and phoenix so ports 8080/5173 stay free for local Spring Boot and Vite.
#>

$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$EnvFile = Join-Path $RepoRoot '.env'

function Write-Info([string] $Message) {
    Write-Host $Message -ForegroundColor Cyan
}

function Write-Warn([string] $Message) {
    Write-Warning $Message
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Warn "Docker CLI not found. Install Docker Desktop and ensure 'docker' is on PATH."
    exit 1
}

if (-not (Test-Path -LiteralPath $EnvFile)) {
    Write-Warn "Missing $EnvFile - copy .env.example to .env in the repo root and set OPENAI_API_KEY (and PORT, DB_*, etc.)."
    exit 1
}

$envText = Get-Content -LiteralPath $EnvFile -Raw
if ($envText -notmatch '(?m)^\s*OPENAI_API_KEY\s*=\s*\S') {
    Write-Warn "OPENAI_API_KEY appears missing or empty in .env. Set it before running the stack."
    exit 1
}

$shell = if (Get-Command pwsh -ErrorAction SilentlyContinue) { 'pwsh' } else { 'powershell' }

Push-Location $RepoRoot
try {
    Write-Info "Starting MySQL, ChromaDB, and Phoenix (docker compose up -d db chromadb phoenix)..."
    & docker compose up -d db chromadb phoenix
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose exited with code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

Write-Info "Waiting for MySQL on port 3307..."
$deadline = (Get-Date).AddSeconds(90)
$ready = $false
while ((Get-Date) -lt $deadline) {
    try {
        $tcp = Test-NetConnection -ComputerName 'localhost' -Port 3307 -WarningAction SilentlyContinue
        if ($tcp.TcpTestSucceeded) {
            $ready = $true
            break
        }
    }
    catch {
        # ignore and retry
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    Write-Warn "MySQL did not become reachable on localhost:3307 within 90s. You can still try starting the backend manually."
}

$repoRootEscaped = $RepoRoot.Replace("'", "''")

$backendCommand = "Set-Location -LiteralPath '$repoRootEscaped'; .\backend\mvnw.cmd -f backend\pom.xml spring-boot:run"
$frontendCommand =
    "Set-Location -LiteralPath '$repoRootEscaped\frontend\homepage'; " +
    "if (-not (Test-Path -LiteralPath '.\node_modules')) { npm install }; " +
    "npm run dev"

Write-Info "Opening backend in a new window..."
Start-Process -FilePath $shell -ArgumentList @('-NoExit', '-Command', $backendCommand) -WorkingDirectory $RepoRoot

Write-Info "Opening frontend in a new window..."
Start-Process -FilePath $shell -ArgumentList @('-NoExit', '-Command', $frontendCommand) -WorkingDirectory $RepoRoot

Write-Info ""
Write-Info "Done. Frontend: http://localhost:5173"
Write-Info "Stop: Ctrl+C in each dev window. Optional: docker compose down (from repo root) when finished."
Write-Info ""
