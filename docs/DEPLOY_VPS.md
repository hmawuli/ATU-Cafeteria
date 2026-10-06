# Deploying the ATU Cafeteria API to a VPS

Generic Linux server deployment (Ubuntu 22.04/24.04) using Nginx + PHP-FPM +
PostgreSQL. Use this when you are not using Docker (see `docker-compose.yml`
and `docker compose up -d --build` for the containerised alternative).

| Component | Stack |
|---|---|
| Web server | Nginx |
| Application | Laravel 12 / PHP 8.4 (PHP-FPM) |
| Database | PostgreSQL 14+ |
| TLS | Let's Encrypt (certbot) |
| Queue/cache | Database queue, file cache (adjust per scale) |

## 1. Provision the server

- Ubuntu 22.04/24.04 LTS, at least 1 vCPU / 1 GB RAM (2 GB recommended).
- Open ports: `22` (SSH), `80`, `443`.
- Point a DNS `A` record (`api.example.com`) at the server IP.

## 2. Install base packages

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y nginx postgresql composer git curl zip unzip

# PHP 8.4 (Ubuntu 24.04 ships 8.3; add the Ondřej PHP PPA for 8.4)
sudo add-apt-repository -y ppa:ondrej/php && sudo apt update
sudo apt install -y php8.4-fpm php8.4-cli php8.4-pgsql \
  php8.4-mbstring php8.4-xml php8.4-bcmath php8.4-curl php8.4-sqlite3 \
  php8.4-intl php8.4-gd
```

Verify: `php -v` and `psql --version`.

## 3. Create the PostgreSQL database

```bash
sudo -u postgres psql

# Inside the psql shell:
CREATE USER atu WITH PASSWORD '<strong-password>';
CREATE DATABASE atu_cafeteria OWNER atu;
\q
```

Or, as the `postgres` OS user:

```bash
sudo -u postgres createuser -P atu          # enter the password when prompted
sudo -u postgres createdb -O atu atu_cafeteria
```

## 4. Deploy the code

```bash
sudo mkdir -p /var/www/atu-cafeteria
sudo chown -R "$USER":"$USER" /var/www/atu-cafeteria
cd /var/www/atu-cafeteria
git clone <your-repository-url> .   # or a release tag/branch
cd backend
composer install --no-dev --no-interaction --prefer-dist --optimize-autoloader
```

## 5. Configure the environment

```bash
cp .env.example .env
php artisan key:generate
```

Then edit `.env`:

```env
APP_ENV=production
APP_DEBUG=false
APP_URL=https://api.example.com
APP_TIMEZONE=Africa/Accra

DB_CONNECTION=pgsql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=atu_cafeteria
DB_USERNAME=atu
DB_PASSWORD=<strong-password>

QUEUE_CONNECTION=database        # production queue (see step 8)
SESSION_DRIVER=file
CACHE_STORE=file

PAYSTACK_SECRET_KEY=<production secret>
PAYSTACK_DEMO_MODE=false
CORS_ALLOWED_ORIGINS=https://your-frontend.com
```

Never commit this file. Keep secrets only in the server's `.env`.

## 6. Migrate and prepare Laravel

```bash
cd /var/www/atu-cafeteria/backend
php artisan migrate --force
php artisan config:cache
php artisan route:cache
php artisan view:cache
php artisan storage:link

sudo chown -R www-data:www-data storage bootstrap/cache
```

> DatabaseSeeder is intentionally empty. Load development data only with the
> guarded seeders (`MenuCategoryAndVendorSeeder`,
> `DevelopmentRestaurantCatalogSeeder`) — never in production.

## 7. Configure Nginx

Create `/etc/nginx/sites-available/atu-cafeteria`:

```nginx
server {
    listen 80;
    server_name api.example.com;
    root /var/www/atu-cafeteria/backend/public;
    index index.php;

    charset utf-8;
    client_max_body_size 20M;

    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    location ~ \.php$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:/run/php/php8.2-fpm.sock;
    }

    location ~ /\.(?!well-known).* { deny all; }
    location ~ /\.ht { deny all; }
}
```

Enable and test:

```bash
sudo ln -s /etc/nginx/sites-available/atu-cafeteria /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

## 8. Production queue and scheduler

```bash
php artisan queue:table
php artisan migrate --force
```

Add a systemd unit `/etc/systemd/system/atu-queue.service`:

```ini
[Unit]
Description=ATU Cafeteria queue worker
After=network.target

[Service]
User=www-data
Group=www-data
WorkingDirectory=/var/www/atu-cafeteria/backend
ExecStart=/usr/bin/php artisan queue:work --sleep=3 --tries=3
Restart=always

[Install]
WantedBy=multi-user.target
```

And a cron entry (every minute) for Laravel's scheduler:

```cron
* * * * * cd /var/www/atu-cafeteria/backend && php artisan schedule:run >> /dev/null 2>&1
```

```bash
sudo systemctl enable --now atu-queue
```

## 9. HTTPS with Let's Encrypt

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.example.com
```

Certbot updates the server block to listen on 443 and keeps certificates
renewed automatically.

## 10. Verify

- `https://api.example.com/health` → `{"app":"ATU Cafeteria API","status":"Healthy","framework":"Laravel 12"}`.
- `https://api.example.com/api/health` → API health JSON.
- `https://api.example.com/api/docs` → interactive OpenAPI docs.
- Point the Flutter app at the API with
  `flutter run --dart-define=API_BASE_URL=https://api.example.com/api/` and
  exercise login, catalog, ordering and wallet flows.

## Backups

Daily encrypted PostgreSQL dump:

```bash
# /etc/cron.d/atu-backup
15 2 * * * root pg_dump -U atu atu_cafeteria | gzip > /var/backups/atu/atu_$(date +\%F).sql.gz
```

Test restoration (`gunzip < ... | psql -U atu atu_cafeteria`) at least
monthly. Keep `.env`, keystores and backups out of git.

## Security checklist

- `APP_DEBUG=false`, `APP_ENV=production`, HTTPS enforced.
- Paystack secrets server-side only, `PAYSTACK_DEMO_MODE=false`.
- `storage/` and `bootstrap/cache` owned by `www-data`.
- Fail2ban or UFW restricting SSH; disable password auth with key-only SSH.
- Dependency audits in CI (`composer audit`) plus scheduled OS updates.