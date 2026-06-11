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

// Register explicit listeners for OrderStatusCompleted event
Event::listen(
    \App\Events\OrderStatusCompleted::class,
    \App\Listeners\SendOrderCompletedNotification::class
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
})->group(function () {
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);

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
    Route::get('/vendor/performance-metrics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
    Route::get('/vendor/performance', [VendorPerformanceController::class, 'getPerformance']);
    Route::get('/vendor/recharts-sales', [VendorPerformanceController::class, 'exportSalesForRecharts']);
    Route::post('/vendor/toggle-status', [VendorController::class, 'toggleStatus']);

    // Vendor Specific endpoints
    Route::put('/vendor/menu/availability', [VendorSpecificController::class, 'updateMenuAvailability']);
    Route::get('/vendor/orders/summary', [VendorSpecificController::class, 'getOrderSummary']);
    Route::post('/vendor/menu/bulk-update', [VendorSpecificController::class, 'bulkUpdateMenu']);
    Route::post('/vendor/menu/bulk-upload', [VendorSpecificController::class, 'bulkUpdateMenu']);

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

    // Vendor Menu Management Protected Endpoints
    Route::post('/menus', [MenuController::class, 'store']);
    Route::put('/menus/{id}', [MenuController::class, 'update']);
    Route::delete('/menus/{id}', [MenuController::class, 'destroy']);
    Route::post('/menu-items', [MenuController::class, 'storeMenuItem']);

    // Chat Conversation Protected Endpoints
    Route::get('/chats/conversation/{otherUserId}', [ChatController::class, 'getConversation']);
    Route::post('/chats/send', [ChatController::class, 'sendMessage']);
    Route::get('/chats/recent', [ChatController::class, 'getRecentChats']);

    // Paystack Payment Integration Protected Endpoints
    Route::post('/paystack/initialize', [PaystackPaymentController::class, 'initialize']);
    Route::get('/paystack/verify/{reference}', [PaystackPaymentController::class, 'verify']);

    // Authenticated Student Orders Endpoints
    Route::get('/student/orders', [OrderController::class, 'getAuthenticatedStudentOrders']);
    Route::post('/student/orders', [OrderController::class, 'storeAuthenticatedStudentOrder']);
});

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
