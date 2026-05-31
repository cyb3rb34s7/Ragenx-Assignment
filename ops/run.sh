#!/usr/bin/env bash
#
# run.sh — operator CLI around the docker-compose lifecycle for the PV backend.
# Usage: ops/run.sh <build|start|stop|test|logs|clean|--help>
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT/docker-compose.yml"
SERVICE="pv-backend"
PORT="8412"

log() { printf '%s %s\n' "[$(date -u +%FT%TZ)]" "$*" >&2; }
die() { log "ERROR: $*"; exit 1; }

require_docker() {
  command -v docker >/dev/null 2>&1 || die "docker is not installed or not on PATH."
  docker compose version >/dev/null 2>&1 || die "Docker Compose v2 is required (the 'docker compose' subcommand)."
  docker info >/dev/null 2>&1 || die "Docker daemon is not running — start Docker and retry."
}

compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

usage() {
  cat <<EOF
Usage: ops/run.sh <command>

Commands:
  build    Build the service image
  start    Start the service (detached) on http://localhost:${PORT}
  stop     Stop and remove the service container
  test     Run the backend test suite (./gradlew test)
  logs     Follow the service logs
  clean    Stop and remove the container, volumes, and locally-built image
  --help   Show this help
EOF
}

main() {
  local cmd="${1:-}"
  case "$cmd" in
    build) require_docker; compose build ;;
    start) require_docker; compose up -d; log "Started. Verify: curl -fsS http://localhost:${PORT}/health" ;;
    stop)  require_docker; compose down ;;
    test)  ( cd "$ROOT/backend" && ./gradlew --no-daemon test ) || die "tests failed." ;;
    logs)  require_docker; compose logs -f "$SERVICE" ;;
    clean) require_docker; compose down --volumes --rmi local ;;
    -h | --help | "") usage ;;
    *) log "unknown command: '$cmd'"; usage; exit 2 ;;
  esac
}

main "$@"
