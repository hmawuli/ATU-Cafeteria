# ATU Cafeteria — Project Defense Guide

## 1. One-minute project description

> **ATU Cafeteria is a mobile food-ordering and cafeteria management system built with Flutter on the client side and Laravel 11 as the REST API backend. Students can browse meals, place and track orders, manage wallet transactions and submit feedback. Vendors manage menus, orders, pickup verification and performance analytics, while administrators manage users, audit records and system reporting.**

## 2. Technology stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | Flutter / Dart | Cross-platform mobile UI |
| State | Provider | Reactive application state |
| Local data | SQLite | Cache/offline resilience |
| Backend | Laravel 11 / PHP | Business logic and REST API |
| Database | SQLite locally; PostgreSQL/MySQL in production | Persistent source of truth |
| Authentication | Bearer/JWT-compatible API token | Secure API access |
| Payments | Paystack | Digital payment integration |

## 3. Architecture

```text
Student / Vendor / Admin
          │
          ▼
     Flutter App
  ┌───────────────┐
  │ Presentation  │
  │ Domain Models │
  │ Data Layer    │
  └───────┬───────┘
          │ HTTPS + JSON
          ▼
     Laravel REST API
          │
          ▼
      Database
```

## 4. Why Flutter + Laravel?

- Flutter gives one maintainable mobile codebase.
- Laravel centralizes security, validation and business rules.
- The API/database remains authoritative, so users do not depend on device-local data.
- SQLite provides resilience when connectivity is temporarily unavailable.
- The architecture is lightweight enough for a 4 GB RAM development machine.

## 5. Security points to explain

1. Authentication is performed by the Laravel backend.
2. Role authorization is enforced server-side.
3. Bearer tokens protect authenticated requests.
4. PIN material is never stored as plain text by the Flutter cache.
5. Payment and AI secrets are server-side environment variables.
6. Audit logs provide traceability for important operations.

## 6. Performance points

- No Android emulator is required during development.
- No Node/React/Vue browser build is required.
- Charts use native Flutter rendering instead of a WebView JavaScript engine.
- Gradle is restricted to one worker with a 768 MB build heap for low-memory machines.
- Generated build/cache directories are excluded from version control.

## 7. Recommended live demonstration

1. Start Laravel API.
2. Start Flutter on a physical Android phone.
3. Log in as a student.
4. Browse a meal and place an order.
5. Show the order in the vendor dashboard.
6. Change the order status.
7. Demonstrate pickup PIN verification.
8. Return to the student and show the updated status.
9. Show feedback/ratings and vendor analytics.
10. Log in as administrator and demonstrate audit/report controls.

## 8. If asked why the system is not a web application

> The current project intentionally uses Flutter as the sole frontend. Laravel is the backend API. This reduces duplicate UI implementations, lowers maintenance cost and gives the project a clear production architecture.
