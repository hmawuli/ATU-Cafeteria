# Deployment Guide: Hosting Laravel & PostgreSQL on Railway (Free Tier)

This guide provides step-by-step instructions for deploying your **ATU Cafeteria Backend (Laravel)** and **PostgreSQL database** to the **Railway** platform. It covers repository preparation, database setup, environment variables, automatic dependency installation, and running database migrations/seeders.

---

## Table of Contents
1. [Prerequisites](#1-prerequisites)
2. [Step 1: Prepare Your Laravel Repository](#step-1-prepare-your-laravel-repository)
3. [Step 2: Create a Railway Project and Add PostgreSQL](#step-2-create-a-railway-project-and-add-postgresql)
4. [Step 3: Connect Laravel to the PostgreSQL Database](#step-3-connect-laravel-to-the-postgresql-database)
5. [Step 4: Configure Laravel Environment Variables on Railway](#step-4-configure-laravel-environment-variables-on-railway)
6. [Step 5: Setup Automatic Database Migrations](#step-5-setup-automatic-database-migrations)
7. [Step 6: Deploy and Verify](#step-6-deploy-and-verify)
8. [Troubleshooting & Pro-Tips](#troubleshooting--pro-tips)

---

## 1. Prerequisites
- A [Railway Account](https://railway.app/) (linked to your GitHub account).
- A [GitHub Repository](https://github.com/) containing your Laravel backend code.
- A Gemini API Key (configured in the AI Studio Secrets panel).

---

## Step 1: Prepare Your Laravel Repository

Currently, your project contains custom directories (`app`, `database`, `routes`, `resources`, etc.) inside the `/laravel-backend` directory. To make this deployable, it must be structured as a fully functional, standard Laravel codebase.

### Preparing the Git Repository
1. On your local machine, create a fresh Laravel 11 project inside a folder:
   ```bash
   composer create-project laravel/laravel cafeteria-backend
   cd cafeteria-backend
   ```
2. Copy the customized directories from your AI Studio project into this new `cafeteria-backend` folder, overwriting the default ones:
   - `app/` (Controllers, Models, etc.)
   - `database/` (Migrations, Seeders)
   - `routes/` (API and web routes)
   - `resources/` (Views and assets)
3. Initialize git and push this repository to your GitHub account:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: ATU Cafeteria Backend"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
   git push -u origin main
   ```

---

## Step 2: Create a Railway Project and Add PostgreSQL

Railway makes provisioning database instances extremely easy:

1. Log into [Railway](https://railway.app/).
2. Click the **+ New Project** button in the top right.
3. Select **Provision PostgreSQL** from the dropdown menu.
4. Railway will create a new empty project and spin up a dedicated PostgreSQL database cluster within a few seconds.

---

## Step 3: Connect Laravel to the PostgreSQL Database

Railway has a powerful **Reference Variables** system. Rather than hardcoding database URLs, we can automatically reference variables exported by your PostgreSQL service inside your Laravel service.

1. Click on your newly created PostgreSQL database card in Railway.
2. Go to the **Variables** tab. You'll see variables like `DATABASE_URL`, `PGPASSWORD`, `PGPORT`, `PGUSER`, etc.
3. Under your project, click **+ New** (or **+ Add Service**) to add a web service.
4. Select **GitHub Repo** and choose the repository you pushed in **Step 1**.

---

## Step 4: Configure Laravel Environment Variables on Railway

Click on your **Laravel Web Service** card, navigate to the **Variables** tab, and add the following keys.

Using Railway's variable replacement syntax (e.g., `${{Postgres.PGHOST}}`), your database connection is automatically kept in sync even if Railway moves your database instance.

### Core Laravel Variables
| Variable Key | Suggested Value / Source | Description |
| :--- | :--- | :--- |
| `APP_NAME` | `"ATU Cafeteria Backend"` | Your application name |
| `APP_ENV` | `production` | Ensures strict security and optimizations |
| `APP_KEY` | `base64:YOUR_GENERATED_KEY...` | Run `php artisan key:generate --show` locally to obtain one |
| `APP_DEBUG` | `false` | Disable details on errors in production |
| `APP_URL` | `${{RAILWAY_PUBLIC_DOMAIN}}` | Automatically binds your Railway assigned URL |
| `LOG_CHANNEL` | `stderr` | Required on cloud platforms so logs show in Railway Dashboard |

### PostgreSQL Database Connection Variables
Use these exact reference values to bind Laravel with your PostgreSQL database:

| Variable Key | Railway Reference Value |
| :--- | :--- |
| `DB_CONNECTION` | `pgsql` |
| `DB_HOST` | `${{Postgres.PGHOST}}` |
| `DB_PORT` | `${{Postgres.PGPORT}}` |
| `DB_DATABASE` | `${{Postgres.PGDATABASE}}` |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |

### Third-Party APIs
| Variable Key | Value / Source | Description |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | `AI_STUDIO_GEMINI_KEY` | Your Google AI Studio API key |

---

## Step 5: Setup Automatic Database Migrations

By default, Railway uses **Nixpacks** to analyze, build, and deploy PHP apps. To automatically run your migrations during every deployment, we configure a custom deployment build step.

Create a file named `railway.json` in the **root** of your GitHub repository with the following content:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS"
  },
  "deploy": {
    "numReplicas": 1,
    "startCommand": "php artisan migrate --force && php artisan db:seed --force && apache2-foreground || php artisan serve --host 0.0.0.0 --port $PORT"
  }
}
```

### What this does:
1. `php artisan migrate --force`: Automatically runs new migrations on deployment without interactive prompts.
2. `php artisan db:seed --force`: Seeds the database (e.g., with default vendors, menus, and roles) on deployment.
3. Starts the PHP web server on the port provided dynamically by Railway (`$PORT`).

---

## Step 6: Deploy and Verify

1. Once you save the `railway.json` and push it to GitHub, Railway will automatically trigger a new build.
2. Watch the progress in the **Deployments** tab.
3. Once completed, Railway will generate a public URL for your backend (e.g. `https://your-app-production.up.railway.app`).
4. To verify the API is running, open the URL in your browser or run:
   ```bash
   curl https://your-app-production.up.railway.app/api/student/budget/analytics
   ```
   *(It should return a `419` or `401 Unauthenticated` JSON response, confirming that the routing is up and active).*

---

## Troubleshooting & Pro-Tips

### 1. Database Migrations Fail
If your build fails at the migration step:
* Double-check your variables. Ensure you are referencing `Postgres` exactly as named in your Railway canvas (if your database card is named `PostgreSQL`, use `${{PostgreSQL.PGHOST}}`).
* Look at the **Deploy Logs** on the Railway dashboard for the exact PHP/PDO database error.

### 2. Custom Web Root
Nixpacks automatically detects Laravel and sets the document root to `/public`. If your public assets or routes are not loading, verify that your repository does not have nested folders, and that `public/index.php` is in the root directory.

### 3. Railway Free Tier Limits
* Railway provides **500 hours** of free execution time and **$5.00** credit per month for new verified accounts.
* Your services will sleep or pause if you run out of credits, which is perfect for development testing and school projects.
