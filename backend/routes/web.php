<?php

use Illuminate\Support\Facades\Route;

// The application UI is Flutter. Laravel exposes the REST API.
Route::get('/', function () {
    return response()->json([
        'app' => 'ATU Cafeteria API',
        'status' => 'Healthy',
        'framework' => 'Laravel 11',
    ]);
});

Route::get('/health', function () {
    return response()->json([
        'app' => 'ATU Cafeteria API',
        'status' => 'Healthy',
        'framework' => 'Laravel 11',
    ]);
});
