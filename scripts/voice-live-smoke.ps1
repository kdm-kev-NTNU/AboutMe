param(
    [Parameter(Mandatory = $true)]
    [string] $BaseUrl,

    [bool] $ExpectRealtimeEnabled = $true,

    [AllowEmptyString()]
    [string] $ApiPrefix = '/api',

    [bool] $CheckElevenLabsToken = $false,

    [string] $ElevenLabsAgentId = ''
)

$ErrorActionPreference = 'Stop'

function Join-Url {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Base,
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $b = $Base.TrimEnd('/')
    $p = $Path.TrimStart('/')
    return "$b/$p"
}

function Get-ResponseContent {
    param(
        [Parameter(Mandatory = $true)]
        $Response
    )

    if ($null -ne $Response.Content) {
        if ($Response.Content -is [string]) {
            return $Response.Content
        }
        if ($Response.Content.GetType().GetMethod('ReadAsStringAsync')) {
            return $Response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        }
    }

    if ($Response.GetResponseStream) {
        $stream = $Response.GetResponseStream()
        try {
            $reader = [System.IO.StreamReader]::new($stream)
            try {
                return $reader.ReadToEnd()
            } finally {
                $reader.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    }

    return ''
}

function Get-ResponseStatusCode {
    param(
        [Parameter(Mandatory = $true)]
        $Response
    )

    if ($Response.StatusCode -is [int]) {
        return [int] $Response.StatusCode
    }
    return [int] $Response.StatusCode.value__
}

function Get-ResponseContentType {
    param(
        [Parameter(Mandatory = $true)]
        $Response
    )

    if ($Response.Headers['Content-Type']) {
        return [string] $Response.Headers['Content-Type']
    }
    if ($Response.Content -and $Response.Content.Headers -and $Response.Content.Headers.ContentType) {
        return [string] $Response.Content.Headers.ContentType
    }
    return ''
}

function Read-JsonResponse {
    param(
        [Parameter(Mandatory = $true)]
        $Response
    )

    $content = Get-ResponseContent -Response $Response
    try {
        return $content | ConvertFrom-Json -ErrorAction Stop
    } catch {
        throw "Expected JSON response but got: $content"
    }
}

function Invoke-SmokeWebRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Uri,
        [Parameter(Mandatory = $true)]
        [string] $Method,
        [hashtable] $Headers = @{},
        [string] $ContentType,
        [AllowNull()]
        [string] $Body
    )

    $iwrParams = @{
        Uri = $Uri
        Method = $Method
        Headers = $Headers
    }
    if ((Get-Command Invoke-WebRequest).Parameters.ContainsKey('TimeoutSec')) {
        $iwrParams.TimeoutSec = 60
    }
    if ($ContentType) {
        $iwrParams.ContentType = $ContentType
    }
    # Windows PowerShell 5.1 rejects a body on GET/HEAD; only attach Body for other verbs when supplied.
    if ($PSBoundParameters.ContainsKey('Body') -and $Method -notin @('GET', 'HEAD')) {
        $iwrParams.Body = $Body
    }
    if ((Get-Command Invoke-WebRequest).Parameters.ContainsKey('SkipHttpErrorCheck')) {
        $iwrParams.SkipHttpErrorCheck = $true
    }

    try {
        return Invoke-WebRequest @iwrParams
    } catch {
        # Windows PowerShell 5.1 throws WebException; newer builds may throw HttpResponseException.
        $resp = $_.Exception.Response
        if ($null -ne $resp) {
            return $resp
        }
        throw
    }
}

$apiPrefixNormalized = if ([string]::IsNullOrWhiteSpace($ApiPrefix)) { '' } else { '/' + $ApiPrefix.Trim('/') }
$statusUrl = Join-Url -Base $BaseUrl -Path "$apiPrefixNormalized/realtime/status"
$sessionUrl = Join-Url -Base $BaseUrl -Path "$apiPrefixNormalized/realtime/session"
$modelsUrl = Join-Url -Base $BaseUrl -Path "$apiPrefixNormalized/realtime/models"
$elevenLabsTokenUrl = Join-Url -Base $BaseUrl -Path "$apiPrefixNormalized/realtime/elevenlabs/token"

Write-Host "Checking realtime status: $statusUrl"
$statusResponse = Invoke-SmokeWebRequest -Uri $statusUrl -Method GET -Headers @{ Accept = 'application/json' }
$statusCode = Get-ResponseStatusCode -Response $statusResponse
if ($statusCode -ne 200) {
    throw "Expected realtime/status to return HTTP 200, got HTTP $statusCode."
}
$statusJson = Read-JsonResponse -Response $statusResponse

if ($null -eq $statusJson.enabled) {
    throw "Expected status response to include an 'enabled' field."
}

$actualEnabled = [bool] $statusJson.enabled
if ($actualEnabled -ne $ExpectRealtimeEnabled) {
    throw "Expected realtime enabled=$ExpectRealtimeEnabled but got enabled=$actualEnabled from $statusUrl"
}

Write-Host "Realtime status OK: enabled=$actualEnabled"

Write-Host "Checking structured realtime/session error response: $sessionUrl"
$sessionResponse = Invoke-SmokeWebRequest `
    -Uri $sessionUrl `
    -Method POST `
    -ContentType 'application/sdp' `
    -Headers @{ Accept = 'application/json'; 'X-Chat-Language' = 'en' } `
    -Body ''

if ($null -eq $sessionResponse) {
    throw "No response received from realtime/session smoke request."
}

$statusCode = Get-ResponseStatusCode -Response $sessionResponse
if ($statusCode -lt 400 -or $statusCode -gt 599) {
    throw "Expected realtime/session smoke request to return 4xx/5xx, got HTTP $statusCode."
}

$contentType = Get-ResponseContentType -Response $sessionResponse
if ($contentType -notmatch 'application/json') {
    throw "Expected JSON error response from realtime/session, got Content-Type '$contentType'."
}

$sessionJson = Read-JsonResponse -Response $sessionResponse
if ([string]::IsNullOrWhiteSpace([string] $sessionJson.error)) {
    throw "Expected realtime/session error response to include a non-empty 'error' field."
}
if ([string]::IsNullOrWhiteSpace([string] $sessionJson.code)) {
    throw "Expected realtime/session error response to include a non-empty 'code' field."
}

Write-Host "Realtime session error contract OK: HTTP $statusCode code=$($sessionJson.code)"

if ($CheckElevenLabsToken) {
    Write-Host "Checking realtime models for ELEVENLABS provider: $modelsUrl"
    $modelsResponse = Invoke-SmokeWebRequest -Uri $modelsUrl -Method GET -Headers @{ Accept = 'application/json' }
    $modelsStatus = Get-ResponseStatusCode -Response $modelsResponse
    if ($modelsStatus -ne 200) {
        throw "Expected realtime/models to return HTTP 200, got HTTP $modelsStatus."
    }
    $modelsJson = Read-JsonResponse -Response $modelsResponse
    $elevenLabsModels = @($modelsJson | Where-Object { $_.provider -eq 'ELEVENLABS' })
    if ($elevenLabsModels.Count -eq 0) {
        throw "Expected at least one ELEVENLABS provider in realtime/models response."
    }

    $agentIdToUse = if ([string]::IsNullOrWhiteSpace($ElevenLabsAgentId)) { $elevenLabsModels[0].id } else { $ElevenLabsAgentId }
    if ([string]::IsNullOrWhiteSpace($agentIdToUse)) {
        throw "Could not resolve an ELEVENLABS agent id from response or parameter."
    }

    Write-Host "Minting ElevenLabs conversation token for agent '$agentIdToUse': $elevenLabsTokenUrl"
    $tokenBody = @{ modelId = $agentIdToUse } | ConvertTo-Json -Compress
    $tokenResponse = Invoke-SmokeWebRequest `
        -Uri $elevenLabsTokenUrl `
        -Method POST `
        -ContentType 'application/json' `
        -Headers @{ Accept = 'application/json' } `
        -Body $tokenBody

    $tokenStatus = Get-ResponseStatusCode -Response $tokenResponse
    if ($tokenStatus -ne 200) {
        $errBody = Get-ResponseContent -Response $tokenResponse
        throw "Expected realtime/elevenlabs/token to return HTTP 200, got HTTP $tokenStatus. Body: $errBody"
    }

    $tokenJson = Read-JsonResponse -Response $tokenResponse
    if ([string]::IsNullOrWhiteSpace([string] $tokenJson.token)) {
        throw "Expected realtime/elevenlabs/token response to include a non-empty 'token' field."
    }
    if ($tokenJson.token -notmatch '^[\w-]+\.[\w-]+\.[\w-]+$') {
        throw "Expected realtime/elevenlabs/token to return a 3-segment JWT, got: $($tokenJson.token.Substring(0, [Math]::Min(40, $tokenJson.token.Length)))..."
    }

    Write-Host "ElevenLabs token mint OK: HTTP $tokenStatus tokenLen=$($tokenJson.token.Length)"
}
