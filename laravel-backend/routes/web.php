<?php

use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return response()->json([
        'app' => 'ATU Cafeteria Backend API',
        'status' => 'Healthy',
        'framework' => 'Laravel 11'
    ]);
});
