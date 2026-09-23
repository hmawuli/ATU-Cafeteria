<?php

namespace App\Providers;

use App\Models\Order;
use App\Models\WalletTransaction;
use App\Observers\OrderStatusObserver;
use App\Observers\WalletTransactionObserver;
use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        //
    }

    public function boot(): void
    {
        Order::observe(OrderStatusObserver::class);
        WalletTransaction::observe(WalletTransactionObserver::class);

        RateLimiter::for('api', function (Request $request) {
            return Limit::perMinute(60)->by($request->user()?->id ?: $request->ip());
        });

        RateLimiter::for('auth', function (Request $request) {
            return Limit::perMinute(5)
                ->by(strtolower(trim((string) $request->input('username', ''))).'|'.$request->ip())
                ->response(function (Request $request, array $headers) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Too many authentication attempts. Please wait a minute and try again.',
                        'error_code' => 'AUTH_RATE_LIMITED',
                    ], 429, $headers);
                });
        });

        // Payment initialization, verification and gateway callbacks are
        // deliberately stricter because they mutate financial state.
        RateLimiter::for('payments', function (Request $request) {
            $identity = $request->user()?->id ?: $request->ip();
            $reference = trim((string) $request->route('reference', ''));

            return Limit::perMinute(10)
                ->by($identity.'|'.$reference)
                ->response(function (Request $request, array $headers) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Too many payment requests. Please wait a minute and try again.',
                        'error_code' => 'PAYMENT_RATE_LIMITED',
                    ], 429, $headers);
                });
        });
    }
}
