# Backup and Restore Procedure

## 1. Automated Backups
Run `scripts/backup.ps1` (Windows) or `scripts/backup.sh` (Linux).
- Produces a compressed PostgreSQL custom dump (`.dump` or `.sql.gz`).
- Computes SHA-256 checksum written alongside the backup.
- Records backup metadata into the `backup_records` database table.

## 2. Restore Testing
Never trust an untested backup. To test restoration:
```bash
powershell -File scripts/restore.ps1 -BackupFile backups/backup_20260910_120000.dump -TargetDb test_restore_db
```
The script verifies the SHA-256 checksum first before piping to `pg_restore`.
