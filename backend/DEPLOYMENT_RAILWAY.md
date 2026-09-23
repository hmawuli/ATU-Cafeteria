# Laravel API Deployment — Railway

The deployable backend is the `backend/` directory. Railway builds it with Nixpacks (`railway.json`) and runs migrations before starting the HTTP service.

## Production deployment

1. **Create a Railway project** → **New Service → Deploy from GitHub repo** (`hmawuli/ATU-Cafeteria`), with the service root set to `backend/`.
2. **Set environment variables** in Railway:
   ```env
   APP_ENV=production
   APP_DEBUG=false
   APP_KEY=<generated application key>
   APP_URL=https://<your-railway-domain>
   DB_CONNECTION=pgsql
   DB_HOST=<postgres-host>
   DB_PORT=5432
   DB_DATABASE=<database>
   DB_USERNAME=<username>
   DB_PASSWORD=<password>
   PAYSTACK_SECRET_KEY=<production secret>
   PAYSTACK_DEMO_MODE=false
   ```
3. **Use managed PostgreSQL for production** rather than relying on a local SQLite file for a long-lived multi-user deployment.
4. Configure the production frontend origins through `CORS_ALLOWED_ORIGINS` and keep all secrets in Railway Variables.
5. Run database migrations during deployment and seed only controlled production reference data. Do not use development credentials in production.
6. Point the Flutter application to the deployed API using the production API configuration.

## Security requirements

- **Do not commit `.env` or secrets.**
- Keep Paystack secret credentials server-side.
- Keep `PAYSTACK_DEMO_MODE=false` in production.
- Use HTTPS for the deployed API.
- Use a managed PostgreSQL database for production workloads.
- Restrict CORS to known production application origins.
- Back up the production database and test restoration procedures.

## Health checks

- Laravel health: `https://<domain>/up`
- API health: `https://<domain>/api/health`

## Post-deployment verification

1. Open `/api/health` and confirm the service reports healthy status.
2. Verify authentication and authorization flows.
3. Verify menu reads and order placement.
4. Verify vendor order processing and pickup verification.
5. Verify wallet and Paystack payment verification.
6. Review application logs and audit records for unexpected errors.
