<?php

namespace App\Providers;

use Illuminate\Support\ServiceProvider;

use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Http\Request;

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
        // Define rate limiting for general API routes (60 requests per minute per IP/User)
        RateLimiter::for('api', function (Request $request) {
            return Limit::perMinute(60)->by($request->user()?->id ?: $request->ip());
        });

        // Define rate limiting for Authentication routes (10 attempts per minute per IP to prevent brute-forcing)
        RateLimiter::for('auth', function (Request $request) {
            return Limit::perMinute(5)->by(strtolower(trim((string) $request->input('username', ''))) . '|' . $request->ip())->response(function (Request $request, array $headers) { return response()->json(['success'=>false,'message'=>'Too many authentication attempts. Please wait a minute and try again.','error_code'=>'AUTH_RATE_LIMITED'],429,$headers); });
        });

        // Define rate limiting specifically for Gemini AI recommendation routes (10 requests per minute per user/IP)
        RateLimiter::for('gemini', function (Request $request) {
            return Limit::perMinute(10)->by($request->user()?->id ?: $request->ip())->response(function (Request $request, array $headers) {
                return response()->json([
                    'success' => false,
                    'message' => 'Too many Gemini AI recommendation requests. Please wait a moment before trying again.',
                    'error_code' => 'GEMINI_RATE_LIMIT_EXCEEDED',
                    'status_code' => 429
                ], 429, $headers);
            });
        });
    }
}
