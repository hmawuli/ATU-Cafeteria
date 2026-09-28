# Staff Quick Start (Vendor Terminals & Admin)

Short operational guide for the people who actually run the cafeteria system —
not developers. Screens are the Flutter app; every action below talks to the
central Laravel API.

## Roles

| Role | What they do |
|---|---|
| **Student** | Order ahead, pay from wallet, pick up with a 4-digit PIN |
| **Vendor / kitchen staff** | Accept orders, cook, mark READY, verify pickup, run kiosk sales, manage menu/stock |
| **Admin** | Create vendors, monitor dashboards and audit logs, adjust wallets, oversee payouts |

## Daily flow at a vendor terminal

1. **Open the app** → **Sign in** with your vendor username and PIN.
2. **New orders** appear on the operations dashboard / order workflow screen.
3. **Start cooking** → set the order to **PREPARING** (single tap).
4. **Done cooking** → set to **READY**. The student immediately gets a
   "ready for pickup" alert.
5. **Hand over** → at pickup, student shows the **4-digit pickup PIN**; enter
   it to verify → order becomes **COMPLETED**.
6. **No-show?** Use **CANCELLED / DECLINED** as appropriate.

## Menu & stock

- The **Menu** screen lists your items, prices and `current_stock`.
- Add a dish: name, price, category, description. Stock starts at the value
  you set (`initial_stock`).
- When you prepare/sell, stock drops automatically from order placements and
  kiosk sales. Replenish via the **Inventory** screen (`adjust`).
- Out-of-stock items are hidden from students automatically.

## Kiosk (walk-in sales)

- The **Kiosk** screen records a sale **without** a student account: pick the
  dish + quantity → cash/POS payment → order created and stock deducted.

## Performance & money

- **Dashboard** shows today’s **daily revenue**, order volumes and the sales
  chart (`daily-revenue`, `recharts-sales`).
- **Finance** shows your earnings; **Payout account** stores the bank/momo
  details used for settlement.
- **Promotions** lets you create discounts that apply at checkout.

## Admin essentials

- **Create a vendor**: Admin → Vendors → **Create vendor** (produces the
  username/PIN the vendor signs in with). The vendor account is a normal user
  row — never share the ADMIN account.
- **Audit trail**: Admin → **Audit logs** lists who did what (logins,
  order mutations, refunds, admin actions). Check it daily.
- **Wallets**: adjust balance only for legitimate corrections — every change
  is audited. Payments are verified by Paystack server-side.
- **Suspension**: you can suspend a user; the app blocks their access
  immediately.

## Where data lives

- The **Laravel API + PostgreSQL** are the source of truth.
- The phone keeps a local cache (SQLite) so screens still open offline; orders
  are never trusted as paid until the server accepts them.
- If the vendor terminal loses connectivity, keep taking PIN-based orders —
  they are **queued on the student’s phone** and delivered automatically when
  the network returns (never duplicated, thanks to idempotency keys).

## Escalation

- Vendor/terminal problems → admin.
- Payment mismatches → check `audit_logs` then the Paystack dashboard; do NOT
  manually credit wallets from memory — use the audited adjustment flow.