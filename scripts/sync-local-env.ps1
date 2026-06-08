# Sync repo-root .env and frontend/homepage/.env for local Docker dev.
# Reads secrets from backend/.env (never printed). Idempotent: updates or appends keys.
param(
    [switch]$FromRailway,
    [string]$RailwayService = 'AboutMe',
    [string]$RailwayFrontendService = 'resilient-emotion'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$rootEnv = Join-Path $root '.env'
$backendEnv = Join-Path $root 'backend\.env'
$dockerEnv = Join-Path $root '.env.docker'
$frontendEnv = Join-Path $root 'frontend\homepage\.env'

function Read-EnvMap([string]$path) {
    $map = @{}
    if (-not (Test-Path $path)) { return $map }
    Get-Content $path | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$' -and $_ -notmatch '^\s*#') {
            $map[$matches[1]] = $matches[2].Trim()
        }
    }
    $map
}

function Merge-EnvFile([string]$path, [hashtable]$updates) {
    $lines = @()
    if (Test-Path $path) { $lines = Get-Content $path }
    $index = @{}
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=') {
            $index[$matches[1]] = $i
        }
    }
    foreach ($key in $updates.Keys) {
        $line = "$key=$($updates[$key])"
        if ($index.ContainsKey($key)) {
            $lines[$index[$key]] = $line
        } else {
            $lines += $line
        }
    }
    if (-not ($lines -match 'Local Docker dev')) {
        $lines += ''
        $lines += '# --- Local Docker dev (scripts/sync-local-env.ps1) ---'
    }
    Set-Content -Path $path -Value $lines -Encoding utf8
}

$backend = Read-EnvMap $backendEnv
$docker = Read-EnvMap $dockerEnv

if ($FromRailway) {
    $env:RAILWAY_CALLER = 'sync-local-env'
    $json = railway variables --json -s $RailwayService 2>$null | ConvertFrom-Json
    if ($json) {
        foreach ($prop in $json.PSObject.Properties) {
            $backend[$prop.Name] = [string]$prop.Value
        }
    }
    $feJson = railway variables --json -s $RailwayFrontendService 2>$null | ConvertFrom-Json
    $vite = @{}
    if ($feJson) {
        foreach ($prop in $feJson.PSObject.Properties) {
            if ($prop.Name -like 'VITE_*') {
                $vite[$prop.Name] = [string]$prop.Value
            }
        }
    }
    if ($vite.Count -gt 0) {
        Merge-EnvFile $frontendEnv $vite
        Write-Host "Updated frontend/homepage/.env from Railway ($RailwayFrontendService)."
    }
}

if (-not $backend['OPENAI_API_KEY']) {
    Write-Error "OPENAI_API_KEY missing. Set it in backend/.env or run: ./scripts/sync-local-env.ps1 -FromRailway"
}

$rootUpdates = @{
    POSTGRES_PASSWORD            = $(if ($docker['POSTGRES_PASSWORD']) { $docker['POSTGRES_PASSWORD'] } else { 'postgres' })
    SPRING_DATASOURCE_PASSWORD   = $(if ($docker['SPRING_DATASOURCE_PASSWORD']) { $docker['SPRING_DATASOURCE_PASSWORD'] } else { 'postgres' })
    OPENAI_API_KEY               = $backend['OPENAI_API_KEY']
    OPENAI_CHAT_ENABLED          = $(if ($backend['OPENAI_CHAT_ENABLED']) { $backend['OPENAI_CHAT_ENABLED'] } else { 'true' })
    PORTFOLIO_REALTIME_ENABLED   = $(if ($backend['PORTFOLIO_REALTIME_ENABLED']) { $backend['PORTFOLIO_REALTIME_ENABLED'] } else { 'true' })
    PORTFOLIO_JWT_SECRET         = $(if ($backend['PORTFOLIO_JWT_SECRET']) { $backend['PORTFOLIO_JWT_SECRET'] } else { 'dev-only-change-me-use-32-chars-minimum!!' })
    AI_BUDGET_ANON_SALT          = $(if ($backend['AI_BUDGET_ANON_SALT']) { $backend['AI_BUDGET_ANON_SALT'] } else { 'portfolio-ai-budget-local-dev' })
    ADMIN_BOOTSTRAP_USERNAME     = $(if ($backend['ADMIN_BOOTSTRAP_USERNAME']) { $backend['ADMIN_BOOTSTRAP_USERNAME'] } else { 'admin' })
    ADMIN_BOOTSTRAP_PASSWORD     = $(if ($backend['ADMIN_BOOTSTRAP_PASSWORD']) { $backend['ADMIN_BOOTSTRAP_PASSWORD'] } else { 'admin' })
}
Merge-EnvFile $rootEnv $rootUpdates
Write-Host "Updated repo-root .env for Docker Compose."

if (-not (Test-Path $frontendEnv)) {
    $viteDefaults = @{
        VITE_POSTHOG_ENABLED = $(if ($backend['POSTHOG_ENABLED']) { $backend['POSTHOG_ENABLED'].ToLower() } else { 'false' })
        VITE_POSTHOG_KEY       = $(if ($backend['POSTHOG_API_KEY']) { $backend['POSTHOG_API_KEY'] } else { '' })
        VITE_POSTHOG_HOST      = $(if ($backend['POSTHOG_HOST']) { $backend['POSTHOG_HOST'] } else { 'https://eu.i.posthog.com' })
    }
    Merge-EnvFile $frontendEnv $viteDefaults
    Write-Host "Created frontend/homepage/.env for Vite/PostHog."
}
