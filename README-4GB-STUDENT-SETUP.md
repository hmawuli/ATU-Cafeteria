# ATU Cafeteria — 4GB RAM Student Setup

This profile is designed for a laptop with 4GB RAM. The goal is to develop and test the project without running Android Studio, an Android emulator, Flutter, Laravel, and VS Code all at once.

## Recommended setup

Use **VS Code + a real Android phone**. Avoid the Android emulator on a 4GB machine.

### Flutter

```bash
cd flutter_project
flutter clean
flutter pub get
flutter analyze
flutter run
```

Connect a physical Android phone with USB debugging enabled. This avoids the large RAM cost of an emulator.

### Laravel API

Use one terminal only:

```bash
cd laravel-backend
php artisan serve --host=0.0.0.0 --port=8000
```

Do not start unnecessary workers, queues, Vite, Docker, or database GUI tools during normal development.

## 4GB rules

1. Do not run Android Studio and an emulator together with VS Code.
2. Use a physical Android phone for Flutter testing.
3. Run only the service you are currently testing.
4. Keep `node_modules`, `vendor`, `.dart_tool`, `build`, and `.gradle` out of VS Code search/watching.
5. Close browser tabs and other heavy applications before an Android build.
6. Prefer a debug build during development; make a release build only when preparing the final APK.
7. If Gradle becomes stuck, close VS Code/Android tools and retry after the machine has recovered.

## Important

The repository contains multiple clients/backends. You do **not** need to run all of them simultaneously to demonstrate the project. Start with the Laravel API and Flutter app, then test the web/admin client separately.

Never commit `.env`, passwords, API keys, payment secrets, or other credentials.
