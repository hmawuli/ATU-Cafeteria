<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

final class RoleMiddleware
{
    public function handle(Request $request, Closure $next, ...$roles): Response
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        if (method_exists($user, 'isActive') && ! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Your account is not active.'], 403);
        }

        $allowed = array_map('strtoupper', $roles);
        if (! in_array(strtoupper((string) $user->role), $allowed, true)) {
            return response()->json(['success' => false, 'message' => 'You are not authorized to perform this action.'], 403);
        }

        return $next($request);
    }
}
