# Payment Gateway Setup (Wallet + Paystack)

How money movement is wired and how to switch it on correctly. The wallet is
**server-authoritative**: the Flutter app never sets a balance — Laravel
verifies every credit with Paystack and records it in an auditable ledger.

## Architecture

```
Flutter app ──REST──▶ Laravel API ──HTTPS──▶ Paystack
                        │  ▲
        initialize ─────┘  └───── webhook (HMAC-verified)
                        │
                 wallet_transactions (ledger) → users.balance
```

- **initialize** (`POST /api/paystack/initialize`) — creates a `Payment` +
  PENDING ledger row and returns the checkout URL. Idempotency-protected.
- **verify** (`GET /api/paystack/verify/{reference}`) — the **only** path that
  credits a wallet; duplicate-safe and amount-checked.
- **webhook** (`POST /api/paystack/webhook`) — HMAC-signed; acknowledges
  charge events, reconciles refunds and vendor settlement transfers. Never
  mints balance.
- **reconcile** (`GET /api/wallet/reconcile`) — proves stored balance matches
  the signed ledger.

## Two modes

| Mode | `PAYSTACK_DEMO_MODE` | Behaviour |
|---|---|---|
| **Demo** | `true` | Initialize returns a simulated URL; verify credits instantly. No Paystack account or key needed (ideal for UAT). |
| **Live** | `false` + real `PAYSTACK_SECRET_KEY` | Real charges through Paystack; webhooks active. |

Switch safely with one command (never hand-edit secrets):

```bash
php artisan paystack:mode demo                         # testing / UAT
php artisan paystack:mode live --secret=sk_live_xxxx    # production
php artisan paystack:status [--ping]                    # show config; --ping proves the key
```

The command edits `backend/.env`, preserves other lines, and locks the file to
`0600`. Restart the backend afterwards so the process reloads config.

## Turning on live payments (one-time)

1. **Create the Paystack account** (only the account owner can do this):
   sign up, verify email/phone, complete business details.
2. Dashboard → **Settings → API Keys** → copy the **Secret Key**
   (start with **Test mode**, `sk_test_…`).
3. On the server:
   ```bash
   php artisan paystack:mode live --secret=sk_test_xxx
   php artisan paystack:status --ping        # expect "✅ Paystack secret is valid"
   ```
4. Dashboard → **Settings → Webhooks** → URL:
   `https://<your-api-domain>/api/paystack/webhook`
   Subscribe to: `charge.success`, `transfer.success`, `transfer.failed`,
   `refund.processed`, `refund.failed`.
5. Test with Paystack **test cards** (Dashboard → Developers → Test cards) to
   fund a wallet, place an order, and request a refund end-to-end.
6. When satisfied, repeat step 2–3 with the **live** secret (`sk_live_…`).

**Never commit or send the secret key.** It lives only in `backend/.env`
(gitignored, `0600`). The app has no key.

## Production guardrails already enforced

- `php artisan config:check --fail-on-prod` fails if `PAYSTACK_SECRET_KEY` is
  missing or `PAYSTACK_DEMO_MODE=true` on staging/production.
- Wallet credits require a **verified** payment or an admin adjustment
  (`WalletTransactionObserver`); successful deposits can't be forged.
- Payments are rate-limited (`throttle:payments`, 10/min per user+reference).
- Refunds/transfers are reconciled by signed webhooks; audit logs record each
  financial action.

## Quick reference

| Action | Endpoint / command |
|---|---|
| Start a top-up | `POST /api/paystack/initialize` |
| Credit (verify) | `GET /api/paystack/verify/{reference}` |
| Wallet + ledger | `GET /api/wallet` |
| Integrity check | `GET /api/wallet/reconcile` |
| Statement (JSON/PDF) | `GET /api/wallet/statement[?pdf=1]` |
| Webhook | `POST /api/paystack/webhook` (Paystack only) |
| Demo/live toggle | `php artisan paystack:mode demo|live` |
| Diagnostics | `php artisan paystack:status --ping` |