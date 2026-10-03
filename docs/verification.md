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
- This verifies the actual static-site API rewrite. The in-app browser tool failed to start; visual browser interaction remains outstanding.
- Backend: `https://ras-assessment-api.onrender.com`; initial Render deploy connected to Neon successfully using TLS with server certificate verification.
- Protected storage probe tests: 3/3 passed alongside AuthTest 2/2, covering admin access, framer denial, missing CSRF, invalid images/count, and oversized files.

Maximum-size multipart delivery and the complete app remain to be verified.
