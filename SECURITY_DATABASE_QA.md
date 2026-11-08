# SeonbaeON Security & Database QA

## Automated regression coverage
The backend test suite includes authentication/session and account signup/login regression tests:
- Secure random 48-byte session tokens, uniqueness and 14-day expiry
- Missing/malformed/expired bearer sessions and suspended users
- Logout token invalidation
- Signup required fields, password minimum, duplicate email and privilege escalation
- Login wrong password, inactive account and role mismatch
- Password-reset response does not disclose whether an email exists

Run locally from the backend repository:
```bash
mvn test
```

## Manual API security verification (use test accounts only)
Set `API` to the backend base URL and `STUDENT_TOKEN`, `TEACHER_TOKEN`, and `ADMIN_TOKEN` to tokens from dedicated test accounts. Never commit tokens.

| Check | Expected |
|---|---|
| GET /api/users/me without Authorization | 401 |
| Student calls /api/workspace/admin/users | 403 |
| Teacher calls /api/workspace/admin/pending-teachers | 403 |
| Student calls /api/workspace/teacher/profile | 403 |
| Signup with role ADMIN | 400 |
| Signup with duplicate email in different case | 409 |
| Login with wrong password | 401 |
| Login for inactive account | 403 |
| Teacher edits another teacher's video/availability/progress item | 404 or 403 |
| Student reads another student's private dashboard | no access |
| Unapproved/unavailable teacher receives new consultation | 409 |

CORS is a browser-enforced policy, not API authentication. Repeat authorization tests with direct HTTP clients; do not infer access control from CORS.

## Database integrity checks
Use a staging database or a dedicated test dataset. Do not run destructive checks against production users.
- Create a student and teacher test pair; submit, approve and complete a consultation; verify linked student and review eligibility.
- Delete the test student and confirm sessions, password-reset tokens, consultations, reviews, lessons, progress notes, hidden dashboard items and notifications are removed.
- Delete a test teacher account and confirm teacher profile, videos, availability, owned consultations and dependent reviews are removed.
- Verify deletion is transactional and retrying a deleted ID is safe.
- Verify no orphaned foreign-key references remain.
- Verify a backup can be restored into a separate database and compare table counts and representative records.

## Security review notes
- Never expose secrets in API responses or logs. Keep Cloudinary/API credentials in Render environment variables.
- Validate and length-limit every user-controlled field; encode output in the frontend and avoid rendering user text as HTML.
- CORS does not prevent curl/Postman/server-side requests. Every private endpoint must authenticate and authorize by role and ownership.
- Do not claim SQL injection/XSS/penetration testing passed without running dedicated tests.
- Do not perform production account deletion or database restore tests without explicit approval and a verified backup.

## Added automated regression tests (QA branch)
- Account deletion: authenticated student cleanup, dependent-record/session cleanup, unauthenticated rejection, and admin self-delete restriction.
- Upload/profile validation: teacher-only signing, profile-required and two-video limit, javascript:/data: URL rejection, and English-only teacher categories.
- SQL injection/XSS input regressions: SQL-like login email is treated as a literal repository lookup; malformed signup emails and oversized consultation messages are rejected; markup is retained as JSON text, never treated as executable HTML by the API.
- Student-to-teacher workflow: student consultation request, teacher approval, and connected-student roster visibility; rejects unapproved teachers and email spoofing.
- CI workflow: `.github/workflows/security-regression.yml` runs the Maven test suite on QA branch pushes and pull requests.

These are controller/unit regression tests, not a substitute for an external penetration test or a browser-based XSS assessment. Confirm frontend user-generated fields are rendered as React text, not `dangerouslySetInnerHTML`.

## Production backup status (2026-11-09)
The connected Render production database `pickmyteacher-db-v2` is on the Free plan and is due to expire 2026-12-04. Render Free Postgres does not provide managed backups or PITR. A documented `pg_dump` and isolated `pg_restore` procedure is in `docs/PRODUCTION_DATABASE_BACKUP_RESTORE.md`. The actual dump/restore acceptance checks remain pending because an external database connection secret and an isolated restore target are not exposed to this environment. Do not mark backup/restore as passed until the archive has been created and restored successfully.
