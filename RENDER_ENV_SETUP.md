# Pick My Teacher backend environment setup

Keep the existing database and Brevo variables unchanged.

Add these two variables to the existing Render backend service to bootstrap the first admin account:
- `ADMIN_EMAIL`: the email address you will use for the admin login
- `ADMIN_PASSWORD`: a new password with at least 12 characters

The admin account is created/updated on backend startup only when both values are configured. Admin signup is not public. Do not commit the password to GitHub.

New API capability:
- Student and Teacher role selection at signup (`role: STUDENT` or `role: TEACHER`)
- Login returns a 14-day opaque bearer session token
- `POST /api/users/logout` revokes the session
- Teacher profile create/update, max two demo video links, and teacher consultation list
- Admin pending-teacher review/approval and consultation status updates
- Student consultation request status list

The API continues to accept existing V1 public teacher listing, login, signup, and password-reset calls. Existing seeded catalogue rows are retained; new teacher-submitted profiles stay hidden until admin approval.

## Important limitations before public launch
- The existing seeded sample teachers/reviews/videos are legacy demo content, not verified real teachers. Replace them with consented real teacher data before public launch.
- Video links are URL references, not uploaded video files.
- The existing consultation creation form still uses email as its lookup key; account-bound consultation creation should be added before relying on it for privacy-sensitive use.
- Run the frontend build and backend Maven tests in CI/Render before treating this as production-ready.
