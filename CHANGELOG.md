# Changelog

Append-only, newest at top. Per-entry format in `docs/conventions.md §2.3`.

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
