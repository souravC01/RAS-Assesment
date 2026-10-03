# Verification record

## Foundation

- Java 21, Node 24, Docker 29.7.2, and GitHub authentication available.
- AuthTest RED: two failures, expected anonymous CSRF endpoint 200 but received 401 before implementation.
- Backend GREEN: `mvnw.cmd test` passed 3 tests against PostgreSQL 16 containers.
- Frontend request tests RED: four expected failures before request handling implementation.
- Frontend GREEN: four request tests passed; `npm run build` succeeded with Vite 8.3.2.
- Neon CLI 8.0.3 authenticated; requested skills and MCP setup completed.
- Linked project `blue-term-51539745`, production branch `br-ancient-bird-b55e67tu`.
- `neon deploy` completed; existing `images` bucket verified private. `.env.local` and `.neon` are Git-ignored.
- Initial planning commit published to main using the user's approved GitHub noreply address.

- Docker image built successfully with Java 21; runtime runs as a non-root user.
- Render Blueprint validated after GitHub integration access was granted.
- Free Render API service: `srv-db0ita3ncjis739nli60`; frontend: `srv-db0itgs9v7es73bl2c6g`.

## Private photo storage

- `PhotoStorageTest` first failed at the unimplemented upload operation (1 executed, 0 skipped).
- `STORAGE_SMOKE=true`, `mvnw.cmd -Dtest=PhotoStorageTest test`: passed at 2026-10-03 16:33 UTC (1 executed, 0 skipped).
- The test uploaded a generated JPEG, denied unsigned reads, fetched identical bytes through a five-minute signed URL, and deleted its random probe object in `finally`.
- AWS SDK v2 uses path-style addressing and bounded request timeouts. Credentials and signed URLs are omitted from this record.

## Hosted login

- 2026-10-03 16:39 UTC: HTTP checks through `https://ras-assessment.onrender.com` passed for health, anonymous 401, login without CSRF 403, wrong password 401, admin/framer login, repeated session reads, and logout.
- Session cookies verified Secure, HttpOnly, SameSite=Lax, Path=/, with no Domain attribute; authenticated reads returned `Cache-Control: no-store`.
- This verifies the actual static-site API rewrite. The in-app browser tool failed to start; the bundled Playwright runtime with installed Chrome then verified the real mobile browser flow.
- Real Chrome at 390×844: framer/admin login, reload, logout, account switching, secure cookie flags, no page errors, and no horizontal overflow passed.
- Backend: `https://ras-assessment-api.onrender.com`; initial Render deploy connected to Neon successfully using TLS with server certificate verification.
- Protected storage probe tests: 3/3 passed alongside AuthTest 2/2, covering admin access, framer denial, missing CSRF, invalid images/count, and oversized files.

## Hosted maximum-size photos

- 2026-10-03T16:45:41Z: five valid 5,000,000-byte JPEG files (25,000,000 image bytes plus multipart framing) passed through the frontend rewrite to the protected API.
- All five signed reads returned identical bytes; unsigned reads were denied. Malformed image -> 400; oversized image -> 413.
- All five probe objects were deleted. The temporary profile and endpoint are removed in the submission implementation.

## Submission logic

- SubmissionCreateTest RED: two missing `/sites` route failures before implementation.
- GREEN: SubmissionCreateTest 2/2 and AuthTest 2/2 passed, including required answers, issue notes, content validation, ownership, Vancouver date boundary, concurrent duplicate, second-upload failure, and a deferred PostgreSQL commit failure with object cleanup.
- Database migrations enforce required values, allowed answers, unique worker/site/date, foreign keys, and photo sizes/keys.
- Hosted submission 1 returned 201. Neon metadata confirmed the signed-in framer, Cedar Grove, October 3, ISSUE, and one 2,000-byte photo; the admin storage-probe route returned 404.

## Authorized reads and filtering

- SubmissionReadTest first failed at the missing history route. PostgreSQL then exposed an untyped null date-filter parameter; using the non-null date column as the omitted bound fixed it.
- GREEN: all 5 related backend tests passed. Ownership history, concealed cross-user detail/photo IDs, admin access, five-minute photo expiry, no-store headers, individual/combined inclusive filters, unknown IDs, reversed dates, and empty counts were checked.
- Hosted checks passed: admin read followed by another framer guessing the same detail/photo returned 404; admin route returned 403; inclusive filtered rows/counts matched; a signed private photo returned the exact stored bytes and unsigned access was denied.

## Worker UI

- Browser RED: the foundation lacked the worker history screen.
- Unit RED: Vancouver date conversion missing and failed session diagnosis masked a 403. GREEN: all 6 request/date tests passed and the production build succeeded.
- The worker browser scenario verifies issue notes/photo, a 375px viewport, mid-form cookie expiry, inline same-account sign-in with unchanged fields/files, explicit submit, private photo loading, history/detail, and an Asia/Kolkata browser retaining the selected Vancouver work date.
- A nested textarea label initially included its entered text; using a separate `htmlFor` label fixed the browser-accessible Notes label. The completed browser run is recorded with the task ledger.

## Admin UI and branding

- Admin acceptance RED: Worker filter absent from the placeholder screen.
- Official green logo and sampled `#0d553a` are documented in [brand.md](brand.md). Instagram cross-link verified; direct content fetch unavailable.
- Admin Chrome acceptance passed: combined worker/site/inclusive dates, matching table/counts, empty counts cleared, delayed older responses discarded, detail navigation, keyboard focus, account switch and 375px layout.
- The mobile table initially leaked an absolutely positioned hidden header outside its scroll area. Native header labeling removed that overflow; the browser check passed afterward.

## Final assessment checks

- `mvnw.cmd clean verify`: BUILD SUCCESS; 6 executed tests passed against fresh PostgreSQL containers. The opt-in storage test was skipped in that ordinary suite.
- Explicit `STORAGE_SMOKE=true`, `mvnw.cmd -Dtest=PhotoStorageTest test`: 1 executed, 0 skipped, passed using real Neon storage.
- `npm ci`, 6 request/date tests, and production build passed; frontend dependency audit reported zero vulnerabilities.
- Permanent hosted endpoint: five 5,000,000-byte photos returned 201; all five authorized signed reads returned exact bytes. Repeating the site/date returned 409 and history contained one record.
- Storage/database reconciliation: 5 fictional submissions, 9 photo rows, 9 private objects, 40,002,000 bytes, zero probe objects. This confirms duplicate compensation left no additional objects.
- Browser network failure preserved draft/files, offered history verification, and issued exactly one write. Different-account re-login cleared the former user's draft.
- ERD rendered with one-off Mermaid CLI 12.0.0 and visually inspected against both migrations. No diagram runtime was added to the app.
- Live account passwords are in a Git-ignored private handoff; no provider credentials or demo passwords are in committed delivery documents.
- Fresh whole-branch review found zero Critical, three Important and zero Minor findings. All three Important findings were reproduced before correction: history navigation discarded a draft, truncated JSON bypassed uncertainty handling, and failed site loading had no recovery.
- One fix pass passed the complete suite on October 3: backend 6 executed/1 opt-in skipped, explicit real storage 1/1, frontend 8/8 and build, and three Chrome recovery scenarios. History now opens separately with the original draft intact; malformed response bodies preserve known errors or explain uncertain writes; sites can be retried and reloaded after same-account sign-in.
- The test wrapper installs dependencies before starting its owned preview server; this resolved the Windows native-module lock encountered before the pause. Dependency audit reported zero vulnerabilities and the tracked-file secret scan passed.
- Render idle/wake verification passed after a controlled sixteen-minute idle period. At 21:18:27 UTC, the first auth response was 401 and fresh login/session read passed. Wake-up took 166.549 seconds; the previous in-memory session had expired after the API process restarted.
- Frontend deployment `830649f` succeeded on Render. Real Chrome loaded its deployed assets at 375px and passed controlled API-failure checks for lost connection, truncated 201 response, failed site lookup, and expired session. Each uncertain write was attempted once; opening history retained the original notes and photo. These failure checks intercept API responses rather than contacting the backend during its idle period.
- Fresh local PostgreSQL 16 setup applied both migrations; the packaged API's `local` profile and Vite proxy passed real framer/admin login, three-site selection, local HttpOnly cookie checks, and logout in Chrome. The verification processes and local container were stopped afterward; the database volume was preserved.
- The initial resumed login woke a stopped Render API. Provider logs show Spring startup at 20:59:28 UTC taking 133 seconds (145 seconds from process start); demo login and session read succeeded at 20:59:40 UTC. The following controlled sixteen-minute idle test records its own outcome separately.

- Final real-backend Chrome walkthroughs passed on the deployed recovery bundle: worker issue/notes/photo submission, private photo read, history/detail, expired-session draft preservation and explicit retry, Vancouver work date in an Asia/Kolkata browser; admin inclusive filters, matching totals, empty results, stale responses, details, keyboard access, 375px layout and role switching.
- The confidential source PDF was removed from both published branches' history after explicit approval. All 13 rewritten commits retained identical non-PDF file blobs; the local PDF stayed byte-identical. Commit hashes above describe the deployments/tested candidates before this approved rewrite.
- Current tracked-file and committed-history scans found no live demo/database/storage credentials. The source PDF, private credential handoff, local provider configuration and generated build files remain excluded.
- Final completion gate after the approved history rewrite passed: backend 6 executed/1 opt-in skipped, real storage 1/1, frontend 8/8, clean dependency install/audit0/build, four Chrome recovery scenarios, whitespace checks and tracked-file secret scan.

All accepted product requirements and delivery artifacts have been checked. Repository visibility remains the candidate's submission step; it is still private.

## Follow-up review fixes — October 3

- Stale actor assertion RED: session B plus expected worker A returned 201. GREEN: it returns 412 before any storage interaction or insert; a missing assertion returns 400. A valid assertion still derives ownership from the session and ignores a body-supplied worker ID.
- Commit acknowledgement RED: the former catch deleted objects after an actual PostgreSQL commit followed by an injected `TransactionSystemException`. GREEN: durable submission/photo rows remain and their objects are retained. A second test actually rolls back but reports an unknown outcome; it also retains objects conservatively. These tests inject acknowledgement loss after a real database outcome rather than cutting a live network socket. Existing deferred-constraint and duplicate tests still prove confirmed failure compensation.
- Full regression gate: backend 9 executed/1 opt-in skipped, explicit Neon storage 1/1, frontend 9/9, clean install/audit0/build, and the four previous Chrome recovery scenarios passed. The new Chrome scenarios cover cross-tab logout/login, focus-triggered account change, the server assertion race with tab broadcasts disabled, and same-user expiry/re-login retaining draft fields/photos. The three account-change scenarios failed before the fix; all four now pass with no unintended creation or automatic write retry.
