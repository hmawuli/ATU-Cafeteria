# Local PostgreSQL Setup

The **Laravel API is the system's data store and it uses **PostgreSQL**
locally — exactly like production (Docker/VPS). The Flutter phone app only
keeps a small SQLite offline cache; every durable record (users, orders,
wallets, audit logs) lives in PostgreSQL.

## Where the local credentials live

The single source of truth is `backend/.env` (gitignored — never commit it):

```env
DB_CONNECTION=pgsql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=atu_cafeteria_backend
DB_USERNAME=postgres
DB_PASSWORD=<your-postgres-password>
```

> The credentials shown here match this machine's PostgreSQL server. On any
> other machine, put *that* machine's values into `backend/.env` — everything
> below reads them from there, so nothing else needs to change.

## 1. Install & start PostgreSQL (Ubuntu)

```bash
sudo apt install -y postgresql postgresql-client
sudo systemctl enable --now postgresql
```

## 2. Create the database (idempotent, credentials-aware)

```bash
make db-create        # scripts/ensure_db.sh — reads backend/.env, creates DB if missing
make db-ping          # verify the connection works
```

Equivalent manual step if you prefer:

```bash
sudo -u postgres createdb -O postgres atu_cafeteria_backend
```

## 3. Migrate, seed & serve

```bash
make seed-dev         # php artisan migrate + development vendors + restaurant catalog
make serve            # Laravel on 0.0.0.0:8000
```

`seed-dev` is production-guarded (refuses under `APP_ENV=production`) and
loads: menu categories, two vendor accounts (`testvendor1` Campus Delight,
`testvendor2` Quick Bites) and the restaurant catalog with stock.

## 4. Verify data is stored in PostgreSQL

```bash
psql "postgresql://postgres:<password>@127.0.0.1:5432/atu_cafeteria_backend" -c "
  SELECT 'users',       count(*) FROM users
  UNION ALL SELECT 'menu_items', count(*) FROM menu_items
  UNION ALL SELECT 'orders',     count(*) FROM orders;"
```

Or through Laravel:

```bash
make db-ping
cd backend && php artisan tinker --execute="print(\App\Models\MenuItem::count().' menu items'.PHP_EOL);"
```

## Automated tests

`php artisan test` runs against **SQLite in-memory** (`phpunit.xml`) so the
suite is fast and hermetic — but CI also runs the **full suite on
PostgreSQL** (`backend-postgres` job), and we run it locally against
`atu_cafeteria_test` to prove production-driver compatibility:

```bash
DB_CONNECTION=pgsql DB_HOST=127.0.0.1 DB_PORT=5432 \
DB_DATABASE=atu_cafeteria_test DB_USERNAME=postgres DB_PASSWORD=<password> \
php artisan test
```

## Docker

The same PostgreSQL behaviour is reproduced in containers
(`docker compose up -d --build`) via the `db` service — see
`docker-compose.yml` and `docs/DEPLOY_VPS.md`.

## Security notes

- `backend/.env` holds the dev password and is **gitignored** (`backend/.env`
  in `.gitignore`) — it can never ship.
- Rotate the password before any environment becomes public/shared.
- Backups: `make db-backup` (compressed `pg_dump`); restore drill monthly.