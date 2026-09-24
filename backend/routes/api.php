<?php

use App\Events\OrderStatusCompleted;
use App\Events\OrderStatusReady;
use App\Http\Controllers\Api\AdminManagementController;
use App\Http\Controllers\Api\AdminOperationsController;
use App\Http\Controllers\Api\InventoryController;
use App\Http\Controllers\Api\AdminReportController;
use App\Http\Controllers\Api\AuditLogController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\ChatController;
use App\Http\Controllers\Api\DailyRevenueController;
use App\Http\Controllers\Api\DeliveredOrderReviewController;
use App\Http\Controllers\Api\FavoriteMenuItemController;
use App\Http\Controllers\Api\FeedbackController;
use App\Http\Controllers\Api\FoodItemController;
use App\Http\Controllers\Api\FoodItemFeedbackController;
use App\Http\Controllers\Api\GroupOrderController;
use App\Http\Controllers\Api\HealthController;
use App\Http\Controllers\Api\InventoryCronController;
use App\Http\Controllers\Api\LoyaltyController;
use App\Http\Controllers\Api\MenuController;
use App\Http\Controllers\Api\MenuItemController;
use App\Http\Controllers\Api\NotificationController;
use App\Http\Controllers\Api\OrderController;
use App\Http\Controllers\Api\OrderItemMetricsController;
use App\Http\Controllers\Api\PaystackPaymentController;
use App\Http\Controllers\Api\SmartCafeteriaController;
use App\Http\Controllers\Api\StudentAuthController;
use App\Http\Controllers\Api\StudentBudgetController;
use App\Http\Controllers\Api\SwaggerController;
use App\Http\Controllers\Api\VendorAuthController;
use App\Http\Controllers\Api\VendorFinanceController;
use App\Http\Controllers\Api\VendorKioskOrderController;
use App\Http\Controllers\Api\VendorInventorySummaryController;
use App\Http\Controllers\Api\VendorPromotionController;
use App\Http\Controllers\Api\VendorPayoutAccountController;
use App\Http\Controllers\Api\VendorController;
use App\Http\Controllers\Api\VendorMenuItemController;
use App\Http\Controllers\Api\VendorMetricsController;
use App\Http\Controllers\Api\VendorPerformanceController;
use App\Http\Controllers\Api\VendorSpecificController;
use App\Http\Controllers\Api\WalletController;
use App\Http\Controllers\Api\WeeklyReportCronController;
use App\Http\Middleware\InactivityTimeout;
use App\Listeners\SendOrderCompletedNotification;
use App\Listeners\SendOrderReadyNotification;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Route;

Event::listen(OrderStatusCompleted::class, SendOrderCompletedNotification::class);
Event::listen(OrderStatusReady::class, SendOrderReadyNotification::class);

// Public authentication endpoints.
Route::post('/register', [AuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:auth');
Route::post('/login/2fa', [AuthController::class, 'verifyTwoFactor'])->middleware('throttle:auth');
Route::post('/password/forgot', [AuthController::class, 'forgotPassword'])->middleware('throttle:auth');
Route::post('/password/reset', [AuthController::class, 'resetPassword'])->middleware('throttle:auth');

// Customer API. Legacy student endpoints remain below for backward compatibility.
Route::post('/student/register', [StudentAuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/student/login', [StudentAuthController::class, 'login'])->middleware('throttle:auth');
Route::post('/vendor/login', [VendorAuthController::class, 'login'])->middleware('throttle:auth');

// Public catalogue endpoints. Product availability is public information; customer
// authentication is still required for cart, checkout and account operations.
Route::get('/catalog/food-items', [FoodItemController::class, 'index']);
Route::get('/catalog/menu-items', [MenuItemController::class, 'index']);

// Documentation and monitoring.
Route::get('/docs', [SwaggerController::class, 'index']);
Route::get('/docs/openapi.json', [SwaggerController::class, 'openapiJson']);
Route::get('/health', [HealthController::class, 'check']);

// Paystack calls this endpoint directly. It is protected by Paystack's HMAC
// signature inside the controller, not by Sanctum or application throttling.
Route::post('/paystack/webhook', [PaystackPaymentController::class, 'webhook']);

Route::middleware(['auth:sanctum', InactivityTimeout::class])->group(function () {
    Route::get('/orders/{orderId}/queue', [SmartCafeteriaController::class, 'queue']);

    // Existing recommendation endpoint retained for legacy clients.
    Route::middleware('role:STUDENT')->get('/student/recommendations', [SmartCafeteriaController::class, 'recommendations']);

    Route::middleware('role:VENDOR')->group(function () {
        Route::get('/vendor/demand-forecast', [SmartCafeteriaController::class, 'demandForecast']);
        Route::post('/vendor/waste', [SmartCafeteriaController::class, 'waste']);
        Route::get('/vendor/waste/summary', [SmartCafeteriaController::class, 'wasteSummary']);
    });

    Route::get('/me', [AuthController::class, 'me']);
    Route::get('/notifications', [NotificationController::class, 'index']);
    Route::post('/notifications/mark-read', [NotificationController::class, 'markAllRead']);
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::post('/email/verification/request', [AuthController::class, 'requestEmailVerification']);
    Route::post('/email/verification/verify', [AuthController::class, 'verifyEmail']);
    Route::post('/admin/security/2fa/request-enable', [AuthController::class, 'requestTwoFactorEnable'])->middleware('role:ADMIN');
    Route::post('/admin/security/2fa/enable', [AuthController::class, 'enableTwoFactor'])->middleware('role:ADMIN');
    Route::post('/admin/security/2fa/disable', [AuthController::class, 'disableTwoFactor'])->middleware('role:ADMIN');
    Route::post('/user/profile', [AuthController::class, 'updateProfile']);
    Route::post('/orders/{id}/cancel', [OrderController::class, 'cancel']);
    Route::get('/orders/{id}/tracking', [OrderController::class, 'trackOrderRealTime']);

    Route::middleware('role:ADMIN')->prefix('admin')->group(function () {
        Route::get('/dashboard', [AdminManagementController::class, 'dashboard'])->middleware('permission:dashboard.view');
        Route::get('/users', [AdminManagementController::class, 'users'])->middleware('permission:users.view');
        Route::patch('/users/{user}/status', [AdminManagementController::class, 'updateUserStatus'])->middleware('permission:users.suspend');
        Route::patch('/users/{user}/admin-level', [AdminManagementController::class, 'updateAdminLevel'])->middleware('permission:users.update');
        Route::get('/vendors', [AdminManagementController::class, 'vendors'])->middleware('permission:vendors.view');
        Route::post('/vendors', [AdminManagementController::class, 'createVendor'])->middleware('permission:vendors.view');
        Route::patch('/vendors/{vendor}/status', [AdminManagementController::class, 'updateVendorStatus'])->middleware('permission:vendors.suspend');
        Route::get('/orders', [AdminManagementController::class, 'orders'])->middleware('permission:orders.view');
        Route::get('/finance/summary', [AdminManagementController::class, 'financeSummary'])->middleware('permission:payments.view');
        Route::post('/finance/users/{user}/wallet-adjustment', [AdminManagementController::class, 'walletAdjustment'])->middleware('permission:payments.adjust');
        Route::get('/settings', [AdminManagementController::class, 'settings'])->middleware('permission:settings.view');
        Route::put('/settings/{key}', [AdminManagementController::class, 'updateSetting'])->middleware('permission:settings.update');
        Route::get('/audit-logs', [AdminManagementController::class, 'auditLogs'])->middleware('permission:audit.view');
        Route::get('/command-center', [SmartCafeteriaController::class, 'commandCenter'])->middleware('permission:dashboard.view');
        Route::get('/security-alerts', [SmartCafeteriaController::class, 'securityAlerts'])->middleware('permission:audit.view');
        Route::get('/security/activity', [AdminManagementController::class, 'securityActivity'])->middleware('permission:audit.view');
        Route::patch('/security-alerts/{alert}/resolve', [SmartCafeteriaController::class, 'resolveSecurityAlert'])->middleware('permission:audit.view');

        Route::get('/inventory/movements', [InventoryController::class, 'index'])->middleware('permission:inventory.view');
        Route::post('/inventory/adjust', [InventoryController::class, 'adjust'])->middleware(['permission:inventory.manage', 'idempotency:required']);

        Route::get('/promotions', [AdminOperationsController::class, 'promotions'])->middleware('permission:promotions.view');
        Route::post('/promotions', [AdminOperationsController::class, 'createPromotion'])->middleware(['permission:promotions.manage', 'idempotency:required']);
        Route::patch('/promotions/{promotion}', [AdminOperationsController::class, 'updatePromotion'])->middleware('permission:promotions.manage');

        Route::get('/refunds', [AdminOperationsController::class, 'refunds'])->middleware('permission:refunds.view');
        Route::post('/orders/{order}/refund', [AdminOperationsController::class, 'refundOrder'])->middleware(['permission:refunds.manage', 'idempotency:required']);

        Route::get('/settlements', [AdminOperationsController::class, 'settlements'])->middleware('permission:settlements.view');
        Route::post('/settlements/generate', [AdminOperationsController::class, 'generateSettlement'])->middleware(['permission:settlements.manage', 'idempotency:required']);
        Route::post('/settlements/{settlement}/payout', [AdminOperationsController::class, 'payoutSettlement'])->middleware(['permission:settlements.manage', 'idempotency:required']);
        Route::post('/settlements/{settlement}/payout/finalize', [AdminOperationsController::class, 'finalizeSettlementPayout'])->middleware(['permission:settlements.manage', 'idempotency:required']);

        Route::get('/support/tickets', [AdminOperationsController::class, 'supportTickets'])->middleware('permission:support.view');
        Route::patch('/support/tickets/{ticket}', [AdminOperationsController::class, 'updateSupportTicket'])->middleware('permission:support.manage');
        Route::get('/reports/sales.csv', [AdminReportController::class, 'exportVendorSalesAndOrdersCsv'])->middleware('permission:reports.view');
        Route::get('/reports/student-orders.csv', [AdminReportController::class, 'exportStudentOrdersCsv'])->middleware('permission:reports.view');
    });

    Route::middleware('role:ADMIN')->group(function () {
        Route::get('/users', [AuthController::class, 'getAllUsers']);
        Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);
        Route::delete('/feedback/{id}', [FeedbackController::class, 'destroy']);
        Route::delete('/audit-logs/{id}', [AuditLogController::class, 'destroy']);
        Route::delete('/orders/{id}', [OrderController::class, 'destroy']);
        Route::delete('/menu-items/{id}', [MenuItemController::class, 'destroyAdmin']);
    });

    // Legacy /student ordering API remains for existing installations.
    Route::post('/paystack/initialize', [PaystackPaymentController::class, 'initialize'])->middleware(['throttle:payments', 'idempotency:required']);
    Route::get('/paystack/verify/{reference}', [PaystackPaymentController::class, 'verify'])->middleware('throttle:payments');

    Route::middleware('role:STUDENT,VENDOR,ADMIN')->group(function () {
        Route::get('/menu-items', [MenuItemController::class, 'index']);
        Route::get('/menus', [MenuController::class, 'index']);
        Route::get('/food-items', [FoodItemController::class, 'index']);
        Route::get('/reviews', [DeliveredOrderReviewController::class, 'index']);
        Route::post('/reviews', [DeliveredOrderReviewController::class, 'store']);
        Route::post('/feedback', [FeedbackController::class, 'store']);
    });

    Route::middleware('role:VENDOR,ADMIN')->group(function () {
        Route::get('/vendor/orders', [OrderController::class, 'getVendorOrders']);
        Route::get('/vendor/orders/{id}', [OrderController::class, 'getVendorOrder']);
        Route::patch('/vendor/orders/{id}/status', [OrderController::class, 'updateStatus']);
        Route::post('/vendor/menu-items', [VendorMenuItemController::class, 'store']);
        Route::put('/vendor/menu-items/{id}', [VendorMenuItemController::class, 'update']);
        Route::delete('/vendor/menu-items/{id}', [VendorMenuItemController::class, 'destroy']);
        Route::get('/vendor/menu-items', [VendorMenuItemController::class, 'index']);
        Route::get('/vendor/metrics', [VendorMetricsController::class, 'index']);
        Route::get('/vendor/performance', [VendorPerformanceController::class, 'index']);
        Route::get('/vendor/finance', [VendorFinanceController::class, 'index']);
        Route::get('/vendor/payout-account', [VendorPayoutAccountController::class, 'show']);
        Route::get('/vendor/payout-banks', [VendorPayoutAccountController::class, 'banks']);
        Route::post('/vendor/payout-account', [VendorPayoutAccountController::class, 'store'])->middleware('idempotency:required');
        Route::get('/vendor/inventory/summary', [VendorInventorySummaryController::class, 'index']);
        Route::patch('/vendor/status', [VendorController::class, 'toggleStatus'])->middleware('idempotency:required');
        Route::post('/vendor/kiosk/orders', [VendorKioskOrderController::class, 'store'])->middleware('idempotency:required');
        Route::get('/vendor/promotions', [VendorPromotionController::class, 'index']);
        Route::post('/vendor/promotions', [VendorPromotionController::class, 'store'])->middleware('idempotency:required');
        Route::patch('/vendor/promotions/{promotion}', [VendorPromotionController::class, 'update']);
        Route::delete('/vendor/promotions/{promotion}', [VendorPromotionController::class, 'destroy'])->middleware('idempotency:required');
        Route::get('/vendor/inventory/movements', [InventoryController::class, 'index'])->middleware('permission:inventory.view');
        Route::post('/vendor/inventory/adjust', [InventoryController::class, 'adjust'])->middleware(['permission:inventory.manage', 'idempotency:required']);
    });

    Route::middleware('role:STUDENT')->group(function () {
        Route::get('/wallet', [WalletController::class, 'index']);
        Route::post('/wallet/top-up', [WalletController::class, 'topUp']);
    });
});
