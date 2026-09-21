<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Laravel\Sanctum\PersonalAccessToken;
use Symfony\Component\HttpFoundation\Response;

/**
 * Protect API resources that must never be callable anonymously.
 *
 * Most application routes already use auth:sanctum explicitly. This guard
 * covers the older common routes (wallet, notifications, chats, orders and
 * Paystack) that historically lived outside that group.
 */
class RequireAuthenticatedApiRoutes
{
    public function handle(Request $request, Closure $next): Response
    {
        if (! $this->requiresAuthentication($request)) {
            return $next($request);
        }

        $token = $request->bearerToken();
        if (! $token) {
            return response()->json([
                'message' => 'Authentication required.',
            ], 401);
        }

        $accessToken = PersonalAccessToken::findToken($token);
        $user = $accessToken?->tokenable;

        if (! $user) {
            return response()->json([
                'message' => 'Unauthenticated.',
            ], 401);
        }

        // Set Laravel's request user resolver so controllers and route
        // closures can safely use $request->user() on these legacy routes.
        $request->setUserResolver(static fn () => $user);

        return $next($request);
    }

    private function requiresAuthentication(Request $request): bool
    {
        $path = trim($request->path(), '/');

        return str_starts_with($path, 'api/wallet/')
            || str_starts_with($path, 'api/notifications')
            || str_starts_with($path, 'api/chats/')
            || str_starts_with($path, 'api/orders')
            || str_starts_with($path, 'api/paystack/');
    }
}
