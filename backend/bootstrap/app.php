<?php

use App\Http\Middleware\AuditAndSanitizeOrderMiddleware;
use App\Http\Middleware\EnsureSecureTransport;
use App\Http\Middleware\IdempotencyMiddleware;
use App\Http\Middleware\PermissionMiddleware;
use App\Http\Middleware\RequestIdMiddleware;
use App\Http\Middleware\RequestPerformanceLogMiddleware;
use App\Http\Middleware\RequireAuthenticatedApiRoutes;
use App\Http\Middleware\RoleMiddleware;
use App\Http\Middleware\SecureHeadersMiddleware;
use App\Http\Middleware\SystemErrorLoggerMiddleware;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Middleware\HandleCors;
use Illuminate\Support\Facades\Route;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
        then: function () {
            Route::middleware('api')
                ->prefix('api')
                ->group(base_path('routes/customer.php'));
        }
    )
    ->withMiddleware(function (Middleware $middleware) {
        $middleware->validateCsrfTokens(except: ['api/*']);
        $middleware->api(append: [
            'throttle:api', EnsureSecureTransport::class,
            RequireAuthenticatedApiRoutes::class, RequestIdMiddleware::class,
            IdempotencyMiddleware::class,
        ]);
        $middleware->alias([
            'role' => RoleMiddleware::class,
            'permission' => PermissionMiddleware::class,
            'idempotency' => IdempotencyMiddleware::class,
        ]);
        $middleware->append(HandleCors::class);
        $middleware->append(SecureHeadersMiddleware::class);
        $middleware->append(RequestPerformanceLogMiddleware::class);
        $middleware->append(SystemErrorLoggerMiddleware::class);
        $middleware->append(AuditAndSanitizeOrderMiddleware::class);
    })
    ->withExceptions(function (Exceptions $exceptions) {
        //
    })->create();
