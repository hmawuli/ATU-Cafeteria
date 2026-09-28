# ATU Cafeteria Management System — developer task runner
#
# `make help` lists every target. Targets delegate to the same scripts the CI
# workflow uses, so behaviour is identical locally and in GitHub Actions.
#
# The Flutter SDK is resolved from PATH unless you override FLUTTER, e.g.:
#   make FLUTTER=/home/you/flutter/bin/flutter test

SHELL := /usr/bin/env bash

# Resolve a working Flutter binary (PATH first, common SDK locations as
# fallback). Override explicitly with:  make FLUTTER=/path/to/flutter test
FLUTTER ?= $(shell scripts/flutter.sh)

.DEFAULT_GOAL := help

.PHONY: help setup hooks backend-deps frontend-deps migrate seed-dev serve connect analyze lint test format check

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

setup: hooks backend-deps frontend-deps ## One-time local setup: git hooks, PHP + Flutter dependencies, .env files
	@echo
	@echo "✓ Setup complete. Next: make seed-dev  (then)  make serve"

hooks: ## Install the repository git hooks (.githooks) for this clone
	scripts/install_hooks.sh

backend-deps: ## Install / update Laravel (Composer) dependencies
	cd backend && composer install --prefer-dist --no-interaction

frontend-deps: ## Install / update Flutter (pub) dependencies
	cd frontend && $(FLUTTER) pub get

migrate: ## Run backend migrations against the development database
	cd backend && php artisan migrate --force

seed-dev: migrate ## Seed development vendors + restaurant catalog (refuses in production)
	cd backend && php artisan db:seed --class=MenuCategoryAndVendorSeeder --force
	cd backend && php artisan db:seed --class=DevelopmentRestaurantCatalogSeeder --force
	@echo "✓ Development vendors and restaurant catalog seeded (port 8000)."

serve: ## Start Laravel on 0.0.0.0:8000 (reachable by a phone on the same Wi-Fi)
	scripts/serve_backend.sh 8000

connect: ## adb reverse — expose 127.0.0.1:8000 to a USB-connected phone
	scripts/connect_phone.sh 8000

analyze: ## Static analysis (Flutter)
	cd frontend && $(FLUTTER) analyze

test: ## Full test suite (Laravel + Flutter)
	cd backend && php artisan test
	cd frontend && $(FLUTTER) test

lint: ## Code style gates (Pint for backend, Flutter analyze for frontend)
	cd backend && ./vendor/bin/pint --test
	cd frontend && $(FLUTTER) analyze

format: ## Apply formatters (Pint for backend; Dart format for frontend)
	cd backend && ./vendor/bin/pint
	cd frontend && dart format lib test

check: ## Full project health check (runs the CI-style gate locally)
	scripts/check_project.sh

# --- Docker (self-hosted backend stack) ---

docker-build: ## Build the backend Docker image
	docker compose build

docker-up: ## Stand up the full stack (Laravel + MySQL + Nginx) in Docker
	docker compose up -d --build

docker-down: ## Stop the Docker stack (volumes persist unless you pass -v)
	docker compose down

docker-logs: ## Tail the Docker stack logs
	docker compose logs -f

docker-migrate: ## Run migrations against the Docker database
	docker compose run --rm app php artisan migrate --force --no-interaction

db-backup: ## Back up the PostgreSQL database (Docker stack or local)
	scripts/db_backup.sh

smoke: ## Smoke-test the running API (/health, /api/health, catalogue)
	scripts/smoke_test.sh

load: ## Load test (default: /api/health, 20 concurrent, 200 requests)
	scripts/load_test.sh

contract: ## Verify every frontend API call has a matching backend route
	scripts/check_api_contract.sh