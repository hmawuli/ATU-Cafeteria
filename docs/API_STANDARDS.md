# ATU Cafeteria API Standards

Conventions every endpoint in this API follows. Client teams (Flutter app,
web dashboards, integrations) should build against this document plus the
OpenAPI specification served at `/api/docs/openapi.json`.

## Base URL and versioning

- The API lives under `/api` (e.g. `https://host/api/...`).
- The API is currently **unversioned**: the contract below is the version
  clients must pin against. Do not call undocumented routes.
- The current contract version is documented in `info.version` of the OpenAPI
  document. When breaking changes are required, the path becomes `/api/v1` and
  both versions coexist for a deprecation window.

## Authentication

- All protected routes require `Authorization: Bearer <token>`.
- Tokens are issued by `POST /api/login` (or `/api/student/login`,
  `/api/vendor/login`) using Laravel Sanctum.
- Sessions are restored with `GET /api/me` (200 with the user, 401 otherwise).
- Admins with 2FA enabled must complete `/api/login/2fa` after `/api/login`
  returns `requires_2fa: true`.
- Default token expiry is 1440 minutes (override with
  `SANCTUM_TOKEN_EXPIRATION`).

## Response envelope

Every JSON response uses the same shape unless explicitly documented
(listed below as *bare-list exceptions*):

```json
{
  "success": true,
  "data": { "...": "payload" }
}
```

Rules:

- **Success** — `"success": true` plus the payload under `data`.
- **Error** — `"success": false` plus:
  - `message`: human-readable summary;
  - `errors`: optional object of field → messages when validation failed.
- **Lists** — `data` is a JSON array. Established legacy keys on specific
  endpoints (`menu_items`) are documented in the OpenAPI spec and consumers
  may read `data` as an alias where supported.
- **Legacy endpoints** — `GET /catalog/food-items` and `GET /api/food-items`
  still exist but are considered legacy: they return the standard
  `{success, data}` envelope yet are deprecated in favour of
  `GET /catalog/menu-items`. Do not build new features on them.

### HTTP status codes used

| Code | Meaning |
|---|---|
| 200 | OK |
| 201 | Created |
| 400 | Validation or business rule failure (with `errors`) |
| 401 | Unauthenticated — always `{"message":"Unauthenticated."}` on `/api/*` |
| 403 | Authenticated but not allowed (RBAC/ownership) |
| 404 | Not found |
| 409 | Conflict (e.g. duplicate idempotent request) |
| 422 | Validation failure on auth/registration endpoints |
| 429 | Rate limited (see `error_code`) |

## Field conventions

- **Identity**: primary keys are the `users.id` for people (customer, vendor);
  `menu_items.id` for catalogue items.
- **Money**: stored and transmitted as decimal numbers (e.g. `35.00`).
- **Enums**: uppercase snake_case (`RECEIVED`, `PREPARING`, `READY`,
  `DELIVERED`, `CANCELLED`).
- **Timestamps**: ISO 8601 UTC where present.
- **Boolean/availability**: `is_available`, `is_open`, `is_featured`.
- **Stock**: `current_stock`, `low_stock_threshold` (integer units).

## Idempotency

Mutating financial/stateful routes require an `X-Idempotency-Key` header
(`idempotency:required` middleware): checkout, wallet top-up, payments,
refunds, settlements, vendor status toggles, promotion/inventory writes.

- The key is client-chosen (UUID recommended).
- Replaying the same key returns the original result without reapplying the
  side effect.
- Omitting the key on a required route returns `419` with a descriptive
  message.

## Rate limiting

| Limiter | Applied to | Limit |
|---|---|---|
| `auth` | login, register, 2FA, password reset | 5 / minute per `username+IP` → `429` + `error_code: AUTH_RATE_LIMITED` |
| `api` | authenticated API (default) | 60 / minute per user or IP |
| `payments` | payment initialize/verify/callback | 10 / minute per `user+reference` → `error_code: PAYMENT_RATE_LIMITED` |

## Security requirements

- HTTPS only in production; `APP_DEBUG=false`.
- Secrets (Paystack keys, DB credentials) exist **only** server-side.
- `PAYSTACK_DEMO_MODE=false` in production; payment verification is performed
  by Laravel, never trusted from the client.
- Wallet credits are transactional with duplicate-success protection.
- Vendors never receive another vendor's data or a customer's pickup PIN.
- CORS is restricted to known production origins
  (`CORS_ALLOWED_ORIGINS`), credentials disabled.

## Testing expectations

Every behavioural change ships with tests:

- Backend: `php artisan test` (feature + unit), style gate `./vendor/bin/pint --test`.
- Frontend: `flutter analyze` and `flutter test`.
- CI enforces all of the above on every push/PR (`flutter.yml`).

## Reference documentation

- Interactive docs: `/api/docs` (Swagger UI), spec: `/api/docs/openapi.json`.
- Vendor permissions: `docs/RBAC.md`.
- Payment/security standard: `docs/PRODUCTION_STANDARD.md`.