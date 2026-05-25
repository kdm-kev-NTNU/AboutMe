<#
.SYNOPSIS
  Quick-check that every SupportedChatModel ID is accepted by its provider API.

.DESCRIPTION
  Sends a minimal completion request (max 5 tokens) per model.
  Exit code 0 = all models valid; non-zero = at least one rejected.

.EXAMPLE
  $env:OPENAI_API_KEY = "sk-..."
  $env:ANTHROPIC_API_KEY = "<your-anthropic-api-key>"
  .\scripts\validate-models.ps1
#>
param(
  [string] $OpenAiKey    = $env:OPENAI_API_KEY,
  [string] $AnthropicKey = $env:ANTHROPIC_API_KEY
)

$ErrorActionPreference = 'Stop'
$failed = @()

$openaiModels = @('gpt-5.4-mini')
$anthropicModels = @('claude-haiku-4-5-20251001')

if ($OpenAiKey) {
  foreach ($m in $openaiModels) {
    Write-Host -NoNewline "  OpenAI  $m ... "
    $body = @{
      model = $m
      messages = @(@{ role = 'user'; content = 'Say hi' })
      max_completion_tokens = 5
    } | ConvertTo-Json -Depth 3

    try {
      $r = Invoke-WebRequest -Uri 'https://api.openai.com/v1/chat/completions' `
        -Method POST `
        -Headers @{ Authorization = "Bearer $OpenAiKey"; 'Content-Type' = 'application/json' } `
        -Body $body `
        -TimeoutSec 30 `
        -ErrorAction Stop
      Write-Host "OK ($($r.StatusCode))" -ForegroundColor Green
    } catch {
      $status = $_.Exception.Response.StatusCode.value__
      Write-Host "FAIL (HTTP $status)" -ForegroundColor Red
      $failed += $m
    }
  }
} else {
  Write-Host '  Skipping OpenAI models (OPENAI_API_KEY not set)' -ForegroundColor Yellow
}

if ($AnthropicKey) {
  foreach ($m in $anthropicModels) {
    Write-Host -NoNewline "  Anthropic  $m ... "
    $body = @{
      model = $m
      messages = @(@{ role = 'user'; content = 'Say hi' })
      max_tokens = 5
    } | ConvertTo-Json -Depth 3

    try {
      $r = Invoke-WebRequest -Uri 'https://api.anthropic.com/v1/messages' `
        -Method POST `
        -Headers @{
          'x-api-key' = $AnthropicKey
          'anthropic-version' = '2023-06-01'
          'Content-Type' = 'application/json'
        } `
        -Body $body `
        -TimeoutSec 30 `
        -ErrorAction Stop
      Write-Host "OK ($($r.StatusCode))" -ForegroundColor Green
    } catch {
      $status = $_.Exception.Response.StatusCode.value__
      Write-Host "FAIL (HTTP $status)" -ForegroundColor Red
      $failed += $m
    }
  }
} else {
  Write-Host '  Skipping Anthropic models (ANTHROPIC_API_KEY not set)' -ForegroundColor Yellow
}

Write-Host ''
if ($failed.Count -gt 0) {
  Write-Host "BROKEN models: $($failed -join ', ')" -ForegroundColor Red
  exit 1
} else {
  Write-Host 'All tested models are valid.' -ForegroundColor Green
  exit 0
}
