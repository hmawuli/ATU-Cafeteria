<?php

use App\Http\Controllers\Api\CustomerAuthController;
use App\Http\Controllers\Api\FavoriteMenuItemController;
use App\Http\Controllers\Api\LoyaltyController;
use App\Http\Controllers\Api\OrderController;
use App\Http\Controllers\Api\StudentBudgetController;
use App\Http\Controllers\Api\WalletController;
use Illuminate\Support\Facades\Route;

// Restaurant-facing customer API. Existing /student routes remain supported.
Route::post('/customer/register', [CustomerAuthController::class, 'register'])->middleware('throttle:auth');
Route::post('/customer/login', [CustomerAuthController::class, 'login'])->middleware('throttle:auth');

Route::middleware(['auth:sanctum'])->group(function () {
    Route::middleware('role:STUDENT,ADMIN')->group(function () {
        Route::get('/customer/orders', [OrderController::class, 'getAuthenticatedStudentOrders']);
        Route::get('/customer/purchased-vendors', [OrderController::class, 'getPurchasedVendors']);
        Route::get('/customer/order-history', [OrderController::class, 'getPersonalOrderHistory']);
        Route::post('/customer/orders', [OrderController::class, 'storeAuthenticatedStudentOrder'])->middleware('throttle:payments');
        Route::post('/customer/cart-checkout', [OrderController::class, 'cartCheckout'])->middleware('throttle:payments');
        Route::get('/customer/orders/poll-ready', [OrderController::class, 'pollOrderStatusReady']);
        Route::get('/customer/orders/stream-ready', [OrderController::class, 'streamOrderStatusReady']);
        Route::get('/customer/favorites', [FavoriteMenuItemController::class, 'index']);
        Route::post('/customer/favorites', [FavoriteMenuItemController::class, 'store']);
        Route::delete('/customer/favorites/{menuItem}', [FavoriteMenuItemController::class, 'destroy']);
        Route::get('/customer/budget', [StudentBudgetController::class, 'show']);
        Route::put('/customer/budget', [StudentBudgetController::class, 'update']);
        Route::get('/customer/loyalty', [LoyaltyController::class, 'index']);
        Route::get('/customer/loyalty/summary', [LoyaltyController::class, 'summary']);
        Route::get('/customer/wallet', [WalletController::class, 'index']);
        Route::post('/customer/wallet/top-up', [WalletController::class, 'topUp'])->middleware('throttle:payments');
    });
});
