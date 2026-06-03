# Accra Technical University (ATU) Cafeteria System - Laravel API Backend

Welcome to the professional Laravel-powered REST API backend for the ATU Cafeteria App. This backend replaces the local SQLite/Room database storage with a dynamic, secure, high-performance central MySQL/PostgreSQL database, allowing students, cafeteria vendors, and administrators to transact in real-time.

---

## 🏗️ Architectural Overview
This backend leverages **Laravel 10.x/11.x** with the following production-grade designs:
- **RESTful API endpoints** mapped perfectly to Jetpack Compose frontend user flows.
- **Transactional Consistency**: Database transactions encapsulate order placements, wallet balances, and pick-up validations.
- **Security Protocols**: Stethoscope pincode verification matches SHA-256 client hashing; secure tokens authenticate requests.
- **Audit Logging**: A centralized middleware/service hooks into actions like `WALLET_PAYMENT`, `ORDER_CREATED`, `PICKUP_VALIDATED`.
- **Pre-Seeded ATU Experience**: Full-scale seeder comprising typical ATU vendors, student accounts, local meals, and baseline scores.

---

## 📁 Directory Structure
Below is the directory template created in this repository:
```text
laravel-backend/
├── app/
│   ├── Http/
│   │   └── Controllers/
│   │       └── Api/
│   │           ├── AuthController.php
│   │           ├── FoodItemController.php
│   │           ├── OrderController.php
│   │           ├── FeedbackController.php
│   │           └── AuditLogController.php
│   └── Models/
│       ├── User.php
│       ├── FoodItem.php
│       ├── Order.php
│       ├── Feedback.php
│       └── AuditLog.php
├── database/
│   ├── migrations/
│   │   ├── 2026_06_02_000001_create_users_table.php
│   │   ├── 2026_06_02_000002_create_food_items_table.php
│   │   ├── 2026_06_02_000003_create_orders_table.php
│   │   ├── 2026_06_02_000004_create_feedback_table.php
│   │   └── 2026_06_02_000005_create_audit_logs_table.php
│   └── seeders/
│       └── DatabaseSeeder.php
├── routes/
│   └── api.php
├── .env.example
└── README.md
```

---

## ⚙️ How to Deploy & Run Local Server

### 1. Prerequisites
- **PHP >= 8.1** installed on your workstation or VPS.
- **Composer** installed.
- **MySQL / SQLite / PostgreSQL** running.

### 2. Setup the Repository
Run the following terminal commands to prepare the backend service:
```bash
# Navigate to backend folder
cd laravel-backend

# Install package dependencies
composer install

# Copy environment file
cp .env.example .env

# Generate unique secure encryption key
php artisan key:generate
```

### 3. Configure Database (.env)
Open the generated `.env` file and configure your database parameters. By default, PostgreSQL is pre-configured for enterprise performance:
```env
DB_CONNECTION=pgsql
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=atu_cafeteria_db
DB_USERNAME=postgres
DB_PASSWORD=your_secure_postgres_password
```

---

## 💎 International-Standard Architecture & Postgres Safeguards
This project uses state-of-the-art backend principles optimized specifically for PostgreSQL:
1. **Precise Decimal Precision**: Floating-point money values are converted to precision storage (`decimal(10, 2)`) to avoid double-precision inaccuracies in financial calculation loops.
2. **PostgreSQL ACID Transactions**: All multi-write paths (e.g., placing orders, submitting feedback, adding menu items, registering users) run inside strict `DB::transaction(callback)` structures to avoid incomplete logs or partial updates in case of connection dropouts.
3. **Advanced Order-Items Architecture**: Realized an international-standard `orders -> order_items` 1-to-many schema. The `orders` table tracks standard enterprise attributes (`user_id`, `total_price`, `status`, etc.) while mapping composite list items through a highly normalized `order_items` sub-table with cascading constraints.
4. **Optimized Foreign Constraints**: Cascading locks are set up to handle complex entity hierarchies natively inside PG engine.
5. **Sanctum API Token Authentication**: All administrative and menu-altering request paths are guarded by standard `auth:sanctum` middleware. Logging in or registering issues a cryptographically secure token.
6. **No-Config Client Architecture**: Modern interceptors in the OkHttp client layer automatically fetch and store the token from `X-Auth-Token` response headers and pass it seamlessly via `Authorization: Bearer <token>` requests. No additional client configuration is required!
7. **Secure Vendor Private Feeds**: Added `GET /api/vendor/my-menu` and `GET /api/vendor/my-orders` routes mapped via `VendorController`. These ensure that an authenticated vendor's inventory and custom transaction histories are parsed strictly using their secure, database-verified Laravel Sanctum session token rather than unsecured parameter queries.
8. **Authorized Case-Insensitive Order Status Updates**: Enhanced the `PUT /api/orders/{id}/status` endpoint with strict role checks (VENDOR/ADMIN) and ownership validation via the Sanctum authenticate token. The endpoint automatically pre-processes and normalizes human-friendly statuses like `'Preparing'` and `'Ready for Pickup'` into standardized uppercase values (`PREPARING`, `READY`) before persisting updates to the database.
9. **Performance Analytics & Aggregation Engine**: Introduced `PerformanceAnalyticsService` which synthesizes multidimensional database calculations: averages order completion times by comparing logs or timestamps with order inception times, calculates unified ratings from feedback audits (food quality, cleanliness, speed, and value), computes completion rates and sales volumes, and generates comparative dashboard comparison structures. Included endpoints `GET /api/vendor/analytics` and `GET /api/vendor/analytics/comparative`.

---

### 4. Run Migrations & Seed Sample Data
Execute the Artisan migration command to build the tables and pre-populate menus (Accra Chicken Jollof, Waakye Supreme, Sobolo, etc.):
```bash
php artisan migrate --seed
```

### 5. Launch the Server
To make the Laravel API reachable from your Android emulator, launch it on your local network/IP:
```bash
# Start local host
php artisan serve --host=0.0.0.0 --port=8000
```
> **Emulator Network Note**: When testing from the Android Studio emulator, point your device connection URL to `http://10.0.2.2:8000`. If you are testing on a physical debug device, use your host workstation's local IP address (e.g., `http://192.168.1.100:8000`) or deploy online via platforms like Railway, Heroku, or digitalocean.

---

## 🛑 Dynamic Android Synchronisation

The Android Jetpack Compose app contains a **dual mode architecture**. You can:
1. **Offline Room Mode (Default)**: Runs immediately inside the device's sandbox. Perfect for previewing the app without starting servers.
2. **Laravel Live Sync Mode**: In the app login screen, expand the **"Developer Option: Laravel Backend Configuration"**. Enable the switch, type in your Laravel base URL (e.g. `http://10.0.2.2:8000/`), and click Apply. The app will immediately authenticate, fetch menus, place orders, log feedback, and records audit trails directly on your remote Laravel database!

---

## 🔗 RESTful API Documentation

### A. Authentication
| Method | Endpoint | Description | Payload Schema |
|---|---|---|---|
| **POST** | `/api/register` | Sign up a new user (Student/Vendor/Admin) | `{"username": "kofi1", "pin": "1234", "name": "Kofi Mensah", "role": "STUDENT", "info": "ATU-2024-X"}` |
| **POST** | `/api/login` | Log in and receive a secure user profile | `{"username": "kofi1", "pin": "1234"}` |
| **GET** | `/api/users` | Retrieve list of all registered users (Admin audit) | None |
| **DELETE** | `/api/users/{id}`| Remove a user profile (Admin function) | None |

### B. Food & Menu Management
| Method | Endpoint | Description | Payload Schema |
|---|---|---|---|
| **GET** | `/api/food-items` | Get list of all available food dishes | None |
| **GET** | `/api/food-items/vendor/{id}`| Get menu items specific to a single Vendor | None |
| **POST** | `/api/food-items` | Add a new dish to the cafeteria board | `{"vendor_id": 10, "name": "Kelewele", "price": 12.0, "category": "Snacks", "description": "Spiced plantains", "image_url": ""}` |
| **PUT** | `/api/food-items/{id}`| Modify dish details/availability | `{"category": "Snacks", "is_available": false}` |
| **DELETE** | `/api/food-items/{id}`| Erase menu item | None |

### C. Purchasing & Pre-Orders
| Method | Endpoint | Description | Payload Schema |
|---|---|---|---|
| **GET** | `/api/orders` | Fetch comprehensive list of orders (Admin) | None |
| **GET** | `/api/orders/customer/{id}`| Get order history of a specific student | None |
| **GET** | `/api/orders/vendor/{id}`| Retrieve incoming pre-orders for a vendor | None |
| **POST** | `/api/orders` | Create an order ticket (Cash / Wallet pay) | `{"customer_id": 1, "vendor_id": 10, "food_item_id": 101, "food_name": "Jollof", "quantity": 2, "unit_price": 25.0, "total_price": 50.0}` |
| **PUT** | `/api/orders/{id}/status`| Update status (`PREPARING`, `READY`, etc.) | `{"status": "READY", "estimated_pickup_time": "10 minutes"}` |
| **POST** | `/api/orders/{id}/verify-pickup`| Conclude handover using 4-digit security PIN | `{"vendor_id": 10, "pickup_pin": "5423"}` |

### D. Compliance Feed & Audit Trails
| Method | Endpoint | Description | Payload Schema |
|---|---|---|---|
| **GET** | `/api/feedback` | Load student ratings and written feedback | None |
| **GET** | `/api/feedback/vendor/{id}` | Load rating streams specific to a vendor | None |
| **POST** | `/api/feedback` | Log post-order quality feedback | `{"order_id": 1001, "vendor_id": 10, "customer_id": 1, "food_quality": 5, "cleanliness": 4, "speed": 4, "value": 5, "comment": "Amazing"}` |
| **GET** | `/api/audit-logs` | Retrieve QA compliance traces | None |
| **POST** | `/api/audit-logs` | Append action audit record manually | `{"user_id": 1, "action": "WALLET_CREDIT", "details": "Loaded 50 Cedis"}` |
