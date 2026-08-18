<?php

use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    $indexPath = public_path('index.html');
    if (file_exists($indexPath)) {
        return response()->file($indexPath);
    }
    return response()->json([
        'app' => 'ATU Cafeteria Backend API & Web Portal',
        'status' => 'Healthy',
        'framework' => 'Laravel 11'
    ]);
});

Route::get('/health', function () {
    return response()->json([
        'app' => 'ATU Cafeteria Backend API',
        'status' => 'Healthy',
        'framework' => 'Laravel 11'
    ]);
});
