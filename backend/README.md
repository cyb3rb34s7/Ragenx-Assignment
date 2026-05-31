# PV Case Processing — Backend

A Spring Boot service for pharmacovigilance case review. It holds the most recent version of a case, merges follow-up extractions onto it with per-field diff annotations, and records reviewer queries against fields. Storage is in-memory — no database, no auth (per the exercise brief).

> Model it as: structured data extracted by AI from a document, validated by a human, with v1↔v2 conflict resolution.

## Stack

| | |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.2 (Spring Web, Validation) |
| Build | Gradle (wrapper included — no local Gradle needed) |
| Boilerplate | Lombok |
| Storage | In-memory (`ConcurrentHashMap`) |
| Logging | SLF4J + Logback, `traceId` on every line |

## Prerequisites

- **JDK 17** on the `PATH` (`java -version` → 17.x). The Gradle wrapper manages Gradle itself.

## Run

```bash
cd backend
./gradlew bootRun          # Windows: .\gradlew.bat bootRun
```

The service starts on **http://localhost:8412**. On startup it loads `src/main/resources/case_v1.json` into memory as version 1 of case **`PV-2026-0451`** (you'll see `cases.seed.loaded caseId=PV-2026-0451 version=1` in the log).

**Port already in use?** Run on another port (everything below then uses that port):

```bash
./gradlew bootRun --args='--server.port=8081'
```

### Build & test

```bash
./gradlew clean build      # compiles, runs all tests, produces build/libs/pv-0.0.1-SNAPSHOT.jar
./gradlew test             # tests only
java -jar build/libs/pv-0.0.1-SNAPSHOT.jar   # run the built jar
```

## Response envelope

Every response — success or error — has the same shape, so a client branches once on `success`:

```jsonc
// success
{ "success": true,  "data": { ... }, "trace_id": "PBLWXFUBRK" }
// error
{ "success": false, "error": { "code": "case.not_found", "message": "...", "details": { ... } }, "trace_id": "PBLWXFUBRK" }
```

- `trace_id` is on **every** response (and the `X-Trace-Id` response header). Send your own `X-Trace-Id` request header to have it propagated; otherwise the server generates one. It tags every log line for the request.
- Clients branch on `error.code`, never on `message`.

## Endpoints

Base URL: `http://localhost:8412`. The four functional endpoints (plus a liveness check) with one `curl` each:

### 1. `GET /cases/{caseId}` — most recent version of a case

```bash
curl -s http://localhost:8412/cases/PV-2026-0451
```
Returns the current `CaseState` (v1 after a fresh start). Unknown case → `404 case.not_found`.

### 2. `POST /cases/{caseId}/follow-ups` — merge a follow-up

Merges a follow-up (same shape as the initial case, plus optional top-level `missing_fields`) onto the stored version and returns the merged case with a per-field `status`: `new` (inserted), `overridden` (+ `previous_value`), `unchanged` (re-sent, same value), or omitted (untouched by this follow-up).

```bash
curl -s -X POST http://localhost:8412/cases/PV-2026-0451/follow-ups \
  -H "Content-Type: application/json" \
  --data @examples/followup-sample.json
```
The bundled `examples/followup-sample.json` overrides `patient.weight_kg` (78→80), adds `adverse_event.recovery_date` (new), re-sends `adverse_event.outcome` (unchanged), leaves all other fields untouched, and surfaces one `missing_fields` entry — returning **version 2**.

> Tip: use `--data @file.json` (not `-d '…'`) so non-ASCII characters in `source` values (e.g. `§`) are sent intact. Unknown case → `404`; an unknown top-level field or a leaf missing `value`/`confidence`/`source` → `400`.

### 3. `POST /queries` — raise a reviewer query against a field

```bash
curl -s -X POST http://localhost:8412/queries \
  -H "Content-Type: application/json" \
  -d '{"case_id":"PV-2026-0451","field_path":"adverse_event.onset_date","question":"Please confirm the onset date against the source."}'
```
Returns the stored query with a generated `id`. Unknown case → `404 query.case_not_found`; blank field → `400 validation.missing_field`.

> All request/response fields are **snake_case** on the wire (`case_id`, `field_path`, `trace_id`, …) — the brief's `{caseId, fieldPath, question}` maps to `{case_id, field_path, question}`.

### 4. `GET /queries?caseId={id}` — list a case's queries

```bash
curl -s "http://localhost:8412/queries?caseId=PV-2026-0451"
```
Returns queries in creation order (`[]` if none). Unknown case → `404`; missing `caseId` → `400`.

### Liveness — `GET /health`

```bash
curl -s http://localhost:8412/health     # {"success":true,"data":{"status":"UP"},"trace_id":"..."}
```

### Backup / restore support

Two endpoints back the ops scripts (`ops/backup.sh`, `ops/restore.sh`):

```bash
curl -s http://localhost:8412/cases                 # list ALL cases (full CaseState[]) — used by backup
curl -s -X PUT http://localhost:8412/cases/PV-2026-0451 \
  -H "Content-Type: application/json" --data @cases-snapshot-item.json   # replace verbatim — used by restore
```
`PUT /cases/{caseId}` imports a `CaseState` **exactly as given** (version, statuses, `previous_value` and all) — idempotent and version-preserving, so restore reinstates the precise checkpoint. The body's `case_id` must match the path (else `400 validation.bad_format`).

## Error codes

| Code | HTTP | When |
|---|---|---|
| `validation.missing_field` | 400 | A required body field is blank, or a required query param is missing |
| `validation.bad_format` | 400 | Malformed JSON, or an unknown top-level field (strict parsing) |
| `case.not_found` | 404 | `GET`/follow-up on an unknown case |
| `case.invalid_follow_up` | 400 | A follow-up leaf is malformed (bad/missing `value`/`confidence`/`source`) |
| `query.case_not_found` | 404 | Query create/list for an unknown case |
| `resource.not_found` | 404 | Unknown route |
| `system.unexpected` | 500 | Unhandled error (stacktrace logged, never returned) |

## Notes & limitations

- **In-memory:** all state resets on restart; every startup re-seeds `PV-2026-0451` at v1. No database by design.
- **No version history:** only the current merged version is kept; `previous_value` is the value immediately before the latest follow-up.
- **`field_path` on a query is not structurally validated** (only non-blank): a reviewer may legitimately query a field the AI couldn't extract (one listed in `missing_fields`), and the UI sends a path it just rendered.
- **`PUT /cases/{id}` is a trusted import.** It stores the provided `CaseState` verbatim and validates only structure (case_id match, at least one section) — it does **not** re-validate leaf content, because it's meant to restore a snapshot this service itself produced via `GET /cases`. Strict JSON parsing still rejects malformed/unknown fields. (It also accepts `status`/`previous_value` directly, by design, for exact-checkpoint restore.)
- **No auth / no production logging pipeline** — out of scope per the brief.

## AI assistance

Built with Claude Code throughout, pair-programming style: requirements analysis, design, implementation, and a self-authored `/super-review` subagent gate before each commit. The Claude Code session log is included at the repo root (`/claude-code-session.jsonl`) for evaluation.
