# ATU Cafeteria RBAC

The application uses role-based access control with server-side permission enforcement.

## Administrative levels

- `SUPER_ADMIN`: unrestricted administrative governance, including roles, settings, finance and audit.
- `CAFETERIA_ADMIN`: normal cafeteria administration: users, vendors, menu oversight, orders, reports, feedback and audit visibility.
- `FINANCE_ADMIN`: financial reporting, payment visibility and controlled wallet adjustments.

## Rules

1. Laravel is the source of truth for authorization.
2. Flutter hides unavailable actions for usability but never replaces server authorization.
3. Passwords/PINs are never returned by APIs.
4. Administrative level changes are restricted to Super Admin.
5. An administrator cannot deactivate their own active account.
6. Financial adjustments require a reason and create a wallet transaction plus audit record.
7. Important administrative changes are audited.
8. Students and vendors are scoped to their own data at the API layer.


## Authentication privileges
Authentication is independent from authorization. Laravel determines the authenticated user's role and permissions; Flutter never submits an asserted role for login. Sensitive admin actions require both an active Sanctum session and the configured permission.

## Account security
- Login is rate limited by username and IP.
- Tokens are stored in Flutter Secure Storage.
- Password/PIN reset revokes all active Sanctum tokens.
- Admin 2FA codes are single-use and expire after 10 minutes.
- Optional email verification is available through the authenticated verification endpoints.
- Production API traffic requires HTTPS.
