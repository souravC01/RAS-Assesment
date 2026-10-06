# RAS Site Safety Forms

A mobile React app for framers to submit daily safety checks and photos, with an admin view for worker/site/date filtering and per-site totals.

- **Live app:** https://ras-assessment.onrender.com
- **API health:** https://ras-assessment-api.onrender.com/api/health
- **Repository:** https://github.com/souravC01/RAS-Assesment

The repository is currently private. The candidate plans to make it public before submission; otherwise, grant the evaluator GitHub access. The app is a fictional assessment demo; Submitted means recorded for inspection, not certified safe.

## Try the hosted demo

Open [RAS Site Safety Forms](https://ras-assessment.onrender.com) and sign in with one of these accounts. No signup or local setup is required. Render's free backend sleeps when idle, so the first sign-in can take a few minutes while it wakes; wait for the request to finish, then retry if an error appears.

### Demo login credentials

| Role | Email | Password | Access |
| --- | --- | --- | --- |
| Admin | `admin@example.test` | `QXpZzoIrTXYZYT73xNK4hYTD` | All submissions, filters, site totals, details and photos |
| Framer A | `framer.a@example.test` | `gwHyu6IqQsfC8zYQPIdz35Hj` | Create and view Alex Morgan's submissions |
| Framer B | `framer.b@example.test` | `xMPk0ooor6AGRnmIt3xJ5M9j` | Create and view Taylor Reed's submissions; test separate ownership |

These are shared accounts for the fictional assessment demo. Use fictional notes and non-sensitive test photos. The existing records and image fixtures are synthetic. Sites are Cedar Grove, Harbour View, and Maple Court.

### Suggested testing walkthrough

1. **Worker history:** Sign in as **Framer A**. On **My submissions**, open an existing record and select **View photo** to load its private photo.
2. **Create a form:** Select **New submission**, choose a site and a date with no existing submission for that account, and answer all eight checks. Use **Pass**, **Issue**, or **Not applicable**; an **Issue** requires explanatory notes and can still be submitted. Add 1–5 JPEG/PNG photos (each at most 5,000,000 bytes) and submit. The saved detail is read-only. Future dates and duplicate account/site/date combinations are rejected.
3. **Separate worker access:** Sign out and sign in as **Framer B**. Its **My submissions** list contains only its own records. Opening Framer A's saved detail URL as Framer B returns an unavailable record; the worker cannot retrieve another worker's photo links either.
4. **Admin inspection:** Sign out and sign in as **Admin**. On **All submissions**, choose worker, job site, or date filters and select **Apply filters**. Check the matching **Submissions by site** totals, select **View** on a record, and inspect its answers and photo. **Reset filters** restores the full list.

For a local installation, passwords come from your `DEMO_*_PASSWORD` environment values rather than this hosted-demo table. Seeding creates missing accounts/sites and never resets existing passwords.

## Stack and routing

React 19 + JavaScript + Vite + React Router; Java 21 + Spring Boot 4.1.1 + Spring Security + JPA + Flyway; Neon PostgreSQL and private S3-compatible object storage.

Render hosts the frontend Static Site and the Docker API Web Service. The frontend rewrites `/api/*` to the API before its SPA fallback. Locally, Vite proxies `/api` to port 8080. This keeps browser requests and session cookies on one origin. The actual hosted rewrite was tested with secure sessions and five 5 MB photos. `render.yaml` reproduces the service settings; the deployed feature branch is `feat/site-safety`, with manual deployment enabled.

## Local setup

Requirements: Java 21, Node 24, Docker, and a private Neon `images` bucket. Maven is included through its wrapper. Use fictional data only.

1. Clone the repository and start PostgreSQL:

   ```powershell
   git clone https://github.com/souravC01/RAS-Assesment.git
   cd RAS-Assesment
   docker compose up -d
   ```

2. Set demo passwords and storage variables in the backend terminal. `.env.example` documents the names; Spring does **not** automatically load a `.env` file.

   ```powershell
   $env:JAVA_HOME='C:\Program Files\Java\jdk-21' # adjust for your installation
   $env:DEMO_SEED_ENABLED='true'
   $env:DEMO_ADMIN_PASSWORD='YOUR_DEMO_PASSWORD_AT_LEAST_12_CHARACTERS'
   $env:DEMO_FRAMER_A_PASSWORD='YOUR_DEMO_PASSWORD_AT_LEAST_12_CHARACTERS'
   $env:DEMO_FRAMER_B_PASSWORD='YOUR_DEMO_PASSWORD_AT_LEAST_12_CHARACTERS'
   $env:S3_ENDPOINT='YOUR_NEON_STORAGE_ENDPOINT'
   $env:S3_REGION='us-east-2'
   $env:S3_BUCKET='images'
   $env:S3_ACCESS_KEY_ID='YOUR_BRANCH_ACCESS_KEY'
   $env:S3_SECRET_ACCESS_KEY='YOUR_BRANCH_SECRET'
   cd backend
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
   ```

   The `local` profile uses PostgreSQL at `localhost:5432`, database `ras`, username `ras`, password `ras-local`. It disables Secure cookies only for local HTTP. Without that profile, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for a TLS database connection. Schema changes run through Flyway; JPA validates the schema.

3. In another terminal:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

   Open the displayed localhost URL. For macOS/Linux use `./mvnw`, `export` for environment variables, and your installed Java path.

The requested Neon setup is represented by `neon.ts`: its `images` bucket is private. `neon login`, `neon link`, `neon config init`, and `neon deploy` configure a developer's branch. Neon's `AWS_*` variables map to the app's `S3_*` variables above. Credentials belong in ignored local files or provider secrets, never frontend `VITE_*` variables.

## Rules and assumptions

- Eight required answers: Pass / Issue / Not applicable; no preselected answers. Issue requires explanatory notes and remains submittable. Notes have a 4,000-character limit.
- Each framer may submit once per job site and work date. A PostgreSQL unique constraint handles concurrent duplicates; another site on the same date is allowed. No edits, public signup, account management, offline mode, charts, or approval/rejection workflow.
- Work dates are `YYYY-MM-DD` calendar values in America/Vancouver, default to today, and cannot be future dates. Submission timestamps are UTC instants. All seeded framers can use all seeded sites.
- Require 1–5 JPEG/PNG photos, each at most **5,000,000 bytes**, with a **26,000,000-byte** multipart request limit. The backend validates actual image contents and caps decoded dimensions at 25 million pixels. Convert HEIC before upload.
- Framers cannot read another framer's details or photo links; these requests return 404. Admin routes require the admin role. Ownership comes from the authenticated session, never a worker ID supplied by the browser.
- BCrypt passwords; active CSRF; HttpOnly/Secure/SameSite=Lax cookies; API responses are no-store. Sessions expire after 30 minutes of inactivity and are lost on API restart. A same-account inline sign-in preserves an in-memory form and its files; writes require explicit retry. Tabs synchronize login/logout changes and refresh identity on focus. Logging out or changing accounts clears prior data/drafts. Submission requests include `X-Expected-Actor`; a session account mismatch returns 412 before uploading photos. Ownership still comes from the authenticated session, never this header.
- Photos persist in private Neon storage, not Render disk. Authorized signed links last five minutes and remain usable until expiry, including after logout. The app can refresh a link.
- Uploads precede a database transaction; failed uploads, confirmed rollbacks, and constraint-rejected commits trigger object cleanup. An unknown commit outcome retains photos because PostgreSQL may have committed their rows. Reconcile the database outcome before removing unreferenced objects. Unknown outcomes, process death, or failed cleanup can leave orphan objects; this version has no durable background reconciliation system.
- A lost response can make write success uncertain. Check history in the offered separate tab before retrying; the draft and photos stay in the original tab. The app never automatically retries a submission. Failed site loading can be retried, and an expired session can be restored inline.
- Render's free API sleeps when idle. The first request can take a few minutes; wait for it to wake, then sign in. The UI shows pending/error states. Restart also invalidates previous sessions.

## Data model

![Four-entity ERD](assets/erd.png)

[Diagram source](assets/erd.mmd). Users and job sites each have many submissions; each submission has 1–5 photos, enforced by the service. Database constraints enforce foreign keys, allowed answers, byte sizes, unique photo keys, and the unique worker/site/date combination. All eight answers use the same PASS / ISSUE / NA constraint.

## Verification

```powershell
cd backend
.\mvnw.cmd clean verify
cd ..\frontend
npm ci
npm test
npm run build
```

Docker must run for PostgreSQL Testcontainers. The real storage smoke is opt-in: set `STORAGE_SMOKE=true` and Neon's `AWS_ENDPOINT_URL_S3`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, then run `mvnw.cmd -Dtest=PhotoStorageTest test`. It creates a random `probe/` JPEG in `images`, checks unsigned denial and identical signed bytes, and deletes it in `finally`. A skipped smoke test is not evidence of working storage.

Recorded verification covered real PostgreSQL, actual commit rollback, ownership/filters, hosted secure sessions/private photos, maximum-size transport, and real mobile Chrome checks.

The final controlled idle/wake check took about 2 minutes 47 seconds and required a fresh sign-in after the API restarted. The confidential assessment PDF is kept locally and excluded from the published branch history.

## Branding sources

The logo and visual direction come from the official [Ron Anderson & Sons Ltd. website](https://www.rasltd.ca/). The bundled [logo](https://images.squarespace-cdn.com/content/v1/603d792c9b2ff375c0f46cb9/8db992b5-db8b-413d-9da1-005016d0b9e0/RAS+Logo_Updated+May+2023_RGB_green.png) and [Gainsborough Sans font](https://file.squarespace-cdn.com/content/v2/namespaces/fonts/libraries/603d792c9b2ff375c0f46cb9/assets/e3ab944a-c588-48c1-a494-7d9bd7d4b7fd/font.otf) were sourced from the supplied design reference for this fictional assessment demo.
