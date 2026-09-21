<?php

namespace App\Providers;

use App\Models\Order;
use App\Observers\OrderStatusObserver;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        //
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        Order::observe(OrderStatusObserver::class);

        // Define rate limiting for general API routes (60 requests per minute per IP/User)
        RateLimiter::for('api', function (Request $request) {
            return Limit::perMinute(60)->by($request->user()?->id ?: $request->ip());
        });

        // Define rate limiting for Authentication routes (5 attempts per minute per IP/User)
        RateLimiter::for('auth', function (Request $request) {
            return Limit::perMinute(5)->by(strtolower(trim((string) $request->input('username', ''))).'|'.$request->ip())->response(function (Request $request, array $headers) {
                return response()->json(['success' => false, 'message' => 'Too many authentication attempts. Please wait a minute and try again.', 'error_code' => 'AUTH_RATE_LIMITED'], 429, $headers);
            });
        });
    }
}
