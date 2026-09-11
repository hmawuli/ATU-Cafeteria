<?php

namespace Database\Seeders;

use App\Models\User;
use App\Models\FoodItem;
use App\Models\Order;
use App\Models\Feedback;
use App\Models\AuditLog;
use App\Models\Vendor;
use App\Models\OrderItem;
use App\Models\WalletTransaction;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     */
    public function run(): void
    {
        // 1. Setup default password hash (SHA-256 for cross-compliance)
        // 1234 -> sha256 to allow immediate login via mobile app PIN codes
        $studHash = hash('sha256', '1234');
        $v1Hash = hash('sha256', '1111');
        $v2Hash = hash('sha256', '2222');
        $v3Hash = hash('sha256', '3333');
        $adminHash = hash('sha256', 'admin123');

        // 2. Create Users
        $stud1 = User::create([
            'id' => 1,
            'username' => 'student',
            'password' => $studHash,
            'role' => 'STUDENT',
            'fullName' => 'Daniel Mensah',
            'student_staff_id' => 'ATU-2024-D45',
            'profile_info' => [
                'department' => 'Computer Science',
                'program' => 'BTech Software Engineering',
                'level' => '300',
                'telephone' => '+233 50 123 4567',
                'email' => 'daniel.mensah@atu.edu.gh'
            ],
            'info' => 'ATU-2024-D45',
            'balance' => 250.00,
            'loyalty_points' => 120,
            'total_spent' => 55.00,
        ]);

        $stud2 = User::create([
            'id' => 2,
            'username' => 'student2',
            'password' => $studHash,
            'role' => 'STUDENT',
            'fullName' => 'Abena Osei',
            'student_staff_id' => 'ATU-2025-S12',
            'profile_info' => [
                'department' => 'Applied Mathematics',
                'program' => 'BTech Statistics',
                'level' => '200',
                'telephone' => '+233 24 987 6543',
                'email' => 'abena.osei@atu.edu.gh'
            ],
            'info' => 'ATU-2025-S12',
            'balance' => 180.00,
            'loyalty_points' => 85,
            'total_spent' => 55.00,
        ]);

        $v1 = User::create([
            'id' => 10,
            'username' => 'maryjoint',
            'password' => $v1Hash,
            'role' => 'VENDOR',
            'fullName' => 'Mary Joint',
            'student_staff_id' => 'ATU-VND-010',
            'profile_info' => [
                'outlet_name' => 'Auntie Mary Special',
                'location' => 'Block C Cafeteria Booth 1',
                'telephone' => '+233 27 111 2222',
                'primary_category' => 'Rice & Local Dishes'
            ],
            'info' => 'Auntie Mary Special',
            'balance' => 45.00,
            'is_open' => true,
        ]);

        $v2 = User::create([
            'id' => 11,
            'username' => 'atkitch',
            'password' => $v2Hash,
            'role' => 'VENDOR',
            'fullName' => 'Kofi Local Kitchen',
            'student_staff_id' => 'ATU-VND-011',
            'profile_info' => [
                'outlet_name' => 'ATU Local Hub',
                'location' => 'Main Dining Annex A Booth 3',
                'telephone' => '+233 26 333 4444',
                'primary_category' => 'Traditional Dishes'
            ],
            'info' => 'ATU Local Hub',
            'balance' => 65.00,
            'is_open' => true,
        ]);

        $v3 = User::create([
            'id' => 12,
            'username' => 'snackbag',
            'password' => $v3Hash,
            'role' => 'VENDOR',
            'fullName' => 'Bakery & Treats',
            'student_staff_id' => 'ATU-VND-012',
            'profile_info' => [
                'outlet_name' => 'ATU Snack Corner',
                'location' => 'Science Block Lobby Kiosk',
                'telephone' => '+233 20 555 6666',
                'primary_category' => 'Pastries & Drinks'
            ],
            'info' => 'ATU Snack Corner',
            'balance' => 0.00,
            'is_open' => false,
        ]);

        // Seed corresponding Vendor profiles in the vendors table
        Vendor::create([
            'user_id' => $v1->id,
            'name' => $v1->fullName,
            'location' => $v1->profile_info['location'],
            'contact_info' => $v1->profile_info['telephone'],
            'operational_status' => 'active',
            'store_name' => $v1->profile_info['outlet_name'],
            'location_within_campus' => $v1->profile_info['location'],
            'contact_email' => $v1->username . '@atu.edu.gh',
            'operational_hours' => '07:30 AM - 06:30 PM',
        ]);

        Vendor::create([
            'user_id' => $v2->id,
            'name' => $v2->fullName,
            'location' => $v2->profile_info['location'],
            'contact_info' => $v2->profile_info['telephone'],
            'operational_status' => 'active',
            'store_name' => $v2->profile_info['outlet_name'],
            'location_within_campus' => $v2->profile_info['location'],
            'contact_email' => $v2->username . '@atu.edu.gh',
            'operational_hours' => '08:00 AM - 06:00 PM',
        ]);

        Vendor::create([
            'user_id' => $v3->id,
            'name' => $v3->fullName,
            'location' => $v3->profile_info['location'],
            'contact_info' => $v3->profile_info['telephone'],
            'operational_status' => 'active',
            'store_name' => $v3->profile_info['outlet_name'],
            'location_within_campus' => $v3->profile_info['location'],
            'contact_email' => $v3->username . '@atu.edu.gh',
            'operational_hours' => '08:00 AM - 05:00 PM',
        ]);

        $admin = User::create([
            'id' => 99,
            'username' => 'admin',
            'password' => $adminHash,
            'role' => 'ADMIN',
            'fullName' => 'Dr. Emmanuel Kaku',
            'student_staff_id' => 'ATU-ADM-099',
            'profile_info' => [
                'office' => 'Quality Assurance Directorate Block B',
                'administrative_title' => 'Director of Academic Quality',
                'telephone' => '+233 55 999 8888',
                'email' => 'emmanuel.kaku@atu.edu.gh'
            ],
            'info' => 'ATU Quality Assurance',
        ]);

        // 3. Create FoodItems
        $foods = [
            [
                'id' => 101,
                'vendor_id' => $v1->id,
                'name' => 'ATU Chicken Jollof Rice',
                'price' => 25.0,
                'category' => 'Lunch Specials',
                'image_url' => '',
                'description' => 'Classic aromatic rice stewed with authentic Ghanaian tomato sauce, served with seasoned fried chicken salad & shito.',
                'is_available' => true,
            ],
            [
                'id' => 102,
                'vendor_id' => $v1->id,
                'name' => 'Zesty Ginger Sobolo',
                'price' => 10.0,
                'category' => 'Drinks',
                'image_url' => '',
                'description' => 'Refreshing chilled local hibiscus flower drink brewed with fresh ginger, pineapple peels, and sweetener.',
                'is_available' => true,
            ],
            [
                'id' => 103,
                'vendor_id' => $v1->id,
                'name' => 'Red-Red Beans Stew',
                'price' => 20.0,
                'category' => 'Lunch Specials',
                'image_url' => '',
                'description' => 'Stewed tender cowpea bean hash in palm palm oil, accompanied by fried ripe sugar-plantain dices.',
                'is_available' => true,
            ],
            [
                'id' => 201,
                'vendor_id' => $v2->id,
                'name' => 'Waakye Supreme',
                'price' => 30.0,
                'category' => 'Traditional',
                'image_url' => '',
                'description' => 'A student favorite! Local black-eyed peas boiled with rice and millet stalks. Accompanying boiled egg, spiced gari, talia, and hot wele shito.',
                'is_available' => true,
            ],
            [
                'id' => 202,
                'vendor_id' => $v2->id,
                'name' => 'Fufu & Goat Light Soup',
                'price' => 35.0,
                'category' => 'Traditional',
                'image_url' => '',
                'description' => 'Rich Ghanaian fufu pounded from fresh cassava and green plantains, submerged in aromatic goat meat soup.',
                'is_available' => true,
            ],
            [
                'id' => 301,
                'vendor_id' => $v3->id,
                'name' => 'Savoury Meat Pie',
                'price' => 15.0,
                'category' => 'Snacks',
                'image_url' => '',
                'description' => 'Crispy, flaky puff pastry loaded with moist, cooked mince beef seasoning.',
                'is_available' => true,
            ],
            [
                'id' => 302,
                'vendor_id' => $v3->id,
                'name' => 'Chilled Coca-Cola',
                'price' => 8.0,
                'category' => 'Drinks',
                'image_url' => '',
                'description' => '330ml Ice-cold Coca-Cola can for dynamic pairing.',
                'is_available' => true,
            ]
        ];

        foreach ($foods as $f) {
            FoodItem::create($f);
            \App\Models\MenuItem::create([
                'id' => $f['id'],
                'vendor_id' => $f['vendor_id'],
                'food_name' => $f['name'],
                'name' => $f['name'],
                'price' => $f['price'],
                'category' => $f['category'],
                'description' => $f['description'],
                'is_available' => $f['is_available'] ?? true,
            ]);
        }

        // 4. Create Historical and Active Orders
        $now = time() * 1000; // millisecond timestamp matching Android client
        $dayInMs = 86400000;

        // Order 1001: Completed
        $order1 = Order::create([
            'id' => 1001,
            'customer_id' => $stud1->id,
            'vendor_id' => $v1->id,
            'food_item_id' => 101,
            'food_name' => 'ATU Chicken Jollof Rice',
            'quantity' => 1,
            'unit_price' => 25.0,
            'total_price' => 25.0,
            'order_timestamp' => $now - $dayInMs * 2,
            'status' => 'COMPLETED',
            'pickup_pin' => '4444',
            'estimated_pickup_time' => '15 mins',
        ]);
        OrderItem::create([
            'order_id' => 1001,
            'food_item_id' => 101,
            'name' => 'ATU Chicken Jollof Rice',
            'quantity' => 1,
            'unit_price' => 25.0,
            'total_price' => 25.0,
        ]);

        // Order 1002: Completed
        $order2 = Order::create([
            'id' => 1002,
            'customer_id' => $stud2->id,
            'vendor_id' => $v1->id,
            'food_item_id' => 102,
            'food_name' => 'Zesty Ginger Sobolo',
            'quantity' => 2,
            'unit_price' => 10.0,
            'total_price' => 20.0,
            'order_timestamp' => $now - $dayInMs * 1,
            'status' => 'COMPLETED',
            'pickup_pin' => '5555',
            'estimated_pickup_time' => '5 mins',
        ]);
        OrderItem::create([
            'order_id' => 1002,
            'food_item_id' => 102,
            'name' => 'Zesty Ginger Sobolo',
            'quantity' => 2,
            'unit_price' => 10.0,
            'total_price' => 20.0,
        ]);

        // Order 1003: Completed
        $order3 = Order::create([
            'id' => 1003,
            'customer_id' => $stud1->id,
            'vendor_id' => $v2->id,
            'food_item_id' => 201,
            'food_name' => 'Waakye Supreme',
            'quantity' => 1,
            'unit_price' => 30.0,
            'total_price' => 30.0,
            'order_timestamp' => $now - $dayInMs * 3,
            'status' => 'COMPLETED',
            'pickup_pin' => '6666',
            'estimated_pickup_time' => '20 mins',
        ]);
        OrderItem::create([
            'order_id' => 1003,
            'food_item_id' => 201,
            'name' => 'Waakye Supreme',
            'quantity' => 1,
            'unit_price' => 30.0,
            'total_price' => 30.0,
        ]);

        // Order 1004: Completed
        $order4 = Order::create([
            'id' => 1004,
            'customer_id' => $stud2->id,
            'vendor_id' => $v2->id,
            'food_item_id' => 202,
            'food_name' => 'Fufu & Goat Light Soup',
            'quantity' => 1,
            'unit_price' => 35.0,
            'total_price' => 35.0,
            'order_timestamp' => $now - $dayInMs * 4,
            'status' => 'COMPLETED',
            'pickup_pin' => '7777',
            'estimated_pickup_time' => '15 mins',
        ]);
        OrderItem::create([
            'order_id' => 1004,
            'food_item_id' => 202,
            'name' => 'Fufu & Goat Light Soup',
            'quantity' => 1,
            'unit_price' => 35.0,
            'total_price' => 35.0,
        ]);

        // Order 1005: Pending Order
        $order5 = Order::create([
            'id' => 1005,
            'customer_id' => $stud1->id,
            'vendor_id' => $v1->id,
            'food_item_id' => 101,
            'food_name' => 'ATU Chicken Jollof Rice',
            'quantity' => 1,
            'unit_price' => 25.0,
            'total_price' => 25.0,
            'order_timestamp' => $now - 5 * 60000, // 5 mins ago
            'status' => 'PENDING',
            'pickup_pin' => '1212',
            'estimated_pickup_time' => '15 mins',
        ]);
        OrderItem::create([
            'order_id' => 1005,
            'food_item_id' => 101,
            'name' => 'ATU Chicken Jollof Rice',
            'quantity' => 1,
            'unit_price' => 25.0,
            'total_price' => 25.0,
        ]);

        // Order 1006: Preparing Order (Multiple items in a single checkout)
        $order6 = Order::create([
            'id' => 1006,
            'customer_id' => $stud2->id,
            'vendor_id' => $v1->id,
            'food_item_id' => 103,
            'food_name' => 'Red-Red Beans Stew',
            'quantity' => 1,
            'unit_price' => 20.0,
            'total_price' => 30.0, // Includes 10.0 Sobolo below
            'order_timestamp' => $now - 12 * 60000, // 12 mins ago
            'status' => 'PREPARING',
            'pickup_pin' => '3434',
            'estimated_pickup_time' => '10 mins',
        ]);
        OrderItem::create([
            'order_id' => 1006,
            'food_item_id' => 103,
            'name' => 'Red-Red Beans Stew',
            'quantity' => 1,
            'unit_price' => 20.0,
            'total_price' => 20.0,
        ]);
        OrderItem::create([
            'order_id' => 1006,
            'food_item_id' => 102,
            'name' => 'Zesty Ginger Sobolo',
            'quantity' => 1,
            'unit_price' => 10.0,
            'total_price' => 10.0,
        ]);

        // Order 1007: Ready for Pickup Order
        $order7 = Order::create([
            'id' => 1007,
            'customer_id' => $stud1->id,
            'vendor_id' => $v3->id,
            'food_item_id' => 301,
            'food_name' => 'Savoury Meat Pie',
            'quantity' => 2,
            'unit_price' => 15.0,
            'total_price' => 38.0, // Includes 8.0 Coca-cola
            'order_timestamp' => $now - 25 * 60000, // 25 mins ago
            'status' => 'READY',
            'pickup_pin' => '7878',
            'estimated_pickup_time' => 'Ready Now',
        ]);
        OrderItem::create([
            'order_id' => 1007,
            'food_item_id' => 301,
            'name' => 'Savoury Meat Pie',
            'quantity' => 2,
            'unit_price' => 15.0,
            'total_price' => 30.0,
        ]);
        OrderItem::create([
            'order_id' => 1007,
            'food_item_id' => 302,
            'name' => 'Chilled Coca-Cola',
            'quantity' => 1,
            'unit_price' => 8.0,
            'total_price' => 8.0,
        ]);

        // Order 1008: Cancelled Order
        $order8 = Order::create([
            'id' => 1008,
            'customer_id' => $stud1->id,
            'vendor_id' => $v2->id,
            'food_item_id' => 201,
            'food_name' => 'Waakye Supreme',
            'quantity' => 1,
            'unit_price' => 30.0,
            'total_price' => 30.0,
            'order_timestamp' => $now - $dayInMs * 5,
            'status' => 'CANCELLED',
            'pickup_pin' => '9090',
            'estimated_pickup_time' => 'Cancelled',
        ]);
        OrderItem::create([
            'order_id' => 1008,
            'food_item_id' => 201,
            'name' => 'Waakye Supreme',
            'quantity' => 1,
            'unit_price' => 30.0,
            'total_price' => 30.0,
        ]);

        // Order 1009: Completed Order
        $order9 = Order::create([
            'id' => 1009,
            'customer_id' => $stud2->id,
            'vendor_id' => $v2->id,
            'food_item_id' => 202,
            'food_name' => 'Fufu & Goat Light Soup',
            'quantity' => 1,
            'unit_price' => 35.0,
            'total_price' => 35.0,
            'order_timestamp' => $now - $dayInMs * 6,
            'status' => 'COMPLETED',
            'pickup_pin' => '1122',
            'estimated_pickup_time' => '15 mins',
        ]);
        OrderItem::create([
            'order_id' => 1009,
            'food_item_id' => 202,
            'name' => 'Fufu & Goat Light Soup',
            'quantity' => 1,
            'unit_price' => 35.0,
            'total_price' => 35.0,
        ]);

        // Order 1010: Completed Order
        $order10 = Order::create([
            'id' => 1010,
            'customer_id' => $stud1->id,
            'vendor_id' => $v1->id,
            'food_item_id' => 103,
            'food_name' => 'Red-Red Beans Stew',
            'quantity' => 1,
            'unit_price' => 20.0,
            'total_price' => 20.0,
            'order_timestamp' => $now - $dayInMs * 1,
            'status' => 'COMPLETED',
            'pickup_pin' => '3344',
            'estimated_pickup_time' => '15 mins',
        ]);
        OrderItem::create([
            'order_id' => 1010,
            'food_item_id' => 103,
            'name' => 'Red-Red Beans Stew',
            'quantity' => 1,
            'unit_price' => 20.0,
            'total_price' => 20.0,
        ]);

        // Order 1011: Declined Order
        $order11 = Order::create([
            'id' => 1011,
            'customer_id' => $stud2->id,
            'vendor_id' => $v1->id,
            'food_item_id' => 101,
            'food_name' => 'ATU Chicken Jollof Rice',
            'quantity' => 1,
            'unit_price' => 25.0,
            'total_price' => 25.0,
            'order_timestamp' => $now - $dayInMs * 2,
            'status' => 'DECLINED',
            'pickup_pin' => '5566',
            'estimated_pickup_time' => 'Sold Out',
        ]);
        OrderItem::create([
            'order_id' => 1011,
            'food_item_id' => 101,
            'name' => 'ATU Chicken Jollof Rice',
            'quantity' => 1,
            'unit_price' => 25.0,
            'total_price' => 25.0,
        ]);


        // 4b. Create Digital Wallet Ledger Transactions
        WalletTransaction::create([
            'user_id' => $stud1->id,
            'type' => 'DEPOSIT',
            'amount' => 300.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-DEP-847291',
            'details' => 'Paystack Virtual Deposit via Mobile Money wallet',
        ]);
        WalletTransaction::create([
            'user_id' => $stud1->id,
            'type' => 'PAYMENT',
            'amount' => 25.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-PAY-1001',
            'details' => 'Payment for Order #1001: ATU Chicken Jollof Rice',
        ]);
        WalletTransaction::create([
            'user_id' => $stud1->id,
            'type' => 'PAYMENT',
            'amount' => 30.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-PAY-1003',
            'details' => 'Payment for Order #1003: Waakye Supreme',
        ]);

        WalletTransaction::create([
            'user_id' => $stud2->id,
            'type' => 'DEPOSIT',
            'amount' => 250.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-DEP-392811',
            'details' => 'Visa Card Top-Up on ATU Escrow System',
        ]);
        WalletTransaction::create([
            'user_id' => $stud2->id,
            'type' => 'PAYMENT',
            'amount' => 20.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-PAY-1002',
            'details' => 'Payment for Order #1002: Zesty Ginger Sobolo',
        ]);
        WalletTransaction::create([
            'user_id' => $stud2->id,
            'type' => 'PAYMENT',
            'amount' => 35.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-PAY-1004',
            'details' => 'Payment for Order #1004: Fufu & Goat Light Soup',
        ]);
        WalletTransaction::create([
            'user_id' => $stud1->id,
            'type' => 'REFUND',
            'amount' => 30.00,
            'status' => 'SUCCESS',
            'reference' => 'TXN-REF-1008',
            'details' => 'Refund for Cancelled Order #1008: Waakye Supreme',
        ]);


        // 5. Create Dynamic Feedback Ratings
        Feedback::create([
            'id' => 1,
            'order_id' => 1001,
            'vendor_id' => $v1->id,
            'customer_id' => $stud1->id,
            'rating_food_quality' => 5,
            'rating_cleanliness' => 4,
            'rating_service_speed' => 4,
            'rating_price_value' => 4,
            'comment' => 'The chicken was very delicious and juicy! A bit crowded but service was quite neat.',
            'timestamp' => $now - $dayInMs * 2,
        ]);

        Feedback::create([
            'id' => 2,
            'order_id' => 1002,
            'vendor_id' => $v1->id,
            'customer_id' => $stud2->id,
            'rating_food_quality' => 4,
            'rating_cleanliness' => 5,
            'rating_service_speed' => 5,
            'rating_price_value' => 5,
            'comment' => 'Chilled sobolo was exactly what I needed after lectures. Extremely clean booth!',
            'timestamp' => $now - $dayInMs * 1,
        ]);

        Feedback::create([
            'id' => 3,
            'order_id' => 1003,
            'vendor_id' => $v2->id,
            'customer_id' => $stud1->id,
            'rating_food_quality' => 5,
            'rating_cleanliness' => 3,
            'rating_service_speed' => 3,
            'rating_price_value' => 5,
            'comment' => 'The waakye was extremely tasty. However, they need to improve speed of service at peak 12:00 PM hours.',
            'timestamp' => $now - $dayInMs * 3,
        ]);

        Feedback::create([
            'id' => 4,
            'order_id' => 1004,
            'vendor_id' => $v2->id,
            'customer_id' => $stud2->id,
            'rating_food_quality' => 4,
            'rating_cleanliness' => 2,
            'rating_service_speed' => 4,
            'rating_price_value' => 4,
            'comment' => 'Pounded fufu was soft and fresh. The chop booth surrounding floor had some napkins; cleanliness can be improved.',
            'timestamp' => $now - $dayInMs * 4,
        ]);

        // 6. Create Launch Audit Trail Log
        AuditLog::create([
            'id' => 1,
            'user_id' => 99,
            'timestamp' => $now,
            'action' => 'SYSTEM_INIT',
            'details' => 'Laravel SQL pre-loader successfully built with authentic ATU students and food joints.',
        ]);

        $this->call(MenuCategoryAndVendorSeeder::class);
    }
}
