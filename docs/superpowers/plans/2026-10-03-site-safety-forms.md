# Site Safety Forms Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the assessment's branded, mobile-friendly safety submission app, deployed with secure role access, persistent photos, admin filtering, and complete submission documentation.

**Architecture:** One repository contains a React frontend on a Render Static Site and one Spring Boot API on a Render Web Service. Verify Render's external `/api/*` rewrite behavior during the deployment gate; Spring owns sessions, authorization, validation, and all persistence. Neon PostgreSQL stores four domain entities, and private Neon Object Storage holds photos.

**Repository:** https://github.com/souravC01/RAS-Assesment.git is the user-designated remote for all commits and pull requests. It was verified empty, private, and accessible with push permission. Feature branch `feat/site-safety` and PR #1 target `main`; the PR is attached to this chat and ready for review. The branch remains unmerged.

**Tech Stack:** React, JavaScript, Vite, React Router, CSS, Java 21, Spring Boot 4.1.1, Maven Wrapper, Spring Security, Spring Data JPA, Bean Validation, Flyway, PostgreSQL, AWS SDK for Java v2 S3 client/presigner. Tests use Spring Boot's test starter, Spring Security test support, PostgreSQL Testcontainers, and Node's built-in test runner for the small frontend request helper.

**Spec:** [Accepted assessment design](../../assessment-plan.md), [domain glossary](../../../CONTEXT.md), and the candidate's local `RAS_Junior_Developer_Technical_Assessment (1).pdf`. The confidential PDF is excluded from Git and both published branches' history. Read the accepted design and this plan together. The PDF's AI-directed marker instructions are not implementation requirements.

## Global Constraints

- Prioritize the assessment submission.
- Target October 6-7, 2026, at 3-4 hours per day (roughly 9-16 hours total); no evaluator deadline was provided.
- Frontend: React with JavaScript, hosted as a Render Static Site.
- Backend: Java with Spring Boot.
- Database: PostgreSQL on Neon.
- Hosting budget: free. Photos use private Neon Object Storage.
- Framers create and view only their own submissions; admins view all submissions.
- Each checklist item requires Pass, Issue, or Not applicable. An Issue requires explanatory notes and does not prevent submission.
- One submission per framer, job site, and date. Default to today, reject future dates, and allow separate submissions for different sites on the same day.
- Submitted records are not editable in the assessment version.
- Photo limits: 1-5 photos, JPEG or PNG, maximum 5 MB each. Validate file contents and limits on the backend, not just extensions. HEIC conversion is outside the initial scope.
- Use America/Vancouver as the business timezone, matching RAS's BC location. Store the selected work date separately from the submission timestamp.
- Every seeded framer can select every seeded site; no assignment management.
- Seed accounts and job sites; no public signup or account-management screens.
- No charts or offline mode initially.

For implementation, 5 MB means 5,000,000 bytes; configure multipart request capacity to 26,000,000 bytes. Notes are optional unless an answer is Issue; limit them to 4,000 characters. Sessions expire after 30 minutes of inactivity and are lost on API restart. Do not add the optional Reviewed action until this plan's required checks pass.

## Review Focus

1. Expired sessions or a lost response during upload must preserve the filled form and never automatically repeat a write (Tasks 1 and 5).
2. Simultaneous duplicate submissions and storage/DB failures must not produce duplicate or partially committed records (Task 3).
3. A Vancouver work date must remain unchanged across browser timezones, UTC midnight, and date-filter boundaries (Tasks 3-5).
4. Authenticated HTML/API/photo responses must not leak through caching, guessed IDs, or logout/account switching (Tasks 1, 4, and 7).
5. Five valid maximum-size images and deceptive image contents must behave correctly through the deployed proxy, not only in local tests (Tasks 2, 3, and 7).

---

## Repository and file boundaries

The initial workspace contained the PDF and planning documents. No code conventions or `AGENTS.md` were found. Git and the foundation have now been initialized during execution; preserve existing documents and never commit secrets. One plan is appropriate because authentication, forms, photos, and admin views share one small domain and must ship together.

Paths below are repository-relative. `B` expands to `backend/src/main/java/ca/ras/safety`, `T` to `backend/src/test/java/ca/ras/safety`, and `R` to `backend/src/main/resources`. Each file listed under a task is a concrete path after this expansion.

| Area | Files and responsibility |
| --- | --- |
| Build/deployment | `backend/pom.xml`, Maven wrapper files, `backend/Dockerfile`, `backend/.dockerignore`, `frontend/package.json`, lockfile, `frontend/index.html`, `frontend/vite.config.js`, `render.yaml`, `.gitignore`, `compose.yaml`, `.env.example` |
| Runtime | `B/SafetyApplication.java`, `R/application.yml`, `R/application-local.yml`; configuration without committed credentials |
| Identity | `B/auth/User.java`, `UserRepository.java`, `SecurityConfig.java`, `AuthController.java`, `DemoSeed.java`; seeded identity and standard Spring session login |
| Sites | `B/site/JobSite.java`, `JobSiteRepository.java`, `SiteController.java`; read-only site lookup |
| Submissions | `B/submission/Submission.java`, `SubmissionRepository.java`, `SubmissionDtos.java`, `SubmissionService.java`, `SubmissionController.java`; create, authorized reads, admin filters/counts |
| Photos | `B/photo/Photo.java`, `PhotoRepository.java`, `PhotoStorage.java`, `PhotoController.java`; private object operations and permission-checked links |
| Shared HTTP | `B/ApiErrors.java`; consistent JSON error responses, no generic application framework |
| React | `frontend/src/main.jsx`, `App.jsx`, `api.js`, `styles.css`; app routing/session state, requests, styling |
| Screens | `frontend/src/pages/Login.jsx`, `WorkerHistory.jsx`, `SubmissionForm.jsx`, `SubmissionDetail.jsx`, `AdminDashboard.jsx` |
| Form vocabulary | `frontend/src/checklist.js`; eight field keys and labels, no dynamic form-builder schema |
| Delivery | `README.md`, `docs/verification.md`, `docs/erd.mmd`, `docs/erd.png`, `docs/handoff.md`, `frontend/public/ras-logo.png` |

Use ordinary concrete classes. DTO records may be nested in `SubmissionDtos`; role and answer enums may live with their owning entity. Do not return JPA entities directly. Preserve normal static module structure; no repository/service interfaces beyond Spring Data's required repository interfaces.

## Shared contracts

Use numeric `Long` IDs, `LocalDate` work dates, and UTC `Instant` timestamps. The role values are `FRAMER`, `ADMIN`; answer values are `PASS`, `ISSUE`, `NA`.

`SubmissionDtos.Checklist` has eight required answer fields: `hardHat`, `highVisibilityVest`, `safetyBoots`, `eyeProtection`, `fallProtection`, `laddersScaffolds`, `toolsCords`, `hazardsControlled`.

`CreateSubmission(Long siteId, LocalDate workDate, Checklist checklist, String notes)` deliberately has no worker ID. `Actor(Long id, String name, String email, String role)` is the JSON shape returned by `/auth/me`; use the authenticated email to load the actual `User` in the backend.

| HTTP contract (all prefixed `/api`) | Result |
| --- | --- |
| `GET /health` | Public `200 {status:"UP"}` without configuration details |
| `GET /auth/csrf` | Public `{headerName,token}` from Spring's session CSRF token |
| `POST /auth/login` | Form-encoded `email,password`, CSRF header; `204`, session cookie; `401` on wrong credentials |
| `GET /auth/me` | `Actor`; `401` without a valid login |
| `POST /auth/logout` | CSRF header; `204`, invalidated session |
| `GET /sites` | Authenticated `[{id,name}]`, ordered by name |
| `POST /submissions` | Framer-only multipart `form` JSON and repeated `photos` parts; `201 {id}` |
| `GET /submissions` | Framer's own `SubmissionRow[]`, newest work date then ID first |
| `GET /submissions/{id}` | Owner/admin `SubmissionDetail`, unauthorized ownership concealed as `404` |
| `GET /photos/{id}/url` | Owner/admin `{url,expiresAt}`; private URL valid for 5 minutes |
| `GET /admin/workers` | Admin-only `[{id,name}]` for all framers |
| `GET /admin/submissions?siteId=&workerId=&from=&to=` | Admin-only `{items:SubmissionRow[],countsBySite:SiteCount[]}`; omitted filters mean all |

`SubmissionRow`: `{id,worker:{id,name},site:{id,name},workDate,submittedAt,status:"Submitted"}`. `SubmissionDetail` adds `checklist`, `notes`, and `photos:[{id,contentType,byteSize}]`. `SiteCount`: `{siteId,siteName,count}`. `SubmissionFilter(Long siteId, Long workerId, LocalDate from, LocalDate to)` applies inclusive work-date bounds. Unknown site/worker filters return empty results, reversed dates return `400`.

Errors are `{message,fieldErrors}` (`fieldErrors` is a string map, empty if not applicable). Use `400` for validation, `401` for login expiry, `403` for role/CSRF rejection, `404` for missing/inaccessible records, `409` for duplicates, `413` for size limits, and `503` for temporary storage unavailability. Do not expose exception text, credentials, or SQL. All `/api` responses carry `Cache-Control: no-store`.

A write with an expired session may encounter CSRF rejection (403) before authentication rejection. On a write's 403, the frontend may check `/auth/me` once: if that returns 401, classify the failure as session expiry; otherwise preserve the 403. Never resend the write automatically. Set private image object cache metadata to `private, no-store`; issued signed links still grant access until their five-minute expiry, including after logout. Document that bounded link lifetime rather than claiming immediate revocation.

## Execution conventions

Run backend commands from `backend/`: `.\mvnw.cmd ...` on Windows, `./mvnw ...` elsewhere. Frontend commands run from `frontend/`. Java 21, Node compatible with the generated Vite project, and Docker must be available. Generate and commit dependency lockfiles; use the pinned versions thereafter. Spring Boot 4.1.1 with Java 21 is supported by the [current system requirements](https://docs.spring.io/spring-boot/system-requirements.html).

Use PostgreSQL Testcontainers for database integration tests, with container setup in each owning test class or Boot's test configuration; no H2 substitution for constraint/date behavior. Use the test starter's mocking support only for external storage failures. Each task's test snippets are assertion contracts inside the named test, not complete test-class bodies. Do not mark checkboxes complete until executing them. Commit only the paths owned by the task after its checks pass.

Environment contract: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`, `DEMO_SEED_ENABLED`, `DEMO_ADMIN_PASSWORD`, `DEMO_FRAMER_A_PASSWORD`, and `DEMO_FRAMER_B_PASSWORD`. Use fictional emails `admin@example.test`, `framer.a@example.test`, and `framer.b@example.test`. Bind Render's `PORT` with local default 8080. Keep the Neon pool small (maximum 5 connections). No secrets need a `VITE_` prefix or inclusion in the frontend bundle.

### Task 1: Deploy database-backed login and the React shell

**Files:** Create build/deployment and runtime files from the map; create `B/auth/{User,UserRepository,SecurityConfig,AuthController,DemoSeed}.java`, `R/db/migration/V1__users.sql`, `T/auth/AuthTest.java`, and `frontend/src/{main.jsx,App.jsx,api.js,styles.css}`, `frontend/src/pages/Login.jsx`. Create `docs/verification.md` for recorded deployment results.

**Interfaces:** Produces the health and auth HTTP contracts, `UserRepository.findByEmail(String): Optional<User>`, and `AuthController.me(Authentication): Actor` (`Actor` is a nested record in `AuthController`). Put the small public health handler in `AuthController` as well. Produces `api(path, options = {}): Promise<object|null>`, `login(email,password): Promise<Actor>`, `logout(): Promise<void>` in `api.js`. `api` prefixes `/api`, includes same-origin credentials, throws an error with numeric `status`, and never retries writes.

- [x] Generate the minimal Boot/Maven and Vite/React projects; initialize Git. Add only the dependencies named above and those needed by the task. Add local PostgreSQL to `compose.yaml`, a Maven-wrapper multi-stage Java 21 Dockerfile, and environment examples with placeholders. Test-only storage dependencies may wait for Task 2.
- [x] Write `AuthTest.loginRequiresCsrfAndPersistsSession`: seed a test user with BCrypt, obtain a real token from `/api/auth/csrf`, then test actual form login and `/me`, rather than only `@WithMockUser`.

```java
mockMvc.perform(post("/api/auth/login").param("email", email).param("password", password))
    .andExpect(status().isForbidden());
// After login with the fetched CSRF token and returned session:
mockMvc.perform(get("/api/auth/me").session(loggedInSession))
    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("FRAMER"))
    .andExpect(header().string("Cache-Control", "no-store"));
```

Also assert wrong credentials -> 401; logout -> session invalidated; a fresh anonymous `/me` -> 401; a restarted session requires login again. Setup variables above come from the test's seeded user and actual MockMvc login response.
- [x] Run `.\mvnw.cmd -Dtest=AuthTest test`; expect failures for missing auth behavior before implementing it.
- [x] Implement the users migration and login with Spring's form-login/session support, BCrypt, JSON success/failure handlers, and active CSRF protection. Session cookie: host-only, HttpOnly, Path `/`, SameSite=Lax, Secure in deployment (local HTTP profile only disables Secure). Use `/auth/csrf` to obtain the token before login and refresh it after login/logout. Do not disable CSRF to make the SPA work. See [Spring's token endpoint guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- [x] Implement the React login shell and logout. Use ordinary component state, no global state library. Show pending/error states; interpret `/me` 401 as signed out. Add explicit no-store headers across security and controller responses.
- [x] Deploy the API as a Render Web Service, connect Neon through a TLS JDBC URL, and deploy the frontend as a Render Static Site. Keep database/S3/demo passwords only in provider secrets. Seed one admin and two framers only when `DEMO_SEED_ENABLED=true`; take passwords from required environment variables and never reset existing accounts on restart.
- [x] Configure Vite's local `/api` proxy and test a Render `/api/*` external rewrite before the SPA fallback. Preserve `/api` on the API destination. Verify POST/multipart forwarding and session cookies rather than assuming static rewrites behave as a full API proxy. Never enable CDN caching for API paths. [Render redirect/rewrite documentation](https://render.com/docs/redirects-rewrites) allows full destination URLs; it is not proof of our authentication or upload behavior. If that route cannot satisfy the accepted requirements, record the failure and prefer serving the built React assets from Spring Boot on the same Render Web Service, subject to updating this topology decision.
- [x] Run `.\mvnw.cmd -Dtest=AuthTest test` and `npm run build`; expect PASS/build success. In the deployed browser, login, reload, logout, and try a different account; verify no cached user data, correct secure cookie flags, and actual CSRF rejection. Record URLs and results without secrets.
- [x] Commit owned files: `git commit -m "feat: deploy secure session login"`.

### Task 2: Prove private photo storage and maximum-size transport

**Files:** Create `B/photo/PhotoStorage.java`, `T/photo/PhotoStorageTest.java`; modify `backend/pom.xml`, `R/application.yml`, `.env.example`, `docs/verification.md`. Temporarily create `B/photo/StorageProbeController.java` under the `storage-probe` profile; delete it in Task 3.

**Interfaces:** `PhotoStorage.put(String key, byte[] bytes, String contentType): void`; `delete(String key): void`; `presign(String key, Duration ttl): URI`. Uses server-only Neon S3 endpoint, region, access key, secret, and bucket settings. Concrete class, no custom storage abstraction. The temporary admin-only `POST /api/admin/storage-probe` accepts repeated `photos`, returns a private 5-minute URL, and removes probe objects after the manual check.

- [x] Write opt-in `PhotoStorageTest.privateObjectRoundTrip` (enabled only by `STORAGE_SMOKE=true`), uploading a generated valid JPEG under a random `probe/` key. Assert unsigned access denied, signed GET 200 with identical bytes, and delete in `finally`. Do not log the signed URL or credentials.

```java
assertThat(unsignedResponse.statusCode()).isIn(401, 403, 404);
assertThat(signedResponse.statusCode()).isEqualTo(200);
assertThat(signedResponse.body()).isEqualTo(uploadedBytes);
```

- [x] With a dedicated test bucket and environment configured, run `.\mvnw.cmd -Dtest=PhotoStorageTest test`; confirm failure without implemented storage operations. A skipped smoke test is not passing evidence.
- [x] Implement the methods with AWS SDK v2, path-style addressing, bounded timeouts, and private bucket access. Never store photos on Render disk. Match the current Neon endpoint/credential settings from its console; [S3 compatibility](https://neon.com/docs/storage/s3-compatibility) is the provider contract.
- [x] Deploy the protected probe on the same Render frontend/API route. Send five valid 5,000,000-byte images with a real CSRF token and session, plus small malformed/oversized requests. Confirm full-size delivery, private reads, and cleanup. Limit the probe to the named profile and never expose anonymous uploads.
- [x] Record the command, timestamp, bytes transferred, and outcome in `docs/verification.md`; rerun the storage smoke and expect PASS. If the route rejects the agreed payload or loses cookies, stop dependent UI work and amend this plan's transport design with evidence. Do not silently reduce the accepted limits or switch providers.
- [x] Commit: `git commit -m "feat: verify private photo storage transport"`.

### Task 3: Create validated, atomic database submissions

**Files:** Create `R/db/migration/V2__sites_submissions_photos.sql`; create mapped site, submission, and photo entity/repository files; create `B/site/SiteController.java`, `B/submission/{SubmissionDtos,SubmissionService,SubmissionController}.java`, `B/ApiErrors.java`, `T/submission/SubmissionCreateTest.java`. Modify `DemoSeed.java`, `SecurityConfig.java`, and `SafetyApplication.java` (Clock bean); delete `StorageProbeController.java`.

**Interfaces:** Consumes Task 2 storage methods. Produces `SubmissionService.create(User actor, CreateSubmission form, List<MultipartFile> photos): Long`, `POST /submissions`, and `GET /sites`. `SubmissionService` receives a `Clock` bean using `America/Vancouver` for deterministic work-date validation. `Photo` has a many-to-one submission relationship and a unique storage key.

- [x] Write `SubmissionCreateTest.rejectsInvalidFormsAndAcceptsReportedIssues` against real PostgreSQL. Define a valid multipart request inline; use a generated decodable JPEG and authenticated framer. Assert the following results with MockMvc status assertions and repository counts:

```java
assertThat(submissionRepository.count()).isEqualTo(1);
assertThat(saved.getWorker().getId()).isEqualTo(framer.getId());
assertThat(saved.getWorkDate()).isEqualTo(LocalDate.of(2026, 10, 3));
assertThat(saved.getPhotos()).hasSize(1);
```

Assert 201 for an ISSUE plus explanatory notes; 400 for missing answers, unknown answer/site, blank issue notes, future date, zero/six photos, malformed/fake JPEG, and overlong notes; 413 for a file above 5,000,000 bytes. Freeze clock at `2026-10-04T06:30:00Z`: Vancouver October 3 accepted, October 4 rejected. Add a worker ID to JSON and prove it cannot change ownership. Admin creation -> 403.
- [x] Add `duplicateAndFailureLeaveNoPartialSubmission`: two concurrent creates for the same worker/site/date yield one success and one 409, while a second site succeeds. Mock the concrete storage class to fail on the second upload; assert no new submission/photos and deletion attempted for the first key. Force a DB commit failure after successful uploads and assert rollback plus cleanup. Run `.\mvnw.cmd -Dtest=SubmissionCreateTest test`; expect the new cases to fail.
- [x] Implement Flyway constraints: unique `(worker_id,site_id,work_date)`, non-null FKs/date/answers, checks allowing only PASS/ISSUE/NA, and unique photo object key. Use eight explicit answer columns. Set JPA schema handling to validate, not auto-update.
- [x] Implement `create` by validating the entire request and bounded image decoding before storage, generating random keys, uploading, and executing the database insert through `TransactionTemplate`. Catch failures outside its commit boundary and attempt deletion of all uploaded keys without masking the original error. Translate the specific duplicate constraint to 409. No durable record exists until all photos succeed. Add a `ponytail:` comment documenting possible orphan objects after process death and lifecycle cleanup as the future remedy.
- [x] Implement error responses from the shared contract, including exceptions from multipart parsing and malformed JSON; enforce identity and FRAMER authority server-side. Remove the storage probe and its enabled profile. Seed fictional sites idempotently.
- [x] Run `.\mvnw.cmd -Dtest=SubmissionCreateTest,AuthTest test`; expect PASS. Submit once through the deployed API, verify database metadata/private photos, and record proof that the probe route no longer exists.
- [x] Commit: `git commit -m "feat: create validated safety submissions"`.

### Task 4: Authorized history, details, photos, and admin queries

**Files:** Modify submission repository/service/controller/DTOs and `SecurityConfig.java`; create `B/photo/PhotoController.java`, `T/submission/SubmissionReadTest.java`.

**Interfaces:** `SubmissionService.history(User): List<SubmissionRow>`; `detail(User, Long id): SubmissionDetail`; `search(SubmissionFilter): AdminResult`. `PhotoController.url(Long id, Authentication): PhotoUrl` authorizes using the parent submission. `AdminResult(List<SubmissionRow> items,List<SiteCount> countsBySite)` and the remaining HTTP contracts use the exact property names above.

- [x] Write `SubmissionReadTest.enforcesOwnershipAndInclusiveFilters` with two framers, two sites, and three work dates. Assert A's history excludes B, A's request for B's detail/photo returns 404, A's admin route returns 403, and admin can read both. Ensure storage presigning is never invoked for denied access.

```java
mockMvc.perform(get("/api/submissions/" + bSubmissionId).session(aSession))
    .andExpect(status().isNotFound());
mockMvc.perform(get("/api/admin/submissions").session(adminSession)
    .param("from", "2026-10-03").param("to", "2026-10-03"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
    .andExpect(jsonPath("$.countsBySite[0].count").value(2));
```

Seed exactly two October 3 records at the same site for these assertions. Also test each filter alone/together, omitted filters, unknown IDs, reversed dates, empty results, and an unauthorized request after another user's cached-looking successful request. Assert no-store for details and photo URLs.
- [x] Run `.\mvnw.cmd -Dtest=SubmissionReadTest test`; expect failure before read routes exist.
- [x] Implement owner-scoped repository reads and admin-only search, with the same filtered result feeding rows and per-site counts. Group that small list in Java; add `ponytail: loads the assessment dataset; paginate and aggregate in SQL if volume grows`. Return zero-count results as an empty counts list, not misleading global totals. Fetch required relationships before mapping DTOs; do not depend on open-session-in-view.
- [x] Implement `/admin/workers` and permission-checked 5-minute photo URLs; return UTC `expiresAt`. UUID/random keys supplement authorization, never replace it.
- [x] Run `.\mvnw.cmd -Dtest=SubmissionReadTest,SubmissionCreateTest,AuthTest test`; expect PASS. Manually verify cross-user URL guessing on the deployment.
- [x] Commit: `git commit -m "feat: add authorized history and admin filters"`.

### Task 5: Complete the mobile worker flow

**Files:** Create `frontend/src/pages/{WorkerHistory,SubmissionForm,SubmissionDetail}.jsx`, `frontend/src/checklist.js`, `frontend/src/api.test.js`; modify `App.jsx`, `api.js`, `styles.css`, `package.json`.

**Interfaces:** `SubmissionForm({user,onCreated})`, `WorkerHistory({onOpen,onNew})`, `SubmissionDetail({id,onBack})`. `api.js` consumes the shared HTTP contract and sends the current CSRF token for writes. Keep the draft in app memory across an inline re-login; never persist files or session tokens to browser storage. `onCreated(id)` opens the detail only after a 201 response.

- [x] Add built-in Node tests `doesNotRetryWrites` and `reportsExpiredSession` in `api.test.js`, stubbing global fetch with the runner's mocking support. Add `"test":"node --test src/api.test.js"` to package scripts.

```javascript
await assert.rejects(api('/submissions', { method: 'POST', body: form }),
  error => error.status === 401);
assert.equal(writeCalls, 1);
```

The test supplies FormData, a CSRF-token response, and a 401 response for the write. Repeat with write 403 followed by `/auth/me` 401: expect the same 401 classification and exactly one write. With `/me` 200, preserve the 403. Add a non-JSON proxy error case: the user receives a readable service error, not a JSON parsing exception. Run `npm test`; expect failures before the helper handles these cases.
- [x] Implement the request helper and a minimal route structure in `App.jsx` with React Router: `/login`, `/submissions`, `/submissions/new`, `/submissions/:id`, `/admin`. Do not redirect away from an expired-session form and discard its state; show re-login inline, refresh CSRF after login, and require an explicit retry. On logout/account change clear prior records and drafts. On uncertain write results, offer history verification before retrying.
- [x] Implement the eight native answer groups with no preselection, site/date fields, 4,000-character notes, photo add/remove/previews, size/type/count feedback, and disabled submit while pending. Backend checks remain authoritative. Derive today via `Intl.DateTimeFormat` with `timeZone:'America/Vancouver'` and `formatToParts`; preserve API `YYYY-MM-DD` strings instead of parsing them as UTC dates.
- [x] Implement history and read-only details with plain text rendering of notes (no HTML injection). Fetch authorized photo URLs on demand; refresh expired links without making the bucket public. Revoke preview object URLs when no longer needed. Show honest empty/loading/error states.
- [x] Run `npm test` and `npm run build`; expect PASS/success. At a 375px viewport, submit an ISSUE with notes/photo, verify history/detail, force session expiry mid-form, re-login as the same user, and confirm fields/files survive. Repeat with a browser timezone different from Vancouver; the work date must not shift. Record these scenarios in `docs/verification.md`.
- [x] Commit: `git commit -m "feat: complete mobile safety form flow"`.

### Task 6: Finish the admin dashboard and RAS styling

**Files:** Create `frontend/src/pages/AdminDashboard.jsx`, `frontend/public/ras-logo.png`; modify `App.jsx`, `styles.css`, `docs/verification.md`.

**Interfaces:** `AdminDashboard({onOpen})` consumes `/sites`, `/admin/workers`, and `/admin/submissions`. Reuses `SubmissionDetail`. The apply-filters action sends only non-empty values; reset clears all four filters. Summary and table come from the same response.

- [x] Before implementing, record a failing browser acceptance check: admin selects one worker, one site, and an inclusive date range; expects matching rows and counts, with columns Worker / Site / Date / Status. Also specify rapid filter changes must not show an older response, and empty results must clear prior summary counts.
- [x] Implement the filters, table, per-site counts, and detail navigation. Use AbortController or a request sequence guard to discard stale responses. Present loading state without showing previous results as if they match new filters. No review action or charts.
- [x] Retrieve the official logo from [RAS's website](https://www.rasltd.ca/), verify any Instagram link through official cross-linking, and record asset provenance. Sample colors from the official asset/site; treat white/charcoal as application neutrals, not claimed brand standards. If Instagram is inaccessible, record that fact rather than inventing a handle. Apply visible labels, keyboard focus, non-color-only issue labels, touch targets of at least 44px, and a readable narrow-screen table layout.
- [x] Run `npm test` and `npm run build`; expect success. Repeat the failing acceptance scenario, rapid changes, no matches, keyboard navigation, worker/admin account switching, and a 375px viewport. Record actual outcomes; do not create a new UI test framework solely for these presentation checks.
- [x] Commit: `git commit -m "feat: add branded admin dashboard"`.

### Task 7: Verify the deployed assessment and package delivery

**Files:** Create `README.md`, `docs/erd.mmd`, `docs/erd.png`, `docs/handoff.md`; update `docs/verification.md`, `.env.example`, and `DemoSeed.java` only as needed for representative sample records.

**Interfaces:** Final artifacts are the public application URL, accessible repository URL, separately supplied demo credentials, and an ERD image linked by README. Handoff is a draft; do not send an email without instruction.

- [x] Seed representative fictional records for two framers, multiple sites and dates, including an ISSUE. Seed submission photos through private storage using the same rules; never insert unusable object keys. Make reruns idempotent and never delete existing submissions. Keep demo passwords outside committed files.
- [x] Run `.\mvnw.cmd verify`, the explicitly enabled storage smoke, `npm ci`, `npm test`, and `npm run build`. Expect all checks green; investigate failures without weakening assertions. Verify migrations against a fresh test database.
- [x] Execute the completion checks in the accepted spec on the final deployment. Include login after idle sleep, logout/cache isolation, ID tampering for records/photos, five maximum-size images, failed uploads, duplicate recovery, and the full worker/admin walkthrough. Never use a real production user or real site safety data. Record evidence and any remaining limitations honestly.
- [x] Render `docs/erd.mmd` as `docs/erd.png` using a one-off diagram renderer, inspect readability, and compare with both migrations. Show four entities, primary/foreign keys, unique worker/site/date constraint, eight answer columns, and one-to-many relationships; annotate the enforced 1-5 photo rule. Do not add a diagram dependency to the app.
- [x] Write README setup commands, environment variable names, local/deployed routing, stack, demo role instructions, assumptions, Render cold start, session reset on restart, unsupported HEIC, orphan-object limitation, ERD link, and verification commands. Include deployed/repository URLs once known. Ensure an evaluator can reproduce setup without the author's machine.
- [x] Prepare `docs/handoff.md` with the two URLs, ERD location, cold-start note, and a reminder to insert demo credentials privately. Practice explaining ownership enforcement, the database duplicate constraint, object-storage compensation, date semantics, and one live modification. No service secrets or demo passwords in this committed draft.
- [x] Check `git diff --check` and inspect staged files for unintended secrets/generated build files. Commit: `git commit -m "docs: package verified assessment delivery"`.

## Time and handoff

Aim for Tasks 1-2 in the first 3-4 hour session, Tasks 3-4 in the second, Tasks 5-6 in the third, and Task 7 in the final 2-4 hours. These are estimates; the first deployment gate determines whether the October 6-7 target remains realistic. Drop optional review/polish before any required feature or security check.

Execution complete: Tasks 1–7 were implemented natively and verified. One fresh whole-branch review identified three recovery issues; one tested fix pass addressed all three, with no deferred minor findings. Hosted idle/wake, final mobile worker/admin walkthroughs, ERD, README, private credentials and the PR handoff are complete. See [verification](../../verification.md) and [execution decisions](../../execution-decisions.md). Repository visibility and the evaluator email remain the candidate's submission steps.
