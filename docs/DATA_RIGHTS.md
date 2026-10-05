# Student Data Rights

How the system honours a user's right to **access** and **erase** their data
whilst keeping the financial ledger auditable.

## Export (right of access)

`GET /api/customer/account/data-export` (authenticated student) returns a
portable JSON bundle:

- `profile` — id, username, email, name, balance, loyalty points, status
- `orders` — the student's order history
- `wallet_transactions` — the full wallet ledger
- `devices` / `addresses` — registered devices and saved addresses
- `exported_at` — timestamp

## Erasure (right to be forgotten)

`DELETE /api/customer/account` (authenticated student, idempotency-key
protected):

1. **Refuses** while the wallet holds a balance (`409`) — spend or withdraw
   first, so money is never stranded.
2. Revokes every API token and unbinds push devices.
3. **Anonymises personal data**: the username becomes `deleted_user_<id>_<rand>`,
   the name becomes "Deleted User", the password is randomised, and
   `profile_info` / `info` / `student_staff_id` are cleared.
4. Keeps the account row and the **financial/order history** intact (amounts,
   refunds, settlements reference the account id) so audits and reconciliation
   remain truthful.
5. Writes an append-only `audit_logs` entry (`CUSTOMER_ACCOUNT_CLOSED`).

## Guarantees

- No personal identifiers survive erasure; only anonymised financial records.
- Deletion is idempotency-protected, so a retry cannot double-apply.
- Every action is auditable; audit rows themselves are append-only.

## Related

- `docs/API_STANDARDS.md` — envelopes, errors, idempotency.
- `docs/PAYMENTS_SETUP.md` — wallet/payment guarantees.
- `PRIVACY_POLICY_TEMPLATE.md`, `TERMS_OF_SERVICE_TEMPLATE.md` — institutional
  policy wording (retention periods should reference this behaviour).