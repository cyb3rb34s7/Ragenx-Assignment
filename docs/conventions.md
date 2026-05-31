# Conventions

> The rulebook. Every commit in this repo follows these. If a convention has no `Why`, it has no force — every rule here is justified, because every rule here will be questioned during the build.
>
> Adapted from a prior project's conventions (Python/FastAPI) for **this** assignment: Java + Spring Boot (Gradle) backend, React (Vite) + Tailwind + zustand + ky frontend, local ops tooling. Domain-specific sections (money/INR, connectors, agents, DB) were dropped; principles, the API contract, trace IDs, the constants rule, the no-fallback rule, and the testing philosophy carry over.

If something here is wrong or stale, fix the convention first, then the code. This file is the source of truth, not whatever happens to be in the repo right now.

---

## 0. Reading order before any work

Every new session, every new module, every "let's pick this up again" — read these first, in order:

1. `context.md` — running log of what's done, in progress, problems faced, solutions applied. **Never repeat a mistake that's already in here.**
2. `CHANGELOG.md` — what changed and why, dated. Use when reverting or tracing regressions.
3. `docs/conventions.md` — this file.
4. `docs/architecture.md` — system design and module map.

Then the relevant module. (The exercise brief is held privately by the reviewers, not committed to this public repo.)

**Why:** Re-reading costs seconds. Redoing a solved mistake costs hours and is the most demoralising waste.

---

## 1. Core principles (in priority order)

1. **KISS.** The simplest thing that satisfies the requirement, written so the next reader holds it in their head. The brief says "don't over-engineer" twice — obey it.
2. **DRY, but only after the third occurrence.** First time: write it. Second: notice. Third: extract. Don't pre-abstract.
3. **No silent fallbacks.** If a path can fail, it fails loudly. A fallback is never inserted silently — see §10.3.
4. **Fail-closed.** When in doubt, reject the operation. Never ship an unverified value, never swallow a failure, never hide an exception.
5. **Trace everything.** Every request gets a `trace_id` that follows it through logs and into the response envelope.
6. **Provenance on every field.** Every extracted field carries its `confidence` and `source`. No value reaches the UI without them.
7. **Honesty over polish.** Known gaps are documented in `context.md` and the README. We list what breaks before anyone asks.

---

## 2. Process discipline

### 2.1 Module workflow (every feature)

| Step | Who | Output |
|---|---|---|
| 1. Define the module | User + me | Acceptance criteria for the slice |
| 2. Write the plan | Me | Plan in chat |
| 3. Review & iterate | User | Approved plan |
| 4. Implement | Me (or coder subagent given this file + plan) | Code + tests, committed |
| 5. Critical review | Reviewer pass against this file | Issues ranked by severity |
| 6. Apply fixes | Me | Updated code |
| 7. Update docs | Me | `context.md` + `CHANGELOG.md`, **same commit** |
| 8. Manual verification | User | Pass / send back |

Steps 1–3 iterate before code. Step 7 is non-optional: if the docs don't reflect the change, the change isn't done.

### 2.2 `context.md`

Single flat file at repo root. Sections it must always contain:

- **Now** — what is in progress.
- **Done** — complete items, one line each, dated.
- **Next** — the ranked queue.
- **Problems & solutions** — dated: problem, root cause, solution, follow-up. **Most important section. Read it every session. Never repeat one.**
- **Decisions** — non-obvious choices with reasoning (why X over Y). Judgmental, vs the changelog's mechanical "what changed."

### 2.3 `CHANGELOG.md`

Single flat file at repo root. Append-only, newest at top. Per entry:

```
## YYYY-MM-DD — short title

**What changed:** one-line summary of the diff.
**Why:** the actual reason — a requirement, a context.md problem, or a user request.
**Files touched:** the load-bearing ones.
**Reverts cleanly?:** yes / no / partially (with reason).
```

**Why:** When something breaks, the first question is "what changed recently?" The changelog answers in seconds; `git log` in minutes.

### 2.4 Conventional Commits

```
<type>(<scope>): <subject>

<body — optional>
```

Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `perf`, `ci`. Scope = module (`cases`, `queries`, `ops`, `frontend/case-review`, etc.).

Examples:
- `feat(cases): merge follow-up with diff annotations`
- `fix(cases): retained fields were dropped from merged response`
- `docs(conventions): adapt rulebook for Spring Boot`

**Why:** Reviewers will read commit history. Conventional commits make speed and care visible. Commit every 10–15 min.

### 2.5 Local checks before commit

- **Java:** `./gradlew test` green for the touched module; `./gradlew build` compiles.
- **TypeScript:** `eslint`, `prettier`, `tsc --noEmit` green.
- **Shell:** `shellcheck` clean on `ops/*.sh`.

If a check fails, fix the cause. Don't bypass with `--no-verify`.

---

## 3. Architecture: vertical slice

Each module owns everything it needs — models, business logic, endpoints (backend) or components/hooks/store (frontend) — in one folder. Cross-module sharing goes through an explicit `common/` (backend) or `shared/` (frontend), never reach-in.

### 3.1 Backend structure

```
backend/src/main/java/com/ragenx/pv/
  PvApplication.java                 # app entry
  modules/
    cases/
      controllers/   CaseController.java
      services/      CaseService.java, MergeService.java
      repositories/  CaseRepository.java        # in-memory store
      models/        Case, ExtractedField, FollowUpRequest, MergedField, MergedCaseResponse
      constants/     Constants.java             # holds FieldStatus enum + module constants
      CaseSeeder.java                           # CommandLineRunner: loads case_v1.json on startup
    queries/
      controllers/   QueryController.java
      services/      QueryService.java
      repositories/  QueryRepository.java        # in-memory store
      models/        Query, CreateQueryRequest
    health/
      controllers/   HealthController.java
  common/
    config/          Jackson, web config
    response/        ApiResponse<T>, ResponseFactory
    error/           ApiException, ErrorCode (enum), GlobalExceptionHandler
    trace/           TraceIdFilter (sets traceId into MDC + response header)
    util/            TraceIdGenerator, JsonLoader, log helper
    constants/       Constants.java             # cross-module constants
backend/src/main/resources/
  application.yml
  case_v1.json                       # bootstrap data on the classpath
backend/src/test/java/com/ragenx/pv/modules/...
```

**Rules:**
- A module's controller calls only its own service. A service may call `common/` utilities and (rarely) another module's service — never another module's controller or repository.
- Folder names follow the user's house style: `controllers/`, `services/`, `models/`, `constants/`, plus `repositories/` for the in-memory store and `utils/`/`bootstrap/` where a module needs them.

### 3.2 Frontend structure

```
frontend/src/
  modules/
    case-review/
      api/           ky calls for this module
      components/     dumb components: receive props, render
      hooks/          all logic lives here
      store/          zustand slice for this module's UI state
      types/          module-specific types
      utils/          module-specific pure functions
      constants.ts    module enums (field status, confidence band, view modes)
      index.ts        public surface of the module
  shared/
    components/       Button, Modal, Loader, Badge, EmptyState, ErrorState, ...
    api/
      client.ts       ky instance + envelope unwrap + global error handling
      errors.ts       ApiError mirroring backend error codes
    store/            app-level zustand (e.g. theme) if needed
    theme/            tailwind tokens, brand colors
    types/            shared types mirroring backend contracts
    constants/        cross-module enums (mirror backend)
    utils/            cross-cutting pure helpers
  app/                thin composition — arranges module surfaces
  styles/             globals.css
```

**Rules:**
- `app/` holds no business logic. It imports a module's `index.ts` and arranges it.
- Other modules import only from a module's `index.ts`, never internals.

### 3.3 The "share on the third time" rule

A util or component moves to `shared/`/`common/` only after a third independent use. Before that, copy. **Why:** premature abstraction hurts more than small-scale duplication; the right shape is visible at the third occurrence.

---

## 4. API contract

One envelope shape for success and error, so the frontend branches once, globally.

### 4.1 Success

```json
{
  "success": true,
  "data": { "...": "whatever the endpoint returns" },
  "trace_id": "tr_9f2c..."
}
```

### 4.2 Error

```json
{
  "success": false,
  "error": {
    "code": "validation.missing_field",
    "message": "Human-readable, safe to show a user.",
    "details": { "...": "optional structured diagnostics" }
  },
  "trace_id": "tr_9f2c..."
}
```

**Rules:**
- Every response carries `trace_id` — including errors, including 500s.
- `success` is the boolean discriminator the frontend branches on.
- Errors **always** have a `code`. UI logic branches on `code`, never on `message`. Messages are display copy.
- `details` is optional structured data (e.g. failing fields). Never secrets.
- HTTP status: 2xx success, 4xx caller fault, 5xx our fault. Envelope shape is identical regardless.

### 4.3 Error code namespacing

`<domain>.<reason>`, defined once as a backend enum (`common/error/ErrorCode.java`) and mirrored as a frontend `as const` union:

- `validation.missing_field`, `validation.bad_format`
- `case.not_found`, `case.invalid_follow_up`
- `query.case_not_found`
- `system.unexpected`

Add codes here as modules need them. Don't invent codes inline.

---

## 5. Trace IDs

### 5.1 Generation
- Generated at the edge by a servlet `Filter` (`common/trace/TraceIdFilter`) for every inbound request. If the caller sent `X-Trace-Id`, honor it; else generate.
- **Format: a 10-char id** — 5 chars encoding the epoch-second timestamp + 5 random chars, from an ambiguity-free charset (`A–Z a–z 2–9`, excluding `I O 0 1 l`). Generated by `common/util/TraceIdGenerator` (carried over from a prior project). Timestamp-prefixing makes ids roughly time-ordered and keeps collisions effectively nil at this scale. **Flagged:** collisions are astronomically unlikely, not provably impossible (5 random chars within one second) — fine for this scope.
- Stored in SLF4J **MDC** under key `traceId` so any code in the request thread can log it without threading it through arguments. The filter clears MDC in a `finally` block.

### 5.2 Propagation
- Echoed back to the caller as the `X-Trace-Id` response header **and** inside every response envelope's `trace_id`.
- The frontend reads `trace_id` from the envelope and tags any retry/follow-up call with the same id via `X-Trace-Id`.

### 5.3 Logging
- **SLF4J via Lombok `@Slf4j`** on each class. Implementation is **Logback** (Spring Boot's default binding) — not Log4j2. MDC works the same regardless.
- Every log line includes the trace id from MDC via the Logback console pattern (`... [%X{traceId}] ...`). **Logs stay human-readable** — readable messages with the trace id bracketed, not raw JSON.
- We do **not** adopt the prior project's full `StructuredLogger` verbatim (its fields — operation, sourceService, region, retryAttempt — are specific to that system). A trimmed helper (`setTraceId` / `clearContext` / `getTraceId`, plus a couple of event helpers) is enough; most classes just use `log.info(...)` directly.
- Event phrasing uses dot notation where it helps: `<area>.<action>.<state>` (e.g. `cases.followup.merged`).
- **No `System.out.println` / `console.log`.** Use the logger.

**Scope note:** Full JSON structured logging / log shipping is intentionally **out of scope** — the brief lists "no production logging setup" as a non-goal. We trace-tag every line; we do not build a log pipeline.

---

## 6. Constants and enums

**No magic strings in critical comparisons. Ever.**

### 6.1 Backend (Java)

One consolidated `Constants.java` per module (capital C — Java requires the public class name to match the filename; a lowercase `constants` won't compile cleanly), holding that module's enums and string constants as nested types:

```java
// modules/cases/constants/Constants.java
public final class Constants {
    private Constants() {}

    // A field absent from the follow-up gets a null status (untouched) — see architecture.md §4.2.
    public enum FieldStatus {
        NEW("new"),                 // inserted by the follow-up
        OVERRIDDEN("overridden"),   // updated; carries previous_value
        UNCHANGED("unchanged");     // re-sent with the same value

        private final String wire;
        FieldStatus(String wire) { this.wire = wire; }
        @JsonValue public String wire() { return wire; }
    }
}
```

Branch on the enum (`if (status == FieldStatus.OVERRIDDEN)`), never on `"overridden"`.

### 6.2 Frontend (TS)

```ts
// modules/case-review/constants.ts
export const FieldStatus = {
  UNCHANGED: "unchanged",
  OVERRIDDEN: "overridden",
  NEW: "new",
  RETAINED: "retained",
} as const;
export type FieldStatus = (typeof FieldStatus)[keyof typeof FieldStatus];
```

Same wire values as backend. Mirrored manually (no codegen at this scale).

### 6.3 What counts as "critical"

Anything used in a comparison, switch, or branch. Display-only labels can be literals. Confidence bands (`low`/`medium`/`high`), view modes, sort keys, status values, error codes — all critical, all enums.

---

## 7. Errors and fallbacks

### 7.1 Don't broad-catch

Forbidden: catching `Exception` and swallowing it. A blanket catch that doesn't re-raise (or re-wrap into a typed `ApiException`) is a silent fallback.

Allowed: catch a specific exception, wrap it into a typed `ApiException` with a `code` and HTTP status, and let the global handler render the envelope.

### 7.2 Domain errors

`common/error/ApiException.java` carries an `ErrorCode` and an HTTP status. A single `@RestControllerAdvice` (`GlobalExceptionHandler`) converts:
- `ApiException` → envelope with its `code` + status.
- Bean-validation failures → `validation.*` with failing fields in `details`.
- Anything else → `system.unexpected`, **stacktrace logged, not returned**.

### 7.3 Fallback policy — ALWAYS HIGHLIGHT

**No silent fallbacks.** If a code path could fall back to a default, **halt and ask the user.** Never silently insert one.

- A reviewer pass flags any new `catch` that doesn't re-raise/re-wrap, and any defaulting (`?? default`, `getOrDefault`, `value || fallback`) in critical logic.
- When implementing, if a fallback seems needed, I stop and ask before writing it.

If a fallback is agreed, it goes in three places:
1. Code: with a structured log of the fallback hit, including `trace_id`.
2. `CHANGELOG.md`: as a deliberate decision with reasoning.
3. README: in the "where it breaks" / known-issues section.

---

## 8. Backend specifics

- Controllers in `<module>/controllers/`. One controller per module.
- **Lombok + builder pattern** to kill boilerplate: `@Getter`/`@Builder`/`@Value` (or `@Data` where mutability is fine) on models, `@RequiredArgsConstructor` for constructor injection in services/controllers, `@Slf4j` for loggers. Construct model objects via their builder, not telescoping constructors. Keep Lombok off anything where explicit code reads clearer.
- Request/response bodies are typed model classes (POJOs/records via Lombok), never raw `Map`/`Object`. Validate inbound with Jakarta annotations (`@NotBlank`, `@Valid`).
- Repositories own storage (here: `ConcurrentHashMap`). Services call repositories; controllers call services.
- Seeding (`case_v1.json` load) runs via a `CommandLineRunner` (`cases/CaseSeeder.java`), reading from the classpath through `common/util/JsonLoader`.
- Time on the wire: UTC ISO-8601 with `Z`. Don't reformat timestamps server-side unless required.

## 9. Frontend specifics

### 9.1 Theme first
Tailwind tokens defined before any component. Brand colors: navy `#0C1A36`, brand blue `#0077B6`, accent teal `#00C2E0`. Confidence bands get their own tokens.

### 9.2 State separation
- **Server data:** fetched via the ky client inside hooks; held in module state (zustand slice or hook state). Kept minimal — this app has one case and a query list.
- **UI state:** zustand — modal open/closed, sort/filter mode, classification selector, theme.
- **Flagged deviation** from the prior project's convention (which mandated TanStack Query for server state): at this scale we use ky + hooks + zustand only, per the current instruction. If server-cache complexity grows, revisit.

### 9.3 API client
`shared/api/client.ts`:
- One `ky` instance: base URL, default headers, `X-Trace-Id` on retries.
- Inbound handling: read the envelope. `success: true` → return `data`. `success: false` → throw a typed `ApiError` carrying `code`, `message`, `trace_id`.
- **Components never see the envelope.** Hooks call the client; components consume the hook.

### 9.4 Dumb components
A component: receives props, renders, may hold cosmetic local state (open/closed). It **cannot** call the API and **cannot** read stores directly — it gets data and callbacks from a hook. All fetches, mutations, derived data, and side effects live in hooks under `<module>/hooks/`.

### 9.5 Reusable primitives
Built once in `shared/components/`, reused everywhere: `Button`, `Modal`, `Loader` (spinner + skeleton), `Badge`, `EmptyState`, `ErrorState`. A module never builds its own `Modal`.

### 9.6 No magic strings in branches
Same as §6. Status comparisons, confidence bands, view modes, sort/filter keys go through `as const` enums.

---

## 10. Testing

### 10.1 What we test (backend, this assignment)
- **Merge logic** — the meaningful part. Edge cases: `new` (insert), `overridden` (update, with `previous_value`), `unchanged` (same value re-sent), untouched field (in stored, absent from follow-up → `status: null`, value preserved), `missing_fields` surfaced from the payload. The brief requires **≥3 unit tests**; merge edge cases are where they go.
- **Validation** — bad/missing payload → `400`; unknown case → `404`; invalid `fieldPath` on a query → meaningful error.
- **Controllers** — happy-path integration via Spring `MockMvc` for envelope shape + status codes.

### 10.2 When
Alongside the implementation, in the same commit. Never "tests later."

### 10.3 Meaningful tests only
A test is meaningful when **its failure indicates a real bug**. Before writing one, state in a sentence: *"this fails when X breaks,"* X being a real, harmful condition. If you can't, don't write it.

**Smell test:** if a test would still pass after replacing its assertions with a no-op, the assertions don't matter — delete it.

Don't write tests that re-assert the type system, that store-and-read-back with no invariant, that mock the unit under test, or that re-implement the logic in the assertion. A small suite that catches real things beats a bloated one that gives false confidence. (Hard-won lesson from prior projects — apply it both when planning and when the tests are actually written.)

---

## 11. AI tool usage disclosure

The brief evaluates *how* we build with Claude Code, and asks for honesty about it.

- `context.md` **Decisions** notes when a non-trivial chunk was AI-generated vs hand-written, where relevant.
- The README has an "AI tools" note summarising it for the reviewer.
- The Claude Code session log (`.jsonl`) is committed at `/claude-code-session.jsonl` after the live session.

We don't apologise for using AI tools. We report it.

---

## 12. When this file is wrong

It will be. Fix it in the same commit as the code that proved it wrong, and note it in `CHANGELOG.md` with type `docs(conventions)`. Don't quietly diverge.
