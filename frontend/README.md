# ATU Cafeteria — Flutter Frontend

This directory contains the **only active user interface** for the ATU Cafeteria system.

## Structure

```text
lib/
├── core/
│   ├── config/
│   └── theme/
├── data/
│   ├── local/
│   └── remote/
├── domain/
│   └── models/
├── presentation/
│   ├── providers/
│   ├── screens/
│   └── widgets/
└── main.dart
```

## Dependencies

- Provider — application state
- HTTP — API adapter
- SQLite — local persistence/cache
- Crypto — PIN hashing compatibility with Laravel
- Intl — date/number formatting

No React, Vue, WebView, D3 or JavaScript frontend is required.
