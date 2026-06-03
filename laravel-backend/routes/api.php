<?php

use Illuminate\Support\Facades\Route;
use Illuminate\Support\Facades\Event;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\StudentAuthController;
use App\Http\Controllers\Api\VendorAuthController;
use App\Http\Controllers\Api\FoodItemController;
use App\Http\Controllers\Api\OrderController;
use App\Http\Controllers\Api\FeedbackController;
use App\Http\Controllers\Api\AuditLogController;
use App\Http\Controllers\Api\VendorController;
use App\Http\Controllers\Api\WalletController;
use App\Http\Controllers\Api\VendorPerformanceController;

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

// Protected Authenticated Endpoints
Route::middleware('auth:sanctum')->group(function () {
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
    Route::get('/vendor/performance', [VendorPerformanceController::class, 'getPerformance']);
    Route::post('/vendor/toggle-status', [VendorController::class, 'toggleStatus']);

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
});

// Menu & Food Items Endpoints (Public Reads)
Route::get('/food-items', [FoodItemController::class, 'index']);
Route::get('/food-items/vendor/{vendorId}', [FoodItemController::class, 'getVendorFoodItems']);

// Pre-Orders & Transactions Endpoints
Route::get('/orders', [OrderController::class, 'index']);
Route::get('/orders/customer/{customerId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/vendor/{vendorId}', [OrderController::class, 'getVendorOrders']);
Route::post('/orders', [OrderController::class, 'store']);

// Customer Compliance & Feedback Endpoints
Route::get('/feedback', [FeedbackController::class, 'index']);
Route::get('/feedback/vendor/{vendorId}', [FeedbackController::class, 'getVendorFeedback']);
Route::post('/feedback', [FeedbackController::class, 'store']);

// Centralised Quality Assurance Traceability Audit Logs Endpoints
Route::get('/audit-logs', [AuditLogController::class, 'index']);
Route::post('/audit-logs', [AuditLogController::class, 'store']);
