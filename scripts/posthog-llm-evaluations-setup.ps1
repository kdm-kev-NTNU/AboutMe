<#
.SYNOPSIS
  Mirrors the PostHog MCP workflow: list evaluations, test Hog sources, create Hog + LLM-judge evals.

.DESCRIPTION
  Uses PostHog private REST API (same as MCP). Requires a Personal API Key with evaluation:read and evaluation:write.
  Project ID: PostHog UI → Project settings → Project API key page (numeric project id in URL).

  Env vars:
    POSTHOG_PERSONAL_API_KEY (required); not the same as POSTHOG_API_KEY used for event capture
    POSTHOG_PROJECT_ID (required)
    POSTHOG_APP_HOST (optional): default derived from POSTHOG_HOST or https://eu.posthog.com

.EXAMPLE
  $env:POSTHOG_PERSONAL_API_KEY = "phx_..."
  $env:POSTHOG_PROJECT_ID = "12345"
  ./scripts/posthog-llm-evaluations-setup.ps1
#>
[CmdletBinding()]
param(
  [string] $PersonalApiKey = $env:POSTHOG_PERSONAL_API_KEY,
  [string] $ProjectId = $env:POSTHOG_PROJECT_ID,
  [string] $AppHost = $env:POSTHOG_APP_HOST
)

$ErrorActionPreference = "Stop"

function Resolve-PostHogAppHost {
  param([string] $Explicit, [string] $FromCaptureHost)
  if ($Explicit) { return $Explicit.TrimEnd("/") }
  $h = $FromCaptureHost
  if (-not $h) { return "https://eu.posthog.com" }
  $h = $h.TrimEnd("/")
  if ($h -match "https://eu\.i\.posthog\.com") { return "https://eu.posthog.com" }
  if ($h -match "https://us\.i\.posthog\.com") { return "https://us.posthog.com" }
  if ($h -match "https://app\.posthog\.com") { return "https://us.posthog.com" }
  return "https://eu.posthog.com"
}

if (-not $PersonalApiKey -or -not $ProjectId) {
  throw @"
Missing POSTHOG_PERSONAL_API_KEY or POSTHOG_PROJECT_ID.
Create a Personal API key (scopes: evaluation:read, evaluation:write) in PostHog: Settings / Personal API keys.
Project ID: Settings / Project / Project ID.
"@
}

$base = Resolve-PostHogAppHost -Explicit $AppHost -FromCaptureHost $env:POSTHOG_HOST
$headers = @{
  Authorization = "Bearer $PersonalApiKey"
}

function Invoke-PhJson {
  param(
    [string] $Method,
    [string] $RelativePath,
    [object] $Body = $null
  )
  $uri = "$base/api/environments/$ProjectId$RelativePath"
  $params = @{
    Uri             = $uri
    Method          = $Method
    Headers         = $headers
    ContentType     = "application/json"
  }
  if ($null -ne $Body) {
    $params.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
  }
  return Invoke-RestMethod @params
}

Write-Host "Using API host: $base" -ForegroundColor Cyan

# --- Preflight: list evaluations ---
Write-Host "`n[1] GET evaluations (preflight)..." -ForegroundColor Cyan
$list = Invoke-PhJson -Method GET -RelativePath "/evaluations/"
$existing = @{}
foreach ($e in $list.results) {
  if (-not $e.deleted) { $existing[$e.name] = $e }
}
Write-Host "Found $($existing.Count) non-deleted evaluation(s)."

function Test-HogSource {
  param([string] $Label, [string] $Source)
  Write-Host "`n  test_hog: $Label" -ForegroundColor Yellow
  $resp = Invoke-PhJson -Method POST -RelativePath "/evaluations/test_hog/" -Body @{
    source        = $Source
    sample_count  = 5
    allows_na     = $false
  }
  foreach ($r in $resp.results) {
    $err = if ($r.error) { " err=$($r.error)" } else { "" }
    Write-Host "    event=$($r.event_uuid) result=$($r.result)$err"
  }
  if ($resp.message) { Write-Host "    message: $($resp.message)" }
}

function Ensure-HogEvaluation {
  param(
    [string] $Name,
    [string] $Description,
    [string] $Source
  )
  if ($existing.ContainsKey($Name)) {
    Write-Host "Skip create (exists): $Name" -ForegroundColor DarkGray
    return
  }
  Test-HogSource -Label $Name -Source $Source
  Write-Host "  POST create: $Name" -ForegroundColor Green
  $created = Invoke-PhJson -Method POST -RelativePath "/evaluations/" -Body @{
    name              = $Name
    description       = $Description
    enabled           = $true
    evaluation_type   = "hog"
    evaluation_config = @{ source = $Source }
    output_type       = "boolean"
    output_config     = @{ allows_na = $false }
  }
  Write-Host "  Created id=$($created.id)" -ForegroundColor Green
  $existing[$Name] = $created
}

function Ensure-LlmJudge {
  param(
    [string] $Name,
    [string] $Description,
    [string] $Prompt,
    [string] $Model = "gpt-4o-mini"
  )
  if ($existing.ContainsKey($Name)) {
    Write-Host "Skip create (exists): $Name" -ForegroundColor DarkGray
    return
  }
  Write-Host "`n  POST create (LLM judge, disabled): $Name" -ForegroundColor Green
  $created = Invoke-PhJson -Method POST -RelativePath "/evaluations/" -Body @{
    name                 = $Name
    description          = $Description
    enabled              = $false
    evaluation_type      = "llm_judge"
    evaluation_config    = @{ prompt = $Prompt }
    output_type          = "boolean"
    output_config        = @{ allows_na = $true }
    model_configuration  = @{
      provider = "openai"
      model    = $Model
    }
  }
  Write-Host "  Created id=$($created.id)" -ForegroundColor Green
  $existing[$Name] = $created
}

# --- Hog evaluators (from docs/posthog-llm-evaluations-setup.md) ---
$hogLatency = @'
let lat := event.properties.$ai_latency
return lat == null or lat <= 15
'@

$hogOutput = @'
let choices := event.properties.$ai_output_choices
return choices != null and size(choices) > 0 and length(choices[1].content) > 0
'@

$hogTokens = @'
let t := event.properties.$ai_output_tokens
return t == null or t <= 2000
'@

$hogError = @'
return event.properties.$ai_is_error != true
'@

Ensure-HogEvaluation -Name "RAG latency under 15s" `
  -Description "Pass when `$ai_latency is missing or ≤ 15 seconds" `
  -Source $hogLatency

Ensure-HogEvaluation -Name "Assistant output non-empty" `
  -Description "Pass when `$ai_output_choices has a first choice with non-empty content (Hog index 1)" `
  -Source $hogOutput

Ensure-HogEvaluation -Name "Output tokens under 2000" `
  -Description "Pass when `$ai_output_tokens is missing or ≤ 2000" `
  -Source $hogTokens

Ensure-HogEvaluation -Name "Generation not marked error" `
  -Description "Pass when `$ai_is_error is not true" `
  -Source $hogError

# --- LLM judges (disabled) ---
$promptRelevance = @'
You are evaluating whether the assistant's reply addresses the user's question. Use the generation event properties: user messages are typically in $ai_input, assistant output in $ai_output_choices. Return true if the reply answers the question, false if it ignores or misses it. Return N/A if there is no clear user question.
'@

$promptFaithfulness = @'
You are evaluating whether the assistant's answer is grounded in RAG context. Use $ai_input, $ai_output_choices, and any retrieved context available on the event (e.g. $ai_context or related properties). If there is no RAG context for this generation, return N/A. Otherwise return true only if factual claims in the answer are supported by the context; false if any claim is unsupported.
'@

Ensure-LlmJudge -Name "Answer addresses user question" `
  -Description "LLM judge: relevance to user question (starts disabled)" `
  -Prompt $promptRelevance

Ensure-LlmJudge -Name "RAG answer grounded in context" `
  -Description "LLM judge: faithfulness to retrieved context (starts disabled)" `
  -Prompt $promptFaithfulness

Write-Host "`nDone. UI: $base/llm-analytics/evaluations" -ForegroundColor Cyan
