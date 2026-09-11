# Flutter Frontend Architecture

The ATU Cafeteria system has a single active frontend: **Flutter**.

```text
frontend/lib
├── core
│   ├── config
│   └── theme
├── data
│   ├── local
│   └── remote
├── domain
│   └── models
├── presentation
│   ├── providers
│   ├── screens
│   └── widgets
└── main.dart
```

Laravel is the backend/API. No Kotlin, Jetpack Compose, React, Vue, Blade or PWA client is required to operate the application.
