# Observability

How the ATU Cafeteria platform is monitored, logged, alerted and kept
operational.

## Health endpoints

| Endpoint | Purpose | Expected |
|---|---|---|
| `GET /health` | Liveness (Laravel) | `200` JSON `{app, status: "Healthy"}` |
| `GET /api/health` | API readiness | `200` JSON `{"status":"healthy",...}` |
| `GET /api/docs/openapi.json` | Contract served | `200` valid OpenAPI JSON |

Use the smoke test to verify all of them at once:

```bash
make smoke          # scripts/smoke_test.sh (default http://127.0.0.1:8000)
scripts/smoke_test.sh https://api.example.com
```

## Backend logs

- Laravel writes to `backend/storage/logs/laravel.log` via the `stack`
  channel. Control verbosity with `LOG_LEVEL` (`error`, `info`, `debug`).
- Errors are also persisted to the `system_logs` table by
  `SystemErrorLoggerMiddleware` (level, status code, method, path, message,
  sanitised stack trace).
- Audit/forensic events live in the `audit_logs` table (retain per regulatory
  guidance; back up with the database).

## Docker logs

```bash
docker compose logs -f app        # PHP-FPM
docker compose logs -f worker     # queue worker
docker compose logs -f scheduler  # Laravel scheduler
docker compose logs -f nginx      # web access/error logs
docker compose logs -f db         # PostgreSQL
```

To bound disk usage, set log rotation on the daemon (e.g.
`/etc/docker/daemon.json`):

```json
{ "log-driver": "json-file", "log-opts": { "max-size": "20m", "max-file": "5" } }
```

## Monitoring & alerting

- Register an uptime monitor (UptimeRobot / Pingdom / cron + health script)
  against `https://<host>/api/health` with a 1-minute interval; alert on
  non-200 or timeout > 5s.
- Alert on 5xx spikes and on `failed_jobs` rows (queue health):
  ```sql
  SELECT count(*) FROM failed_jobs WHERE failed_at > now() - interval '1 hour';
  ```
- Watch disk, memory and PostgreSQL connection count on the host.
- Send a daily `pg_dump` to another machine: `make db-backup` (Docker stack
  or local). Restore drill at least monthly.

## Flutter crash reporting

`CrashReporting` (Firebase Crashlytics) is wired into `main()` and safely
skips when Firebase is not configured. Enable it at build time:

```bash
flutter build apk --release \
  --dart-define=ATU_FIREBASE_API_KEY=... \
  --dart-define=ATU_FIREBASE_APP_ID=... \
  --dart-define=ATU_FIREBASE_MESSAGING_SENDER_ID=... \
  --dart-define=ATU_FIREBASE_PROJECT_ID=...
```

The same `--dart-define` values are used by FCM
(`PushNotificationService`). Without them both services no-op silently.
Review crash reports in the Firebase console (Crashlytics) grouped by release.

## Runbooks at a glance

| Situation | Action |
|---|---|
| `GET /api/health` failing | Check `docker compose ps`, `docker compose logs app`, DB reachability (`make db-backup` exercises the connection) |
| Queue backlog | `docker compose logs worker`; restart with `docker compose restart worker`; check `failed_jobs` |
| Scheduler missed runs | `docker compose logs scheduler`; confirm `schedule:work` is up |
| Disk filling | Docker log rotation above + `du -sh storage/logs`, prune old DB dumps |
| Under attack / abuse | Rate limits are per `auth`, `api`, `payments` — inspect `audit_logs` for `AUTH_FAILURE` bursts |