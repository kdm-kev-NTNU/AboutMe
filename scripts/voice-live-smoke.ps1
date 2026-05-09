param(
    [Parameter(Mandatory = $true)]
    [string] $BaseUrl,

    [bool] $ExpectRealtimeEnabled = $true,

    [string] $ApiPrefix = '/api'
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

    $args = @{
        Uri = $Uri
        Method = $Method
        Headers = $Headers
    }
    if ($ContentType) {
        $args.ContentType = $ContentType
    }
    if ($null -ne $Body) {
        $args.Body = $Body
    }
    if ((Get-Command Invoke-WebRequest).Parameters.ContainsKey('SkipHttpErrorCheck')) {
        $args.SkipHttpErrorCheck = $true
    }

    try {
        return Invoke-WebRequest @args
    } catch [Microsoft.PowerShell.Commands.HttpResponseException] {
        return $_.Exception.Response
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) {
            return $_.Exception.Response
        }
        throw
    }
}

$apiPrefixNormalized = if ([string]::IsNullOrWhiteSpace($ApiPrefix)) { '' } else { '/' + $ApiPrefix.Trim('/') }
$statusUrl = Join-Url -Base $BaseUrl -Path "$apiPrefixNormalized/realtime/status"
$sessionUrl = Join-Url -Base $BaseUrl -Path "$apiPrefixNormalized/realtime/session"

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
