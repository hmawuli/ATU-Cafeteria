# Laravel API Deployment — Railway

The deployable backend is the `backend/` directory.

## Required environment variables

At minimum configure:

```env
APP_ENV=production
APP_DEBUG=false
APP_KEY=<generated-secure-key>
APP_URL=https://<your-railway-domain>

DB_CONNECTION=<production-driver>
DB_HOST=<host>
DB_PORT=<port>
DB_DATABASE=<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>

PAYSTACK_SECRET_KEY=<server-secret>
```

Do not commit `.env` or production secrets.

## Deployment

Configure Railway to use `backend/` as the service root. The supplied `railway.json` runs database migrations before starting the HTTP service.

For a production database, use PostgreSQL or the database service recommended by your deployment environment rather than the local SQLite file.

## Health checks

- Laravel health endpoint: `/up`
- Application API health endpoint: `/api/health`

## After deployment

1. Open the API health endpoint.
2. Run migrations if they were not executed by the deployment command.
3. Confirm authentication.
4. Confirm menu reads.
5. Confirm order placement.
6. Confirm vendor status updates.
7. Confirm payment configuration before enabling real payments.
8. Point Flutter to the HTTPS API endpoint using `API_BASE_URL`.
