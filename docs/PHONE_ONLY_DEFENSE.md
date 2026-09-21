# ATU Cafeteria — Phone-Only Project Defense

The repository contains a dedicated offline defense build. It does **not** require Laravel, PHP, Wi-Fi, mobile data, or a laptop at runtime.

## Build

From `frontend/`:

```bash
flutter pub get
flutter analyze
flutter build apk --release --dart-define=ATU_DEFENSE_MODE=true
```

APK output:

```text
frontend/build/app/outputs/flutter-apk/app-release.apk
```

The GitHub Actions workflow `.github/workflows/defense-apk.yml` also builds and uploads the APK as the `ATU-Cafeteria-Defense-APK` artifact.

## Defense accounts

| Role | Username | PIN |
|---|---|---|
| Student | `student` | `1234` |
| Vendor | `maryjoint` | `1111` |
| Admin | `admin` | `admin123` |

A second student (`student2` / `1234`) and second vendor (`atkitch` / `2222`) are also seeded locally.

## Demonstration flow

1. Install `ATU-Cafeteria-Defense.apk` on the Android phone.
2. Turn off Wi-Fi and mobile data.
3. Open the app.
4. Sign in as the student.
5. Select food and place an order. The wallet is debited in the local SQLite transaction.
6. Sign out and enter the vendor account.
7. Advance the order through `ORDER_PLACED → PREPARING → READY_FOR_PICKUP → COMPLETED`.
8. Sign back in as the student and review the completed order.
9. Enter the admin account to demonstrate users, orders, reviews and audit activity.

## Architecture

The normal build remains Flutter → Laravel API. The defense build uses the same Flutter application entry point but switches to a local API-shaped service backed by SQLite. This keeps the defense independent of the network while leaving the Laravel production path intact.

The offline data layer stores users, food items, orders, wallet ledger entries, reviews and audit logs locally on the phone.
