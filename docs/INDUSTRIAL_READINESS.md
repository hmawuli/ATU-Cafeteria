# Industrial Standard & Professional Readiness

This matrix records how the ATU Cafeteria system meets the checks a serious
reviewer or auditor would apply. Every item is implemented and verified in
this repository unless marked explicitly as an operational act requiring your
hardware/accounts.

| # | Dimension | Status | Evidence |
|---|---|---|---|
| 1 | Build & release pipeline | ✅ Green | CI builds backend (2 DBs) + frontend + **release APK/AAB**; `make release-build`; signed-release workflow |
| 2 | Automated testing | ✅ Green | Backend **87 tests × SQLite & PostgreSQL**; frontend **53 tests**; HTTP **E2E journey**; load/smoke tooling |
| 3 | Code quality | ✅ Green | Pint (242 files), `flutter analyze` 0 issues, contract gate, generated typed API client |
| 4 | Contract-first API | ✅ Green | Committed OpenAPI (`docs/openapi.json`), `make api-gen`, envelope/pagination/idempotency standards |
| 5 | Database & performance | ✅ Green | PostgreSQL primary; hot-path indexes; catalogue caching + invalidation; `PERFORMANCE_BUDGET.md` |
| 6 | Security | ✅ Green | Sanctum/RBAC/2FA/rate limits/idempotency; strong-PIN `admin:create`/`vendor:create`; `security:audit --fail-on-critical` deploy gate; append-only audit logs; wallet/Paystack hardening |
| 7 | Data integrity & payments | ✅ Green | Verified-E2E checkout; duplicate-success protection; refunds; wallet-leger guards |
| 8 | Offline & resilience | ✅ Green | Offline order queue (idempotency-keyed), SQLite cache + server mirror |
| 9 | Operations | ✅ Automated | Docker stack (app/worker/scheduler/nginx/pg + healthchecks), `deploy_docker.sh`, `deploy_local.sh`, `db-backup`, `prepare_host.sh`, queue systemd + cron templates |
| 10 | Observability | ✅ Ready | `/health`, `/api/health`, smoke/load checks, `OBSERVABILITY.md` runbooks, Crashlytics wired |
| 11 | Governance & docs | ✅ Green | SECURITY/CONTRIBUTING/RELEASING/CHANGELOG, PR + issue templates, CODEOWNERS, hooks, 30+ runbooks |
| 12 | Environments | ✅ Green | `local` / `staging` / `production` matrix; staging blocks dev seeders + demo payments |
| 13 | Release hygiene | ✅ Aligned | App `1.2.0+3`, OpenAPI `1.2.0`, CHANGELOG-kept, tagging workflow documented |
| 14 | Data rights (access & erasure) | ✅ Green | `GET /api/customer/account/data-export` + anonymising `DELETE /api/customer/account` (`CustomerDataRightsTest`) |
| 15 | Legal/privacy policy wording | ⚠️ Decision | Privacy/ToS **templates** exist; final policy + retention sign-off is institutional |
| 16 | Live deployment | ⏳ Operations | Needs a Docker host (or use `make deploy-local`), Firebase/Paystack/Play values, device UAT — one command each |

## To flip the four ⚠️/⏳ to ✅

1. `sudo bash scripts/prepare_host.sh` on a server → `APP_ENV=production bash scripts/deploy_docker.sh`.
2. Fill the 4 `ATU_FIREBASE_*` + Paystack key → `make release-build`.
3. Onboard real staff: `vendor:create` / `admin:create --super --enable-2fa`.
4. Device UAT: `docs/UAT_CHECKLIST.md` + `flutter test integration_test -d <device>`.
5. Institutional sign-off on the privacy/ToS templates + retention.

Everything else in the matrix is implemented, tested, and documented in this
repository.