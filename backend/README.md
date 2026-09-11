# ATU Cafeteria — Laravel API

Laravel 11 backend for the ATU Cafeteria Flutter application.

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

```bash
cd backend
composer install
cp .env.example .env
mkdir -p database
touch database/database.sqlite
php artisan key:generate
php artisan migrate --seed
php artisan serve --host=0.0.0.0 --port=8000
```

SQLite is the recommended local database for the 4 GB development machine. Use PostgreSQL/MySQL in production according to the deployment environment.

## Quality checks

```bash
php artisan test
./vendor/bin/pint --test
```

## Health

The API exposes `/api/health` for application monitoring and Laravel's `/up` health endpoint.
