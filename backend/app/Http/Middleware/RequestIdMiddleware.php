<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Symfony\Component\HttpFoundation\Response;

class RequestIdMiddleware
{
    /**
     * Attach a stable correlation ID to every request/response.
     * A trusted caller supplied ID is preserved only when it is a safe UUID;
     * otherwise the application generates a new one.
     */
    public function handle(Request $request, Closure $next): Response
    {
        $incoming = trim((string) $request->header('X-Request-ID', ''));
        $requestId = (bool) preg_match(
            '/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/',
            $incoming
        ) ? $incoming : (string) Str::uuid();

        $request->headers->set('X-Request-ID', $requestId);

        $response = $next($request);
        $response->headers->set('X-Request-ID', $requestId);

        return $response;
    }
}
