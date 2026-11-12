# Preflight Script for Task 20
$ErrorActionPreference = 'Stop'

Write-Host "=== PREFLIGHT: 1. Repository & Target Database Verification ==="
$currentDir = Get-Location
Write-Host "Current Directory: $currentDir"
$dbTarget = "business_billing_db"
$pgHost = "localhost"
$pgPort = "5432"
$pgUser = "postgres"
Write-Host "Target Database: $dbTarget on $pgHost`:$pgPort as $pgUser"

Write-Host "`n=== PREFLIGHT: 2. Check Existing Backup Checksum ==="
$oldDump = ".\backups\backup_business_billing_db_test_phase3.dump"
$oldShaFile = ".\backups\backup_business_billing_db_test_phase3.dump.sha256"
$computedHash = (Get-FileHash -Path $oldDump -Algorithm SHA256).Hash.ToLower()
$expectedHash = (Get-Content $oldShaFile).Trim().ToLower()
if ($computedHash -eq $expectedHash) {
    Write-Host "Existing backup SHA256 MATCH: $computedHash"
} else {
    Write-Error "Hash mismatch! Expected $expectedHash but got $computedHash"
}

Write-Host "`n=== PREFLIGHT: 3. Disk Space Check ==="
Get-PSDrive -PSProvider FileSystem | Select-Object Name, @{Name="FreeGB";Expression={[math]::Round($_.Free/1GB,2)}}, @{Name="UsedGB";Expression={[math]::Round($_.Used/1GB,2)}} | Format-Table -AutoSize

Write-Host "`n=== PREFLIGHT: 4. Active Connections to $dbTarget ==="
$env:PGPASSWORD = 'postgres'
$psqlPath = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
& $psqlPath -h $pgHost -p $pgPort -U $pgUser -d $dbTarget -c "SELECT pid, usename, client_addr, state, query FROM pg_stat_activity WHERE datname='business_billing_db';"

Write-Host "`n=== PREFLIGHT: 5. Create Fresh Pre-Migration Backup ==="
$newDump = ".\backups\backup_business_billing_db_pre_v7_v10.dump"
$newShaFile = ".\backups\backup_business_billing_db_pre_v7_v10.dump.sha256"
$pgDumpPath = 'C:\Program Files\PostgreSQL\18\bin\pg_dump.exe'
& $pgDumpPath -h $pgHost -p $pgPort -U $pgUser -F c -b -f $newDump $dbTarget
if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump failed with exit code $LASTEXITCODE"
}
$newHash = (Get-FileHash -Path $newDump -Algorithm SHA256).Hash.ToLower()
Set-Content -Path $newShaFile -Value $newHash
$dumpItem = Get-Item $newDump
Write-Host "New Pre-Migration Backup Created: $newDump"
Write-Host "Size: $($dumpItem.Length) bytes"
Write-Host "SHA256: $newHash"

Write-Host "`n=== PREFLIGHT: 6. Verify New Backup Readability ==="
$pgRestorePath = 'C:\Program Files\PostgreSQL\18\bin\pg_restore.exe'
& $pgRestorePath --list $newDump | Select-Object -First 20
if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_restore --list failed on new dump!"
}

Write-Host "`n>>> PREFLIGHT CHECKS PASSED 100% <<<"
