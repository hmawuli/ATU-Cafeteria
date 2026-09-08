<?php

use App\Http\Controllers\Api\VendorStaffController;
use Illuminate\Support\Facades\Route;

Route::prefix('api/vendor')
    ->middleware(['auth:sanctum', 'vendor'])
    ->group(function () {
        Route::get('/overview', [VendorStaffController::class, 'overview']);
        Route::get('/workers', [VendorStaffController::class, 'workers']);
        Route::post('/workers', [VendorStaffController::class, 'createWorker']);
        Route::patch('/workers/{workerId}', [VendorStaffController::class, 'updateWorker']);
        Route::delete('/workers/{workerId}', [VendorStaffController::class, 'deleteWorker']);
        Route::post('/shifts', [VendorStaffController::class, 'createShift']);
        Route::patch('/shifts/{shiftId}', [VendorStaffController::class, 'updateShift']);
        Route::delete('/shifts/{shiftId}', [VendorStaffController::class, 'deleteShift']);
        Route::get('/reviews', [VendorStaffController::class, 'reviews']);
    });
