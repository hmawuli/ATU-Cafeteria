<?php

use App\Http\Controllers\Api\AdminController;
use App\Http\Controllers\Api\AuthController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Administrator API
|--------------------------------------------------------------------------
| Authentication: Laravel Sanctum personal access token.
| Authorization: explicit ADMIN role + admin token ability.
| No client-side role flag can grant these privileges.
*/

Route::prefix('api/admin')
    ->middleware(['auth:sanctum', 'admin'])
    ->group(function () {
        Route::get('/overview', [AdminController::class, 'overview']);
        Route::get('/users', [AdminController::class, 'users']);
        Route::post('/vendors', [AdminController::class, 'createVendor']);
        Route::patch('/vendors/{vendorId}', [AdminController::class, 'updateVendor']);
        Route::delete('/vendors/{vendorId}', [AdminController::class, 'deleteVendor']);
        Route::post('/administrators', [AuthController::class, 'registerAdmin']);
        Route::delete('/users/{id}', [AuthController::class, 'deleteUser']);
    });
