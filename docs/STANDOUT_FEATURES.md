# Stand-out Features

The differentiators that set the ATU Cafeteria platform apart from a
typical campus cash-and-queue stall — all built on the audited, idempotent,
PostgreSQL-driven core.

## 1. Public "What's Open Now" board + stall directory

- **`GET /api/public/stalls`** (no auth) → `{success, data:[{id, store_name,
  is_open, campus, open_menu_items}]}`. Optional `?campus=` filter for
  multi-campus deployments.
- **`GET /stalls`** → a clean, server-rendered HTML page (no Flutter needed)
  listing every stall's live open/closed status and menu size. Great for
  marketing screens, parents, and visitors.
- **`GET /api/stalls/{id}`** (no auth) → a single stall with its live menu.

> Marketing tip: link `https://<host>/stalls` from posters and social media —
> it's always current because it reads the same API the app uses.

## 2. Stall QR / deep-link ordering

Scanning (or typing) a **stall code** should land a student straight on that
stall's menu. The API side is ready:

- QR payload → `atu://stall/{vendorId}` or `https://<host>/stall/{vendorId}`.
- In Flutter, route that URI to `/api/stalls/{id}`, render the menu, and let
  the student order with the standard checkout.

**Device/config steps (yours to run on a phone):**
1. Add a camera scanner (`mobile_scanner` package) that reads the QR and
   pushes the stall id to the app's navigation.
2. Use the deep-link handler to catch the scan result and open the stall menu.

## 3. Scheduled pre-ordering

Order today, pick up tomorrow — or "skip the queue at exactly 1pm".

- `PUT /api/orders/{id}/schedule` (auth: owner vendor/admin) with
  `{"pickup_at": "2026-10-01T13:00:00Z"}` sets `orders.scheduled_pickup_at`.
- Also available on the client: `CafeteriaProvider.schedulePickup(orderId, pickupAt)`.
- Every schedule is written to `audit_logs` (`ORDER_SCHEDULED`).

## 4. Health & sustainability badges

Derived automatically from each menu item's `dietary_tags` /
`allergen_info` (zero fabrication):

`GET /api/catalog/menu-items` and `GET /api/stalls/{id}` return a `badges`
array per item: `VEGAN`, `VEGETARIAN`, `GLUTEN-FREE`, `FEATURED`.

Pair this with `GET /api/vendor/waste/summary` for the sustainability story
(waste-reduction analytics) to market "know what you eat, watch the waste."

## 5. Loyalty streaks & milestones

`GET /api/customer/loyalty/summary` (auth student) now returns:

- `streak_days` — consecutive days (ending today) with a completed order.
- `milestone_target_days` / `days_to_next_milestone` — next habit milestone
  (7 → 30 days).

## 6. Digital receipts (JSON + printable PDF)

- **`GET /api/orders/{id}/receipt`** (auth: owner/vendor/admin) → JSON order,
  vendor and wallet ledger.
- **`GET /api/orders/{id}/pdf`** → a clean A4 PDF receipt generated
  offline (no external PDF service).

> Optional next step: auto-email the receipt on order completion by sending
> the same data through Laravel Mail (`MAIL_FROM_ADDRESS` is already
> configured in `.env`).

## 7. Multi-campus / franchise-ready

`vendors.campus` (default `Accra Technical University`) powers the
`?campus=` filter on the public board; vendors can be grouped and marketed
per campus or franchise site without code changes.

---

### Feature → endpoint summary

| Feature | Endpoint |
|---|---|
| Open-now board (JSON) | `GET /api/public/stalls[?campus=]` |
| Open-now board (HTML) | `GET /stalls` |
| Stall menu (QR/deep-link target) | `GET /api/stalls/{id}` |
| Scheduled pick-up | `PUT /api/orders/{id}/schedule` |
| Health badges | `GET /api/catalog/menu-items` (per item `badges`) |
| Loyalty streak | `GET /api/customer/loyalty/summary` |
| Receipt (JSON) | `GET /api/orders/{id}/receipt` |
| Receipt (PDF) | `GET /api/orders/{id}/pdf` |