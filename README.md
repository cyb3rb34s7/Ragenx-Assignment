# PV Case Processing

A small pharmacovigilance case-review system built for a take-home exercise: an AI extracts structured fields from a document, a human reviewer validates them, and follow-up extractions are merged with per-field diff annotations (v1↔v2 conflict resolution).

```
backend/    Java 17 + Spring Boot service (in-memory). API details: backend/README.md
ops/        Operability: run/backup/restore scripts (this README's "Operations" section)
frontend/   React reviewer screen (Phase 2 — built live)
docs/        conventions.md, architecture.md (with diagrams), the brief
```

The service runs on **port 8412** and seeds case `PV-2026-0451` from `backend/src/main/resources/case_v1.json` on startup.

## Quickstart

```bash
make start        # build (first run) + start the container; service on http://localhost:8412
curl -fsS http://localhost:8412/health
make stop
```

Run without Docker (local JVM): `cd backend && ./gradlew bootRun`. Full API reference and per-endpoint `curl` examples are in **`backend/README.md`**.

---

## Operations

> The runbook. Written for whoever is on call at 2am.

### Prerequisites
- **Docker** (Desktop or Engine) with Compose v2 — for build/run.
- **bash**, **GNU make** — to run `ops/*.sh` and the `Makefile` (Linux/macOS, or Git-Bash/WSL on Windows).
- **curl** and **jq** — required by `backup.sh`/`restore.sh`. The scripts check for both and exit with a clear message if missing.

### Build & deploy
```bash
make build        # docker compose build  (multi-stage: JDK build -> JRE runtime, non-root, pinned bases)
make start        # docker compose up -d  -> http://localhost:8412
make logs         # follow logs (Ctrl-C to stop following)
make stop         # docker compose down
make clean        # down + remove volumes and the locally-built image
```
All of these delegate to `ops/run.sh`, which fails gracefully if the Docker daemon isn't running. Direct form: `ops/run.sh <build|start|stop|test|logs|clean|--help>`.

### Verify the service is healthy
```bash
docker compose ps                         # STATUS should show "healthy" (after ~20s warmup)
curl -fsS http://localhost:8412/health    # {"success":true,"data":{"status":"UP"},"trace_id":"..."}
```
The container has a built-in `HEALTHCHECK` hitting `/health`. Every response (and log line) carries a `trace_id` — grep logs for it to follow one request end-to-end.

### Back up and restore data
The in-memory store resets on every restart (re-seeds to v1), so back up before restarting if follow-ups have been applied.
```bash
make backup                                       # -> backups/cases_<UTC-timestamp>.json
make restore FILE=backups/cases_<ts>.json DRY_RUN=--dry-run   # preview, no changes
make restore FILE=backups/cases_<ts>.json                     # apply
```
- **Backup** = `GET /cases` snapshot of the in-memory store, written verbatim.
- **Restore** = `PUT /cases/{id}` for each case — reinstates the **exact checkpoint** (version, statuses, previous_values). Idempotent: re-running yields the same state.
- `backup.sh` is cron-safe (no prompts, logs to stderr with timestamps, writes atomically via a temp file, non-zero exit on failure). Override the target with `PV_BASE_URL` / `BACKUP_DIR` env vars.
- If a restore fails partway (one case errors), it stops loud and non-zero; because each PUT is idempotent, **just re-run the same command** once the cause is fixed — already-restored cases are unaffected.

### Debug a failed startup
1. `make logs` (or `docker compose logs pv-backend`). Look at the last ~30 lines.
2. **Port 8412 already in use** → another process holds it: `docker compose down`, free the port, retry. (We moved off 8080 precisely because it's commonly occupied.)
3. **`Docker daemon is not running`** from `run.sh` → start Docker Desktop/Engine.
4. **App aborts at startup with "Failed to load required resource: case_v1.json"** → the seed file is missing/corrupt on the classpath; restore it under `backend/src/main/resources/`.
5. **Build fails** → run `make build` and read the Gradle output; if base-image pull fails, check network / the pinned `eclipse-temurin` tags in `backend/Dockerfile`.

### Requests are failing — what to check first
1. **Is it up and healthy?** `docker compose ps` → `healthy`; `curl /health` → 200.
2. **Read the error envelope.** Every error returns `{"success":false,"error":{"code":...},"trace_id":...}`. Branch on `code`: `*.not_found` (404) = unknown case/route; `validation.*` (400) = bad input; `system.unexpected` (500) = our bug → check logs for that `trace_id` (full stacktrace is logged, never returned).
3. **Trace it.** Pass `-H "X-Trace-Id: my-trace"` and grep the logs for `my-trace` to follow the request.
4. **Port/mapping.** Confirm `8412:8412` in `docker-compose.yml` matches the app's `server.port`.

### Known limitations
- In-memory storage — all state resets on restart (re-seeds v1). No database by design.
- `backup.sh`/`restore.sh` require `curl` + `jq` and a running service.
- No auth; no production log pipeline (out of scope per the brief).

---

## AI assistance

Built with Claude Code throughout (analysis → design → implementation), with a self-authored `/super-review` subagent gate before each commit. The session log is committed at `/claude-code-session.jsonl`.
