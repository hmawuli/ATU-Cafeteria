<?php

use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Support\Facades\Route;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
        then: function () {
            Route::middleware('api')->prefix('api/v1')->group(__DIR__.'/../routes/api.php');
            Route::middleware('api')->group(__DIR__.'/../routes/admin.php');
            Route::middleware('api')->group(__DIR__.'/../routes/vendor_staff.php');
        }
    )
    ->withMiddleware(function (Middleware $middleware) {
        $middleware->validateCsrfTokens(except: ['api/*']);
        $middleware->api(append: ['throttle:api']);
        $middleware->alias([
            'admin' => \App\Http\Middleware\AdminMiddleware::class,
            'vendor' => \App\Http\Middleware\VendorMiddleware::class,
        ]);
        $middleware->append(\Illuminate\Http\Middleware\HandleCors::class);
        $middleware->append(\App\Http\Middleware\SecureHeadersMiddleware::class);
        $middleware->append(\App\Http\Middleware\RequestPerformanceLogMiddleware::class);
        $middleware->append(\App\Http\Middleware\SystemErrorLoggerMiddleware::class);
        $middleware->append(\App\Http\Middleware\AuditAndSanitizeOrderMiddleware::class);
    })
    ->withExceptions(function (Exceptions $exceptions) {
        //
    })->create();
