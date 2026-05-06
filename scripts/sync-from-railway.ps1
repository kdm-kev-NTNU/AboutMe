<#
.SYNOPSIS
  Sync public.vector_store from Railway Postgres to local Docker Postgres using pg_dump + psql.

.DESCRIPTION
  Requires PostgreSQL client tools (pg_dump, psql) on PATH.

  Remote (Railway): set RAILWAY_PGHOST, RAILWAY_PGPORT, RAILWAY_PGDATABASE, RAILWAY_PGUSER, RAILWAY_PGPASSWORD
  (or PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD if you do not use conflicting local PG* vars).

  Local defaults match docker-compose.yml: localhost:5432, database aboutme, user postgres.

.PARAMETER Clean
  TRUNCATE public.vector_store on the local database before importing.

.PARAMETER Merge
  Import without truncating first.

.EXAMPLE
  $env:RAILWAY_PGHOST = "xxx.railway.app"
  $env:RAILWAY_PGDATABASE = "railway"
  $env:RAILWAY_PGUSER = "postgres"
  $env:RAILWAY_PGPASSWORD = "secret"
  .\scripts\sync-from-railway.ps1 -Clean
#>
param(
  [switch] $Clean,
  [switch] $Merge
)

$ErrorActionPreference = "Stop"

if (-not $Clean -and -not $Merge) {
  Write-Error "Specify -Clean or -Merge"
}
if ($Clean -and $Merge) {
  Write-Error "Use only one of -Clean or -Merge"
}

$rh = if ($env:RAILWAY_PGHOST) { $env:RAILWAY_PGHOST } else { $env:PGHOST }
$rport = if ($null -ne $env:RAILWAY_PGPORT -and $env:RAILWAY_PGPORT -ne "") { $env:RAILWAY_PGPORT } elseif ($null -ne $env:PGPORT -and $env:PGPORT -ne "") { $env:PGPORT } else { "5432" }
$rdb = if ($env:RAILWAY_PGDATABASE) { $env:RAILWAY_PGDATABASE } else { $env:PGDATABASE }
$ruser = if ($env:RAILWAY_PGUSER) { $env:RAILWAY_PGUSER } else { $env:PGUSER }
$rpass = if ($null -ne $env:RAILWAY_PGPASSWORD) { $env:RAILWAY_PGPASSWORD } else { $env:PGPASSWORD }

if ([string]::IsNullOrWhiteSpace($rh) -or [string]::IsNullOrWhiteSpace($rdb) -or [string]::IsNullOrWhiteSpace($ruser)) {
  Write-Error "Set RAILWAY_PGHOST, RAILWAY_PGDATABASE, RAILWAY_PGUSER (and RAILWAY_PGPASSWORD)."
}

$lh = if ($env:LOCAL_PGHOST) { $env:LOCAL_PGHOST } else { "localhost" }
$lport = if ($env:LOCAL_PGPORT) { $env:LOCAL_PGPORT } else { "5432" }
$ldb = if ($env:LOCAL_PGDATABASE) { $env:LOCAL_PGDATABASE } else { "aboutme" }
$luser = if ($env:LOCAL_PGUSER) { $env:LOCAL_PGUSER } else { "postgres" }
$lpass = if ($null -ne $env:LOCAL_PGPASSWORD) { $env:LOCAL_PGPASSWORD } else { "postgres" }

$tmp = [System.IO.Path]::GetTempFileName() + ".sql"
try {
  $env:PGPASSWORD = $rpass
  & pg_dump -h $rh -p $rport -U $ruser -d $rdb `
    --schema=public --table=public.vector_store `
    --data-only --no-owner --no-privileges `
    -f $tmp
  if ($LASTEXITCODE -ne 0) { throw "pg_dump failed with exit $LASTEXITCODE" }

  $env:PGPASSWORD = $lpass
  if ($Clean) {
    & psql -h $lh -p $lport -U $luser -d $ldb -v ON_ERROR_STOP=1 -c "TRUNCATE TABLE public.vector_store;"
    if ($LASTEXITCODE -ne 0) { throw "psql TRUNCATE failed with exit $LASTEXITCODE" }
  }

  & psql -h $lh -p $lport -U $luser -d $ldb -v ON_ERROR_STOP=1 -f $tmp
  if ($LASTEXITCODE -ne 0) { throw "psql restore failed with exit $LASTEXITCODE" }

  Write-Host "Done: vector_store synced from $rh to $lh"
}
finally {
  Remove-Item -Force -ErrorAction SilentlyContinue $tmp
  Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
