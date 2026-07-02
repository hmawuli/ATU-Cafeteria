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

// Protected Authenticated Endpoints - Supports both Sanctum and Secure JWT Auth
Route::middleware(function ($request, $next) {
    $authHeader = $request->header('Authorization') ?: $request->header('X-Auth-Token');
    if ($authHeader && preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
        $token = $matches[1];
    } else {
        $token = $authHeader;
    }

    if ($token) {
        $user = \App\Services\JwtService::getUserFromToken($token);
        if ($user) {
            auth()->setUser($user);
            $request->setUserResolver(function () use ($user) {
                return $user;
            });
            return $next($request);
        }
    }

    // Pass through Sanctum if no JWT was validated
    return app(\Illuminate\Auth\Middleware\Authenticate::class)->handle($request, function ($req) use ($next) {
        return $next($req);
    }, 'sanctum');
})->group(function () use ($checkStudent, $checkVendor) {
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::post('/user/profile', [AuthController::class, 'updateProfile']);
    Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);
    Route::post('/orders/{id}/cancel', [OrderController::class, 'cancel']);
    Route::get('/orders/{id}/tracking', [OrderController::class, 'trackOrderRealTime']);

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
