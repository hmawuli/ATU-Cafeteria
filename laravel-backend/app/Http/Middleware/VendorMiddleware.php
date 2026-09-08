<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class VendorMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        if (!$user || strtoupper((string) $user->role) !== 'VENDOR' || !$user->tokenCan('vendor')) {
            return response()->json(['success' => false, 'message' => 'Vendor privileges are required.'], 403);
        }
        return $next($request);
    }
}
