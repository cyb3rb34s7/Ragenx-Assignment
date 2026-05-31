# Convenience targets — thin delegation to ops/ scripts.
# `make restore FILE=backups/cases_<ts>.json`
.PHONY: help build start stop test logs clean backup restore

help:
	@echo "Targets:"
	@echo "  build    Build the service image"
	@echo "  start    Start the service on http://localhost:8412"
	@echo "  stop     Stop the service"
	@echo "  test     Run the backend test suite"
	@echo "  logs     Follow service logs"
	@echo "  clean    Stop and remove container, volumes, and image"
	@echo "  backup   Snapshot all cases to backups/"
	@echo "  restore  Restore from a backup: make restore FILE=<path> [DRY_RUN=--dry-run]"

build:
	./ops/run.sh build

start:
	./ops/run.sh start

stop:
	./ops/run.sh stop

test:
	./ops/run.sh test

logs:
	./ops/run.sh logs

clean:
	./ops/run.sh clean

backup:
	./ops/backup.sh

restore:
	./ops/restore.sh $(DRY_RUN) $(FILE)
