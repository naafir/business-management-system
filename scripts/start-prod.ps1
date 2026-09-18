#!/usr/bin/env pwsh
# ==============================================================================
# ApexBilling — Production Startup Script
# Usage: .\scripts\start-prod.ps1
# ==============================================================================

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  ApexBilling — Production Startup" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. Check .env exists ──────────────────────────────────────────────────────
$envFile = Join-Path $rootDir ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "[ERROR] .env file not found at: $envFile" -ForegroundColor Red
    Write-Host "        Copy .env.example to .env and fill in your production values." -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] .env file found." -ForegroundColor Green

# ── 2. Validate required env vars ─────────────────────────────────────────────
$required = @("DB_PASSWORD", "JWT_SECRET", "DB_NAME", "DB_USER")
$envContent = Get-Content $envFile | Where-Object { $_ -match "^[^#]" }
$missing = @()

foreach ($key in $required) {
    $found = $envContent | Where-Object { $_ -match "^$key=" -and ($_ -split "=", 2)[1].Trim() -ne "" }
    if (-not $found) { $missing += $key }
}

if ($missing.Count -gt 0) {
    Write-Host "[ERROR] Missing required environment variables in .env:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "        - $_" -ForegroundColor Yellow }
    exit 1
}
Write-Host "[OK] All required environment variables are set." -ForegroundColor Green

# ── 3. Check Docker is running ────────────────────────────────────────────────
try {
    docker info | Out-Null
    Write-Host "[OK] Docker is running." -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# ── 4. Build and start containers ─────────────────────────────────────────────
Write-Host ""
Write-Host "[...] Building and starting all services..." -ForegroundColor Yellow
Set-Location $rootDir

docker-compose --env-file .env up --build -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] docker-compose failed. Check the output above." -ForegroundColor Red
    exit 1
}

# ── 5. Wait for backend health check ─────────────────────────────────────────
Write-Host ""
Write-Host "[...] Waiting for backend to become healthy (up to 90 seconds)..." -ForegroundColor Yellow
$maxWait = 90
$waited = 0
$healthy = $false

while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 5
    $waited += 5
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $healthy = $true
            break
        }
    } catch {
        Write-Host "  Waiting... ($waited/$maxWait s)" -ForegroundColor DarkGray
    }
}

if (-not $healthy) {
    Write-Host "[WARN] Backend did not respond in time. Check logs:" -ForegroundColor Yellow
    Write-Host "       docker logs billing_backend" -ForegroundColor Yellow
} else {
    Write-Host "[OK] Backend is healthy!" -ForegroundColor Green
}

# ── 6. Summary ────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  ApexBilling is UP!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Frontend:  http://localhost" -ForegroundColor White
Write-Host "  API:       http://localhost/api/v1" -ForegroundColor White
Write-Host "  Health:    http://localhost/api/v1/health" -ForegroundColor White
Write-Host ""
Write-Host "  Login:     admin / Admin@SecurePass2026!" -ForegroundColor DarkYellow
Write-Host ""
Write-Host "  View logs:   docker-compose logs -f" -ForegroundColor DarkGray
Write-Host "  Stop:        docker-compose down" -ForegroundColor DarkGray
Write-Host ""
