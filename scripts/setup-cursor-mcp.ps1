# One-time setup for Cursor MCP: .cursor/mcp.json template, Railway CLI, Docker MCP Toolkit profile "aboutme".
# Run from repo root: .\scripts\setup-cursor-mcp.ps1

$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$McpExample = Join-Path $RepoRoot ".cursor\mcp.json.example"
$McpLocal = Join-Path $RepoRoot ".cursor\mcp.json"

function Require-Command([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command on PATH: $Name"
    }
}

if (-not (Test-Path -LiteralPath $McpLocal)) {
    if (-not (Test-Path -LiteralPath $McpExample)) {
        throw "Missing $McpExample"
    }
    Copy-Item -LiteralPath $McpExample -Destination $McpLocal
    Write-Host "Created $McpLocal from .cursor/mcp.json.example"
} else {
    Write-Host "Keeping existing $McpLocal"
}

Write-Host ""
Write-Host "Set API keys in .cursor/mcp.json:"
Write-Host "  ELEVENLABS_API_KEY  (ElevenLabs MCP)"
Write-Host "  RAPIDCHART_API_TOKEN (RapidChart MCP, rc_... from https://rapidchart.com/settings)"
Write-Host ""

Require-Command railway
Require-Command docker

Write-Host "Installing Railway MCP for Cursor (stdio via Railway CLI)..."
& railway mcp install --agent cursor
if ($LASTEXITCODE -ne 0) { throw "railway mcp install failed with exit code $LASTEXITCODE" }

Write-Host "Pulling Docker MCP catalog..."
& docker mcp catalog pull mcp/docker-mcp-catalog:latest
if ($LASTEXITCODE -ne 0) { throw "docker mcp catalog pull failed with exit code $LASTEXITCODE" }

$profiles = & docker mcp profile list 2>&1 | Out-String
if ($profiles -notmatch "aboutme") {
    Write-Host "Creating Docker MCP profile 'aboutme'..."
    & docker mcp profile create --name aboutme --id aboutme
    if ($LASTEXITCODE -ne 0) { throw "docker mcp profile create failed with exit code $LASTEXITCODE" }
}

Write-Host "Adding docker-docs and github-official to profile 'aboutme'..."
& docker mcp profile server add aboutme `
    --server catalog://mcp/docker-mcp-catalog/docker-docs `
    --server catalog://mcp/docker-mcp-catalog/github-official
if ($LASTEXITCODE -ne 0) { throw "docker mcp profile server add failed with exit code $LASTEXITCODE" }

Write-Host ""
Write-Host "Done. Restart Cursor (Settings -> Tools & MCP) so Railway and MCP_DOCKER reconnect."
Write-Host "Railway: run 'railway login' if MCP tools report auth errors."
Write-Host "GitHub MCP: set GITHUB_PERSONAL_ACCESS_TOKEN in Docker MCP Toolkit secrets if needed."
