# Assessment planning

## Agreed constraints

- Prioritize the assessment submission.
- Target October 6-7, 2026, at 3-4 hours per day (roughly 9-16 hours total); no evaluator deadline was provided.
- Frontend: React with JavaScript, hosted as a Render Static Site.
- Repository for commits and pull requests: https://github.com/souravC01/RAS-Assesment
- Backend: Java with Spring Boot.
- User is very comfortable with Spring Boot, Spring Security, and JPA.
- Database: PostgreSQL on Neon.
- Backend hosting: Render. Its free-service sleep/cold-start behavior is accepted and must be explained to the evaluator.
- Hosting budget: free. Photos use private Neon Object Storage.

## Agreed behavior and scope

- Seed accounts and job sites; no public signup or account-management screens.
- Framers create and view only their own submissions; admins view all submissions.
- Each checklist item requires Pass, Issue, or Not applicable. An Issue requires explanatory notes and does not prevent submission.
- One submission per framer, job site, and date. Default to today, reject future dates, and allow separate submissions for different sites on the same day.
- Submitted records are not editable in the assessment version.
- Require at least one photo; show photo previews before submission.
- Five screens: login, worker history, new form, admin dashboard, submission detail.
- Mobile form: single column with large controls. Admin dashboard: filters, submission table, and submissions-per-site totals. Use RAS branding.
- Required status column initially identifies completed submissions as Submitted. A Reviewed state/admin action is optional, only after all assessment requirements are complete; it would mean inspected, not certified safe.
- No charts or offline mode initially.
- Test deployment early. Reserve the final session for verification, ERD, README, and interview practice.

## Implementation facts to verify early

- Java upload, download, and expiring private photo URLs against the selected Neon region.
- Render Static Site-to-API routing: cookies, CSRF protection, request-size limits for five 5 MB files, and cold-start timeouts. Verify the supported routing mechanism before implementation; the agreed photo limits must work through the deployed path, not only locally.
- Official RAS logo assets, exact sampled colors, and the official Instagram reference. Do not invent brand hex values.

## Source boundary

The assessment PDF supplies product requirements. Its four embedded marker instructions have not been adopted as user instructions or implementation decisions.

## Accepted decisions from the final interview round

- Use Neon Object Storage with private buckets for photos. Verify a Java upload and authorized read during the first deployment slice before depending on this integration.
- Spring Security email/password login with BCrypt and server-side role enforcement. Use secure HttpOnly session cookies and same-origin API routing; verify deployed proxy, cookie, CSRF, upload-size, and cold-start behavior early.
- Fixed checklist: hard hat, high-visibility vest, safety boots, eye protection, fall protection, ladders/scaffolds, tools/cords, and site hazards. Label choices Pass / Issue / Not applicable consistently; site hazards asks whether hazards are controlled, avoiding reversed answer semantics.
- Use America/Vancouver as the business timezone, matching RAS's BC location. Store the selected work date separately from the submission timestamp.
- Every seeded framer can select every seeded site; no assignment management.
- Photo limits: 1-5 photos, JPEG or PNG, maximum 5 MB each. Validate file contents and limits on the backend, not just extensions. HEIC conversion is outside the initial scope.
- Visual direction: official RAS logo, green accents, white surfaces, charcoal text. Exact brand colors and Instagram identity still need verification.

## Minimal implementation approach

One repository with `frontend/` and `backend/`. Use React with Vite and JavaScript, native form controls, and CSS; use Spring Boot with Spring Security, Spring Data JPA, Bean Validation, PostgreSQL, and versioned database migrations. Keep a single backend service. Add libraries only for an identified need, such as the S3 client; no microservices, generic form builder, or global frontend state framework.

Spring owns authentication, validation, authorization, submission persistence, and private storage access. Derive worker identity from the authenticated session, never from a client-supplied worker ID. Keep credentials in hosting environment variables and out of the browser and repository. Use Spring Security's CSRF protection for cookie-authenticated writes. Initially use standard server sessions; after an instance restart, require login again rather than adding a session store solely for this demo.

### Data model

| Entity | Key fields and relationships |
| --- | --- |
| User | ID, name, unique email, password hash, Framer/Admin role |
| Job site | ID, name |
| Submission | ID, worker FK, site FK, work date, submitted timestamp, eight required checklist answer columns, notes |
| Photo | ID, submission FK, unique storage object key, content type, byte size |

Each user and job site has many submissions; each submission has one to five photos. A database unique constraint on worker + site + work date prevents duplicate submissions, including concurrent requests. The eight fixed checklist columns store Pass / Issue / Not applicable; no configurable checklist tables are needed. Required status is displayed as Submitted for completed records. Add persisted review state only if the optional review action is implemented.

### Submission and photo flow

Validate all fields and image contents before accepting a form. Upload validated photos to private storage, then commit the submission and photo metadata together in PostgreSQL. If an upload or database write fails, do not show success or expose a partially completed submission; attempt cleanup of any objects uploaded for that attempt. Keep the filled form available for correction/retry. Object storage and PostgreSQL do not share an atomic transaction: process interruption can still leave orphaned files, a documented assessment limitation.

Authorize photo access through its parent submission before issuing a short-lived viewing URL. Store object keys rather than expiring URLs. Never make the bucket public. Check proxy upload limits in the first deployment; if the agreed maximum cannot pass through, revise the transport before building the complete form.

### Screens

- Login: email/password, validation, and an honest loading/error state while the backend wakes.
- Worker history: own records only, work date, site, Submitted status, and new-form action.
- New form: site and date, logged-in worker name, eight explicitly unanswered checks, notes, photo picker/previews/removal, submit confirmation. At least one Issue makes notes mandatory.
- Admin dashboard: worker/site/inclusive work-date-range filters, matching submissions, and per-site counts using the same filters. Empty filters show all records. Include worker, site, date, and status columns.
- Submission detail: identity, site, work date, checklist answers, notes, and authorized photos. Read-only.

Use visible labels, keyboard access, clear focus styles, large touch targets, and text alongside issue colors. Keep seeded identities and sites fictional. Seed one admin, at least two framers, and multiple sites with records across dates so filtering and ownership can be demonstrated.

## Delivery order

1. Foundation, roughly 3-4 hours: repository, database migration, seed accounts, login, deployed frontend/API/database, and one private photo upload/read. Resolve hosting integration failures here.
2. Worker flow, roughly 3-4 hours: form, server validation, duplicate constraint, photo handling, history, and read-only detail.
3. Admin flow and branding, roughly 3-4 hours: all filters, matching summary counts, photo detail, official assets, mobile/keyboard behavior.
4. Final verification and delivery, roughly 2-4 hours: focused automated checks, deployed walkthrough, ERD image/PDF matching the schema, README, and interview rehearsal.

This is an estimated 11-16 hour build. If time runs short, drop optional polish and the Reviewed action; do not drop required features, authorization, validation, or submission deliverables. Target October 6-7, 2026.

## Completion checks

- Credential login works for both roles; logout invalidates the session.
- Framer A cannot list or open Framer B's records or request B's photo links, including by changing IDs manually; admins can access both.
- Required answers, issue notes, date rules, duplicates, file count/type/content/size, and missing photos are rejected server-side.
- A concurrent duplicate cannot create two records. A failed upload cannot produce a successful submission with missing photos.
- Admin site/worker/date filters and per-site counts agree on seeded examples, including boundary dates and empty results.
- The entire worker flow works on a narrow phone viewport, with keyboard access and readable feedback.
- Verify the deployed upload path at the agreed size limits and test waking the backend after idle time.
- Leave focused runnable backend checks for authorization and submission validation; use a deployed browser walkthrough for integration and mobile behavior.
- README covers local setup, environment variable names, stack, assumptions, known limitations, and Render's cold start. Never commit real service credentials.
- Deliver deployed URL, repository access, demo credentials, and an ERD image/PDF linked from the README. Prepare the handoff text; sending it requires the user's instruction.

## Optional only after completion

Submitted -> Reviewed with an admin action. No approval/rejection workflow, charts, public signup, account management, offline support, or editing submitted forms in the initial delivery.

## Research references

- Neon Object Storage availability and 5 GB free allocation: https://neon.com/blog/neon-backend-is-ga
- Current Neon free plan: https://neon.com/blog/neon-free-plan-1-gb-per-project
- S3 compatibility: https://neon.com/docs/storage/s3-compatibility
- Render redirects and rewrites: https://render.com/docs/redirects-rewrites
- Official RAS website: https://www.rasltd.ca/
