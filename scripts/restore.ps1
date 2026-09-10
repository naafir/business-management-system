param (
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$TargetDb = "business_billing_db_restore",
    [string]$DbUser = "postgres"
)

if (-not (Test-Path $BackupFile)) {
    Write-Error "Backup file not found: $BackupFile"
    exit 1
}

$ChecksumFile = "${BackupFile}.sha256"
if (Test-Path $ChecksumFile) {
    $ExpectedHash = (Get-Content $ChecksumFile).Trim()
    $ActualHash = (Get-FileHash -Path $BackupFile -Algorithm SHA256).Hash
    if ($ExpectedHash -ne $ActualHash) {
        Write-Error "CHECKSUM VERIFICATION FAILED! Backup may be corrupted."
        exit 1
    }
    Write-Host "[RESTORE] SHA256 Checksum verified successfully." -ForegroundColor Green
}

Write-Host "[RESTORE] Restoring $BackupFile to $TargetDb..." -ForegroundColor Cyan
if (Get-Command pg_restore -ErrorAction SilentlyContinue) {
    & pg_restore -h $DbHost -p $DbPort -U $DbUser -d $TargetDb -v --clean --if-exists $BackupFile
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[RESTORE] Database restored successfully!" -ForegroundColor Green
    } else {
        Write-Error "[RESTORE] pg_restore failed with exit code $LASTEXITCODE"
    }
} else {
    Write-Warning "[RESTORE] pg_restore command not found in PATH."
}
