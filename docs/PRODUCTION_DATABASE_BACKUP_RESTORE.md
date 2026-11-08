# Production database backup and restore runbook

## Current production database facts

- Render resource: `pickmyteacher-db-v2`
- Database: PostgreSQL 18, Singapore
- Current compute plan: Free
- Created: 2026-11-04
- Render expiry shown by the API: 2026-12-04

**Critical:** Render Free Postgres does not include managed backups or point-in-time recovery and expires after 30 days. Upgrade the database to a paid plan before the expiry date if this database must remain available. Keep an independent logical export regardless of plan.

## Create an independent logical backup (Windows PowerShell)

Install PostgreSQL 18 client tools. In Render Dashboard, open the database's Info page and copy its *External Database URL*. Do not paste the URL into chat, GitHub, source files, or logs.

```powershell
$env:PGSSLMODE = "require"
$env:PGDATABASE_URL = Read-Host "Paste the External Database URL (input is hidden only if you use a secure prompt)"
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
pg_dump --dbname="$env:PGDATABASE_URL" --format=custom --no-owner --no-privileges --file="seonbaeon-$stamp.dump"
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed" }
pg_restore --list "seonbaeon-$stamp.dump" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Backup archive validation failed" }
Get-FileHash "seonbaeon-$stamp.dump" -Algorithm SHA256
```

Store the dump and checksum in encrypted, access-controlled storage outside Render. Never commit a dump to Git.

## Restore verification (isolated database only)

Create a separate empty PostgreSQL database (preferably a paid temporary Render database in Singapore or a local PostgreSQL 18 instance). Obtain its External Database URL separately.

```powershell
$env:RESTORE_DATABASE_URL = Read-Host "External URL for the EMPTY test database"
pg_restore --dbname="$env:RESTORE_DATABASE_URL" --no-owner --no-privileges --exit-on-error "PATH_TO_BACKUP.dump"
if ($LASTEXITCODE -ne 0) { throw "Restore failed" }
```

Compare table counts and representative non-sensitive records between source and restored databases. Do not run `--clean`, `DROP`, or restore into production. Keep the restored test instance isolated and delete it only after verification and explicit owner approval.

## Current schema inventory

The production database currently exposes these public tables:
`app_data_migrations`, `auth_sessions`, `consultation_requests`, `dashboard_hidden_items`, `lessons`, `password_reset_tokens`, `reviews`, `student_progress_notes`, `teacher_availability_slots`, `teacher_videos`, `teachers`, `user_notifications`, `users`.

A read-only inspection on 2026-11-09 confirmed the tables exist. Estimated live-row counts are not a backup and are not restore verification.

## Acceptance record

- [ ] External logical backup created by an authorized operator
- [ ] Archive list/checksum verified
- [ ] Restored into a separate empty database
- [ ] Table counts and representative records compared
- [ ] Backup stored outside Render with restricted access

This runbook intentionally does not claim a backup or restore has been executed. The Render connection secret is not available to the test runner, and the current Free database has no managed backup/PITR feature.
