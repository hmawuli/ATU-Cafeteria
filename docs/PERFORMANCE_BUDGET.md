# Performance Budget

Reference numbers to keep every release fast. Run the load test the same way
each time and compare; treat a regression beyond the budgets as a release
blocker.

## Baseline command

```bash
make load            # defaults: http://127.0.0.1:8000/api/health, 20 concurrent, 200 req
scripts/load_test.sh https://api.example.com/api/catalog/menu-items 50 500
scripts/load_test.sh https://api.example.com/api/login 20 100
```

Use two representative endpoints:

| Endpoint | What it exercises | Budget |
|---|---|---|
| `GET /api/catalog/menu-items` | public read path + DB | p95 < 300 ms, > 100 req/s on reference host |
| `POST /api/login` | auth + token issue + rate limit | p95 < 800 ms, > 40 req/s |

General budget (any endpoint):

- **Error rate:** 0% 5xx in tests.
- **429s:** expected once the `auth` limiter (5/min per identity) is engaged — do not count as errors.
- **p95 latency:** within 1.5× of the previous release's p95.
- **Throughput:** within 20% of the previous release's throughput on the same host.

## What qualifies as a regression

- New DB query added to a hot path (catalogue, orders, wallet) without an index.
- N+1 loads in serialized JSON responses.
- Missing response caching (e.g. catalogue when it is public and static).
- Synchronous external calls (Paystack) inside ordinary request handling.

## When to act

- Exceeds a budget → add an index, cache the response, or paginate before
  merging the change.
- Suspicious but within budget → note it in the PR and re-run on the next
  release candidate.

## Notes for honesty

- Numbers depend on the host (1 vCPU vs 4 vCPU). Always re-baseline on the
  same machine for comparisons.
- The budget goes up as hardware improves; keep the *ratio* stable.
- Database size matters: re-run after migrations that grow the biggest tables.