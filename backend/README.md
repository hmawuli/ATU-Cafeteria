# ATU Cafeteria — Laravel API

Laravel 12 backend for the ATU Cafeteria Flutter application.

## Responsibilities

- authentication and authorization
- students, vendors and administrators
- menus and food items
- order lifecycle and pickup verification
- wallet/payment integration
- feedback, reviews and audit logging
- vendor/admin analytics
- notifications and real-time support endpoints

## Architecture

```text
Flutter
  │
  └── JSON/HTTP + Bearer token
          │
          ▼
Laravel API
  ├── Controllers
  ├── Requests / Resources
  ├── Models
  ├── Services
  ├── Events / Listeners
  └── Notifications
          │
          ▼
       Database
```

There is no active Blade/PWA/browser frontend. `routes/api.php` is the application API boundary and `routes/web.php` only exposes minimal service metadata.

## Local setup

Requirements: PHP 8.4+, Composer and a running PostgreSQL server.

```bash
cd backend
composer install
cp .env.example .env          # set DB_DATABASE / DB_USERNAME / DB_PASSWORD
sudo -u postgres createdb atu_cafeteria   # once, if the DB does not exist yet
php artisan key:generate
php artisan migrate
php artisan serve --host=0.0.0.0 --port=8000
```

PostgreSQL is the primary database for both local development and production
(`DB_CONNECTION=pgsql`). Automated tests switch to SQLite automatically via
`phpunit.xml`, so `php artisan test` needs no extra database.

## Quality checks

```bash
php artisan test
./vendor/bin/pint --test
```

## Health

The API exposes `/api/health` for application monitoring and `/health` for
infrastructure checks.
