<?php

use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up'
    )
    ->withMiddleware(function (Middleware $middleware) {
        // Disable CSRF verification for API endpoints
        $middleware->validateCsrfTokens(except: [
            'api/*',
        ]);

        $middleware->api(append: [
            'throttle:api',
            \App\Http\Middleware\EnsureSecureTransport::class,
        ]);

        $middleware->alias([
            'role' => \App\Http\Middleware\RoleMiddleware::class,
            'permission' => \App\Http\Middleware\PermissionMiddleware::class,
        ]);
        
        // Add CORS support
        $middleware->append(\Illuminate\Http\Middleware\HandleCors::class);

        // Security headers middleware
        $middleware->append(\App\Http\Middleware\SecureHeadersMiddleware::class);

        // Request performance logging middleware
        $middleware->append(\App\Http\Middleware\RequestPerformanceLogMiddleware::class);

        // System error database logger middleware
        $middleware->append(\App\Http\Middleware\SystemErrorLoggerMiddleware::class);

        // Audit and sanitize incoming order requests middleware
        $middleware->append(\App\Http\Middleware\AuditAndSanitizeOrderMiddleware::class);
    })
    ->withExceptions(function (Exceptions $exceptions) {
        //
    })->create();
