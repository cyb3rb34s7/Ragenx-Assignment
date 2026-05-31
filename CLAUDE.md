# CLAUDE.md

Guidance for Claude Code working in this repo. Read this first, every session.

## What this is

A hiring build-exercise: a **pharmacovigilance (PV) case-processing** demo. AI extracts structured fields from a document; a human reviewer validates them; follow-up updates arrive and the system shows **what changed** (v1 vs v2 conflict resolution).

Domain knowledge is **not** required. Mental model: *structured data extracted by AI, validated by a human, with versioned diff/merge.*

Three deliverables in one repo:

- `/backend` — Java 17+ / Spring Boot (Gradle). Case merge + queries API. In-memory, no DB, no auth.
- `/ops` — Dockerfile, compose, shell scripts, Makefile, runbook. "Hand to an on-call engineer at 2am."
- `/frontend` — React (Vite) + Tailwind + zustand + ky. The reviewer's screen. Built live.

Bootstrap data: `backend/src/main/resources/case_v1.json`. The full exercise brief is held privately by the reviewers and is intentionally **not committed** to this public repo.

## Reading order before any work

1. `context.md` — running state: Now / Done / Next / Problems & solutions / Decisions. **Never repeat a logged mistake.**
2. `CHANGELOG.md` — what changed and why, dated. For reverting and tracing regressions.
3. `docs/conventions.md` — the rulebook.
4. `docs/architecture.md` — the system design and module map.
5. Then the relevant module.

## Non-negotiable principles (full detail in `docs/conventions.md`)

- **KISS, then DRY (after the 3rd occurrence).** Don't pre-abstract. Don't over-engineer — the brief says so twice.
- **No silent fallbacks. Ever.** If a path can fail, it fails loudly. If a fallback seems necessary, **STOP and ask the user** — never insert one silently. Every accepted fallback is logged with `trace_id` and recorded in `CHANGELOG.md`.
- **No magic strings in critical logic.** Status values, error codes, field statuses, view modes — all live in constants/enums. Branch on the enum, never on a literal.
- **Trace everything.** Every request gets a `trace_id` that appears in every log line for that request and in every response envelope (success *and* error).
- **Consistent response envelope.** Success and error share one shape so the frontend handles both at one global layer. See `docs/conventions.md §4`.
- **Vertical slice architecture.** One module per feature, self-contained. Backend and frontend both.
- **Dumb components, smart hooks.** Frontend components render; all logic lives in hooks.
- **Honesty over polish.** Known gaps go in `context.md` and the README before anyone asks.

## Doc maintenance rule

A change is not done until `context.md` and `CHANGELOG.md` reflect it — updated **in the same commit** as the change.

## Commits

Conventional Commits (`feat`, `fix`, `refactor`, `docs`, `chore`, `test`). Scope = module. Commit every 10–15 min — the reviewers grade progression, not just final state.

## Tech stack quick reference

| Layer | Choice |
|---|---|
| Backend | Java 17+, Spring Boot 3.x, Gradle |
| Boilerplate | Lombok + builder pattern |
| Logging | SLF4J + Logback + MDC `traceId`, human-readable |
| Backend validation | Jakarta Bean Validation |
| Storage | In-memory (`ConcurrentHashMap`), no DB |
| Frontend | Vite + React + TypeScript |
| Styling | Tailwind (theme/tokens first) |
| State | zustand (UI state only) |
| HTTP client | ky (one client, global envelope handling) |
| Ops | Docker (multi-stage, non-root, pinned), bash, Make |

Base Java package: `com.ragenx.pv`.
