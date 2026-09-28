# User Acceptance Testing (UAT) Checklist

Manual test pass for each release. Do this on a physical Android phone or
emulator against the deployed API (production-like), with a fresh test user.

## Prerequisites

- Backend deployed and healthy: `scripts/smoke_test.sh https://<host>`
- App built with the deployed API:
  `flutter run --dart-define=API_BASE_URL=https://<host>/api/`

## 1. Authentication & account

- [ ] Register a new student account (email + password, STUDENT role).
- [ ] Duplicate email registration is rejected with a clear error.
- [ ] Login succeeds and token persists across app restarts (`/me` restore).
- [ ] Logout revokes the session/device.
- [ ] Wrong PIN logs an `AUTH_FAILURE` audit entry (admin can see it).
- [ ] Password reset flow works with the emailed code.

## 2. Catalogue & search

- [ ] Customer home shows the live menu from `GET /api/catalog/menu-items`.
- [ ] Vendor filter and search return the expected items.
- [ ] Unavailable items are marked and cannot be ordered.
- [ ] Vendor menu management (vendor login) lists the vendor's own items.

## 3. Ordering, readiness & pickup

- [ ] Place an order; wallet debited; order appears in history.
- [ ] Low balance blocks checkout with a clear message.
- [ ] Vendor sees the new order; pickup PIN is never visible to other vendors.
- [ ] Vendor advances order to PREPARING → READY.
- [ ] Ready-order polling raises the in-app alert within ~30s.
- [ ] Student picks up with the correct 4-digit PIN; order completes.

## 4. Wallet & payments

- [ ] Wallet balance shows and reconciles after an order.
- [ ] Top-up flow verifies a real Paystack reference and credits the wallet
  transactionally.
- [ ] Refund path restores balance (if refunds are enabled).

## 5. Vendor operations & analytics

- [ ] Vendor dashboard opens: metrics, daily revenue, recharts, by_date data.
- [ ] Vendor menu CRUD with stock: creating/adjusting inventory updates
  `current_stock`.
- [ ] Kiosk order entry works for walk-ins.
- [ ] Promotions create and apply.

## 6. Admin command centre

- [ ] Admin dashboard aggregates real (not fabricated) metrics.
- [ ] Audit log shows security events (AUTH_FAILURE, refunds, admin actions).
- [ ] User suspension blocks that user's access immediately.

## 7. Resilience

- [ ] Offline: app opens and shows cached catalogue (SQLite), no crash.
- [ ] Airplane mode + retry recovers once connectivity returns.
- [ ] App process killed mid-checkout → reopening does not double-charge
  (idempotency key present).

## Sign-off

- [ ] All must-pass items above are green
- [ ] No crash reports in Crashlytics (if enabled)
- [ ] Backend logs show no 5xx errors during the pass
- [ ] Approved by the maintainer before tagging the release