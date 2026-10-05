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
use Illuminate\Auth\AuthenticationException;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Middleware\HandleCors;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

$app = Application::configure(basePath: dirname(__DIR__))
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
        // Behind a managed proxy (Vercel), trust its X-Forwarded-* headers so
        // the HTTPS transport guard and generated URLs see the real scheme.
        $trustedProxies = getenv('TRUSTED_PROXIES');
        if (is_string($trustedProxies) && $trustedProxies !== '') {
            $middleware->trustProxies(at: $trustedProxies);
        }

        $middleware->validateCsrfTokens(except: ['api/*']);
        $middleware->api(append: [
            'throttle:api', EnsureSecureTransport::class,
            RequireAuthenticatedApiRoutes::class, RequestIdMiddleware::class,
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
        $exceptions->render(function (AuthenticationException $exception, Request $request) {
            if ($request->is('api/*')) {
                return response()->json([
                    'message' => 'Unauthenticated.',
                ], 401);
            }
        });
    })->create();

// ---------------------------------------------------------------------------
// Serverless container support (Vercel).
//
// Vercel runs the app in an ephemeral container where only /tmp is writable.
// When LARAVEL_STORAGE_PATH is present (a real process environment variable),
// redirect Laravel's writable storage there so compiled views, caches and logs
// keep working. getenv() is used deliberately: bootstrap/app.php runs before the
// .env file is loaded, but platform-provided environment variables are already
// available. Both options are no-ops when unset (local dev, CI, VPS).
// ---------------------------------------------------------------------------
$serverlessStorage = getenv('LARAVEL_STORAGE_PATH');
if (is_string($serverlessStorage) && $serverlessStorage !== '') {
    foreach ([
        $serverlessStorage,
        $serverlessStorage.'/app',
        $serverlessStorage.'/framework',
        $serverlessStorage.'/framework/cache',
        $serverlessStorage.'/framework/cache/data',
        $serverlessStorage.'/framework/sessions',
        $serverlessStorage.'/framework/views',
        $serverlessStorage.'/logs',
    ] as $dir) {
        if (! is_dir($dir)) {
            @mkdir($dir, 0775, true);
        }
    }

    $app->useStoragePath($serverlessStorage);
}

return $app;
