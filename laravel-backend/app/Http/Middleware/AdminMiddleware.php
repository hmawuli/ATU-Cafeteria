<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Authorization boundary for administrator-only API endpoints.
 * Authentication is performed by Laravel Sanctum; this middleware only
 * authorizes the already-authenticated user by role and token ability.
 */
class AdminMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();

        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        if (strtoupper((string) $user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Forbidden. Administrator privileges are required.',
            ], 403);
        }

        $token = method_exists($user, 'currentAccessToken')
            ? $user->currentAccessToken()
            : null;

        if ($token && method_exists($user, 'tokenCan') && !$user->tokenCan('admin')) {
            return response()->json([
                'success' => false,
                'message' => 'Forbidden. Invalid administrator token scope.',
            ], 403);
        }

        return $next($request);
    }
}
