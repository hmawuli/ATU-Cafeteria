<?php

use App\Events\OrderStatusCompleted;
use App\Events\OrderStatusReady;
use App\Http\Controllers\Api\AdminManagementController;
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
use App\Http\Controllers\Api\OrderController;
use App\Http\Controllers\Api\OrderItemMetricsController;
use App\Http\Controllers\Api\PaystackPaymentController;
use App\Http\Controllers\Api\SmartCafeteriaController;
use App\Http\Controllers\Api\StudentAuthController;
use App\Http\Controllers\Api\StudentBudgetController;
use App\Http\Controllers\Api\SwaggerController;
use App\Http\Controllers\Api\VendorAuthController;
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
use App\Models\Order;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Route;

// Register explicit listeners for OrderStatusCompleted event
Event::listen(
    OrderStatusCompleted::class,
    SendOrderCompletedNotification::class
);

// Register explicit listeners for OrderStatusReady event
Event::listen(
    OrderStatusReady::class,
    SendOrderReadyNotification::class
);

/*
|--------------------------------------------------------------------------
| ATU Cafeteria REST API Routes
|--------------------------------------------------------------------------
*/

// Auth Endpoints (Generic)
Route::post('/register', [AuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:auth');
Route::post('/login/2fa', [AuthController::class, 'verifyTwoFactor'])->middleware('throttle:auth');
Route::post('/password/forgot', [AuthController::class, 'forgotPassword'])->middleware('throttle:auth');
Route::post('/password/reset', [AuthController::class, 'resetPassword'])->middleware('throttle:auth');

// Specialized Student Sanctum Auth Endpoints
Route::post('/student/register', [StudentAuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/student/login', [StudentAuthController::class, 'login'])->middleware('throttle:auth');

// Specialized Vendor Sanctum Auth Endpoints
// Vendor accounts are created by administrators; public vendor registration is intentionally disabled.
Route::post('/vendor/login', [VendorAuthController::class, 'login'])->middleware('throttle:auth');

// OpenAPI Swagger UI interactive docs
Route::get('/docs', [SwaggerController::class, 'index']);
Route::get('/docs/openapi.json', [SwaggerController::class, 'openapiJson']);

// API Health Check Route for Railway monitoring
Route::get('/health', [HealthController::class, 'check']);

// Define role checks for Student vs Vendor
// Protected authenticated endpoints. Sanctum is the single API authentication mechanism.
Route::middleware(['auth:sanctum', InactivityTimeout::class])->group(function () {
    // Smart cafeteria services: queue, recommendations, demand, waste and security.
    Route::get('/orders/{orderId}/queue', [SmartCafeteriaController::class, 'queue']);
    Route::middleware('role:STUDENT')->get('/student/recommendations', [SmartCafeteriaController::class, 'recommendations']);
    Route::middleware('role:VENDOR')->group(function () {
        Route::get('/vendor/demand-forecast', [SmartCafeteriaController::class, 'demandForecast']);
        Route::post('/vendor/waste', [SmartCafeteriaController::class, 'waste']);
        Route::get('/vendor/waste/summary', [SmartCafeteriaController::class, 'wasteSummary']);
    });

    Route::get('/me', [AuthController::class, 'me']);
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::post('/email/verification/request', [AuthController::class, 'requestEmailVerification']);
    Route::post('/email/verification/verify', [AuthController::class, 'verifyEmail']);
    Route::post('/admin/security/2fa/request-enable', [AuthController::class, 'requestTwoFactorEnable'])->middleware('role:ADMIN');
    Route::post('/admin/security/2fa/enable', [AuthController::class, 'enableTwoFactor'])->middleware('role:ADMIN');
    Route::post('/admin/security/2fa/disable', [AuthController::class, 'disableTwoFactor'])->middleware('role:ADMIN');
    Route::post('/user/profile', [AuthController::class, 'updateProfile']);
    Route::post('/orders/{id}/cancel', [OrderController::class, 'cancel']);
    Route::get('/orders/{id}/tracking', [OrderController::class, 'trackOrderRealTime']);

    // --- Administrative Governance & RBAC ---
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
        Route::get('/reports/sales.csv', [AdminReportController::class, 'exportVendorSalesAndOrdersCsv'])->middleware('permission:reports.view');
        Route::get('/reports/student-orders.csv', [AdminReportController::class, 'exportStudentOrdersCsv'])->middleware('permission:reports.view');
    });

    // --- Admin-Only Data Deletion Endpoints ---
    Route::middleware('role:ADMIN')->group(function () {
        Route::get('/users', [AuthController::class, 'getAllUsers']);
        Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);
        Route::delete('/feedback/{id}', [FeedbackController::class, 'destroy']);
        Route::delete('/audit-logs/{id}', [AuditLogController::class, 'destroy']);
        Route::delete('/orders/{id}', [OrderController::class, 'destroy']);
        Route::delete('/menu-items/{id}', [MenuItemController::class, 'destroyAdmin']);
    });

    // --- Student-Only Routes ---
    Route::middleware('role:STUDENT,ADMIN')->group(function () {
        // Authenticated Student Orders Endpoints
        Route::get('/student/orders', [OrderController::class, 'getAuthenticatedStudentOrders']);
        Route::get('/student/order-history', [OrderController::class, 'getPersonalOrderHistory']);
        Route::post('/student/orders', [OrderController::class, 'storeAuthenticatedStudentOrder']);
        Route::post('/v1/student/orders', [OrderController::class, 'storeAuthenticatedStudentOrder']);
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

        // Student Loyalty Points & Rewards Endpoints
        Route::get('/student/loyalty/summary', [LoyaltyController::class, 'getLoyaltySummary']);
        Route::post('/student/loyalty/preview-discount', [LoyaltyController::class, 'previewDiscount']);

        // Student Shared Group Order Endpoints
        Route::post('/student/group-order', [GroupOrderController::class, 'createSession']);
        Route::get('/student/group-order/{code}', [GroupOrderController::class, 'getSessionDetails']);
        Route::post('/student/group-order/{code}/contribute', [GroupOrderController::class, 'contributeItem']);
        Route::delete('/student/group-order/{code}/items/{itemId}', [GroupOrderController::class, 'removeContribution']);
        Route::post('/student/group-order/{code}/lock', [GroupOrderController::class, 'lockSession']);
        Route::post('/student/group-order/{code}/cancel', [GroupOrderController::class, 'cancelSession']);
        Route::post('/student/group-order/{code}/checkout', [GroupOrderController::class, 'checkoutSession']);
    });

    // --- Vendor-Only Routes ---
    Route::middleware('role:VENDOR,ADMIN')->group(function () {
        Route::get('/vendor/orders/unread-count', function (Request $request) {
            $vendorId = $request->user()->id;
            $orders = Order::where('vendor_id', $vendorId)
                ->whereIn('status', ['PENDING', 'ORDER_PLACED'])
                ->orderByDesc('id')
                ->get();

            return response()->json([
                'count' => $orders->count(),
                'orders' => $orders,
            ]);
        });

        // Protected Vendor Menu & Food Items Endpoints
        Route::post('/food-items', [FoodItemController::class, 'store']);
        Route::post('/food-items/bulk-toggle', [FoodItemController::class, 'bulkToggle']);
        Route::put('/food-items/{id}', [FoodItemController::class, 'update']);
        Route::delete('/food-items/{id}', [FoodItemController::class, 'destroy']);

        // Protected Vendor Pre-Orders & Hand-offs
        Route::post('/orders/bulk-update', [OrderController::class, 'bulkUpdateStatus']);
        Route::put('/orders/{id}/status', [OrderController::class, 'updateStatus']);
        Route::patch('/orders/{id}/status', [OrderController::class, 'patchStatus']);
        Route::post('/orders/{id}/verify-pickup', [OrderController::class, 'verifyAndCompletePickup']);
        Route::post('/feedback/{id}/reply', [FeedbackController::class, 'reply']);
        Route::get('/vendor/feedback/sentiment', [FeedbackController::class, 'getFeedbackSentimentReport']);
        Route::get('/vendor/{vendorId}/feedback/sentiment', [FeedbackController::class, 'getFeedbackSentimentReport']);

        // Authenticated Vendor Private Feeds
        Route::get('/vendor/my-menu', [VendorController::class, 'getMyFoodItems']);
        Route::get('/vendor/my-orders', [VendorController::class, 'getMyOrders']);
        Route::get('/vendor/analytics', [VendorController::class, 'getMyAnalytics']);
        Route::get('/vendor/analytics/comparative', [VendorController::class, 'getComparativeAnalytics']);
        Route::get('/vendor/performance-metrics', [VendorPerformanceController::class, 'getVendorPerformanceMetrics']);
        Route::get('/vendor/performance', [VendorPerformanceController::class, 'getPerformance']);
        Route::get('/vendor/sales-summary', [VendorMetricsController::class, 'getVendorSalesSummary']);
        Route::get('/vendor/sales-trend-30-days', [VendorMetricsController::class, 'getVendorSalesTrend30Days']);
        Route::get('/vendor/recharts-sales', [VendorPerformanceController::class, 'exportSalesForRecharts']);
        Route::get('/vendor/reports/weekly', [WeeklyReportCronController::class, 'downloadWeeklyReportPdf']);
        Route::get('/vendor/reports/weekly/{vendorId}', [WeeklyReportCronController::class, 'downloadWeeklyReportPdf']);
        Route::get('/vendor/daily-revenue', [DailyRevenueController::class, 'getDailyRevenue']);
        Route::get('/vendor/{vendorId}/daily-sales-revenue', [DailyRevenueController::class, 'getVendorDailyRevenue']);
        Route::post('/vendor/toggle-status', [VendorController::class, 'toggleStatus']);
    });
    // Order Items Dashboard Metrics Endpoints
    Route::get('/vendor/order-items-metrics', [OrderItemMetricsController::class, 'getDashboardMetrics']);
    Route::get('/vendor/order-items-revenue', [OrderItemMetricsController::class, 'getDailyRevenueMetrics']);
    Route::get('/vendor/order-items-menu-metrics', [OrderItemMetricsController::class, 'getMenuItemMetrics']);

    // System diagnostics logs routes
    Route::get('/system/logs', [VendorController::class, 'getDiagnosticLogs']);
    Route::post('/system/logs/clear', [VendorController::class, 'clearDiagnosticLogs']);

    // Vendor Specific endpoints
    Route::get('/vendor/analytics/trends', [VendorSpecificController::class, 'getAnalyticsTrends']);
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

    // Standard Resource route for Vendors to manage their specific MenuItems
    Route::apiResource('vendor-menu-items', VendorMenuItemController::class);
});

// --- Common Authenticated Routes ---
// Digital Wallet & Core Transactions Subsystem
Route::get('/wallet/balance', [WalletController::class, 'getBalance']);
Route::get('/wallet/transactions', [WalletController::class, 'getTransactions']);
Route::post('/wallet/deposit', [WalletController::class, 'deposit']);
Route::post('/wallet/transfer', [WalletController::class, 'transfer']);
Route::post('/wallet/payout', [WalletController::class, 'requestPayout']);

// Database Notifications Subsystem
Route::get('/notifications', function (Request $request) {
    return response()->json([
        'success' => true,
        'notifications' => $request->user()->notifications()->orderBy('created_at', 'desc')->get(),
    ]);
});
Route::post('/notifications/mark-read', function (Request $request) {
    $request->user()->unreadNotifications->markAsRead();

    return response()->json([
        'success' => true,
        'message' => 'All database notifications marked as read.',
    ]);
});

// Chat Conversation Protected Endpoints
Route::get('/chats/conversation/{otherUserId}', [ChatController::class, 'getConversation']);
Route::post('/chats/send', [ChatController::class, 'sendMessage']);
Route::get('/chats/recent', [ChatController::class, 'getRecentChats']);

// Authenticated order and transaction endpoints
Route::get('/orders', [OrderController::class, 'index']);
Route::get('/orders/{id}', [OrderController::class, 'show']);
Route::get('/orders/{id}/receipt', [OrderController::class, 'downloadReceipt']);
Route::get('/orders/customer/{customerId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/student/{studentId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/history/{studentId}', [OrderController::class, 'getCustomerOrders']);
Route::get('/orders/vendor/{vendorId}', [OrderController::class, 'getVendorOrders']);
Route::post('/orders', [OrderController::class, 'store']);

// Paystack Payment Integration Protected Endpoints
Route::post('/paystack/initialize', [PaystackPaymentController::class, 'initialize']);
Route::get('/paystack/verify/{reference}', [PaystackPaymentController::class, 'verify']);

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

// Admin Reporting & CSV Export Endpoints
Route::middleware(['auth:sanctum', 'role:ADMIN', 'permission:reports.view'])->get('/admin/export-sales-csv', [AdminReportController::class, 'exportVendorSalesAndOrdersCsv']);
Route::middleware(['auth:sanctum', 'role:ADMIN', 'permission:reports.view'])->get('/admin/export-student-orders-csv', [AdminReportController::class, 'exportStudentOrdersCsv']);

// System Status Monitoring (JSON health check of DB & Cache)
Route::get('/system/status', [VendorController::class, 'getSystemHealth']);
