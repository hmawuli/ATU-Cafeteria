<?php

namespace App\Http\Middleware;

use App\Models\SystemLog;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpFoundation\Response;

class SystemErrorLoggerMiddleware
{
    /**
     * Handle an incoming request and check response status for logging.
     *
     * @param  Closure(Request): (Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $response = $next($request);

        $statusCode = $response->getStatusCode();
        $targetCodes = [401, 403, 404, 500];

        if (in_array($statusCode, $targetCodes)) {
            $exception = $response->exception ?? null;
            $message = '';
            $stackTrace = null;

            if ($exception) {
                $message = $exception->getMessage();
                $stackTrace = $exception->getTraceAsString();
            } else {
                $message = "HTTP Error {$statusCode} occurred on API route: ".$request->path();
                // Attempt to pull message from JSON response content if possible
                try {
                    $content = json_decode($response->getContent(), true);
                    if (is_array($content) && (isset($content['message']) || isset($content['error']))) {
                        $message .= ' | Info: '.($content['message'] ?? $content['error']);
                    }
                } catch (\Throwable $e) {
                    // Ignore content extraction failures
                }
            }

            if (empty($message)) {
                $message = "Failed with status code {$statusCode}";
            }

            // Determine appropriate logging level
            $level = 'ERROR';
            if ($statusCode === 401 || $statusCode === 403) {
                $level = 'SECURITY';
            } elseif ($statusCode === 404) {
                $level = 'WARNING';
            }

            try {
                SystemLog::create([
                    'level' => $level,
                    'status_code' => $statusCode,
                    'method' => $request->method(),
                    'path' => $request->path(),
                    'message' => $message,
                    'stack_trace' => $stackTrace,
                    'ip_address' => $request->ip(),
                    'user_agent' => [
                        'user_id' => Auth::id() ?: null,
                        'agent' => $request->userAgent(),
                        'query_params' => $request->query(),
                    ],
                ]);
            } catch (\Throwable $e) {
                Log::error('Failed to log system error to database: '.$e->getMessage());
            }
        }

        return $response;
    }
}
