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

Complete app behavior remains to be verified.
