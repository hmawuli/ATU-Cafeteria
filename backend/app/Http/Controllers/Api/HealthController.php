<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Queue;

class HealthController extends Controller
{
    /**
     * Perform system health check of the application.
     * Verifies database connectivity, cache functionality, and queue worker status.
     *
     * @return JsonResponse
     */
    public function check(Request $request)
    {
        $status = 'UP';
        $details = [];

        // 1. Check Database Connectivity
        try {
            // Force connection and run a simple query
            DB::connection()->getPdo();
            $dbCheck = DB::select('SELECT 1');

            if (! empty($dbCheck)) {
                $details['database'] = [
                    'status' => 'UP',
                    'message' => 'Database connection established successfully.',
                    'driver' => DB::connection()->getDriverName(),
                ];
            } else {
                $status = 'DEGRADED';
                $details['database'] = [
                    'status' => 'DOWN',
                    'message' => 'Database query returned empty result.',
                ];
            }
        } catch (\Exception $e) {
            $status = 'DOWN';
            $details['database'] = [
                'status' => 'DOWN',
                'message' => $e->getMessage(),
            ];
        }

        // 3. Check Cache Status
        try {
            Cache::put('health_check_key', 'OK', 10);
            $cacheVal = Cache::get('health_check_key');

            if ($cacheVal === 'OK') {
                $details['cache'] = [
                    'status' => 'UP',
                    'message' => 'Cache is functioning normally.',
                    'driver' => config('cache.default'),
                ];
            } else {
                if ($status !== 'DOWN') {
                    $status = 'DEGRADED';
                }
                $details['cache'] = [
                    'status' => 'DOWN',
                    'message' => 'Cache write succeeded but read failed.',
                ];
            }
        } catch (\Exception $e) {
            if ($status !== 'DOWN') {
                $status = 'DEGRADED';
            }
            $details['cache'] = [
                'status' => 'DOWN',
                'message' => $e->getMessage(),
            ];
        }

        // 3. Check Queue Status
        try {
            $connectionName = config('queue.default');
            $connection = Queue::connection($connectionName);
            $size = $connection->size();

            $details['queue'] = [
                'status' => 'UP',
                'message' => 'Queue system is reachable.',
                'driver' => $connectionName,
                'pending_jobs' => $size,
            ];
        } catch (\Exception $e) {
            if ($status !== 'DOWN') {
                $status = 'DEGRADED';
            }
            $details['queue'] = [
                'status' => 'DOWN',
                'message' => $e->getMessage(),
            ];
        }

        // 4. System Info
        $systemInfo = [
            'php_version' => PHP_VERSION,
            'laravel_version' => app()->version(),
            'environment' => app()->environment(),
            'os' => PHP_OS,
        ];

        return response()->json([
            'status' => $status,
            'timestamp' => now()->toIso8601String(),
            'services' => $details,
            'system' => $systemInfo,
        ], $status === 'DOWN' ? 503 : 200);
    }
}
