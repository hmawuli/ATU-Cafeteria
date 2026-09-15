<?php

namespace App\Http\Middleware;

use App\Models\AuditLog;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class AuditAndSanitizeOrderMiddleware
{
    /**
     * Handle an incoming request.
     *
     * @return mixed
     */
    public function handle(Request $request, Closure $next)
    {
        // Identify if this is an order-related route or a post request with inputs
        $isOrderRoute = $request->is('*orders*') || $request->is('*order-item*');

        if ($isOrderRoute && $request->isMethod('POST')) {
            $inputs = $request->all();
            $sanitizedInputs = [];

            foreach ($inputs as $key => $value) {
                if (is_string($value)) {
                    // Sanitize strings: strip HTML/script tags and prevent XSS or basic malicious sequences
                    $clean = strip_tags($value);
                    $clean = htmlspecialchars($clean, ENT_QUOTES, 'UTF-8');
                    $sanitizedInputs[$key] = $clean;
                } elseif (is_array($value)) {
                    $sanitizedInputs[$key] = $this->sanitizeArray($value);
                } else {
                    $sanitizedInputs[$key] = $value;
                }
            }

            // Replace input in request object with sanitized versions
            $request->replace($sanitizedInputs);

            // Audit the request
            try {
                $user = $request->user();
                $userId = $user ? $user->id : null;

                AuditLog::create([
                    'timestamp' => (int) (time() * 1000),
                    'user_id' => $userId,
                    'action' => 'ORDER_PLACEMENT_AUDIT',
                    'details' => json_encode([
                        'url' => $request->fullUrl(),
                        'method' => $request->method(),
                        'ip' => $request->ip(),
                        'payload' => $this->maskSensitiveData($sanitizedInputs),
                    ]),
                ]);
            } catch (\Exception $e) {
                Log::error('AuditAndSanitizeOrderMiddleware Log Error: '.$e->getMessage());
            }
        }

        return $next($request);
    }

    /**
     * Sanitize nested array inputs.
     */
    private function sanitizeArray(array $array): array
    {
        foreach ($array as $key => $value) {
            if (is_string($value)) {
                $array[$key] = htmlspecialchars(strip_tags($value), ENT_QUOTES, 'UTF-8');
            } elseif (is_array($value)) {
                $array[$key] = $this->sanitizeArray($value);
            }
        }

        return $array;
    }

    /**
     * Mask sensitive keys to prevent logging passwords/PINs.
     */
    private function maskSensitiveData(array $payload): array
    {
        $sensitiveKeys = ['password', 'pin', 'pickup_pin', 'credit_card', 'cvv', 'token'];
        foreach ($payload as $key => $value) {
            if (in_array(strtolower($key), $sensitiveKeys)) {
                $payload[$key] = '********';
            }
        }

        return $payload;
    }
}
