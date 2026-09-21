# Laravel API Deployment — Railway

The deployable backend is the `backend/` directory. Railway builds it with
Nixpacks (`railway.json`) and runs migrations before starting the HTTP service.

## Fastest path to a fully working demo

1. **Create a Railway project** → **New Service → Deploy from GitHub repo**
   (`hmawuli/ATU-Cafeteria`), set the **service root to `backend/`**.
2. **Set environment variables** (Variables tab):
   ```env
   APP_ENV=production
   APP_DEBUG=false
   APP_KEY=<paste from: php artisan key:generate --show>
   APP_URL=https://<your-railway-domain>
   DB_CONNECTION=sqlite
   PAYSTACK_SECRET_KEY=<optional, only for real payments>
   ```
3. **Add a Volume** to the backend service:
   - Mount path: **`/app/database`**
   - This keeps the SQLite file (`database/database.sqlite`) on a persistent
     disk — without it, all data is wiped on every redeploy/restart. Single
     volume, ~1 GB is plenty.
4. **Seed demo data once** after the first deploy (the app needs menu items,
   vendors and users to be usable):
   - Railway → backend service → **Connect shell** (or a one-off command):
     ```
     php artisan db:seed --force
     ```
   - This creates the demo users (students `PIN 1234`, vendors
     `maryjoint/1111`, `atkitch/2222`, `snackbag/3333`, admin `admin123`),
     the food catalogue, orders and wallet history.
5. **Point the phone app at the backend** — no rebuild needed:
   - Open the app → login screen → **API server settings** (link under the
     "Secure ATU Cafeteria access" row).
   - Enter `https://<your-railway-domain>` → **Save**.
   - The same installed APK now works anywhere (4G/Wi-Fi, laptop off).

## Notes

- **Do not commit `.env` or secrets.** All config is set through Railway's
  Variables panel.
- **SQLite + volume is perfect for a demo/defense.** For a long-lived
  multi-user production system, prefer a Railway-managed **PostgreSQL**
  (set `DB_CONNECTION=pgsql`, `DB_HOST`, `DB_PORT`, `DB_DATABASE`,
  `DB_USERNAME`, `DB_PASSWORD`) — the code works with either.
- LLRT/stateless caveat: sessions and the file cache are ephemeral per
  instance, which is fine for the API (token auth is stateless).

## Health checks

- Laravel health: `https://<domain>/up`
- API health: `https://<domain>/api/health`

## After deployment checklist

1. Open `/api/health` in a browser — expect `{"status":"UP",...}`.
2. Seed data (step 4 above) if you want the demo catalogue.
3. In the app, set the **API server** URL and sign in
   (`maryjoint / 1111` for a vendor, `admin123` for admin).
4. Confirm menu reads → order placement → vendor order workflow → admin
   dashboard.