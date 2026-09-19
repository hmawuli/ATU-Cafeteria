<?php

use App\Http\Middleware\AuditAndSanitizeOrderMiddleware;
use App\Http\Middleware\EnsureSecureTransport;
use App\Http\Middleware\PermissionMiddleware;
use App\Http\Middleware\RequestPerformanceLogMiddleware;
use App\Http\Middleware\RequireAuthenticatedApiRoutes;
use App\Http\Middleware\RoleMiddleware;
use App\Http\Middleware\SecureHeadersMiddleware;
use App\Http\Middleware\SystemErrorLoggerMiddleware;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Middleware\HandleCors;

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
            EnsureSecureTransport::class,
            RequireAuthenticatedApiRoutes::class,
        ]);

        $middleware->alias([
            'role' => RoleMiddleware::class,
            'permission' => PermissionMiddleware::class,
        ]);

        // Add CORS support
        $middleware->append(HandleCors::class);

        // Security headers middleware
        $middleware->append(SecureHeadersMiddleware::class);

        // Request performance logging middleware
        $middleware->append(RequestPerformanceLogMiddleware::class);

        // System error database logger middleware
        $middleware->append(SystemErrorLoggerMiddleware::class);

        // Audit and sanitize incoming order requests middleware
        $middleware->append(AuditAndSanitizeOrderMiddleware::class);
    })
    ->withExceptions(function (Exceptions $exceptions) {
        //
    })->create();
