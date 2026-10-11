# Production Hardening Checklist (professional standard)

Run this checklist before every promotion to **live**. `make check` covers the
code gates; this covers the operational gates. Track each item with the exact
command or owner.

## P0 — required before go-live

- [ ] **Live payments** — set `PAYSTACK_SECRET_KEY`/`PAYSTACK_PUBLIC_KEY`,
      `PAYSTACK_DEMO_MODE=false`, and verify one real webhook via
      `php artisan paystack:status`.
- [ ] **Email works** — add SMTP vars (Vercel project) and test
      `php artisan notify:test` / password reset actually arrives.
- [ ] **Admin 2FA on** — production `admin:create` defaults 2FA on; every admin
      must be created or converted with 2FA, and no shared demo PINs remain:
      `php artisan security:audit --fail-on-critical`.
- [ ] **Real catalogue** — real vendors (`vendor:create`), real menu/prices;
      remove `testvendor1/2` and seeded demo data.
- [ ] **Signed release** — production keystore → `production-release.yml` →
      signed `.aab` for Play; generate the privacy policy + data-safety form.
- [ ] **Deploy gate** — run `bash scripts/security_gate.sh` on the target
      environment.

## P1 — trust & auditability

- [ ] **FCM + Crashlytics** — configure the 4 `ATU_FIREBASE_*` values and upload
      Crashlytics symbols in CI so crashes are readable.
- [ ] **Uptime + backups** — uptime monitor on `/api/health`; Neon PITR enabled;
      quarterly restore drill documented.
- [ ] **CI enforced** — fix the GitHub billing lock so the workflow gate runs on
      every push; keep `composer audit`, `pint`, `php artisan test`, and
      `flutter analyze/test` green (all verified locally).

## P2 — polish

- [ ] **Exports** — vendor Finance → Export orders (CSV); Workforce → Export
      workers/wages (CSV). Implemented and shipped.
- [ ] **Legal in-app** — show Privacy Policy / ToS / refund policy links; get
      institutional sign-off on `docs/PRIVACY_POLICY_TEMPLATE.md` and
      `docs/TERMS_OF_SERVICE_TEMPLATE.md`.
- [ ] **Dependency hygiene** — keep Dependabot on; upgrade majors deliberately
      (`flutter_lints`, `intl`, `qr`, `flutter_secure_storage`, Flutter SDK).
- [ ] **Localization** — English is the baseline; add Twi/Ga locales when
      required by the campus.

## Operational rules

- Never treat simulated payments, simulated notifications, or client-computed
  totals as production results.
- Every admin/worker action is audit-logged; review `audit_logs` daily.
- Keep `APP_DEBUG=false` and `LOG_CHANNEL=stderr` on the live container.