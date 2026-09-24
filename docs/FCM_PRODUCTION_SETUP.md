# FCM production setup

ATU Cafeteria now registers customer devices with the Laravel notification service and sends real Firebase Cloud Messaging pushes for order-ready and completed events.

## Configure the Flutter app

Provide the following build-time values. Do not commit service-account credentials, private keys, or secrets:

- `ATU_FIREBASE_API_KEY`
- `ATU_FIREBASE_APP_ID`
- `ATU_FIREBASE_MESSAGING_SENDER_ID`
- `ATU_FIREBASE_PROJECT_ID`
- `ATU_FIREBASE_STORAGE_BUCKET` (optional)
- `ATU_APP_VERSION` (optional; defaults to the pubspec version)

Example Android release build:

```bash
flutter build appbundle --release \
  --dart-define=ATU_FIREBASE_API_KEY=... \
  --dart-define=ATU_FIREBASE_APP_ID=... \
  --dart-define=ATU_FIREBASE_MESSAGING_SENDER_ID=... \
  --dart-define=ATU_FIREBASE_PROJECT_ID=... \
  --dart-define=ATU_FIREBASE_STORAGE_BUCKET=... \
  --dart-define=ATU_APP_VERSION=1.1.0
```

Android 13+ notification permission is declared in the application manifest and runtime permission is requested by the push service.

## Configure the Laravel server

Set these production environment values:

```env
FCM_PROJECT_ID=your-project-id
FCM_ACCESS_TOKEN=short-lived-oauth-access-token
```

The repository deliberately does not create fake push delivery when Firebase is unavailable. A missing or invalid token is logged and the app continues using the in-app notification center.

## Operational requirements

Generate Firebase credentials in the Firebase/Google Cloud project that owns the application, restrict service-account access, and rotate the FCM access token using your production secret manager.

On iOS, enable Push Notifications and Background Modes > Remote notifications for the Runner target and configure APNs in Firebase before release.

## What is already implemented

- Authenticated customers register an installation and push token.
- Token refresh re-registers the current installation.
- Logout revokes that installation for the current customer.
- Laravel sends real FCM pushes when a customer order becomes READY or COMPLETED.
- The existing database notification center remains available when push delivery is unavailable.