# Smooth Setup on a 4 GB RAM Linux Computer

## Recommended tools

- VS Code
- Flutter SDK
- PHP 8.2+
- Composer
- A physical Android phone

Avoid running Android Studio and an Android emulator simultaneously on a 4 GB machine.

## One-time setup

From the project root:

```bash
./scripts/setup_4gb_linux.sh
```

If the script is not executable:

```bash
chmod +x scripts/setup_4gb_linux.sh
./scripts/setup_4gb_linux.sh
```

## Start Laravel

```bash
cd backend
php artisan migrate --seed
php artisan serve --host=0.0.0.0 --port=8000
```

Keep this terminal open.

## Start Flutter

For Android emulator:

```bash
cd frontend
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8000/api/
```

For a physical phone, use the computer's LAN IP instead of `10.0.2.2`.

## Low-memory rules

- Run one major build operation at a time.
- Close browsers/tabs you do not need.
- Prefer a physical phone over an emulator.
- Do not run `flutter clean` repeatedly; it forces a full rebuild.
- Do not commit `.dart_tool`, `build`, `vendor` or SQLite runtime files.

## Troubleshooting

### Flutter command not found

Install Flutter and add its `bin` directory to PATH, then reopen the terminal.

### Android platform files are missing

Run:

```bash
cd frontend
flutter create . --platforms=android --no-pub
flutter pub get
```

### Phone cannot reach Laravel

Find the development computer's LAN IP and run Flutter with:

```bash
flutter run --dart-define=API_BASE_URL=http://YOUR_LAN_IP:8000/api/
```

Also make sure Laravel is started with `--host=0.0.0.0`.
