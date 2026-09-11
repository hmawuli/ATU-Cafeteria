# ATU Cafeteria Authentication Standard

## Authentication flow
1. Flutter submits username and password/PIN to the Laravel API over HTTPS in production.
2. Laravel validates the account, status and credential.
3. Legacy SHA-256 client credentials are accepted only for migration and upgraded to Laravel hashing after successful authentication.
4. Admin accounts with 2FA enabled receive a single-use, expiring verification code before a Sanctum token is issued.
5. Flutter stores the issued token in Flutter Secure Storage.
6. `/api/me` restores and validates an existing session.
7. Logout revokes the current Sanctum token.

## Authorization
Laravel is the source of truth for role and permission checks. The Flutter client never decides that a user is an administrator.

Administrative roles are represented by `admin_level` and permissions are resolved server-side:
- `SUPER_ADMIN`
- `CAFETERIA_ADMIN`
- `FINANCE_ADMIN`

## Credential recovery
Password reset codes are hashed in the database, expire after 10 minutes, have a five-attempt limit, and revoke all existing Sanctum tokens after a successful reset.

## Email verification
Users may attach a verified email address to their profile. Verification codes are single-use and expire after 10 minutes.

## Rate limiting
Authentication endpoints use a username + IP key and are limited to five attempts per minute. This is intentionally conservative for a low-volume campus application.

## Production requirements
- Set `APP_ENV=production`.
- Configure HTTPS at the reverse proxy/load balancer.
- Configure SMTP for real email delivery.
- Set a strong `APP_KEY`.
- Set `SANCTUM_TOKEN_EXPIRATION` to the desired lifetime in minutes.
- Never commit `.env` or payment secrets.
