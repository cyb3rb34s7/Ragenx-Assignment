# Changelog

Append-only, newest at top. Per-entry format in `docs/conventions.md §2.3`.

---

## 2026-06-01 — dev CORS for the Phase 2 frontend

**What changed:** Added `common/config/WebConfig.java` — permissive CORS (`/**`, any origin, GET/POST/PUT/OPTIONS, exposes `X-Trace-Id`) so the React dev server (a different origin) can call the API from the browser during the live build.

**Why:** Phase 2 prep — without CORS the browser blocks cross-origin calls from the Vite dev server to `:8412`.

**Verification:** build green; live preflight + GET return the expected `Access-Control-Allow-Origin` and `Access-Control-Expose-Headers: X-Trace-Id`.

**Files touched:** `backend/src/main/java/com/ragenx/pv/common/config/WebConfig.java`.

**Reverts cleanly?:** yes.

---

## 2026-06-01 — remove proprietary brief/email from the public repo

**What changed:** Removed `docs/BUILD_EXERCISE_BRIEF.md` and `docs/Email.md` from the repo (the company's hiring materials shouldn't be published in a public repo). Updated the dangling references in `CLAUDE.md` and `docs/conventions.md` to note the brief is held privately. The Claude Code session log is shared with the reviewers by email (not committed) for the same reason.

**Why:** Privacy — the repo is public; the brief/email/transcript are the company's proprietary content. The brief itself permits sharing the session log by email instead of committing.

**Files touched:** deleted `docs/BUILD_EXERCISE_BRIEF.md`, `docs/Email.md`; edited `CLAUDE.md`, `docs/conventions.md`.

**Reverts cleanly?:** yes (the files remain in earlier git history).

---

## 2026-06-01 — Phase 1B operability (Docker, compose, ops scripts, runbook)

**What changed:** Added the operability layer. `backend/Dockerfile` — multi-stage (Temurin 17 JDK build → JRE runtime), non-root user, pinned base tags, layer-cached deps, HEALTHCHECK; `backend/.dockerignore`. `docker-compose.yml` — single service on 8412 with healthcheck + `init: true`. `ops/run.sh` (build|start|stop|test|logs|clean, `--help`, exit codes, Docker+Compose-v2 guards), `ops/backup.sh` (GET /cases → atomic timestamped snapshot, jq/curl guards, cron-safe), `ops/restore.sh` (PUT each case back verbatim, `--dry-run`, idempotent). `Makefile` (.PHONY delegation). Root `README.md` with the "Operations" runbook. Root `.gitattributes` forcing `eol=lf` on shell scripts. `build.gradle`: disabled the plain `jar` so only the bootable jar is produced.

**Why:** Phase 1B — make the service operable ("hand to an on-call engineer at 2am").

**Review:** `/super-review` flagged one CRITICAL (the `COPY *.jar` glob could match a `-plain.jar`) — fixed by disabling the plain jar. Applied the medium/low fixes too: atomic backup write (temp+mv), `.data` array validation, Compose-v2 guard in run.sh, `init: true`, `.gitattributes` LF enforcement, and a runbook note on idempotent restore re-runs.

**Verification:** `docker compose build` SUCCESSFUL (417 MB image); container healthy in ~3s; `/health` + `/cases` respond; full `backup → overwrite → restore` round-trip reinstates the exact checkpoint; scripts fail gracefully without jq. Backend `gradlew clean build` still green (39 tests, single jar).

**Files touched:** `backend/Dockerfile`, `backend/.dockerignore`, `docker-compose.yml`, `ops/*.sh`, `Makefile`, `README.md`, `.gitattributes`, `backend/build.gradle`, `context.md`.

**Reverts cleanly?:** yes.

---

## 2026-06-01 — list + import endpoints (backup/restore support) + port → 8412

**What changed:** Added `GET /cases` (returns all cases — backup snapshot) and `PUT /cases/{caseId}` (imports/replaces a case with the exact `CaseState` verbatim — idempotent, version-preserving — for restore). Made `CaseState`/`MergedField` deserializable (`@Jacksonized`) and added a `@JsonCreator` to `FieldStatus` so a full case round-trips through JSON. `CaseRepository` gained `findAll()` (ordered) + `put()`; `CaseService` gained `getAll()` + `replace()` (path/body id-match guard → `validation.bad_format`). Changed the service port from 8080 → **8412** (8080 is a global Docker container on the dev machine).

**Why:** To make backup/restore a faithful **exact-checkpoint** round-trip (not a lossy follow-up replay): backup snapshots the in-memory store as-is; restore reinstates each case verbatim including version, diff statuses, and previous_values. Decided with the user after analysing that derived fields can't be restored via the follow-up path.

**Verification:** `gradlew clean build` SUCCESSFUL, 37 tests (4 new: list, PUT-verbatim, PUT-idempotent, PUT-mismatch). Live on :8412 — PUT a v7 checkpoint with an overridden field → GET returns it exactly (version, status, previous_value preserved).

**Files touched:** `modules/cases/{controllers,services,repositories,models,constants}/**`, `application.yml`, `docs/architecture.md §6`, `backend/README.md`, `context.md`.

**Reverts cleanly?:** yes.

---

## 2026-06-01 — backend README + run instructions

**What changed:** Added `backend/README.md`: overview, stack, prerequisites, run/build/test instructions (incl. the port-override for the Docker-on-8080 case), response-envelope explanation, a `curl` example per endpoint (GET case, POST follow-up, POST query, GET queries, plus `/health`), an error-code table, notes/limitations, and an AI-assistance note. Added `backend/examples/followup-sample.json` (a realistic follow-up exercising overridden/new/unchanged + `missing_fields`) so the POST example is copy-pasteable via `--data @file` (keeps `§` intact).

**Why:** Phase 1A deliverable — "README with 4 curl examples, one per endpoint" + runnable instructions.

**Verification:** Ran every documented curl against a live instance. Caught and fixed a bug: the query example used camelCase (`caseId`/`fieldPath`) but the wire is snake_case — corrected to `case_id`/`field_path` with a clarifying note.

**Files touched:** `backend/README.md`, `backend/examples/followup-sample.json`, `context.md`.

**Reverts cleanly?:** yes — docs/example only.

---

## 2026-06-01 — queries module (raise + list)

**What changed:** Built the `queries` vertical slice. `POST /queries` raises a reviewer query `{caseId, fieldPath, question}` against a field (returns the stored `Query` with a UUID id); `GET /queries?caseId=` lists a case's queries in creation order. `QueryRepository` is `ConcurrentHashMap<caseId, List<Query>>` with an atomic `compute` append and immutable (`List.copyOf`) reads. `QueryService` checks case existence via `CaseService.findCase` (new non-throwing accessor) — cross-module access through the service, never the repo. Added a `MissingServletRequestParameterException` → 400 handler (missing `?caseId`). Removed the unused `VALIDATION_INVALID_FIELD_PATH` code.

**Why:** Phase 1A endpoints 3–4. Kept deliberately lean per user direction: a query is a persisted note; no `created_at`, no provenance/trace field on the query, and **no field-path structural validation** (a reviewer may query a `missing_fields` entry, and the UI sends a path it just rendered) — `fieldPath` is `@NotBlank` only.

**Review:** `/super-review` found no critical/high *code* defects. Flagged doc-drift (the docs still claimed field-path validation) — fixed in `architecture.md §5.3/§6`, `conventions.md §4.3`, `context.md`, and the skill. Added a `QueryRepositoryTest` locking the immutable-snapshot + insertion-order invariant.

**Verification:** `gradlew clean build` SUCCESSFUL, 31 tests. Live curl: POST valid → 200 (UUID id); GET lists it; unknown case → 404 `query.case_not_found`; blank question → 400; missing `caseId` → 400.

**Files touched:** `backend/.../modules/queries/**`, `CaseService.java`, `GlobalExceptionHandler.java`, `ErrorCode.java`, `docs/architecture.md`, `docs/conventions.md`, `context.md`, `.claude/skills/super-review/SKILL.md`.

**Reverts cleanly?:** yes.

---

## 2026-05-31 — cases module (merge + seed + GET)

**What changed:** Built the `cases` vertical slice. `GET /cases/{caseId}` returns the current merged case; `POST /cases/{caseId}/follow-ups` UPSERT-merges with per-field diff (`new`/`overridden`+`previous_value`/`unchanged`/untouched-null). On startup `CaseSeeder` loads `case_v1.json` through the SAME `FollowUpRequest` model and the SAME pure `MergeService.merge` (with `current=null` → baseline v1), so every boot exercises the full pipeline. `CaseRepository` is `ConcurrentHashMap`-backed with an atomic `compute` (seed/follow-up read-modify-write and the 404 guard both inside the atomic section). Added `common/util/JsonLoader` (fail-loud) and enabled `spring.jackson.deserialization.fail-on-unknown-properties` (strict, fail-closed). `MergeService` is a pure, stateless function; `CaseService` orchestrates; controllers thin.

**Why:** Phase 1A core requirement — follow-up merge with diff annotations, in-memory, seeded from `case_v1.json`. Pipeline-reuse design (user's) so boot is self-testing.

**Review:** Ran `/super-review` — no critical findings. Applied fixes: 404 precedence over leaf validation (moved inside the atomic guard), validation `reason` codes extracted to `Constants.ValidationReason`, `CaseSeeder` guard for a missing `case_id`, and +4 tests (brand-new section, blank value, null section, invalid-leaf-on-unknown-case). Accepted known gap: no concurrency test (#9) — atomicity is structural via `compute`.

**Verification:** `gradlew clean build` SUCCESSFUL, 24 tests pass. Live curl (on :8081): GET v1 → POST follow-up (overridden+previous_value, new, unchanged, untouched) → GET v2 persisted; 404 on unknown case; 400 on invalid leaf and on unknown top-level field.

**Files touched:** `backend/.../modules/cases/**`, `backend/.../common/util/JsonLoader.java`, `application.yml`, `docs/architecture.md`, `context.md`.

**Reverts cleanly?:** yes.

---

## 2026-05-31 — super-review pre-commit skill

**What changed:** Added `.claude/skills/super-review/SKILL.md` — a project-level skill that spawns a read-only subagent to review uncommitted changes against `docs/conventions.md` before every commit. Adapted from a prior project's `/super-review`: dropped finance/webhook/session vectors, added vectors for this stack — Spring singleton shared-state concurrency, our response-envelope contract, no-silent-fallbacks, magic-strings/enums, vertical-slice boundaries, merge correctness, meaningful-tests-only, and frontend dumb-components/hooks.

**Why:** User wants an enforced review gate before commits, with explicit focus on unnecessary fallbacks and class/instance-field state that breaks concurrent requests.

**Files touched:** `.claude/skills/super-review/SKILL.md`.

**Reverts cleanly?:** yes.

---

## 2026-05-31 — common/ layer + health endpoint

**What changed:** Built the cross-cutting `common/` layer: response envelope (`ApiResponse<T>`, `ApiError`, `ResponseFactory`), error handling (`ErrorCode` enum, `ApiException`, `GlobalExceptionHandler` with `@RestControllerAdvice`), trace propagation (`TraceIdFilter` + `TraceContext` MDC wrapper, `TraceIdGenerator` adapted from `docs/reference/` into `common/util`), and `Constants`. Replaced `application.properties` with `application.yml` (port 8080, Jackson `SNAKE_CASE` + `non_null`, `throw-exception-if-no-handler-found`, trace-id log pattern). Added the `health` module (`GET /health`) as the first consumer. Tests: `TraceIdGeneratorTest` (id format) and `HealthControllerTest` (success envelope + trace generation + inbound-trace honoring + 404 error envelope).

**Why:** Every feature module depends on this contract (envelope, errors, trace) per `docs/conventions.md §4–§7` and `docs/architecture.md §3.1`. Health wired now so the contract is verifiable end-to-end immediately.

**Verification:** `gradlew clean build` SUCCESSFUL (all tests pass); live `curl` on :8081 confirmed 200 success envelope with generated `X-Trace-Id`, inbound trace honored, and 404 error envelope.

**Files touched:** `backend/src/main/java/com/ragenx/pv/common/**`, `.../modules/health/controllers/HealthController.java`, `backend/src/main/resources/application.yml`, tests, `docs/architecture.md §4.3`.

**Reverts cleanly?:** yes.

---

## 2026-05-31 — Scaffold, repo reorg, git connect, Boot downgrade

**What changed:** Extracted the Spring Initializr zip into `/backend`. Reorganized the repo to its target shape: spec/reference docs into `docs/` (with `docs/reference/` for the prior-project Java samples), `case_v1.json` into `backend/src/main/resources/`, operational files (`CLAUDE.md`, `CHANGELOG.md`, `context.md`) kept at root per user decision. Added root `.gitignore`. Initialized git on `main` and connected remote `origin` (cyb3rb34s7/Ragenx-Assignment). Downgraded the scaffold from Spring Boot 4.0.6 / Gradle 9.5.1 to **Spring Boot 3.4.2 / Gradle 8.12**, replacing the SB4 modular starters (`spring-boot-starter-webmvc`, `*-webmvc-test`, `*-validation-test`) with the standard `spring-boot-starter-web` + `spring-boot-starter-test`.

**Why:** User chose the battle-tested 3.4.x line over bleeding-edge 4.0 for a time-boxed build (fewer surprises, standard starter names, more references). Repo reorg per user instruction to make the folder represent the repo root.

**Files touched:** `backend/build.gradle`, `backend/gradle/wrapper/gradle-wrapper.properties`, `.gitignore`, repo layout.

**Reverts cleanly?:** yes.

---

## 2026-05-31 — Data-model reasoning + Mermaid fix

**What changed:** Fixed a Mermaid parse error in the request-lifecycle sequence diagram (`?:` and `;` in a message label broke the lexer — rewrote as plain prose). Expanded `architecture.md §5` with the data-model reasoning: `ExtractedField`/`MergedField`/`CaseState` roles, sections as generic maps (for `new`-field support), one shared shape for `GET` and `POST /follow-ups`, no version history (current `CaseState` + version counter), request-vs-response DTO strategy, Lombok builders, global snake_case Jackson.

**Why:** User asked to explain the Mermaid error and the data-modeling / DTO approach. Captured the decisions in the doc rather than only in chat.

**Files touched:** `docs/architecture.md`, `context.md`.

**Reverts cleanly?:** yes — docs only.

---

## 2026-05-31 — Merge model revised to UPSERT + null status

**What changed:** Reworked the merge design in `architecture.md §4` and the `FieldStatus` enum. Merge is now an explicit UPSERT (update existing / insert new). Statuses: `new`, `overridden` (+ `previous_value`), `unchanged`; the "field in stored but absent from follow-up" case is now `status: null` (untouched), replacing the dropped `retained` status. Removed `diff_summary` from the merged response (unprompted addition, not needed — frontend derives counts). Clarified that `missing_fields` is a brief requirement (surfaced from the follow-up payload), not an addition.

**Why:** User direction on merge semantics: UPSERT, no `retained`, null = untouched. `diff_summary` challenged and dropped per KISS. `unchanged` kept because the brief mandates it and it carries a distinct provenance signal vs `null`.

**Files touched:** `docs/architecture.md`, `docs/conventions.md`, `context.md`.

**Reverts cleanly?:** yes — docs only.

---

## 2026-05-31 — Doc refinements from user review

**What changed:** Trace IDs switched from UUID to the prior project's 10-char `TraceIdGenerator` (timestamp+random, ambiguity-free charset). Logging pinned to SLF4J + Lombok `@Slf4j` + Logback + MDC `traceId` (human-readable, not the full `StructuredLogger`). Added Lombok + builder pattern as a backend convention. Consolidated module constants into one `Constants.java` (capital C) per module. Clarified and renamed the startup loader `CaseSeeder.java` (dropped the "bootstrap" folder). Recorded the rationale for keeping `cases`/`queries` as separate modules.

**Why:** User review/directives on module division, naming, Lombok, and the trace-ID/logging approach (prior-project `TraceIdGenerator.java` and `StructuredLogger.java` provided as references).

**Files touched:** `CLAUDE.md`, `docs/conventions.md`, `docs/architecture.md`, `context.md`.

**Reverts cleanly?:** yes — docs only.

---

## 2026-05-31 — Documentation scaffolding

**What changed:** Created the project's living documentation: `CLAUDE.md` (session guidance), `docs/conventions.md` (rulebook adapted from a prior Python/FastAPI project to Spring Boot + React/Vite/Tailwind/zustand/ky), `docs/architecture.md` (system design with Mermaid diagrams: vertical-slice module maps, trace-ID request lifecycle, merge-logic flow, data model, ops layout), `context.md` (running state + decisions + open questions), and this `CHANGELOG.md`.

**Why:** User direction — document the architecture and conventions before any code, maintain a persistent changelog (for tracing/reverting) and a running context file. The exercise also grades process and communication, so the docs are first-class deliverables.

**Files touched:** `CLAUDE.md`, `docs/conventions.md`, `docs/architecture.md`, `context.md`, `CHANGELOG.md`.

**Reverts cleanly?:** yes — docs only, no code.
