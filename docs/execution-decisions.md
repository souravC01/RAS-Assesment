# Execution decisions

October 3, 2026. Native implementation with one fresh whole-branch review and one tested correction pass. This preserves the decision record after temporary execution files are removed.

## Rulings I made

- Use the fresh, project-only workspace on feat/site-safety rather than creating a second checkout — no existing application or default-branch changes need isolation — no separate worktree cleanup.
- Run shell commands outside the sandbox after its helper failed before process creation — user authorized retry and the read-only elevated check succeeded — elevated commands require scoped review.
- Use per-command Git safe.directory for this exact workspace — Git was initialized by the sandbox account and execution now uses the real user — no global trust expansion.
- Initial main commit published after explicit user authorization; GitHub email privacy rejection resolved with user's approved repo-local noreply email and correction of the unpushed commit — no published history rewritten.
- User supplied Neon project blue-term-51539745 and exact CLI setup — production branch linked and exact preview.buckets policy retained despite CLI GA warning — no project recreation.
- Neon tooling files remain local/ignored; neon.ts and dependency lockfiles are versioned — avoids shipping local account/skill installation state — skills can be reinstalled using documented CLI command.
- Begin independent storage smoke while Task 1 deployment awaits repository access — no deployed transport or UI completion claimed — full-size deployment gate remains mandatory.
- Use random probe/ keys in the already-provisioned empty private images bucket, with finally cleanup — preserves the user's exact bucket policy and avoids unnecessary infrastructure — failed cleanup could leave a small isolated probe object.
- Use the official Render CLI for Docker creation and documented routes API for new static-site rewrites — connector lacks Docker/root-directory and route support — same render.yaml remains reproducible configuration.
- Remove temporary probe tests with the deleted probe route after its deployed gate passes — production submission tests replace the transport-specific controller tests — historic probe evidence remains in Git and verification record.
- Prepare independent read endpoints while Task 3 deployment finishes — the deployed transport gate already passed and creation tests are green — Task 3 remains incomplete until its real create/metadata check passes.
- Commit deploy candidates before hosted acceptance because Render builds the Git branch — retain draft PR and incomplete task status until the hosted checks pass — unverified candidates remain isolated to the assessment demo/feature branch.
- Start the single final fresh review while the sixteen-minute provider idle check runs — implementation/artifacts and all other gates are complete; reviewer must avoid hosted traffic — any wake-related code change must join the one tested fix pass before completion.
- Signup, editing, review actions, charts and offline operation remain excluded — preserves the user's assessment-first scope — those workflows are unavailable.
- HEIC conversion remains excluded — accepted JPEG/PNG-only validation stays visible — phone users must convert HEIC before upload.
- Pagination and SQL aggregation remain deferred — the fictional assessment dataset is small and counts match the filtered rows — larger datasets need pagination and database aggregation.
- Sessions remain in memory — restart sign-in and inline draft recovery are the accepted free-hosting behavior — a process restart requires authentication again.
- Background orphan reconciliation remains deferred — failed writes compensate synchronously, with process-death limits documented — interruption or failed deletion can leave an orphan object.
- Five-minute signed links remain usable after logout — their short expiry is documented and ownership gates issuance — a copied issued link works until expiry.
- Hosted wake behavior is verified independently rather than judged by the source reviewer — the reviewer made no app requests during the idle gate — the recorded idle outcome must describe observed behavior honestly.
- Credential/deployment verification uses the implementer's recorded checks — reviewer was prohibited from reading secrets — no independent second-person review of secret values.
- Finish by pushing and updating the existing PR under the user's prior authorization — no repeat integration question is needed — branch remains unmerged until the user reviews it.
- Use fictional records created through the real submission workflow as the hosted demonstration fixtures — this keeps photos valid and obeys the unique worker/site/date rule without direct metadata inserts — a fresh local database initially has no submissions until a framer creates them.
- Leave repository visibility and evaluator access with the user — user replied they will eventually make it public, which is a future intention rather than authorization to publish now — evaluator access remains the candidate's final submission step.
- Exclude the confidential assessment PDF from the current source tree, preserving the local copy — the final requirement check found it tracked in the initial commit despite the planning-only bootstrap request — published history still retains it unless the user approves history rewriting. Explicit approval requested; never force-push without that answer.
- Rewrite only main and feat/site-safety to remove the confidential PDF after explicit user approval — prepare a disposable local mirror, verify all app/document blobs unchanged, publish with exact expected-head leases and preserve the local PDF — commit IDs change and old clones must refresh; provider deployment hashes remain historical evidence.
- Update Task7 BASE to f7a36efec6339e4c8a2da1da22712876d0b85123 using the verified commit map — the PDF-only rewrite changed52b2db9's hash while preserving its other files — older hashes in verification remain historical deployment evidence.

## Review corrections

- fixed history navigation destroying the draft — Chrome history recovery RED missing _blank → GREEN separately opened history and unchanged notes/files; suite backend6 + storage1 + frontend8 + Chrome3 passed.
- fixed response-body failures bypassing uncertain-write handling — interrupted successful write and malformed401 tests RED raw SyntaxError → GREEN status0/history advice and preserved401, one write; suite backend6 + storage1 + frontend8 + Chrome3 passed.
- fixed failed/expired site loading permanently blocking the form — Chrome retry/expired RED missing controls → GREEN retry, inline login, site reload and draft/files preserved; suite backend6 + storage1 + frontend8 + Chrome3 passed.

## Deferred minors

None raised by the final reviewer.
