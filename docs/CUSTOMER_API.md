# Customer API

Canonical restaurant-facing API namespace:

- POST /api/customer/register
- POST /api/customer/login
- GET /api/customer/me
- POST /api/customer/logout
- GET /api/customer/orders
- POST /api/customer/orders
- POST /api/customer/cart-checkout
- POST /api/customer/orders/{id}/cancel
- GET /api/customer/orders/{id}/tracking
- GET /api/customer/loyalty
- POST /api/customer/loyalty/preview-discount
- GET /api/customer/wallet
- POST /api/customer/wallet/top-up
- GET/POST/PUT/DELETE /api/customer/addresses
- GET/POST/DELETE /api/customer/devices
- GET/POST /api/customer/support/tickets
- DELETE /api/customer/account
- GET/POST/DELETE /api/customer/reviews

Versioned aliases are available under /api/v1/customer.

Critical write requests must send an Idempotency-Key header. Keys must be reused for retries of the exact same operation.
