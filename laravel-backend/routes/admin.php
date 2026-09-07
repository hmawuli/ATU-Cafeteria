<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\Api\AdminController;

/*
|--------------------------------------------------------------------------
| ATU Cafeteria Administrator API
|--------------------------------------------------------------------------
| AdminController performs a server-side JWT signature + ADMIN role check
| on every endpoint. No client-side role flag can grant these privileges.
*/

Route::prefix('api/admin')->group(function () {
    Route::get('/overview', [AdminController::class, 'overview']);
    Route::get('/users', [AdminController::class, 'users']);
    Route::post('/vendors', [AdminController::class, 'createVendor']);
    Route::patch('/vendors/{vendorId}', [AdminController::class, 'updateVendor']);
    Route::delete('/vendors/{vendorId}', [AdminController::class, 'deleteVendor']);
});
