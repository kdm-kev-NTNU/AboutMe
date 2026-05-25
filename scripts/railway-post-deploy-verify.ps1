# Post-deploy smoke: health, admin session cookie (no basicToken), CSRF on admin mutation.
# Usage:
#   .\scripts\railway-post-deploy-verify.ps1 -BaseUrl https://your-backend.up.railway.app `
#     -AdminUsername kevin -AdminPassword 'your-password'

param(
    [Parameter(Mandatory = $true)]
    [string] $BaseUrl,

    [Parameter(Mandatory = $true)]
    [string] $AdminUsername,

    [Parameter(Mandatory = $true)]
    [string] $AdminPassword
)

$ErrorActionPreference = 'Stop'

function Join-Url {
    param([string] $Base, [string] $Path)
    return ($Base.TrimEnd('/') + '/' + $Path.TrimStart('/'))
}

$base = $BaseUrl.TrimEnd('/')
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "1. GET /actuator/health"
$health = Invoke-WebRequest -Uri (Join-Url $base '/actuator/health') -UseBasicParsing
if ($health.StatusCode -ne 200) { throw "Health returned $($health.StatusCode)" }
Write-Host "   OK ($($health.StatusCode))"

Write-Host "2. POST /auth/login (session cookie, no Basic in body)"
$loginBody = @{ username = $AdminUsername; password = $AdminPassword } | ConvertTo-Json
$login = Invoke-WebRequest -Uri (Join-Url $base '/auth/login') -Method POST `
    -ContentType 'application/json' -Body $loginBody -WebSession $session -UseBasicParsing
if ($login.StatusCode -ne 200) { throw "Login returned $($login.StatusCode): $($login.Content)" }
$loginJson = $login.Content | ConvertFrom-Json
if ($loginJson.role -ne 'ADMIN') { throw "Expected ADMIN role, got $($loginJson.role)" }
$setCookie = $login.Headers['Set-Cookie']
if (-not $setCookie -or ($setCookie -notmatch 'PORTFOLIO_SESSION')) {
    throw "Missing PORTFOLIO_SESSION Set-Cookie header"
}
if ($login.Content -match 'basicToken') {
    throw "Login response must not contain basicToken"
}
Write-Host "   OK — PORTFOLIO_SESSION cookie set, role=$($loginJson.role)"

Write-Host "3. GET /auth/me (cookie auth)"
$me = Invoke-WebRequest -Uri (Join-Url $base '/auth/me') -WebSession $session -UseBasicParsing
if ($me.StatusCode -ne 200) { throw "/auth/me returned $($me.StatusCode)" }
Write-Host "   OK"

Write-Host "4. Prime CSRF — GET /admin/tools/ai/status"
$statusReq = Invoke-WebRequest -Uri (Join-Url $base '/admin/tools/ai/status') -WebSession $session -UseBasicParsing
if ($statusReq.StatusCode -ne 200) { throw "AI status returned $($statusReq.StatusCode)" }
$xsrfCookie = $session.Cookies.GetCookies($base) | Where-Object { $_.Name -eq 'XSRF-TOKEN' }
if (-not $xsrfCookie) {
    throw "Missing XSRF-TOKEN cookie after authenticated GET (CSRF not primed)"
}
$xsrf = $xsrfCookie.Value
Write-Host "   OK — XSRF-TOKEN present"

Write-Host "5. POST /admin/tools/ai/kill-switch with X-XSRF-TOKEN"
$killBody = '{"open":false}' 
$kill = Invoke-WebRequest -Uri (Join-Url $base '/admin/tools/ai/kill-switch') -Method POST `
    -WebSession $session -UseBasicParsing `
    -ContentType 'application/json' -Body $killBody `
    -Headers @{ 'X-XSRF-TOKEN' = $xsrf }
if ($kill.StatusCode -notin 200, 202) {
    throw "Kill-switch POST returned $($kill.StatusCode): $($kill.Content)"
}
Write-Host "   OK ($($kill.StatusCode)) — CSRF accepted"

Write-Host "6. GET /health/vectorstore"
$vs = Invoke-WebRequest -Uri (Join-Url $base '/health/vectorstore') -UseBasicParsing
if ($vs.StatusCode -ne 200) { throw "vectorstore health returned $($vs.StatusCode)" }
Write-Host "   OK"

Write-Host ""
Write-Host "Post-deploy verification passed."
Write-Host "Reminder: clear ADMIN_BOOTSTRAP_PASSWORD in Railway after first admin exists."
