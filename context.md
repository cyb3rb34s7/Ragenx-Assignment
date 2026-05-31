# Context

Running state of the build. Read this first every session. Newest notes near the top of each section.

---

## Now

- `common/` layer + `health` module built and **verified** (build green; live curl on :8081 passed envelope/trace/404 checks). Next: the `cases` module.
- **Env note:** port **8080 is occupied by Docker** (`com.docker.backend` + WSL relay) on this machine. The app's committed port is 8080 (correct per brief); free 8080 (or stop the container) before the live session, or run with `--server.port=<n>`.

## Done

- 2026-05-31 — Read the brief (`BUILD_EXERCISE_BRIEF.md`), email, and `case_v1.json` end to end; explained requirements back to the user.
- 2026-05-31 — Adapted prior project's `conventions.md` (Python/FastAPI) to this stack (Spring Boot / Gradle backend, React+Vite / Tailwind / zustand / ky frontend). Created `CLAUDE.md` and `docs/architecture.md` with diagrams. Iterated merge model (UPSERT + null status), dropped `diff_summary`, expanded data-model reasoning.
- 2026-05-31 — Extracted the Initializr zip into `/backend`; moved spec/reference docs into `docs/` (kept `CLAUDE.md`/`CHANGELOG.md`/`context.md` at root per user); `case_v1.json` → `backend/src/main/resources/`; reference Java samples → `docs/reference/`. Added root `.gitignore`. `git init -b main` + remote `origin`.
- 2026-05-31 — Downgraded scaffold from Boot 4.0.6 / Gradle 9.5.1 to Boot 3.4.2 / Gradle 8.12; fixed starter names (`web`, `test`). Initial 2 commits pushed to `origin/main`.
- 2026-05-31 — Wired `common/`: `ApiResponse`/`ApiError`/`ResponseFactory` envelope, `ErrorCode`/`ApiException`/`GlobalExceptionHandler`, `TraceIdFilter` + `TraceContext` (adapted `TraceIdGenerator`), `Constants`. `application.properties`→`application.yml` (port 8080, snake_case, non_null, trace-id log pattern). Added `health` module (`GET /health`). Tests: `TraceIdGeneratorTest`, `HealthControllerTest` (success/inbound-trace/404). Build green; live-curl verified on :8081.

## Next (ranked)

1. `cases` module: models (`ExtractedField`, `MergedField`, `CaseState`, `FollowUpRequest`) → `CaseRepository` → `CaseSeeder` (load case_v1.json via a `JsonLoader`) → `GET /cases/{id}` → `MergeService` + `POST /cases/{id}/follow-ups` + ≥3 merge tests.
2. `queries` module (`POST /queries`, `GET /queries?caseId=`), then backend README (4 curl examples).
3. Ops: Dockerfile, compose, scripts, Makefile, runbook.
4. Frontend (live phase): theme → shared primitives → `case-review` module.

Note: `JsonLoader` (common/util) still to be created with the `cases` slice (deferred from the common slice since nothing needed it yet).

## Build setup (confirmed 2026-05-31)

- System Java: **17** (Zulu 17.0.17) → Spring Initializr Java 17, Gradle wrapper (no system Gradle).
- Spring Initializr: Gradle-Groovy, Jar, Group `com.ragenx`, Artifact `pv`, Name `pv`, Package `com.ragenx.pv`, Boot latest stable 3.x.
- Dependencies: **Spring Web, Lombok, Validation** only. No Actuator (own `/health` in our envelope), no DevTools. `spring-boot-starter-test` included by default. Logback default for logging.
- Output goes into `/backend`.

## Problems & solutions

_(none yet — record problem, root cause, solution, follow-up as they occur. Never repeat one.)_

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

## Open questions for the user

1. Base package `com.ragenx.pv` vs `ai.parallelloop.pv` — confirm.
2. Real GitHub collaborator handle (brief has placeholder `[@gargi-github-handle]`) — needed before push.
3. `case_v2_followup_payload.json` is referenced by the brief but **not present** in this folder. It's shared at the live session. For Phase 1A tests we'll use a synthetic follow-up modelled on `case_v1.json`'s shape. Confirm that's fine.
4. Keep `cases`/`queries` as two modules (current plan) or collapse to one? — defaulting to two.
5. Logback (default) vs Log4j2 — using Logback; flag if you want Log4j2 pulled in.

_Resolved:_ trace IDs now use the prior project's 10-char `TraceIdGenerator` (was the UUID open question).
