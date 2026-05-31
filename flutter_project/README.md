# ATU Cafeteria Hub - Flutter Frontend Client

This directory contains the production-grade **Flutter & Dart** companion application code for your Final Year Project at Accra Technical University. 

It satisfies your requirement of having a **Flutter-based application**, utilizing SQLite for local database operations and state persistence (`sqflite`), Provider for reactive model architectural binders, and the `google_generative_ai` service hook for predictive vendor performance auditing!

---

## 🚀 Presentation Features Included in Flutter
1. **Security-Locked Multi-Role Gate:** Single Login and Register screen with validation routing to `STUDENT`, `VENDOR`, and `ADMIN` channels.
2. **Student Handshake Gateway (Browse, Wallet & ID):**
   - **Browse Menu:** Real-time Accra culinary catalog filtering (Jollof, Waakye, Sobolo) by category with student announcer headers.
   - **Order Compilation:** Secure digital cart compiling quantities, wallet checks, and pay-on-delivery options.
   - **Secure Pickup Verification:** Generates unique 4-digit token security PINs to coordinate custody handover with cooks.
   - **Auditor Feedback Loop:** Forms for students to post ratings on Taste, Sanitation, Velocity, and Value.
   - **Smart Student ID:** Virtual NFC campus identifier card visual with barcode/QR and transaction ledger history.
3. **Vendor Kitchen Controller (Incoming, Menus, Analytics):**
   - **Order Dispatcher:** Control panels to accept, declining, cooking (`PREPARING`), and marking meals ready.
   - **Custody Verified Handover:** Popup form to verify the consumer's secure pickup PIN to mark orders as `COMPLETED`.
   - **Reactive Menu Catalog:** Instantly toggle food item availability, listing, and deletions from the vendor's storefront.
   - **Compliance & Gemini AI Bulletins:** Aggregates student reviews to compile Strengths/Weaknesses and runs a Gemini inference model to output quality-compliance bulletins!
4. **Admin Auditor Console (Compliance Board, Central Ledger):**
   - **National Compliance Tracker:** Interactive scoreboards scoring all food vendors across taste, speed, and waste sanitation.
   - **Central Logs Audit Ledger:** Tracks and highlights security logins, wallet topups, order placements, and token validations chronologically.
   - **Academic Statistics Board:** Reports total merchants, completed orders, listing rates, and issues warnings for under-performing booths automatically.

---

## 🛠️ How to Export and Load on your Local Machine

This setup is fully prepared to run directly on your personal computer (using VS Code, Android Studio, or IntelliJ IDEA!):

### 1. Download the Codebase ZIP
In the AI Studio interface:
- Open the settings menu or click the **Export selectivity ZIP** to download the entire project directory.
- Extract the ZIP on your local machine.

### 2. Open in VS Code / Android Studio
- Focus your terminal or folder focus directly on the extracted `flutter_project/` folder:
  ```bash
  cd flutter_project
  ```

### 3. Initialize Flutter Dependencies
Run the command below (or let VS Code/Android Studio run it automatically upon detecting `pubspec.yaml`) to install Provider, SQLite, Generative AI, and other libraries:
- Run:
  ```bash
  flutter pub get
  ```

### 4. Boot the Emulator / Mobile Device
- Ensure an Android Emulator, iOS Simulator, or physical device is connected.
- Execute the app:
  ```bash
  flutter run
  ```

### Default Credentials for Presentation Demonstration:
- **Scholar Student:** Username: `student` | PIN: `1234`
- **Merchant Cook (Vendor):** Username: `maryjoint` | PIN: `1111`
- **Academic Admin (QA Board):** Username: `admin` | PIN: `admin123`

---

## 🏛️ Project Architecture
- `lib/main.dart` - Application entry setting up colors matching ATU Sapphire standard primary rules, routing maps, and database auto-seeding.
- `lib/models/models.dart` - Strictly-typed Dart objects representing database structures (`User`, `FoodItem`, `Order`, `Feedback`, `AuditLog`).
- `lib/database/db_helper.dart` - Native SQLite handler creating five interconnected relational tables and automatic indexes for lightning-fast lookups.
- `lib/viewmodel/cafeteria_provider.dart` - Centralizing all state operations, transaction processing, MoMo credits, token verification algorithms, and fallback Gemini prompt parsing.
- `lib/screens/` - Elegant Material 3 user interface files beautifully arranged for tablets, foldables, and mobile screens.
