<?php

use App\Http\Controllers\Api\CustomerAccountController;
use App\Http\Controllers\Api\CustomerAddressController;
use App\Http\Controllers\Api\CustomerAuthController;
use App\Http\Controllers\Api\CustomerDeviceController;
use App\Http\Controllers\Api\CustomerDiscoveryController;
use App\Http\Controllers\Api\AuthController;
use App\Http\Controllers\Api\CustomerSupportController;
use App\Http\Controllers\Api\ProductionCartCheckoutController;
use App\Http\Controllers\Api\FavoriteMenuItemController;
use App\Http\Controllers\Api\LoyaltyController;
use App\Http\Controllers\Api\OrderController;
use App\Http\Controllers\Api\StudentBudgetController;
use App\Http\Controllers\Api\WalletController;
use App\Http\Middleware\InactivityTimeout;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Customer API
|--------------------------------------------------------------------------
| Canonical restaurant-facing customer routes. Legacy /student routes in
| routes/api.php remain only for existing installations during migration.
*/

$customerRoutes = function () {
    Route::post('/register', [CustomerAuthController::class, 'register'])->middleware('throttle:auth');
    Route::post('/login', [CustomerAuthController::class, 'login'])->middleware('throttle:auth');

    Route::middleware(['auth:sanctum', InactivityTimeout::class, 'role:STUDENT'])->group(function () {
        Route::get('/me', [AuthController::class, 'me']);
        Route::post('/logout', [AuthController::class, 'logout']);
        Route::post('/pin/change', [AuthController::class, 'changePin'])->middleware('idempotency:required');

        Route::get('/discovery', [CustomerDiscoveryController::class, 'index']);
        Route::get('/recommendations', [\App\Http\Controllers\Api\SmartCafeteriaController::class, 'recommendations']);

        Route::get('/orders', [OrderController::class, 'getAuthenticatedStudentOrders']);
        Route::get('/purchased-vendors', [OrderController::class, 'getPurchasedVendors']);
        Route::get('/order-history', [OrderController::class, 'getPersonalOrderHistory']);
        Route::post('/orders', [OrderController::class, 'storeAuthenticatedStudentOrder'])
            ->middleware(['throttle:payments', 'idempotency:required']);
        Route::post('/cart-checkout', [ProductionCartCheckoutController::class, 'store'])
            ->middleware(['throttle:payments', 'idempotency:required']);
        Route::post('/orders/{id}/cancel', [OrderController::class, 'cancel'])
            ->middleware('idempotency:required');
        Route::get('/orders/{id}/tracking', [OrderController::class, 'trackOrderRealTime']);

        Route::get('/favorites', [FavoriteMenuItemController::class, 'index']);
        Route::post('/favorites', [FavoriteMenuItemController::class, 'store']);
        Route::delete('/favorites/{menuItem}', [FavoriteMenuItemController::class, 'destroy']);

        Route::get('/budget', [StudentBudgetController::class, 'show']);
        Route::put('/budget', [StudentBudgetController::class, 'update']);
        Route::get('/loyalty', [LoyaltyController::class, 'index']);
        Route::get('/loyalty/summary', [LoyaltyController::class, 'summary']);
        Route::post('/loyalty/preview-discount', [LoyaltyController::class, 'previewDiscount']);

        Route::get('/reviews', [\App\Http\Controllers\Api\DeliveredOrderReviewController::class, 'index']);
        Route::post('/reviews', [\App\Http\Controllers\Api\DeliveredOrderReviewController::class, 'store'])
            ->middleware('idempotency:required');

        Route::get('/wallet', [WalletController::class, 'index']);
        Route::post('/wallet/top-up', [WalletController::class, 'topUp'])
            ->middleware(['throttle:payments', 'idempotency:required']);

        Route::get('/devices', [CustomerDeviceController::class, 'index']);
        Route::post('/devices', [CustomerDeviceController::class, 'store']);
        Route::patch('/devices/{device}/heartbeat', [CustomerDeviceController::class, 'heartbeat']);
        Route::delete('/devices/{device}', [CustomerDeviceController::class, 'destroy']);

        Route::get('/addresses', [CustomerAddressController::class, 'index']);
        Route::post('/addresses', [CustomerAddressController::class, 'store'])
            ->middleware('idempotency:required');
        Route::put('/addresses/{address}', [CustomerAddressController::class, 'update']);
        Route::delete('/addresses/{address}', [CustomerAddressController::class, 'destroy']);
        Route::post('/addresses/{address}/default', [CustomerAddressController::class, 'makeDefault']);

        Route::get('/support/tickets', [CustomerSupportController::class, 'index']);
        Route::post('/support/tickets', [CustomerSupportController::class, 'store'])
            ->middleware('idempotency:required');
        Route::get('/support/tickets/{ticket}', [CustomerSupportController::class, 'show']);

        Route::delete('/account', [CustomerAccountController::class, 'destroy'])
            ->middleware('idempotency:required');
    });
};

// New application paths.
Route::prefix('customer')->group($customerRoutes);

// Versioned aliases for future clients.
Route::prefix('v1/customer')->group($customerRoutes);
