# Mobile polish — accessibility & offline UX

Two improvement tracks for the Flutter client. One is a review checklist
(accessibility/UX), the other a design for offline order queueing. Both need a
quick device pass to validate before shipping — this document is the plan and
the checklist.

## 1. Accessibility & UX audit

Run this on a physical device, both light and dark theme, with text scaling
set to large in Android settings.

### Screen reader (TalkBack)
- [ ] Every tappable element has a readable label (no bare icons with empty
  semantics). Key screens: headers, cart, vendor kiosk, checkout.
- [ ] Form fields have labels announced before the hint.
- [ ] Order/status updates are announced (`Semantics(liveRegion: true)` on the
  live alert banner).
- [ ] Charts/analytics provide a text summary instead of only SVG graphics.

### Contrast & tap targets
- [ ] Text passes WCAG AA (4.5:1) in both themes — azure on white, amber
  accents on dark backgrounds need verification.
- [ ] Interactive elements ≥ 48 dp effective touch target.
- [ ] Focus states are visible (keyboard/TalkBack navigation).

### Layout
- [ ] Text scale 1.3× has no overflow exceptions (run the widget tests with
  `tester.view.physicalSize` variants).
- [ ] Long vendor names / currency strings truncate cleanly.
- [ ] Empty states (no orders, no catalogue) have a friendly message + action.

## 2. Offline order queueing (design)

Goal: a student can compose and dispatch an order while offline; it is sent
once connectivity returns — without double-charging.

### Server contract (already idempotent — good)
`POST /api/customer/orders` is `idempotency:required`. Each order already
carries an `Idempotency-Key`, so retrying an order after reconnecting cannot
create duplicates server-side.

### Client design
1. **Queue store** — persist pending orders in SQLite (`DbHelper`), keyed by
   their idempotency key: `{key, payload, created_at}`.
2. **Compose** — the checkout flow writes the payload to the queue and shows
   "Queued — we'll send it when you're online".
3. **Flush** — on reconnect (connectivity plugin / after a failed request),
   drain the queue oldest-first with `ApiClient.post(..., idempotencyKey: key)`.
4. **Reconcile** — on success remove from queue; on a 4xx validation error
   drop it and surface the message to the user; on 5xx/network keep it.
5. **Order status** — while queued, show "Pending" in order history; the
   server is the source of truth once accepted.

### Constraints
- Only single-vendor orders are queued (cart checkout is verified online).
- Wallet balance is validated server-side at flush time — a queued order may
  be rejected if the balance changed; the app must surface that.
- Do not auto-flush payments/mutations other than orders.

### Next step
Implement after addressing the accessibility items; the quad of
(sqflite queue table + connectivity flush + idempotent retry) is isolated and
unit-testable without touching the vendor/admin flows.

## 3. Verifying the finish line

Run `docs/UAT_CHECKLIST.md` on a device, then confirm:

- [ ] TalkBack reads every core flow end-to-end.
- [ ] Large text has no overflows.
- [ ] Offline order shows "Queued" and completes after reconnect with no
  duplicate charge (check `orders` table for one row with that idempotency key).