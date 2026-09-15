<?php

namespace App\Http\Middleware;

use App\Models\RequestPerformanceLog;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpFoundation\Response;

class RequestPerformanceLogMiddleware
{
    /**
     * Start time of the request.
     *
     * @var float
     */
    protected $startTime;

    /**
     * Handle an incoming request.
     *
     * @param  Closure(Request): (Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        // Record the start time using high precision
        $this->startTime = microtime(true);

        return $next($request);
    }

    /**
     * Handle tasks after the response has been sent to the browser.
     */
    public function terminate(Request $request, Response $response): void
    {
        try {
            // Calculate elapsed time in milliseconds
            $endTime = microtime(true);
            $executionTimeMs = ($endTime - ($this->startTime ?? LARAVEL_START)) * 1000;

            // Get memory usage in Megabytes (MB)
            $memoryUsageMb = memory_get_peak_usage(true) / (1024 * 1024);

            // Filter out sensitive data from request payload for security
            $payload = $request->except(['password', 'password_confirmation', 'token']);
            $payloadJson = ! empty($payload) ? json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) : null;

            // Log performance metrics to the database table
            RequestPerformanceLog::create([
                'method' => $request->method(),
                'path' => $request->path(),
                'status_code' => $response->getStatusCode(),
                'execution_time_ms' => round($executionTimeMs, 2),
                'memory_usage_mb' => round($memoryUsageMb, 2),
                'ip_address' => $request->ip(),
                'request_payload' => $payloadJson,
            ]);

            // If the request took more than 500ms, flag it as a slow request in standard logs
            if ($executionTimeMs > 500) {
                Log::warning(sprintf(
                    'Slow API Request detected: %s [%s] took %.2fms (Memory: %.2fMB)',
                    $request->method(),
                    $request->path(),
                    $executionTimeMs,
                    $memoryUsageMb
                ));
            }
        } catch (\Throwable $e) {
            // Safe fallback logging to file if database insertion fails
            Log::error('Performance logging failed: '.$e->getMessage());
        }
    }
}
