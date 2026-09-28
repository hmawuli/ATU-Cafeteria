# Data Flow Between Pages

How data moves through the app and the API, and the conventions every screen
must follow so state stays consistent **across** pages.

## The pattern at a glance

```
   ┌────────────────────────────────────────────────────────────┐
   │  Screens (pages)                                            │
   │   Home · Login · Customer · Order · Vendor · Workflow · …  │
   └─────┬───────────────▲──────────────────────────┬────────────┘
   read / watch          │ notifyListeners()         │ provider methods
         │               │                            │
   ┌─────▼───────────────┴───────────┐     ┌──────────▼──────────┐
   │  Shared providers (memory)      │     │  ApiClient (typed)  │
   │  CafeteriaProvider              │────▶│  REST → Laravel API │
   │  CartProvider · AdminState      │     │  → PostgreSQL        │
   └───────────────▲─────────────────┘     └──────────┬──────────┘
                   │ falls back / mirrors            │ server is
   ┌───────────────┴───────────┐                     │ source of truth
   │  SQLite (offline cache)   │──────────────────────┘
   └───────────────────────────┘
```

## The rules (already enforced in code)

1. **One source of truth per domain, in one provider.** Authentication, the
   catalogue, orders, wallet, feedback/audit, vendor operations and payments
   are all state on `CafeteriaProvider` (`context.watch` to rebuild,
   `context.read` to fire an action). `CartProvider` owns the cart,
   `AdminStateProvider` owns admin screens.

2. **Screens never do raw HTTP.** Every network call flows through a provider
   method → `ApiClient.request` / `rawRequest` (tokens, idempotency keys, JSON
   error mapping, HTTPS enforcement). The single page that historically did
   raw `http` (`vendor_order_workflow_screen`) now calls
   `provider.refreshVendorOrders()` / `provider.completePickup()` instead.

3. **A mutation must refresh shared state.** Mutations call
   `refreshAllData()` / `refreshVendorOrders()` / `refreshPendingOrderCount()`
   and `notifyListeners()`, so a change made on one page (order completed,
   wallet credited, pickup verified) is immediately visible on every other
   page that watches the same state. Page-local `setState` is only for
   transient UI (dialogs, busy flags, errors).

4. **Server first, SQLite as a mirror.** Reads prefer the API; on failure the
   provider falls back to the local database so pages still open offline. The
   Laravel API + PostgreSQL remain the source of truth; queued offline orders
   are flushed with idempotency keys (never duplicated).

5. **Explicit navigation only where it helps.** Pages receive minimal typed
   data via route `arguments` (ids like `orderId`, `menuItemId`); the rest is
   looked up from the providers — not passed through dozens of constructors.

## Sequence for a typical action

1. User taps on page A → screen calls `context.read<CafeteriaProvider>().foo()`
   (no raw HTTP).
2. Provider validates local readiness → `ApiClient.request(...)` to Laravel.
3. Laravel writes to PostgreSQL, returns a typed response.
4. Provider updates its state and calls `notifyListeners()`.
5. Page B (watching that state) rebuilds with fresh data automatically.
6. SQLite is refreshed as an offline mirror; the UI never depends on it.

## Cross-page examples already working

- **Login → any page**: `_currentUser`, token and wallet persist in the
  provider; every page reads the same session.
- **Pickup verified on vendor workflow page** → the provider's
  `vendorOrders` refresh propagates the completed status to the operations
  dashboard and admin screens at once.
- **Wallet top-up** → Paystack verify + `refreshAllData()` → balance updates
  on every page.
- **Order placed offline** → queued with an idempotency key; on reconnect
  `flushPendingOrders()` delivers it and the count/status updates everywhere.

## When to grow beyond this pattern

The single `CafeteriaProvider` is pragmatic for this scale, but if the team
and the feature set grow, split state into focused objects (Auth, Catalogue,
Orders, Wallet, Vendor) — each with its own controller, service and tests —
while keeping the five rules above. Start with the ones that change most
independently (Orders, Wallet).