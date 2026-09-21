# 🚀 Deploying the ATU Cafeteria Laravel API to InfinityFree

Target domain: **`https://atucafeteria.free.nf`** · Deploy type: **API only**

Three files were prepared for you:

| File | What it is |
|---|---|
| `atu_cafeteria_webroot.zip` | The complete Laravel API, pre-arranged for InfinityFree (upload → unzip) |
| `atu_cafeteria.sql` | Database schema **+ demo seed data** (import in phpMyAdmin) |
| `DEPLOY_INFINITYFREE.md` | This guide |

The mobile Flutter app stays as-is. In the app's **login screen → API server settings**,
enter `https://atucafeteria.free.nf` once — no rebuild needed.

---

## Step 1 — Control panel (hPanel) prep *(2 minutes)*

1. Log into [https://dash.infinityfree.com/accounts](https://dash.infinityfree.com/accounts).
2. **PHP version** → in the row for `atucafeteria.free.nf`, click *Manage* → *PHP version*
   and select **PHP 8.2** (or the highest version available on your account) → *Save*.
3. **SSL** → *Manage* → *SSL Certificates* → enable the **free SSL certificate** for
   the domain (Let’s Encrypt). ⚠️ The API *requires* HTTPS (it rejects plain HTTP).
4. **MySQL databases** → *MySQL Databases* → **Create Database**.
   - Database name: `atu_cafeteria` (it becomes `if0_XXXXXXX_atu_cafeteria`).
   - Set a **password** and write it down.
   - Note the **MySQL Host** shown there (e.g. `sql123.infinityfree.com`), the full
     **database name** (`if0_XXXXXXX_atu_cafeteria`) and **username** (`if0_XXXXXXX`).

## Step 2 — Upload the API *(5–10 minutes, do once)*

1. In hPanel open the **File Manager** for `atucafeteria.free.nf` (it points at `htdocs/`).
2. Upload **`atu_cafeteria_webroot.zip`** into `htdocs/`.
3. Click the file → **Extract** (or *Extract Archive*). You should now see in `htdocs/`:
   ```
   htdocs/
   ├── .htaccess          ← routes all requests to the app
   ├── index.php          ← Laravel front controller
   └── laravel-app/       ← the whole Laravel app (locked with .htaccess)
   ```
4. Open `htdocs/laravel-app/` → edit **`.env`** and fill in the **4 database lines**
   with what you noted in Step 1.4:
   ```
   DB_HOST=sqlXXX.infinityfree.com      ← the MySQL Host from hPanel
   DB_DATABASE=if0_XXXXXXX_atu_cafeteria
   DB_USERNAME=if0_XXXXXXX
   DB_PASSWORD=your_db_password
   ```
   Nothing else in `.env` needs to change. (Keep `APP_DEBUG=false`!)

## Step 3 — Import the database *(2 minutes)*

1. In hPanel → **phpMyAdmin** (it opens pre-selected to your account database).
2. Select the **`if0_XXXXXXX_atu_cafeteria`** database on the left.
3. Top tab **Import** → *Choose File* → pick **`atu_cafeteria.sql`** → **Go**.
4. You should see **35 tables** appear in the list on the left.

## Step 4 — Verify *(2 minutes)*

Open in a browser:

- `https://atucafeteria.free.nf/api/health`
  → expect `{"status":"UP", ... "database": {"status":"UP", "driver":"mysql"}}`
- Login test:
  ```bash
  curl -X POST https://atucafeteria.free.nf/api/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","pin":"admin123"}'
  ```
  → expect `"success":true` and the admin user with a `token`.

## Step 5 — Point the Flutter app at it

1. Open the app.
2. **Login screen → “API server settings”** (link under the *Secure ATU Cafeteria access* row).
3. Enter **`https://atucafeteria.free.nf`** → **Save**.
4. Sign in with demo accounts:
   - Admin `admin / admin123`
   - Vendor `maryjoint / 1111`, `atkitch / 2222`, `snackbag / 3333`
   - Students: PIN `1234` (username `student` / `student2`)

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Secure HTTPS transport is required` | You opened it over `http://`. Always use `https://`. SSL must be enabled (Step 1.3). |
| `500` / blank page on the API | Check `htdocs/laravel-app/storage/logs/laravel.log`. Most common cause: DB credentials in `.env` (Step 2.4). |
| Blue/error page on the website root | The API intentionally returns JSON only; visiting `/` in a browser isn’t a “site” — use `/api/health`. |
| Database import errors | Make sure you imported into the correct database *in phpMyAdmin*, and the file is `atu_cafeteria.sql` (not the ZIP). |
| Storage permission warnings | In File Manager set 755 (or 775) on `htdocs/laravel-app/storage/` and `htdocs/laravel-app/bootstrap/cache/`. |

## Notes / limits

- InfinityFree has **no SSH / Composer / terminal** — that’s why everything is pre-built
  here and the DB is imported via SQL.
- The app needs **PHP 8.2+** (Step 1.2) because Laravel 11 requires it.
- Sessions, cache and logs use the `file` driver → they live in `laravel-app/storage`
  and are shared-hosting friendly (no background queue needed; `QUEUE_CONNECTION=sync`).
- Security: `laravel-app/.htaccess` denies all web access to the source and `.env`.
- ⚠️ **Known advisory:** Laravel 11.x has CVE-2026-48019 (CRLF injection in an email
  validation rule, patched in 12.60+). It does not affect this mailer (`MAIL_MAILER=log`)
  but worth noting for a real production release.
- Redeploys: replace files in `htdocs/` (upload + extract over it). The database on
  InfinityFree keeps its data — don’t re-import the SQL unless you want a fresh reset.