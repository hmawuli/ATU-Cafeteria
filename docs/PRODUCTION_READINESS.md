# ATU Cafeteria — Production Readiness

## Target architecture

Flutter is the customer/staff client. Laravel is the business-logic API and system of record. SQLite is an offline client cache only.

## Implemented production controls

- Canonical customer API with /api/customer and /api/v1/customer
- Legacy /student compatibility retained during migration
- Server-side authentication, role and permission checks
- Required idempotency for critical writes
- Order numbers and financial snapshot fields
- First-class payment records
- Wallet transaction balance-before/balance-after ledger fields
- Order status history
- Inventory movement auditing
- Inventory row locking and negative-stock prevention
- Refund records and wallet refunds
- Customer device/session management
- Customer saved addresses
- Customer support tickets
- Customer account closure with audit retention
- Promotions, vendor-owned promotions and promotion redemptions
- Vendor settlement data model, vendor finance dashboard and generation endpoint
- Aggregate checkout sessions with one payment spanning one or more vendor orders
- Multi-line vendor orders and server-authoritative checkout price preview
- Vendor-operated walk-in kiosk sales recorded as KIOSK channel transactions
- Inventory health summary with low-stock and out-of-stock visibility
- Database-backed notification center for customers and vendors
- FCM delivery that reports failure when production credentials are missing
- Debug-only local demo data
- Debug-only backend URL override
- Production release signing through protected CI credentials
- Automated database cleanup scheduling
- Production database backups with configured remote storage
- Request IDs, performance logging and security headers

## Required before go-live

1. Configure a supported production database (MySQL or PostgreSQL).
2. Configure real mail delivery and test password reset/email verification.
3. Configure Firebase Cloud Messaging credentials and register real device tokens.
4. Configure Paystack production credentials and complete webhook verification.
5. Configure remote encrypted backups and test a restore.
6. Configure staging and run the full migration/test suite.
7. Complete privacy policy, terms, refund and cancellation policy review.
8. Validate tax/fees and accounting treatment for the operating jurisdiction.
9. Run concurrency tests for inventory, payments, wallet debits, refunds, checkout sessions and webhook duplication.
10. Configure and test vendor payout account/settlement operations before enabling live payouts.
11. Complete release smoke testing on a physical Android device.

## Operational rule

Never treat a simulated payment, simulated notification, fabricated report, or client-calculated total as a production result.
