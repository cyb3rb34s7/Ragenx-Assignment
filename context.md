# Context

Running state of the build. Read this first every session. Newest notes near the top of each section.

---

## Now

- **All three phases shipped & pushed.** Backend (Phase 1A) + ops (Phase 1B) + the Phase 2 **frontend reviewer screen** (Vite/React-TS/Tailwind/ky/zustand) live in `/frontend`, build green, pushed to `origin/main`.
- **Live-session backend tweaks (pushed):** per-request completion log line in `TraceIdFilter` (trace id now in logs for every request, not just errors); optional `follow_up_number` accepted on follow-ups and reflected on the case (the real v2 payload sends it; strict parsing would otherwise 400).
- **Pending:** card-layout redesign of the reviewer screen (`Section` + `FieldCard`, UI-only) — plan approved, not yet built.
- **Stray file to resolve:** `docs/claude-code-session.jsonl` is untracked and NOT gitignored — the private session log must NOT be committed to the public repo (we share it by email). Decide: delete it, or gitignore it. The canonical copy lives in the gitignored `backups/`.
- **Phase 1A + 1B detail.** Backend: 7 endpoints, 39 tests. Ops: Dockerfile (multi-stage, non-root, pinned, 417 MB), docker-compose (+healthcheck, init), `ops/run.sh|backup.sh|restore.sh`, Makefile, root README "Operations" runbook. All `/super-review`'d; verified live.
- **Known gap (accepted):** `CaseRepository.put` (PUT/import) and `compute` (follow-up/seed) are each atomic per key but not mutually serialized — a concurrent PUT + follow-up on the same case could lost-update. Acceptable: restore runs against a quiesced service, single in-memory case. Same class as the "no concurrency test" note.
- **`jq` is NOT installed on this dev box** — `backup.sh`/`restore.sh` require it (and `curl`). The scripts will check for both and fail gracefully; the runbook lists them as prerequisites (install via choco/scoop/winget locally, or rely on the grader's Unix env).
- **Port:** the service's known port is **8412** (changed from 8080 on 2026-06-01 — 8080 is permanently occupied by a global Docker container on the dev machine). All run/ops/README references use 8412.

## Done

- 2026-06-01 — Phase 2 frontend **reviewer screen** built and pushed: `useCase` hook (fetch), zustand UI store (sort/filter/classification/modal), `FieldRow` + `RaiseQueryModal`, full `ReviewPage` (grouped sections, confidence color-coding, overridden conflict view with `previous_value`, raise-query → POST `/queries`, sort-low-first + conflicts-only filter, missing_fields banner, loading/error states). ky envelope client + snake_case types. Replaced the empty workspace wholesale with the completed build.
- 2026-06-01 — Backend live-session tweaks (pushed): `TraceIdFilter` logs a per-request completion line (method/path/status/latency) with the trace id; optional `follow_up_number` added to `FollowUpRequest` + reflected on `CaseState` (carried through both merge branches). Dev CORS (`WebConfig`) for the React origin.
- 2026-05-31 — Read the brief (`BUILD_EXERCISE_BRIEF.md`), email, and `case_v1.json` end to end; explained requirements back to the user.
- 2026-05-31 — Adapted prior project's `conventions.md` (Python/FastAPI) to this stack (Spring Boot / Gradle backend, React+Vite / Tailwind / zustand / ky frontend). Created `CLAUDE.md` and `docs/architecture.md` with diagrams. Iterated merge model (UPSERT + null status), dropped `diff_summary`, expanded data-model reasoning.
- 2026-05-31 — Extracted the Initializr zip into `/backend`; moved spec/reference docs into `docs/` (kept `CLAUDE.md`/`CHANGELOG.md`/`context.md` at root per user); `case_v1.json` → `backend/src/main/resources/`; reference Java samples → `docs/reference/`. Added root `.gitignore`. `git init -b main` + remote `origin`.
- 2026-05-31 — Downgraded scaffold from Boot 4.0.6 / Gradle 9.5.1 to Boot 3.4.2 / Gradle 8.12; fixed starter names (`web`, `test`). Initial 2 commits pushed to `origin/main`.
- 2026-06-01 — Built `queries` module: `CreateQueryRequest`/`Query` models, `QueryRepository` (atomic `compute` append, immutable reads), `QueryService` (cross-module case-existence check via `CaseService.findCase`), `QueryController` (`POST /queries`, `GET /queries?caseId=`). Added `CaseService.findCase` + a missing-query-param→400 handler; removed unused `VALIDATION_INVALID_FIELD_PATH`. **Dropped field-path structural validation** (lean per user; a reviewer may query a `missing_fields` entry, and the UI sends a rendered path) — `fieldPath` is `@NotBlank` only. 31 tests green; `/super-review` flagged only doc-drift (fixed) + a snapshot-immutability test (added). Live-curl verified.
- 2026-05-31 — Built `cases` module: `JsonLoader`, models (`ExtractedField`/`MergedField`/`CaseState`/`FollowUpRequest`), `CaseRepository` (atomic `compute`), pure `MergeService` (null→baseline + diff), `CaseSeeder`, `CaseService`, `CaseController`. Strict parsing (`fail-on-unknown-properties`). 24 tests green. Ran `/super-review` (no criticals); applied fixes: 404-before-validation (atomic), reason-code constants, seeder `case_id` guard, +4 tests (new-section, blank-value, null-section, invalid-leaf-on-unknown-case). Known gap (accepted): no concurrency test (#9) — atomicity is structural via `compute`.
- 2026-05-31 — Wired `common/`: `ApiResponse`/`ApiError`/`ResponseFactory` envelope, `ErrorCode`/`ApiException`/`GlobalExceptionHandler`, `TraceIdFilter` + `TraceContext` (adapted `TraceIdGenerator`), `Constants`. `application.properties`→`application.yml` (port 8080, snake_case, non_null, trace-id log pattern). Added `health` module (`GET /health`). Tests: `TraceIdGeneratorTest`, `HealthControllerTest` (success/inbound-trace/404). Build green; live-curl verified on :8081.

## Next (ranked)

1. **Card-layout redesign** of the reviewer screen — `Section` + `FieldCard` dumb components, fields as cards in a responsive grid (value + colored confidence pill + status badge; overridden cards show previous value + centered Raise Query). UI-only; plan approved.
2. Resolve `docs/claude-code-session.jsonl` (delete or gitignore — keep the private log out of the public repo).
3. Email the latest session log (`backups/claude-code-session.jsonl`).

## Build setup (confirmed 2026-05-31)

- System Java: **17** (Zulu 17.0.17) → Spring Initializr Java 17, Gradle wrapper (no system Gradle).
- Spring Initializr: Gradle-Groovy, Jar, Group `com.ragenx`, Artifact `pv`, Name `pv`, Package `com.ragenx.pv`, Boot latest stable 3.x.
- Dependencies: **Spring Web, Lombok, Validation** only. No Actuator (own `/health` in our envelope), no DevTools. `spring-boot-starter-test` included by default. Logback default for logging.
- Output goes into `/backend`.

## Problems & solutions

- **2026-06-01 — README query curl example failed (`validation.bad_format`).** Root cause: wrote the body in camelCase (`caseId`/`fieldPath`), but the wire is snake_case (global Jackson `SNAKE_CASE`) + strict parsing, so `caseId`/`fieldPath` were unknown properties → 400. Caught by running the documented curls before commit. Solution: README uses snake_case (`case_id`/`field_path`) + a note that the brief's `{caseId,…}` maps to snake_case on the wire.
- **2026-05-31 — `curl -d` follow-up POSTs returned `validation.bad_format` on Windows.** Root cause: the `§` character in hand-typed `"source":"p.3 §2"` was mangled by the shell, producing invalid bytes Jackson couldn't parse — NOT a code bug (MockMvc tests with `§` pass; `§` loaded from `case_v1.json` serializes fine). Solution: use ASCII in ad-hoc curl, or `--data @payload.json` for README examples. Follow-up: README curl examples must use `--data @file.json`.

## Decisions

- **2026-05-31 (revised) — Merge is UPSERT; statuses are `new` / `overridden` / `unchanged` / `null`.** Per user: update existing field, insert new field. The brief's "your call" 4th case (field in stored, absent from follow-up) → **`status: null` (untouched)**, value left as-is. Dropped the earlier `retained` idea — it invented a status implying an action the system never took; `null` is the honest signal and distinguishes "never mentioned" from "re-sent unchanged." Kept `unchanged` because the brief mandates it and it's a real provenance signal. See `architecture.md §4.2`.
- **2026-05-31 — Dropped `diff_summary` from the merged response.** It was my unprompted addition (status counts); not in the brief, frontend can derive counts itself. Removed per KISS / user challenge.
- **2026-05-31 — `missing_fields` retained (brief requirement, not an addition).** The follow-up payload carries a top-level `missing_fields` array; brief requires surfacing it in the merged response. Clarified for the record since it was queried. It represents fields the **AI tried and failed to extract** — distinct from fields the follow-up didn't mention (those are `status: null`/untouched).
- **2026-05-31 — Data modeling.** Sections = generic `Map<String, Map<String, MergedField>>` (not typed POJOs) so `new` fields/sections are accepted; validation is structural. `GET` and `POST /follow-ups` return the **same** `CaseState` shape (v1 has null statuses) — one shape for the frontend. **No version history**: store only current `CaseState` + `version` counter (simplified from the earlier `List<Case>` idea — `previous_value` only needs the value right before this follow-up). Request DTOs (`FollowUpRequest`, `CreateQueryRequest`) separate from stored model; response = stored model wrapped in `ApiResponse<T>`. Lombok `@Value`/`@Builder`; Jackson global `SNAKE_CASE`. See `architecture.md §5.2`.
- **2026-05-31 — Adapted conventions, did not copy.** Dropped prior-project domain sections (money/INR, connectors, agents, SQLite/repositories-as-DB, pagination, idempotency-keys, config secrets). Kept principles, API envelope, trace IDs, constants rule, no-fallback rule, testing philosophy. Translated Python→Java and FastAPI→Spring Boot patterns.
- **2026-05-31 — Frontend state: zustand + ky, no TanStack Query.** Deviates from prior convention §12.2 (which mandated TanStack for server state). At this scale (one case + a query list) ky-in-hooks + zustand-for-UI is simpler. Flagged.
- **2026-05-31 — Trace ID = 10-char id (5 timestamp + 5 random, ambiguity-free charset) via the prior project's `TraceIdGenerator`.** Replaces the earlier UUID idea (user directive). Timestamp prefix → roughly time-ordered; collisions effectively nil at this scale (not provably impossible — flagged). MDC key `traceId`.
- **2026-05-31 — Logging: SLF4J via Lombok `@Slf4j` + Logback (Spring Boot default, NOT Log4j2) + MDC `traceId` on every line; human-readable, no JSON pipeline.** Did not adopt prior `StructuredLogger` verbatim (its fields are CMS-specific); a trimmed helper suffices. Brief's "no production logging setup" non-goal respected.
- **2026-05-31 — Lombok + builder pattern** across backend models/services to cut boilerplate (user directive). Off where explicit code reads clearer.
- **2026-05-31 — Constants consolidated into one `Constants.java` per module** (user directive), nested enums inside. Capital C is mandatory — Java public-class-name = filename; lowercase `constants.java` won't compile cleanly. Flagged to user.
- **2026-05-31 — Kept `cases` and `queries` as separate modules.** Queries is a distinct top-level REST resource (`/queries`, not nested) with its own storage/model; keeps the `cases` merge logic clean. Offered to merge if user prefers fewer modules.
- **2026-05-31 — "bootstrap" clarified → renamed `CaseSeeder.java`** (a `CommandLineRunner` in the `cases` module), dropped the one-class folder. It's just the startup loader for `case_v1.json`.
- **2026-05-31 — `diff_summary` added to merged response (unprompted).** Cheap; makes frontend conflict-filter and counts trivial. Full-stack-coherence win.
- **2026-05-31 — Frontend framework: Vite + React + TS.** Fastest cold start for a 45-min live build vs Next.js.
- **2026-05-31 — Base Java package: `com.ragenx.pv`.** Matches repo name. Trivially changeable to `ai.parallelloop.pv`.

## Cases module — confirmed decisions (2026-05-31)

- **Follow-up shape grounded in the brief's two signals:** "same shape as the initial case" + a top-level `missing_fields` array. The authoritative payload is `case_v2_followup_payload.json`, shared only at the live session; Phase 1A uses a synthetic follow-up modeling those signals.
- **Strict top-level parsing (user choice, fail-closed):** `FollowUpRequest` + `ExtractedField` use `@JsonIgnoreProperties(ignoreUnknown=false)`. Unknown top-level key OR extra leaf property → `HttpMessageNotReadableException` → `validation.bad_format` (400). **Caveat:** an unanticipated top-level key in the live payload would 400 mid-demo; flipping to tolerant is a one-annotation change if needed.
- **Sections are open:** section/field names are free-form map keys, so follow-ups may carry fields/sections not in `case_v1` (the `new`-status path). Leaves are strict `{value, confidence, source}`.
- **Body `case_id`/`version` modeled (for strict parsing) but ignored:** path is authoritative for id; version is server-managed (increments per follow-up).
- **Case-level metadata** (`case_classification`/`extracted_at`/`source_document`) updated from the follow-up when present, retained otherwise.
- **Status compares on `value` only:** same value + changed confidence/source → `unchanged`, with confidence/source refreshed to the follow-up's latest. Different value → `overridden` (+ `previous_value`).
- **Test follow-up** mirrors `case_v1.json` + `missing_fields` and includes new + overridden + unchanged + untouched fields in one POST.
- **Single pipeline for seed + follow-up (user's design):** bootstrap parses `case_v1.json` into the SAME `FollowUpRequest` model and runs it through the SAME `MergeService.merge`, so the whole pipeline is exercised on every boot. `merge(current, incoming)`: `current == null` → **baseline** (version 1, statuses null, no diff); `current != null` → diff merge (version+1). The 404-on-unknown-case guard lives ONLY in the follow-up endpoint path (inside `repo.compute`, atomic) so the endpoint never creates a case, but the seeder can. Repository owns the `ConcurrentHashMap` + atomic `compute`; `MergeService` is a pure function (no deps, easy unit tests); `CaseService` orchestrates (find/404, seedInitial, applyFollowUp).

## Open questions for the user

1. Base package `com.ragenx.pv` vs `ai.parallelloop.pv` — confirm.
2. Real GitHub collaborator handle (brief has placeholder `[@gargi-github-handle]`) — needed before push.
3. `case_v2_followup_payload.json` is referenced by the brief but **not present** in this folder. It's shared at the live session. For Phase 1A tests we'll use a synthetic follow-up modelled on `case_v1.json`'s shape. Confirm that's fine.
4. Keep `cases`/`queries` as two modules (current plan) or collapse to one? — defaulting to two.
5. Logback (default) vs Log4j2 — using Logback; flag if you want Log4j2 pulled in.

_Resolved:_ trace IDs now use the prior project's 10-char `TraceIdGenerator` (was the UUID open question).
