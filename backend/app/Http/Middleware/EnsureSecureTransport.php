<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

final class EnsureSecureTransport
{
    public function handle(Request $request, Closure $next): Response
    {
        if (app()->environment('production') && ! $request->secure()) {
            return response()->json([
                'success' => false,
                'message' => 'Secure HTTPS transport is required.',
                'error_code' => 'HTTPS_REQUIRED',
            ], 426);
        }

        return $next($request);
    }
}
