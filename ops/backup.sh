#!/usr/bin/env bash
#
# backup.sh — snapshot all cases from the running service to a timestamped JSON file.
# Safe to schedule as a cron job: no prompts, logs to stderr with timestamps, non-zero on failure.
#
# Env overrides:
#   PV_BASE_URL  (default http://localhost:8412)
#   BACKUP_DIR   (default <repo>/backups)
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${PV_BASE_URL:-http://localhost:8412}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups}"

log() { printf '%s %s\n' "[$(date -u +%FT%TZ)]" "$*" >&2; }
die() { log "ERROR: $*"; exit 1; }

command -v curl >/dev/null 2>&1 || die "curl is required but not installed."
command -v jq   >/dev/null 2>&1 || die "jq is required but not installed."

mkdir -p "$BACKUP_DIR" || die "cannot create backup directory: $BACKUP_DIR"
out="$BACKUP_DIR/cases_$(date -u +%Y%m%dT%H%M%SZ).json"

log "Backing up all cases from $BASE_URL"
resp="$(curl -fsS "$BASE_URL/cases")" || die "GET /cases failed — is the service up at $BASE_URL?"

printf '%s' "$resp" | jq -e '.success == true and (.data | type == "array")' >/dev/null 2>&1 \
  || die "unexpected response from /cases (expected a success envelope with a data array)."

# Write atomically: render to a temp file first, then move into place, so a failed jq run
# never leaves a truncated/zero-byte "backup" behind (matters for a cron job).
tmp="$(mktemp "$BACKUP_DIR/.cases.XXXXXX.json")" || die "cannot create temp file in $BACKUP_DIR"
trap 'rm -f "$tmp"' EXIT
printf '%s' "$resp" | jq '.data' > "$tmp" || die "failed to render backup"
mv "$tmp" "$out" || die "failed to finalize $out"
trap - EXIT

count="$(jq 'length' "$out")"
log "Wrote $count case(s) to $out"
