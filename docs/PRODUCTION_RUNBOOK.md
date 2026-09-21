# Production-Level Runbook

## First setup on Linux

```bash
./scripts/setup_4gb_linux.sh
```

The script creates the Android platform files with Flutter, installs Dart dependencies, prepares Laravel and generates the application key.

## Start backend

```bash
cd backend
php artisan migrate --seed
php artisan serve --host=0.0.0.0 --port=8001
```

## Start Flutter

For an Android emulator:

```bash
cd frontend
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8001/api/
```

For a physical phone, replace the host with the development computer's LAN IP:

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.100:8001/api/
```

## Release checklist

- `APP_ENV=production`
- `APP_DEBUG=false`
- configure a managed production database
- set `APP_KEY`
- restrict `CORS_ALLOWED_ORIGINS` to trusted browser origins
- run `php artisan migrate --force`
- run `php artisan config:cache`
- run `php artisan route:cache`
- run `php artisan view:cache` only if Blade views are introduced again
- use HTTPS
- verify `/api/health`
- run backend tests before deployment
- build Flutter with the production API endpoint
