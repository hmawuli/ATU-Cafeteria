# Running ATU Cafeteria on a 4 GB Linux laptop

## First setup

From the project root:

```bash
chmod +x scripts/setup_4gb_linux.sh
./scripts/setup_4gb_linux.sh
make seed-dev    # migrate + seed development vendors and restaurant catalog
```

This installs dependencies, prepares Laravel, and activates the repo git
hooks (`.githooks/`).

## Daily run

Terminal 1:

```bash
make serve         # Laravel on 0.0.0.0:8000 (or: cd backend && php artisan serve)
```

Terminal 2:

```bash
cd frontend
flutter run
```

Connect a physical Android phone with USB debugging enabled. Do **not** start an Android emulator on a 4 GB machine.

## If the laptop becomes slow

Close Chrome tabs and other applications, stop Laravel when it is not needed, and use a physical phone. Avoid running Android Studio and VS Code simultaneously.

## Release validation

Use the normal release build when you need to validate the Android production configuration:

```bash
cd frontend
flutter build apk --release
flutter build appbundle --release
```

For actual distribution, use the protected production signing workflow rather than committing a keystore to the repository.
