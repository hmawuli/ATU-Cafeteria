<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use App\Models\RequestPerformanceLog;
use Illuminate\Support\Facades\Log;

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
     * @param  \Illuminate\Http\Request  $request
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     * @return \Symfony\Component\HttpFoundation\Response
     */
    public function handle(Request $request, Closure $next): Response
    {
        // Record the start time using high precision
        $this->startTime = microtime(true);

        return $next($request);
    }

    /**
     * Handle tasks after the response has been sent to the browser.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  \Symfony\Component\HttpFoundation\Response  $response
     * @return void
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
            $payloadJson = !empty($payload) ? json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) : null;

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
            Log::error('Performance logging failed: ' . $e->getMessage());
        }
    }
}
