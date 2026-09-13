<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;
use App\Models\RequestPerformanceLog;
use App\Models\SystemLog;
use Illuminate\Support\Facades\Log;

class LogApiRequestsAndResponses
{
    /**
     * Handle an incoming request and log its response and latency.
     */
    public function handle(Request $request, Closure $next): Response
    {
        $startTime = microtime(true);

        $response = $next($request);

        try {
            $durationMs = round((microtime(true) - $startTime) * 1000, 2);
            $statusCode = $response->getStatusCode();
            $path = $request->path();
            $method = $request->method();

            // Filter out sensitive parameters from payload
            $requestPayload = $request->except(['password', 'password_confirmation', 'pin', 'token']);
            $payloadJson = !empty($requestPayload) ? json_encode($requestPayload, JSON_UNESCAPED_SLASHES) : null;

            // Capture response content preview (limit to 1000 chars)
            $responseContent = $response->getContent();
            $responsePreview = is_string($responseContent) ? substr($responseContent, 0, 1000) : null;

            // Save to RequestPerformanceLog
            if (class_exists(RequestPerformanceLog::class)) {
                RequestPerformanceLog::create([
                    'method' => $method,
                    'path' => $path,
                    'status_code' => $statusCode,
                    'execution_time_ms' => $durationMs,
                    'memory_usage_mb' => round(memory_get_peak_usage(true) / (1024 * 1024), 2),
                    'ip_address' => $request->ip(),
                    'request_payload' => $payloadJson,
                ]);
            }

            // Log AI endpoints and errors into SystemLog table if available
            if ($statusCode >= 400) {
                if (class_exists(SystemLog::class)) {
                    SystemLog::create([
                        'level' => $statusCode >= 500 ? 'ERROR' : ($statusCode >= 400 ? 'WARNING' : 'INFO'),
                        'event' => 'API_REQUEST_LOG',
                        'message' => sprintf("[%s] %s -> %d (%s ms)", $method, $path, $statusCode, $durationMs),
                        'context' => json_encode([
                            'ip' => $request->ip(),
                            'payload' => $requestPayload,
                            'response_preview' => $responsePreview,
                            'duration_ms' => $durationMs,
                        ]),
                    ]);
                }
            }
        } catch (\Throwable $e) {
            Log::error('LogApiRequestsAndResponses error: ' . $e->getMessage());
        }

        return $response;
    }
}
