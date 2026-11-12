# Migration Execution Script for Task 20
$ErrorActionPreference = 'Stop'

Write-Host "=========================================================="
Write-Host "PHASE 2: FLYWAY MIGRATION EXECUTION ON business_billing_db"
Write-Host "=========================================================="

$env:DB_HOST = 'localhost'
$env:DB_PORT = '5432'
$env:DB_NAME = 'business_billing_db'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'postgres'

Write-Host "Target Connection Details:"
Write-Host "  DB_HOST     : $env:DB_HOST"
Write-Host "  DB_PORT     : $env:DB_PORT"
Write-Host "  DB_NAME     : $env:DB_NAME"
Write-Host "  DB_USER     : $env:DB_USER"
Write-Host "  Target JDBC : jdbc:postgresql://$($env:DB_HOST):$($env:DB_PORT)/$($env:DB_NAME)"

# Safety assertion: Must explicitly be business_billing_db
if ($env:DB_NAME -ne 'business_billing_db') {
    Write-Error "CRITICAL SAFETY ABORT: Target is NOT business_billing_db!"
    exit 1
}

$backendDir = Join-Path (Get-Location) "backend"
Push-Location $backendDir

try {
    Write-Host "`n--- Step 2.1: Pre-Migration Flyway Info ---"
    .\mvnw.cmd flyway:info
    if ($LASTEXITCODE -ne 0) {
        Write-Error "flyway:info failed with exit code $LASTEXITCODE"
    }

    Write-Host "`n--- Step 2.2: Executing flyway:migrate (EXACTLY ONCE) ---"
    .\mvnw.cmd flyway:migrate
    $migrateExitCode = $LASTEXITCODE
    if ($migrateExitCode -ne 0) {
        Write-Error "CRITICAL: flyway:migrate failed with exit code $migrateExitCode! Halting immediately."
    }

    Write-Host "`n--- Step 2.3: Post-Migration Flyway Info ---"
    .\mvnw.cmd flyway:info
    if ($LASTEXITCODE -ne 0) {
        Write-Error "post-migration flyway:info failed with exit code $LASTEXITCODE"
    }

    Write-Host "`n>>> PHASE 2 MIGRATION COMPLETED SUCCESSFULLY <<<"
}
finally {
    Pop-Location
}
