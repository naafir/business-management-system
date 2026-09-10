param (
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$DbName = "business_billing_db",
    [string]$DbUser = "postgres",
    [string]$BackupDir = "..\backups"
)

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$OutputFile = "$BackupDir\backup_${DbName}_${Timestamp}.dump"
$ChecksumFile = "${OutputFile}.sha256"

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

Write-Host "[BACKUP] Starting PostgreSQL database backup for $DbName..." -ForegroundColor Cyan

# Execute pg_dump if installed
if (Get-Command pg_dump -ErrorAction SilentlyContinue) {
    & pg_dump -h $DbHost -p $DbPort -U $DbUser -F c -b -v -f $OutputFile $DbName
    if ($LASTEXITCODE -eq 0) {
        $Hash = (Get-FileHash -Path $OutputFile -Algorithm SHA256).Hash
        $Hash | Out-File -FilePath $ChecksumFile
        Write-Host "[BACKUP] SUCCESS: Backup saved to $OutputFile" -ForegroundColor Green
        Write-Host "[BACKUP] SHA256: $Hash" -ForegroundColor Green
    } else {
        Write-Error "[BACKUP] pg_dump failed with exit code $LASTEXITCODE"
    }
} else {
    Write-Warning "[BACKUP] pg_dump command not found in PATH. Ensure PostgreSQL client tools are installed or use Docker exec."
}
