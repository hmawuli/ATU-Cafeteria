# ATU Cafeteria — Final Defense Checklist

## Student journey

- [ ] Register a student account.
- [ ] Sign in and restore the session after restarting the app.
- [ ] Load Today's Menu from Laravel.
- [ ] Confirm unavailable meals cannot be ordered.
- [ ] Confirm duplicate food names are not repeated in the menu.
- [ ] Open food details and add an item to the cart.
- [ ] Change quantity and confirm totals update.
- [ ] Checkout with the cafeteria wallet.
- [ ] Confirm insufficient wallet balance is rejected without creating an order.
- [ ] Confirm a successful checkout creates the order and clears the cart.
- [ ] Open My Orders and confirm the new order appears.
- [ ] Open live order tracking.
- [ ] Confirm the pickup PIN is displayed when the order is ready.
- [ ] Complete the order through the vendor workflow.
- [ ] Submit a vendor review after completion.

## Wallet and payment

- [ ] Open Profile → Top Up Wallet.
- [ ] Enter a valid amount.
- [ ] Confirm Paystack initialization returns a reference.
- [ ] In demo mode, verify the simulated transaction and confirm the wallet is credited.
- [ ] With a real Paystack key, open the authorization URL and complete Mobile Money/Card checkout.
- [ ] Verify the transaction through Laravel before crediting the wallet.
- [ ] Repeat verification and confirm the wallet is not credited twice.
- [ ] Confirm direct online order payment does not also debit the cafeteria wallet.

## Vendor journey

- [ ] Sign in as a vendor.
- [ ] Confirm only that vendor's orders are displayed.
- [ ] Receive a newly placed student order.
- [ ] Move PENDING/ORDER_PLACED → PREPARING.
- [ ] Move PREPARING → READY.
- [ ] Verify the student's 4-digit pickup PIN.
- [ ] Confirm READY → COMPLETED.
- [ ] Confirm the student receives the order-status notification.

## Admin journey

- [ ] Sign in as an administrator.
- [ ] Review dashboard metrics.
- [ ] Create a vendor account.
- [ ] Suspend/reactivate users or vendors where permitted.
- [ ] Review orders and finance summary.
- [ ] Review audit logs and security alerts.
- [ ] Confirm admin-only endpoints reject non-admin users.

## Local quality gates

From the repository root:

```bash
cd backend
php artisan test
./vendor/bin/pint --test

cd ../frontend
flutter pub get
flutter analyze
flutter test
flutter build apk --release
```

## Physical Android phone

For a phone on the same network as the development laptop, start Laravel with:

```bash
cd backend
php artisan serve --host=0.0.0.0 --port=8001
```

Use the laptop's LAN address in the Flutter API configuration. Verify connectivity before testing login:

```bash
curl -I http://<LAPTOP-LAN-IP>:8001
```

## Release note

The Android application ID is now `com.atu.cafeteria`. The repository intentionally keeps debug signing for local/defense builds. A production release requires a real Android keystore and secure production API/Paystack configuration.
