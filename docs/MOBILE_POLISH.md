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

## 2. Offline order queueing (implemented — verify on device)

**Implemented:** `DbHelper.pending_orders` table, the `PendingOrderQueue`
service (`lib/services/pending_order_queue.dart`) and wiring in the provider —
an order composed while offline is queued with its idempotency key and flushed
through `POST /api/customer/orders` during `refreshAllData` once connectivity
returns. Unit-tested (`test/pending_order_queue_test.dart`): success delivers,
4xx drops, 5xx/network retries.

### Behaviour to verify on a real device
1. **Compose** — airplaned client places an order → it is persisted locally and
   shown as queued.
2. **Flush** — reconnect and trigger a refresh → the order delivers over
   `POST /api/customer/orders` with its idempotency key.
3. **Reconcile** — verify `orders` contains exactly **one** row for that key
   (no duplicate charge), and the queue row is removed on success.

## 3. Verifying the finish line

Run `docs/UAT_CHECKLIST.md` on a device, then confirm:

- [ ] TalkBack reads every core flow end-to-end.
- [ ] Large text has no overflows.
- [ ] Offline order shows "Queued" and completes after reconnect with no
  duplicate charge (check `orders` table for one row with that idempotency key).