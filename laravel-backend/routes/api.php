<?php

use Illuminate\Support\Facades\Route;
use Illuminate\Support\Facades\Event;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\StudentAuthController;
use App\Http\Controllers\Api\VendorAuthController;
use App\Http\Controllers\Api\FoodItemController;
use App\Http\Controllers\Api\OrderController;
use App\Http\Controllers\Api\FeedbackController;
use App\Http\Controllers\Api\FoodItemFeedbackController;
use App\Http\Controllers\Api\AuditLogController;
use App\Http\Controllers\Api\VendorController;
use App\Http\Controllers\Api\WalletController;
use App\Http\Controllers\Api\VendorPerformanceController;
use App\Http\Controllers\Api\MenuController;
use App\Http\Controllers\Api\ChatController;
use App\Http\Controllers\Api\PaystackPaymentController;
use App\Http\Controllers\Api\VendorSpecificController;
use App\Http\Controllers\Api\PassportAuthController;
use App\Http\Controllers\Api\DailyRevenueController;
use App\Http\Controllers\Api\MenuItemController;
use App\Http\Controllers\Api\InventoryCronController;
use App\Http\Controllers\Api\OrderItemMetricsController;

// Register explicit listeners for OrderStatusCompleted event
Event::listen(
    \App\Events\OrderStatusCompleted::class,
    \App\Listeners\SendOrderCompletedNotification::class
);

// Register explicit listeners for OrderStatusReady event
Event::listen(
    \App\Events\OrderStatusReady::class,
    \App\Listeners\SendOrderReadyNotification::class
);

/*
|--------------------------------------------------------------------------
| ATU Cafeteria REST API Routes
|--------------------------------------------------------------------------
*/

// Auth Endpoints (Generic)
Route::post('/register', [AuthController::class, 'register']);
Route::post('/login', [AuthController::class, 'login']);
Route::get('/users', [AuthController::class, 'getAllUsers']);

// Specialized Student Sanctum Auth Endpoints
Route::post('/student/register', [StudentAuthController::class, 'register']);
Route::post('/student/login', [StudentAuthController::class, 'login']);

// Specialized Vendor Sanctum Auth Endpoints
Route::post('/vendor/register', [VendorAuthController::class, 'register']);
Route::post('/vendor/login', [VendorAuthController::class, 'login']);
Route::get('/vendor/dashboard', [VendorController::class, 'dashboardView']);

// Progressive Web App (PWA) manifest route
Route::get('/manifest.json', function() {
    return response()->json([
        'name' => 'ATU Cafeteria Vendor Portal',
        'short_name' => 'ATU Vendor',
        'description' => 'Accra Technical University Cafeteria Vendor Management System',
        'start_url' => '/api/vendor/dashboard?vendor_id=10',
        'display' => 'standalone',
        'orientation' => 'portrait',
        'background_color' => '#f8fafc',
        'theme_color' => '#4f46e5',
        'icons' => [
            [
                'src' => 'https://img.icons8.com/color/192/hamburger.png',
                'sizes' => '192x192',
                'type' => 'image/png',
                'purpose' => 'any'
            ],
            [
                'src' => 'https://img.icons8.com/color/512/hamburger.png',
                'sizes' => '512x512',
                'type' => 'image/png',
                'purpose' => 'any'
            ]
        ]
    ], 200, [
        'Content-Type' => 'application/json',
        'Access-Control-Allow-Origin' => '*'
    ]);
});

// PWA Service Worker route
Route::get('/service-worker.js', function() {
    $sw = <<<JS
const CACHE_NAME = 'atu-vendor-cache-v1';
const urlsToCache = [
    '/api/manifest.json',
    'https://cdn.tailwindcss.com',
    'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap'
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME).then(cache => {
            return cache.addAll(urlsToCache);
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cacheName => {
                    if (cacheName !== CACHE_NAME) {
                        return caches.delete(cacheName);
                    }
                })
            );
        })
    );
    self.clients.claim();
});

self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') return;
    
    event.respondWith(
        fetch(event.request).catch(() => {
            return caches.match(event.request);
        })
    );
});

// Listener for background or foreground client page notifications
self.addEventListener('message', event => {
    if (event.data && event.data.type === 'NEW_ORDER') {
        self.registration.showNotification(event.data.title, {
            body: event.data.body,
            icon: 'https://img.icons8.com/color/192/hamburger.png',
            vibrate: [200, 100, 200],
            badge: 'https://img.icons8.com/color/192/hamburger.png',
            data: {
                url: event.data.url || '/api/vendor/dashboard?vendor_id=10'
            }
        });
    }
});

self.addEventListener('notificationclick', event => {
    event.notification.close();
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
            for (const client of clientList) {
                if (client.url.indexOf('/api/vendor/dashboard') !== -1 && 'focus' in client) {
                    return client.focus();
                }
            }
            if (clients.openWindow) {
                return clients.openWindow(event.notification.data?.url || '/api/vendor/dashboard?vendor_id=10');
            }
        })
    );
});
JS;
    return response($sw, 200, [
        'Content-Type' => 'application/javascript',
        'Access-Control-Allow-Origin' => '*'
    ]);
});

// Lightweight order counter for real-time notification polling
Route::get('/vendor/orders/unread-count', function (\Illuminate\Http\Request $request) {
    $vendorId = $request->input('vendor_id');
    if (!$vendorId) {
        return response()->json(['count' => 0, 'orders' => []]);
    }
    
    // Fetch pending/placed orders
    $orders = \App\Models\Order::where('vendor_id', $vendorId)
        ->whereIn('status', ['PENDING', 'ORDER_PLACED'])
        ->orderBy('id', 'desc')
        ->get();

    return response()->json([
        'count' => count($orders),
        'orders' => $orders
    ]);
});

// Student QR Code view for a single food item
Route::get('/student/order-item/{id}', function ($id) {
    $item = \App\Models\FoodItem::with('vendor')->find($id);
    if (!$item) {
        return response("Food item #{$id} was not found on this cafeteria server.", 404);
    }
    // Get seeded student users for selector
    $students = \App\Models\User::where('role', 'STUDENT')->get();
    return view('student.order_item', compact('item', 'students'));
});

// Student QR Code place order action
Route::post('/student/order-item/{id}/place', function (\Illuminate\Http\Request $request, $id) {
    $item = \App\Models\FoodItem::find($id);
    if (!$item) {
        return redirect()->back()->withErrors(['message' => 'The selected food item does not exist.']);
    }

    $qty = intval($request->input('quantity', 1));
    if ($qty < 1) $qty = 1;

    $studentId = $request->input('student_id', 1); // fallback to ID 1
    $student = \App\Models\User::find($studentId);

    // Generate random secure 4 digit PIN
    $pin = strval(rand(1000, 9999));
    $totalPrice = $item->price * $qty;

    $order = \App\Models\Order::create([
        'customer_id' => $studentId,
        'student_id' => $studentId,
        'user_id' => $studentId,
        'vendor_id' => $item->vendor_id,
        'food_item_id' => $item->id,
        'food_name' => $item->name,
        'quantity' => $qty,
        'unit_price' => $item->price,
        'total_price' => $totalPrice,
        'order_timestamp' => time() * 1000,
        'status' => 'ORDER_PLACED',
        'pickup_pin' => $pin,
        'estimated_pickup_time' => $request->input('pickup_time', 'In 15 Mins'),
    ]);

    // Create Audit Log entry
    \App\Models\AuditLog::create([
        'user_id' => $studentId,
        'timestamp' => time() * 1000,
        'action' => 'ORDER_PLACED',
        'details' => "Student '{$student->fullName}' successfully placed order #{$order->id} for {$qty}x '{$item->name}' (PIN: {$pin}) via scanned QR counter page.",
    ]);

    return view('student.order_success', compact('order', 'item', 'student'));
});

// Interactive Vendor Dashboard Web Actions (for Blade templates compatibility)
Route::post('/vendor/food-items', function (\Illuminate\Http\Request $request) {
    $validator = \Illuminate\Support\Facades\Validator::make($request->all(), [
        'vendor_id' => 'required|integer|exists:users,id',
        'name' => 'required|string|max:255',
        'price' => 'required|numeric|min:0',
        'category' => 'required|string',
        'description' => 'required|string|min:10|max:1000',
        'initial_stock' => 'nullable|integer|min:1',
        'low_stock_threshold' => 'nullable|integer|min:0',
    ]);

    if ($validator->fails()) {
        return redirect()->back()->withErrors($validator)->withInput();
    }

    $data = [
        'vendor_id' => $request->input('vendor_id'),
        'name' => $request->input('name'),
        'price' => $request->input('price'),
        'category' => $request->input('category'),
        'description' => $request->input('description'),
        'is_available' => true,
    ];

    if (\Illuminate\Support\Facades\Schema::hasColumn('food_items', 'initial_stock')) {
        $data['initial_stock'] = (int)$request->input('initial_stock', 50);
    }
    if (\Illuminate\Support\Facades\Schema::hasColumn('food_items', 'low_stock_threshold')) {
        $data['low_stock_threshold'] = (int)$request->input('low_stock_threshold', 10);
    }

    $food = \App\Models\FoodItem::create($data);

    // Create audit log
    \App\Models\AuditLog::create([
        'user_id' => $food->vendor_id,
        'timestamp' => time() * 1000,
        'action' => 'MENU_ITEM_CREATED',
        'details' => "Added dish: '{$food->name}' to category '{$food->category}' via Vendor Blade Dashboard.",
    ]);

    return redirect()->back()->with('success', "Food item '{$food->name}' added successfully!");
});

Route::post('/vendor/food-items/{id}/delete', function ($id) {
    $food = \App\Models\FoodItem::find($id);
    if ($food) {
        $name = $food->name;
        $vendorId = $food->vendor_id;
        $food->delete();

        // Create audit log
        \App\Models\AuditLog::create([
            'user_id' => $vendorId,
            'timestamp' => time() * 1000,
            'action' => 'MENU_ITEM_DELETED',
            'details' => "Eradicated menu item '{$name}' from vending list via Vendor Blade Dashboard.",
        ]);

        return redirect()->back()->with('success', "Food item '{$name}' deleted successfully!");
    }
    return redirect()->back()->with('error', "Food item not found.");
});

Route::post('/vendor/food-items/{id}/toggle-status', function ($id) {
    $food = \App\Models\FoodItem::find($id);
    if ($food) {
        $food->is_available = !$food->is_available;
        $food->save();

        $statusStr = $food->is_available ? 'In Stock' : 'Out of Stock';

        // Create audit log
        \App\Models\AuditLog::create([
            'user_id' => $food->vendor_id,
            'timestamp' => time() * 1000,
            'action' => 'MENU_ITEM_UPDATED',
            'details' => "Updated availability of '{$food->name}' to '{$statusStr}' via Vendor Blade Dashboard.",
        ]);

        return redirect()->back()->with('success', "Food item '{$food->name}' is now {$statusStr}!");
    }
    return redirect()->back()->with('error', "Food item not found.");
});

Route::get('/vendor/export-csv', function (\Illuminate\Http\Request $request) {
    $vendorId = $request->query('vendor_id', 10);
    $user = \App\Models\User::find($vendorId);
    if (!$user) {
        return response("Vendor not found", 404);
    }

    $service = new \App\Services\PerformanceAnalyticsService();
    $metrics = $service->getVendorReport($user->id);

    $orders = \App\Models\Order::where('vendor_id', $user->id)
        ->with(['customer', 'student'])
        ->orderBy('id', 'desc')
        ->get();

    $filename = "vendor_report_" . str_replace(' ', '_', strtolower($user->fullName)) . "_" . date('Ymd_His') . ".csv";

    $headers = [
        "Content-type"        => "text/csv",
        "Content-Disposition" => "attachment; filename=$filename",
        "Pragma"              => "no-cache",
        "Cache-Control"       => "must-revalidate, post-check=0, pre-check=0",
        "Expires"             => "0"
    ];

    $callback = function() use ($user, $metrics, $orders) {
        $file = fopen('php://output', 'w');

        // UTF-8 BOM
        fputs($file, "\xEF\xBB\xBF");

        // 1. Vendor Header
        fputcsv($file, ["VENDOR PERFORMANCE METRICS REPORT"]);
        fputcsv($file, ["Vendor Profile", $user->fullName]);
        fputcsv($file, ["Student/Staff ID", $user->student_staff_id]);
        fputcsv($file, ["Outlet Name", $user->profile_info['outlet_name'] ?? 'N/A']);
        fputcsv($file, ["Report Generated At", date('Y-m-d H:i:s')]);
        fputcsv($file, []);

        // 2. Metrics Block
        fputcsv($file, ["METRIC", "VALUE"]);
        fputcsv($file, ["Escrow Balance (GH₵)", number_format($user->balance, 2)]);
        fputcsv($file, ["Average Preparation Time", $metrics['completion_time_metrics']['average_formatted'] ?? 'N/A']);
        fputcsv($file, ["Fastest Preparation Time", $metrics['completion_time_metrics']['fastest_formatted'] ?? 'N/A']);
        fputcsv($file, ["Completion Rate (%)", ($metrics['order_metrics']['completion_rate_percentage'] ?? '100') . "%"]);
        fputcsv($file, ["Total Completed Sales (GH₵)", number_format($metrics['order_metrics']['total_completed_revenue'] ?? 0.0, 2)]);
        fputcsv($file, ["Total Orders Placed", $metrics['order_metrics']['total_orders_placed'] ?? 0]);
        fputcsv($file, ["Quality Score Rating (out of 5.0)", $metrics['rating_metrics']['overall_average_rating'] ?? '5.0']);
        fputcsv($file, ["Total Customer Feedback Count", $metrics['rating_metrics']['total_feedback_count'] ?? 0]);
        fputcsv($file, []);

        // 3. Order History Header
        fputcsv($file, ["COMPLETE TRANSACTIONS & ORDER HISTORY"]);
        fputcsv($file, ["Order ID", "Customer Name", "Dish Name", "Quantity", "Unit Price (GH₵)", "Total Price (GH₵)", "Date Placed", "Status", "Estimated Pickup Time"]);

        // 4. Order Rows
        foreach ($orders as $order) {
            $customerName = $order->customer ? $order->customer->fullName : ($order->student ? $order->student->fullName : 'Unknown Customer');
            $dateStr = date('Y-m-d H:i:s', $order->order_timestamp / 1000);
            fputcsv($file, [
                $order->id,
                $customerName,
                $order->food_name ?? 'N/A',
                $order->quantity ?? 1,
                number_format($order->unit_price ?? 0.0, 2),
                number_format($order->total_price ?? 0.0, 2),
                $dateStr,
                $order->status,
                $order->estimated_pickup_time ?? 'N/A'
            ]);
        }

        fclose($file);
    };

    return response()->stream($callback, 200, $headers);
});

Route::post('/vendor/orders/{id}/update-status', function (\Illuminate\Http\Request $request, $id) {
    $order = \App\Models\Order::find($id);
    if (!$order) {
        return redirect()->back()->with('error', "Order not found.");
    }

    $newStatus = strtoupper($request->input('status'));
    $oldStatus = $order->status;

    // Validate newStatus
    $validStatuses = ['PENDING', 'ORDER_PLACED', 'PREPARING', 'READY', 'COMPLETED', 'DECLINED', 'CANCELLED'];
    if (!in_array($newStatus, $validStatuses)) {
        return redirect()->back()->with('error', "Invalid status: {$newStatus}");
    }

    $order->status = $newStatus;
    $order->order_status = $newStatus;
    $order->save();

    // Create audit log
    \App\Models\AuditLog::create([
        'user_id' => $order->vendor_id,
        'timestamp' => time() * 1000,
        'action' => 'ORDER_STATUS_CHANGED',
        'details' => "Order #{$order->id} status updated from '{$oldStatus}' to '{$newStatus}' via Vendor Blade Dashboard.",
    ]);

    // Dispatch Events/Notifications if needed (so background triggers work flawlessly)
    if ($newStatus === 'READY') {
        try {
            event(new \App\Events\OrderStatusReady($order));
        } catch (\Exception $e) {
            \Illuminate\Support\Facades\Log::error("Failed to fire OrderStatusReady event: " . $e->getMessage());
        }
    } elseif ($newStatus === 'COMPLETED') {
        try {
            event(new \App\Events\OrderStatusCompleted($order));
        } catch (\Exception $e) {
            \Illuminate\Support\Facades\Log::error("Failed to fire OrderStatusCompleted event: " . $e->getMessage());
        }
    }

    return redirect()->back()->with('success', "Order #{$order->id} status updated to {$newStatus} successfully!");
});

// Explicit Laravel Breeze & Passport OAuth2 endpoints
Route::post('/oauth/token', [PassportAuthController::class, 'issueOAuthToken']);
Route::post('/breeze/student/register', [PassportAuthController::class, 'registerStudent']);
Route::post('/breeze/vendor/register', [PassportAuthController::class, 'registerVendor']);

// Define role checks for Student vs Vendor
$checkStudent = function ($request, $next) {
    $user = $request->user();
    if (!$user) {
        return response()->json([
            'success' => false,
            'message' => 'Unauthenticated.'
        ], 401);
    }
    
    // Check role column
    $role = strtoupper($user->role);
    if ($role !== 'STUDENT' && $role !== 'ADMIN') {
        return response()->json([
            'success' => false,
            'message' => 'Unauthorized. This endpoint is for students only.'
        ], 403);
    }
    
    // Verify Sanctum ability if using Sanctum tokens
    if (method_exists($user, 'tokenCan') && $user->currentAccessToken() && !$user->tokenCan('student') && $role === 'STUDENT') {
        return response()->json([
            'success' => false,
            'message' => 'Unauthorized token ability for student.'
        ], 403);
    }
    
    return $next($request);
};

$checkVendor = function ($request, $next) {
    $user = $request->user();
    if (!$user) {
        return response()->json([
            'success' => false,
            'message' => 'Unauthenticated.'
        ], 401);
    }
    
    // Check role column
    $role = strtoupper($user->role);
    if ($role !== 'VENDOR' && $role !== 'ADMIN') {
        return response()->json([
            'success' => false,
            'message' => 'Unauthorized. This endpoint is for vendors only.'
        ], 403);
    }
    
    // Verify Sanctum ability if using Sanctum tokens
    if (method_exists($user, 'tokenCan') && $user->currentAccessToken() && !$user->tokenCan('vendor') && $role === 'VENDOR') {
        return response()->json([
            'success' => false,
            'message' => 'Unauthorized token ability for vendor.'
        ], 403);
    }
    
    return $next($request);
};

$checkAdmin = function ($request, $next) {
    $user = $request->user();
    if (!$user) {
        return response()->json([
            'success' => false,
            'message' => 'Unauthenticated.'
        ], 401);
    }
    
    $role = strtoupper($user->role);
    if ($role !== 'ADMIN') {
        return response()->json([
            'success' => false,
            'message' => 'Unauthorized. This endpoint requires ADMIN privileges.'
        ], 403);
    }
    
    return $next($request);
};

// Protected Authenticated Endpoints - Supports both Sanctum and Secure JWT Auth
Route::middleware(function ($request, $next) {
    $authHeader = $request->header('Authorization') ?: $request->header('X-Auth-Token');
    if ($authHeader && preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
        $token = $matches[1];
    } else {
        $token = $authHeader;
    }

    $jwtUser = null;
    if ($token) {
        $jwtUser = \App\Services\JwtService::getUserFromToken($token);
        if ($jwtUser) {
            auth()->setUser($jwtUser);
            $request->setUserResolver(function () use ($jwtUser) {
                return $jwtUser;
            });
        }
    }

    // Enforce Inactivity Session Timeout for Mobile App Users (STUDENT role)
    $enforceTimeout = function ($user) {
        if ($user && strtoupper($user->role) === 'STUDENT') {
            $lastActivity = null;
            if ($user->profile_info && is_array($user->profile_info)) {
                $lastActivity = $user->profile_info['last_activity_at'] ?? null;
            }

            $timeoutDuration = 900; // 15 minutes inactivity timeout (900 seconds)

            if ($lastActivity && (time() - $lastActivity) > $timeoutDuration) {
                // Revoke current Sanctum access tokens
                if (method_exists($user, 'tokens')) {
                    $user->tokens()->delete();
                }

                return response()->json([
                    'success' => false,
                    'message' => 'Session expired due to inactivity. Please log in again.'
                ], 401);
            }

            // Update last activity timestamp
            $profile = is_array($user->profile_info) ? $user->profile_info : [];
            $profile['last_activity_at'] = time();
            $user->profile_info = $profile;
            $user->save();
        }
        return null;
    };

    if ($jwtUser) {
        $timeoutResponse = $enforceTimeout($jwtUser);
        if ($timeoutResponse) {
            return $timeoutResponse;
        }
        return $next($request);
    }

    // Pass through Sanctum if no JWT was validated
    return app(\Illuminate\Auth\Middleware\Authenticate::class)->handle($request, function ($req) use ($next, $enforceTimeout) {
        $user = $req->user();
        if ($user) {
            $timeoutResponse = $enforceTimeout($user);
            if ($timeoutResponse) {
                return $timeoutResponse;
            }
        }
        return $next($req);
    }, 'sanctum');
})->group(function () use ($checkStudent, $checkVendor, $checkAdmin) {
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::post('/user/profile', [AuthController::class, 'updateProfile']);
    Route::post('/orders/{id}/cancel', [OrderController::class, 'cancel']);
    Route::get('/orders/{id}/tracking', [OrderController::class, 'trackOrderRealTime']);

    // --- Admin-Only Data Deletion Endpoints ---
    Route::middleware($checkAdmin)->group(function () {
        Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);
        Route::delete('/feedback/{id}', [FeedbackController::class, 'destroy']);
        Route::delete('/audit-logs/{id}', [AuditLogController::class, 'destroy']);
        Route::delete('/orders/{id}', [OrderController::class, 'destroy']);
        Route::delete('/menu-items/{id}', [MenuItemController::class, 'destroyAdmin']);
    });

    // --- Student-Only Routes ---
    Route::middleware($checkStudent)->group(function () {
        // Authenticated Student Orders Endpoints
        Route::get('/student/orders', [OrderController::class, 'getAuthenticatedStudentOrders']);
        Route::post('/student/orders', [OrderController::class, 'storeAuthenticatedStudentOrder']);
        Route::post('/student/cart-checkout', [OrderController::class, 'cartCheckout']);
    });

    // --- Vendor-Only Routes ---
    Route::middleware($checkVendor)->group(function () {
        // Protected Vendor Menu & Food Items Endpoints
        Route::post('/food-items', [FoodItemController::class, 'store']);
        Route::put('/food-items/{id}', [FoodItemController::class, 'update']);
        Route::delete('/food-items/{id}', [FoodItemController::class, 'destroy']);

        // Protected Vendor Pre-Orders & Hand-offs
        Route::put('/orders/{id}/status', [OrderController::class, 'updateStatus']);
        Route::post('/orders/{id}/verify-pickup', [OrderController::class, 'verifyAndCompletePickup']);

        // Authenticated Vendor Private Feeds
        Route::get('/vendor/my-menu', [VendorController::class, 'getMyFoodItems']);
        Route::get('/vendor/my-orders', [VendorController::class, 'getMyOrders']);
        Route::get('/vendor/analytics', [VendorController::class, 'getMyAnalytics']);
        Route::get('/vendor/analytics/comparative', [VendorController::class, 'getComparativeAnalytics']);
        Route::get('/vendor/analytics/gemini-report', [VendorController::class, 'getMyGeminiReport']);
        Route::get('/vendor/{vendorId}/gemini-report', [VendorController::class, 'getVendorGeminiReport']);
        Route::get('/vendor/analytics/gemini-order-insights', [VendorController::class, 'getMyGeminiOrderInsights']);
        Route::get('/vendor/{vendorId}/gemini-order-insights', [VendorController::class, 'getVendorGeminiOrderInsights']);
        Route::get('/vendor/performance-metrics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
        Route::get('/vendor/performance', [VendorPerformanceController::class, 'getPerformance']);
        Route::get('/vendor/recharts-sales', [VendorPerformanceController::class, 'exportSalesForRecharts']);
        Route::get('/vendor/daily-revenue', [DailyRevenueController::class, 'getDailyRevenue']);
        Route::post('/vendor/toggle-status', [VendorController::class, 'toggleStatus']);

        // Order Items Dashboard Metrics Endpoints
        Route::get('/vendor/order-items-metrics', [OrderItemMetricsController::class, 'getDashboardMetrics']);
        Route::get('/vendor/order-items-revenue', [OrderItemMetricsController::class, 'getDailyRevenueMetrics']);
        Route::get('/vendor/order-items-menu-metrics', [OrderItemMetricsController::class, 'getMenuItemMetrics']);

        // Vendor Specific endpoints
        Route::put('/vendor/menu/availability', [VendorSpecificController::class, 'updateMenuAvailability']);
        Route::get('/vendor/orders/summary', [VendorSpecificController::class, 'getOrderSummary']);
        Route::post('/vendor/menu/bulk-update', [VendorSpecificController::class, 'bulkUpdateMenu']);
        Route::post('/vendor/menu/bulk-upload', [VendorSpecificController::class, 'bulkUpdateMenu']);

        // Vendor Menu Management Protected Endpoints
        Route::post('/menus', [MenuController::class, 'store']);
        Route::put('/menus/{id}', [MenuController::class, 'update']);
        Route::delete('/menus/{id}', [MenuController::class, 'destroy']);
        Route::post('/menu-items', [MenuController::class, 'storeMenuItem']);
        
        // Dedicated CRUD endpoints for Menu Items
        Route::post('/vendors/menu-items', [MenuItemController::class, 'store']);
        Route::put('/vendors/menu-items/{id}', [MenuItemController::class, 'update']);
        Route::delete('/vendors/menu-items/{id}', [MenuItemController::class, 'destroy']);
    });

    // --- Common Authenticated Routes ---
    // Digital Wallet & Core Transactions Subsystem
    Route::get('/wallet/balance', [WalletController::class, 'getBalance']);
    Route::get('/wallet/transactions', [WalletController::class, 'getTransactions']);
    Route::post('/wallet/deposit', [WalletController::class, 'deposit']);
    Route::post('/wallet/transfer', [WalletController::class, 'transfer']);
    Route::post('/wallet/payout', [WalletController::class, 'requestPayout']);

    // Database Notifications Subsystem
    Route::get('/notifications', function (\Illuminate\Http\Request $request) {
        return response()->json([
            'success' => true,
            'notifications' => $request->user()->notifications()->orderBy('created_at', 'desc')->get()
        ]);
    });
    Route::post('/notifications/mark-read', function (\Illuminate\Http\Request $request) {
        $request->user()->unreadNotifications->markAsRead();
        return response()->json([
            'success' => true,
            'message' => 'All database notifications marked as read.'
        ]);
    });

    // Chat Conversation Protected Endpoints
    Route::get('/chats/conversation/{otherUserId}', [ChatController::class, 'getConversation']);
    Route::post('/chats/send', [ChatController::class, 'sendMessage']);
    Route::get('/chats/recent', [ChatController::class, 'getRecentChats']);

    // Paystack Payment Integration Protected Endpoints
    Route::post('/paystack/initialize', [PaystackPaymentController::class, 'initialize']);
    Route::get('/paystack/verify/{reference}', [PaystackPaymentController::class, 'verify']);
});

// Automated Inventory & Availability Cron Checker routes
Route::match(['get', 'post'], '/cron/check-availability', [InventoryCronController::class, 'checkAndNotify']);

// Menu, Standalone Menu Items, & Food Items Endpoints (Public Reads)
Route::get('/menus', [MenuController::class, 'index']);
Route::get('/menus/vendor/{vendorId}', [MenuController::class, 'getVendorMenu']);
Route::get('/menu-items', [MenuController::class, 'listMenuItems']);
Route::get('/menu-items/vendor/{vendorId}', [MenuController::class, 'getVendorMenuItems']);

Route::get('/food-items', [FoodItemController::class, 'index']);
Route::get('/food-items/vendor/{vendorId}', [FoodItemController::class, 'getVendorFoodItems']);

// Pre-Orders & Transactions Endpoints
Route::get('/orders', [OrderController::class, 'index']);
Route::get('/orders/{id}', [OrderController::class, 'show']);
Route::get('/orders/customer/{customerId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/student/{studentId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/history/{studentId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/vendor/{vendorId}', [OrderController::class, 'getVendorOrders']);
Route::post('/orders', [OrderController::class, 'store']);

Route::get('/vendor/performance-analytics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
Route::get('/vendor/performance-metrics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
Route::get('/vendor/performance-recharts', [VendorPerformanceController::class, 'exportSalesForRecharts']);
Route::get('/vendor/statistics', [VendorPerformanceController::class, 'getAggregatedStatistics']);

Route::get('/dashboard/order-items-metrics', [OrderItemMetricsController::class, 'getDashboardMetrics']);
Route::get('/dashboard/order-items-revenue', [OrderItemMetricsController::class, 'getDailyRevenueMetrics']);
Route::get('/dashboard/order-items-menu-metrics', [OrderItemMetricsController::class, 'getMenuItemMetrics']);

// Customer Compliance & Feedback Endpoints
Route::get('/feedback', [FeedbackController::class, 'index']);
Route::get('/feedback/vendor/{vendorId}', [FeedbackController::class, 'getVendorFeedback']);
Route::post('/feedback', [FeedbackController::class, 'store']);

Route::get('/food-items/feedback', [FoodItemFeedbackController::class, 'index']);
Route::get('/food-items/{foodItemId}/feedback', [FoodItemFeedbackController::class, 'getByFoodItem']);
Route::post('/food-items/feedback', [FoodItemFeedbackController::class, 'store']);

// Centralised Quality Assurance Traceability Audit Logs Endpoints
Route::get('/audit-logs', [AuditLogController::class, 'index']);
Route::post('/audit-logs', [AuditLogController::class, 'store']);
