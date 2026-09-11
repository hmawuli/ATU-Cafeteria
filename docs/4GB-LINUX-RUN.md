# Running ATU Cafeteria on a 4 GB Linux laptop

## First setup

From the project root:

```bash
chmod +x scripts/setup_4gb_linux.sh
./scripts/setup_4gb_linux.sh
```

## Daily run

Terminal 1:

```bash
cd backend
php artisan serve
```

Terminal 2:

```bash
cd frontend
flutter run
```

Connect a physical Android phone with USB debugging enabled. Do **not** start an Android emulator on a 4 GB machine.

## If the laptop becomes slow

Close Chrome tabs and other applications, stop Laravel when it is not needed, and use a physical phone. Avoid running Android Studio and VS Code simultaneously.

For the defense, prefer:

```bash
flutter run --release
```

only after the normal build works. Release builds can take longer to compile, so do not rebuild unnecessarily immediately before the presentation.
