<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class SecureHeadersMiddleware
{
    /**
     * Handle an incoming request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function handle(Request $request, Closure $next): Response
    {
        $response = $next($request);

        // Prevent Clickjacking (X-Frame-Options)
        $response->headers->set('X-Frame-Options', 'DENY');

        // Prevent MIME Sniffing (X-Content-Type-Options)
        $response->headers->set('X-Content-Type-Options', 'nosniff');

        // Enable XSS Protection (X-XSS-Protection)
        $response->headers->set('X-XSS-Protection', '1; mode=block');

        // Referrer Policy
        $response->headers->set('Referrer-Policy', 'no-referrer-when-downgrade');

        // Content Security Policy (CSP) for API - Secure-by-default, restrict source loading
        $response->headers->set('Content-Security-Policy', "default-src 'none'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; connect-src 'self' https:; frame-ancestors 'none'; form-action 'self'; upgrade-insecure-requests");

        // HTTP Strict Transport Security (HSTS) if connection is HTTPS or behind a reverse proxy
        if ($request->isSecure() || $request->header('X-Forwarded-Proto') === 'https') {
            $response->headers->set('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
        }

        // Permissions Policy (formerly Feature-Policy)
        $response->headers->set('Permissions-Policy', 'geolocation=(), microphone=(), camera=(), interest-cohort=()');

        // Prevent information disclosure of server software
        $response->headers->remove('X-Powered-By');
        $response->headers->remove('Server');

        return $response;
    }
}
