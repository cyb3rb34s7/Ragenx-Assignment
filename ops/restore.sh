#!/usr/bin/env bash
#
# restore.sh — restore cases from a backup file by PUTting each one back verbatim.
# PUT /cases/{id} is idempotent and version-preserving, so this reinstates the exact checkpoint.
# Supports --dry-run (shows what would happen, makes no changes).
#
# Usage: ops/restore.sh [--dry-run] <backup-file.json>
# Env overrides: PV_BASE_URL (default http://localhost:8412)
#
set -euo pipefail

BASE_URL="${PV_BASE_URL:-http://localhost:8412}"
DRY_RUN=false
FILE=""

log() { printf '%s %s\n' "[$(date -u +%FT%TZ)]" "$*" >&2; }
die() { log "ERROR: $*"; exit 1; }
usage() { echo "Usage: ops/restore.sh [--dry-run] <backup-file.json>" >&2; }

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    -h | --help) usage; exit 0 ;;
    -*) die "unknown option: $arg" ;;
    *) FILE="$arg" ;;
  esac
done

[[ -n "$FILE" ]] || { usage; exit 2; }
[[ -f "$FILE" ]] || die "backup file not found: $FILE"
command -v curl >/dev/null 2>&1 || die "curl is required but not installed."
command -v jq   >/dev/null 2>&1 || die "jq is required but not installed."

jq -e 'type == "array"' "$FILE" >/dev/null 2>&1 || die "backup file is not a JSON array of cases: $FILE"
count="$(jq 'length' "$FILE")"
log "Restoring $count case(s) to $BASE_URL (dry-run=$DRY_RUN)"
[[ "$count" -gt 0 ]] || { log "Nothing to restore."; exit 0; }

for i in $(seq 0 $((count - 1))); do
  case_json="$(jq -c ".[$i]" "$FILE")"
  id="$(printf '%s' "$case_json" | jq -r '.case_id')"
  [[ -n "$id" && "$id" != "null" ]] || die "case at index $i has no case_id"
  ver="$(printf '%s' "$case_json" | jq -r '.version')"

  if [[ "$DRY_RUN" == true ]]; then
    log "DRY-RUN: would PUT /cases/$id (version $ver)"
    continue
  fi

  log "PUT /cases/$id (version $ver)"
  printf '%s' "$case_json" \
    | curl -fsS -X PUT "$BASE_URL/cases/$id" -H 'Content-Type: application/json' --data-binary @- \
    | jq -e '.success == true' >/dev/null 2>&1 \
    || die "restore failed for case $id"
done

log "Restore complete ($count case(s))."
