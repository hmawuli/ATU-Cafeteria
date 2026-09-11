# Smart Cafeteria Features

The ATU Cafeteria platform includes lightweight, production-friendly smart services that do not require a heavy ML stack.

## 1. Smart Queue
`GET /api/orders/{orderId}/queue` calculates the current vendor queue position and an estimated wait time from recent fulfillment history. Access is restricted to the order's student, its vendor, or an administrator.

## 2. Personal Recommendations
`GET /api/student/recommendations` recommends currently available food using the student's order history as the primary signal and availability as the safety constraint.

## 3. Demand Forecast
`GET /api/vendor/demand-forecast` uses a moving average over recent order quantities to estimate next-day portions. The response includes a confidence score based on the amount of historical data.

## 4. Food Waste Analytics
Vendors can record prepared/sold/wasted quantities through `POST /api/vendor/waste`. `GET /api/vendor/waste/summary` provides a 30-day waste rate.

## 5. QR Collection Pass
Flutter uses `qr_flutter` to render a collection pass containing an order identifier and pickup credential. The existing server-side pickup verification remains authoritative; a QR scan must never bypass server verification.

## 6. Admin Command Center
`GET /api/admin/command-center` provides active orders, sales, average wait, waste rate, security alerts and system status.

## 7. Security Alerts
Administrators can review and resolve security alerts through the admin API. Authentication failures remain audit logged; future anomaly rules can create `SecurityAlert` records without introducing a heavyweight monitoring stack.

## 8. Notifications and Real-Time Tracking
The existing notification/event system remains the authoritative mechanism for order status changes. Smart features build on it instead of replacing it.

## Design principle
All smart features are intentionally lightweight. They use Laravel queries and small calculations rather than a large AI/ML runtime, which keeps local development practical on a 4 GB RAM computer.
