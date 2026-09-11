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
use App\Http\Controllers\Api\WeeklyReportCronController;
use App\Http\Controllers\Api\DeliveredOrderReviewController;
use App\Http\Controllers\Api\OrderItemMetricsController;
use App\Http\Controllers\Api\VendorMenuItemController;
use App\Http\Controllers\Api\VendorMetricsController;
use App\Http\Controllers\Api\FavoriteMenuItemController;
use App\Http\Controllers\Api\IngredientDemandController;
use App\Http\Controllers\Api\StudentBudgetController;
use App\Http\Controllers\Api\LoyaltyController;
use App\Http\Controllers\Api\GroupOrderController;
use App\Http\Controllers\Api\AdminReportController;
use App\Http\Controllers\Api\HealthController;
use App\Http\Controllers\Api\SwaggerController;

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
// Public registration may create STUDENT or VENDOR accounts only.
// ADMIN accounts are provisioned by an existing ADMIN below.
Route::post('/register', [AuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:auth');

// Specialized Student Sanctum Auth Endpoints
Route::post('/student/register', [StudentAuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/student/login', [StudentAuthController::class, 'login'])->middleware('throttle:auth');

// Specialized Vendor Sanctum Auth Endpoints
Route::post('/vendor/register', [VendorAuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/vendor/login', [VendorAuthController::class, 'login'])->middleware('throttle:auth');
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
// Import Workbox from Google CDN
importScripts('https://storage.googleapis.com/workbox-cdn/releases/6.4.1/workbox-sw.js');

if (self.workbox) {
    console.log('Workbox loaded successfully');
    
    // Force immediate takeover of service worker clients
    self.workbox.core.skipWaiting();
    self.workbox.core.clientsClaim();

    // 1. Caching static assets with CacheFirst strategy (Tailwind, Fonts, Icons)
    self.workbox.routing.registerRoute(
        ({request}) => request.destination === 'style' || 
                       request.destination === 'script' || 
                       request.destination === 'font' || 
                       request.destination === 'image',
        new self.workbox.strategies.CacheFirst({
            cacheName: 'atu-static-assets',
            plugins: [
                new self.workbox.expiration.ExpirationPlugin({
                    maxEntries: 50,
                    maxAgeSeconds: 30 * 24 * 60 * 60,
                }),
            ],
        })
    );

    // 2. NetworkFirst strategy for Cafeteria Menu / Food Items
    self.workbox.routing.registerRoute(
        ({url}) => url.pathname.includes('/food-items') || url.pathname.includes('/menus'),
        new self.workbox.strategies.NetworkFirst({
            cacheName: 'atu-cafeteria-menu-cache',
            plugins: [
                new self.workbox.expiration.ExpirationPlugin({
                    maxEntries: 100,
                    maxAgeSeconds: 24 * 60 * 60,
                }),
            ],
        })
    );

    // 3. NetworkFirst strategy for Student / User Profiles
    self.workbox.routing.registerRoute(
        ({url}) => url.pathname.includes('/user/profile') || url.pathname.includes('/users'),
        new self.workbox.strategies.NetworkFirst({
            cacheName: 'atu-user-profile-cache',
            plugins: [
                new self.workbox.expiration.ExpirationPlugin({
                    maxEntries: 10,
                    maxAgeSeconds: 7 * 24 * 60 * 60,
                }),
            ],
        })
    );

    // Generic GET offline fallback
    self.workbox.routing.registerRoute(
        ({request}) => request.method === 'GET',
        new self.workbox.strategies.NetworkFirst({
            cacheName: 'atu-general-get-cache',
        })
    );
} else {
    console.log('Workbox failed to load. Falling back to manual cache strategies.');
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
}

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

// OpenAPI Swagger UI interactive docs
Route::get('/docs', [SwaggerController::class, 'index']);
Route::get('/docs/openapi.json', [SwaggerController::class, 'openapiJson']);

// API Health Check Route for Railway monitoring
Route::get('/health', [HealthController::class, 'check']);

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

Route::post('/vendor/food-items/{id}/update-inventory', function (\Illuminate\Http\Request $request, $id) {
    $food = \App\Models\FoodItem::findOrFail($id);
    
    $validator = \Illuminate\Support\Facades\Validator::make($request->all(), [
        'initial_stock' => 'required|integer|min:1',
        'low_stock_threshold' => 'required|integer|min:0',
    ]);

    if ($validator->fails()) {
        return redirect()->back()->withErrors($validator)->withInput();
    }

    $food->initial_stock = (int)$request->input('initial_stock');
    $food->low_stock_threshold = (int)$request->input('low_stock_threshold');
    $food->save();

    return redirect()->back()->with('success', "Inventory for '{$food->name}' updated successfully!");
});

// Role guards
$checkStudent = function ($request, $next) {
    $user = $request->user();
    if (!$user) {
        return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
    }
    if (strtoupper($user->role) !== 'STUDENT') {
        return response()->json(['success' => false, 'message' => 'Unauthorized. This endpoint is for students only.'], 403);
    }
    return $next($request);
};

$checkVendor = function ($request, $next) {
    $user = $request->user();
    if (!$user) {
        return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
    }
    $role = strtoupper($user->role);
    if ($role !== 'VENDOR') {
        return response()->json(['success' => false, 'message' => 'Unauthorized. This endpoint is for vendors only.'], 403);
    }
    
    // Verify Sanctum ability if using Sanctum tokens
    if (method_exists($user, 'tokenCan') && $user->currentAccessToken() && !$user->tokenCan('vendor') && $role === 'VENDOR') {
        return response()->json(['success' => false, 'message' => 'Unauthorized token ability for vendor.'], 403);
    }
    
    return $next($request);
};

$checkAdmin = function ($request, $next) {
    $user = $request->user();
    if (!$user) {
        return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
    }
    
    $role = strtoupper($user->role);
    if ($role !== 'ADMIN') {
        return response()->json(['success' => false, 'message' => 'Unauthorized. This endpoint requires ADMIN privileges.'], 403);
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
        // User administration and destructive operations are ADMIN-only.
        Route::get('/users', [AuthController::class, 'getAllUsers']);
        Route::post('/admin/users', [AuthController::class, 'registerAdmin']);
        Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);
        Route::delete('/feedback/{id}', [FeedbackController::class, 'destroy']);
        Route::delete('/audit-logs/{id}', [AuditLogController::class, 'destroy']);
        Route::delete('/orders/{id}', [OrderController::class, 'destroy']);
        Route::delete('/menu-items/{id}', [MenuItemController::class, 'destroyAdmin']);
        Route::get('/admin/export-sales-csv', [AdminReportController::class, 'exportVendorSalesAndOrdersCsv']);
        Route::get('/admin/export-student-orders-csv', [AdminReportController::class, 'exportStudentOrdersCsv']);
    });

    // --- Student-Only Routes ---
    Route::middleware($checkStudent)->group(function () {
        // Authenticated Student Orders Endpoints
        Route::get('/student/orders', [OrderController::class, 'getAuthenticatedStudentOrders']);
        Route::get('/student/order-history', [OrderController::class, 'getPersonalOrderHistory']);
        Route::post('/student/orders', [OrderController::class, 'storeAuthenticatedStudentOrder']);
        Route::post('/student/cart-checkout', [OrderController::class, 'cartCheckout']);
        Route::get('/student/orders/poll-ready', [OrderController::class, 'pollOrderStatusReady']);
        Route::get('/student/orders/stream-ready', [OrderController::class, 'streamOrderStatusReady']);

        // Favorite Menu Items Endpoints
        Route::get('/student/favorites', [FavoriteMenuItemController::class, 'index']);
        Route::post('/student/favorites', [FavoriteMenuItemController::class, 'store']);
        Route::delete('/student/favorites/{id}', [FavoriteMenuItemController::class, 'destroy']);

        // Student Budget & Spending Analytics Endpoints
        Route::get('/student/budget/analytics', [StudentBudgetController::class, 'getBudgetAnalytics']);
        Route::post('/student/budget/limit', [StudentBudgetController::class, 'setBudgetLimit']);
        Route::post('/student/budget/clear', [StudentBudgetController::class, 'clearBudgetLimit']);

        // Loyalty
        Route::get('/student/loyalty', [LoyaltyController::class, 'index']);
        Route::post('/student/loyalty/redeem', [LoyaltyController::class, 'redeem']);
    });

    // --- Vendor-Only Routes ---
    Route::middleware($checkVendor)->group(function () {
        Route::get('/vendor/orders', [OrderController::class, 'getVendorOrders']);
        Route::post('/vendor/orders/{id}/status', [OrderController::class, 'updateStatus']);
        Route::get('/vendor/inventory', [VendorController::class, 'inventory']);
        Route::get('/vendor/performance', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
        Route::get('/vendor/performance-report', [VendorPerformanceController::class, 'downloadReport']);
        Route::get('/vendor/analytics', [VendorSpecificController::class, 'analytics']);
        Route::get('/vendor/metrics', [VendorMetricsController::class, 'getVendorMetrics']);
        Route::get('/vendor/ingredient-demand', [IngredientDemandController::class, 'getDemand']);
        Route::post('/vendor/menu-items', [VendorMenuItemController::class, 'store']);
        Route::put('/vendor/menu-items/{id}', [VendorMenuItemController::class, 'update']);
        Route::delete('/vendor/menu-items/{id}', [VendorMenuItemController::class, 'destroy']);
        Route::post('/vendor/menu/availability', [MenuController::class, 'updateAvailability']);
        Route::get('/vendor/reports/weekly', [WeeklyReportCronController::class, 'downloadReport']);
    });

    // --- Shared Authenticated Routes ---
    Route::get('/notifications', [AuditLogController::class, 'notifications']);
    Route::post('/notifications/read-all', [AuditLogController::class, 'markAllRead']);
    Route::get('/chats/conversation/{otherUserId}', [ChatController::class, 'getConversation']);
    Route::post('/chats/send', [ChatController::class, 'sendMessage']);
    Route::get('/chats/recent', [ChatController::class, 'getRecentChats']);

    // Paystack Payment Integration Protected Endpoints
    Route::post('/paystack/initialize', [PaystackPaymentController::class, 'initialize']);
    Route::get('/paystack/verify/{reference}', [PaystackPaymentController::class, 'verify']);
});

// Automated Inventory & Availability Cron Checker routes
Route::match(['get', 'post'], '/cron/check-availability', [InventoryCronController::class, 'checkAndNotify']);
Route::post('/vendor/inventory/trigger-threshold', [InventoryCronController::class, 'checkItemStock']);
Route::match(['get', 'post'], '/cron/weekly-performance-report', [WeeklyReportCronController::class, 'emailWeeklyReports']);

// Menu, Standalone Menu Items, & Food Items Endpoints (Public Reads)
Route::get('/menus', [MenuController::class, 'index']);
Route::get('/menus/vendor/{vendorId}', [MenuController::class, 'getVendorMenu']);
Route::get('/menu-items', [MenuController::class, 'listMenuItems']);
Route::get('/menu-items/search', [MenuController::class, 'search']);
Route::get('/menu-items/vendor/{vendorId}', [MenuController::class, 'getVendorMenuItems']);

Route::get('/food-items', [FoodItemController::class, 'index']);
Route::get('/food-items/vendor/{vendorId}', [FoodItemController::class, 'getVendorFoodItems']);

// Pre-Orders & Transactions Endpoints
Route::get('/orders', [OrderController::class, 'index']);
Route::get('/orders/{id}', [OrderController::class, 'show']);
Route::get('/orders/{id}/receipt', [OrderController::class, 'downloadReceipt']);
Route::get('/orders/customer/{customerId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/student/{studentId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/history/{studentId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/vendor/{vendorId}', [OrderController::class, 'getVendorOrders']);
Route::post('/orders', [OrderController::class, 'store']);

Route::get('/vendor/performance-analytics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
Route::get('/vendor/performance-metrics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
Route::get('/vendor/performance-recharts', [VendorPerformanceController::class, 'exportSalesForRecharts']);
Route::get('/vendor/statistics', [VendorPerformanceController::class, 'getAggregatedStatistics']);

// Vendor Rating & Completion Speed Metrics Endpoints
Route::get('/vendor/{vendorId}/metrics', [VendorMetricsController::class, 'getVendorMetrics']);
Route::get('/vendor/{vendorId}/estimated-wait-time', [OrderController::class, 'getVendorWaitTime']);
Route::get('/vendors/metrics', [VendorMetricsController::class, 'getAllVendorsMetrics']);

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

// Delivered Order Reviews & Ratings Endpoints
Route::get('/reviews', [DeliveredOrderReviewController::class, 'index']);
Route::get('/reviews/vendor/{vendorId}', [DeliveredOrderReviewController::class, 'getVendorReviews']);
Route::get('/reviews/food-item/{foodItemId}', [DeliveredOrderReviewController::class, 'getFoodItemReviews']);
Route::post('/reviews', [DeliveredOrderReviewController::class, 'store']);

// Centralised Quality Assurance Traceability Audit Logs Endpoints
Route::get('/audit-logs', [AuditLogController::class, 'index']);
Route::post('/audit-logs', [AuditLogController::class, 'store']);

// System Status Monitoring (JSON health check of DB & Cache)
Route::get('/system/status', [VendorController::class, 'getSystemHealth']);
