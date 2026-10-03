# Assessment handoff draft

Application: https://ras-assessment.onrender.com

Repository: https://github.com/souravC01/RAS-Assesment (currently private; candidate plans to make it public before submission, or must grant evaluator access)

PR: https://github.com/souravC01/RAS-Assesment/pull/1

ERD: [erd.png](erd.png); setup and assumptions: [README](../README.md); validation: [verification.md](verification.md).

Supply the admin and both framer passwords privately alongside their emails listed in README. No database, storage, or provider secrets belong in the email. Render's free API may take a few minutes to wake after inactivity; sessions are lost on restart. The deployed demo uses fictional safety records and synthetic photo fixtures.

No email has been sent. This is the content checklist for the candidate's final submission.

The assessment PDF is marked confidential and is excluded from the current source tree. Its initial published Git history also needs removal before making this repository public; otherwise keep the repository private and grant evaluator access.

## Suggested demonstration

1. Sign in as a framer, inspect history, and create a form for an unused site/date with an Issue, explanatory notes and a JPEG/PNG photo.
2. Open its immutable detail and private photo. Explain that Submitted does not certify safety.
3. Switch framers and demonstrate that the other worker's record/photo IDs return 404.
4. Sign in as admin; filter by worker, site and inclusive date range, inspect matching site totals, reset, and demonstrate empty results.

## Interview practice

- **Ownership:** Spring Security gates roles. Controllers load the account from the session's email, and owner-scoped queries constrain worker IDs. A random photo key supplements authorization. The request body cannot choose its worker.
- **Duplicates:** Application validation improves messages, but the database's `uq_worker_site_date` constraint is the concurrency authority. Two racing creates produce one 201 and one 409; the loser cleans up uploaded objects.
- **Atomicity:** Object storage and PostgreSQL cannot share one ordinary transaction. Validate first, upload unique keys, then insert through `TransactionTemplate`; catch failures outside its commit boundary to compensate. A deferred-trigger integration test proves actual commit failure cleanup. Process death remains the documented orphan-object risk.
- **Dates:** Work date is a Vancouver calendar date, not a timestamp. `LocalDate` and browser `YYYY-MM-DD` strings preserve it; a Vancouver `Clock` controls future-date validation. UTC submission time remains separate. Filters compare dates inclusively.
- **Sessions and retries:** Cookie sessions use Spring Security CSRF. A write's 403 may precede authentication rejection, so the client checks `/me` once and never resends the write. Files stay in component memory through same-user re-login; a different user clears the draft.
- **Hosting:** Render static rewrite keeps the API same-origin and passed a real five-photo, 25 MB transport check. Database/photo persistence lives in Neon. Free-service sleep was an accepted assessment tradeoff.
- **Live change rehearsal:** Explain how to alter an existing checklist label without changing its persisted key. For a new persisted answer, update the DTO/entity, a new Flyway migration, validation/tests, form labels, details and ERD. Avoid silently changing a migration already deployed.
- **Scope:** No extra review actions, signup, charts or offline mode were added. The implementation focuses on the assessment requirements.
