# 🎓 ATU Cafeteria Food Ordering & Real-Time Management System
## Academic Project Defense & Technical Demonstration Guide
**Institution:** Accra Technical University (ATU)  
**Project Title:** Smart Campus Cafeteria Ordering, Real-Time Tracking & Vendor Oversight Platform  

---

## 1. Executive Summary & Problem Statement

### 🛑 The Problem on Campus
1. **Severe Congestion & Queues**: Long physical queues during 12:00 PM – 2:00 PM peak lecture breaks cause student delays and missed classes.
2. **Food Wastage & Stockouts**: Vendors lack demand forecasting tools, resulting in running out of popular meals (e.g. Waakye, Jollof) or overpreparing perishable dishes.
3. **Order Errors & Verification Loss**: Manual paper tickets lead to misplaced meals, order disputes, and slow counter hand-offs.

### 💡 The Proposed Solution
A synchronized, multi-platform ecosystem:
- **Student Client**: Digital pre-ordering, wallet payments, dietary macro insights, and a **Live 4-Stage Status Tracker** with a secure 4-digit pickup PIN.
- **Vendor Kitchen Hub**: Live incoming kitchen ticket queue, preparation workflow controls, and fast PIN validation.
- **Admin Oversight**: Real-time sales volume KPIs, audit telemetry, and vendor revenue analytics.
- **Backend & Cloud Architecture**: Dual offline-first database synchronization (Room DB) backed by a robust REST API (Laravel 11) and WebSocket events.

---

## 2. Technical Architecture & Tech Stack

| Layer | Technologies Used | Key Purpose |
|---|---|---|
| **Mobile Client (Android)** | Kotlin, Jetpack Compose, Material 3, Coroutines, Flow | Native performance, smooth animations, biometric checkout |
| **Local Persistence** | Android Room Database (SQLite), Shared Preferences | 100% offline access to menus, cached orders, and fallback diagnostics |
| **Cross-Platform / iOS** | Flutter (Dart), Provider, Dio | Cross-platform compatibility for iOS Safari/App users |
| **Web Portal** | HTML5, Tailwind CSS, Lucide Icons, JavaScript PWA | Zero-install web access for any student or panel judge |
| **Backend API** | PHP 8.2+, Laravel 11, SQLite/MySQL | RESTful APIs, Token Authentication, WebSocket event broadcasting |
| **Security** | BiometricPrompt API, Hashed PINs, Role-Based Access Control | Secure student checkout and counterfeit-proof meal collection |

---

## 3. Core System Workflows (Live Defense Demo Script)

### 🍔 Scenario 1: Student Pre-Order & Checkout
1. Student opens the app, views categorized cafeteria menus (Main Dish, Traditional, Drinks, Snacks).
2. Adds items to tray (e.g. *Spiced Jollof Rice with Chicken* + *Chilled Sobolo*).
3. Student completes biometric/wallet checkout.
4. An encrypted order record is created with a unique **4-digit Pickup PIN** (e.g. `5821`).

### ⏱️ Scenario 2: Real-Time 4-Stage Order Progression Tracker
The system visually advances the student's meal through 4 live phases:
1. **Stage 1: Order Received** — Ticket printed at Akwaaba Kitchen.
2. **Stage 2: Kitchen Preparing** — Food is actively cooking/plated on the stove.
3. **Stage 3: Out for Delivery / Counter Ready** — Meal is boxed at the counter waiting for student.
4. **Stage 4: Delivered / Collected** — PIN verified at counter; transaction closed.

### 👩‍🍳 Scenario 3: Vendor Kitchen Operations
1. Vendor sees incoming orders in real-time.
2. Taps **"Start Preparing"** ➔ **"Mark Ready for Counter"**.
3. Student arrives at counter and presents PIN `5821`.
4. Vendor enters PIN into the verification box ➔ Order instantly marks as **Delivered**.

---

## 4. Key Questions & Model Answers for the Defense Panel

### Q1: What happens if the internet cuts out during a rush hour?
> **Answer:** *"The application implements an **Offline-First Architecture**. All menus, order histories, and user profiles are stored locally in the on-device **Room Database**. If connectivity is lost, the user can continue viewing their active orders and verified PINs. Once connectivity resumes, background coroutines sync pending transactions with the Laravel backend without data loss."*

### Q2: How do you prevent unauthorized students from collecting someone else's food?
> **Answer:** *"Every order generates a cryptographically random, one-time 4-digit Pickup PIN upon checkout. The vendor must verify this PIN at the counter before handing over the meal, completely eliminating order disputes."*

### Q3: Why is this system beneficial to cafeteria vendors?
> **Answer:** *"Vendors gain operational visibility: pre-orders let kitchen staff prepare portion quantities 15–20 minutes before peak lecture breaks. The ATU Analytics dashboard also provides demand forecasting, preventing food waste and inventory stockouts."*

### Q4: How is cross-platform accessibility guaranteed?
> **Answer:** *"The platform includes a native Android Jetpack Compose app, a cross-platform Flutter client, and a responsive Single Page Web Application that runs instantly in any browser without needing app store installation."*
