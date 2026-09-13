# ATU Cafeteria — Production Architecture

## System boundary

The system has one user interface and one backend service:

```text
Flutter Mobile App
       |
       | HTTPS/JSON + Bearer token
       v
Laravel 11 REST API
       |
       v
Database / integrations
```

The old Android/Kotlin, Blade, PWA and JavaScript browser interfaces are not part of the active application architecture.

## Flutter layers

```text
lib/
├── core/          # configuration and theme
├── data/          # local persistence and remote API adapters
├── domain/        # framework-independent business models
├── presentation/  # providers, screens and reusable widgets
└── main.dart      # composition root
```

### Rules

1. Domain models must not import Flutter widgets.
2. Data code owns persistence/network concerns.
3. Presentation code owns UI state and user interaction.
4. API URLs are environment-driven with `--dart-define`.
5. SQLite is a local cache/offline fallback, not the production source of truth.
6. Laravel is the authoritative source for accounts, orders, payments and audit data.

## Laravel layers

```text
app/
├── Http/Controllers/Api/  # API endpoints
├── Http/Requests/         # validation
├── Http/Resources/        # response shaping
├── Models/                # persistence
├── Services/              # business/integration logic
├── Events/                # domain events
├── Listeners/             # side effects
└── Notifications/        # user/system notifications
```

## Security boundary

- Authentication is performed by Laravel.
- The Flutter client sends a SHA-256 PIN representation required by the existing backend contract.
- Authenticated endpoints use the returned bearer token.
- Role checks remain server-side; the Flutter role only controls navigation/UI.

## Performance boundary

For the 4 GB RAM development machine:

- no Android emulator is required;
- no WebView chart engine is required;
- no Node/React/Vue build is required;
- Gradle workers are limited to one;
- the Android build heap is capped at 768 MB;
- SQLite is used locally;
- generated build/cache folders are excluded from Git.
