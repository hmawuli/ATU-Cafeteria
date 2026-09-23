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
     * Public liveness/readiness check.
     *
     * Deliberately avoids returning exception text, runtime versions, database
     * driver names, queue sizes, or other infrastructure details to anonymous
     * callers. Those details belong in server-side monitoring and logs.
     */
    public function check(Request $request): JsonResponse
    {
        $status = 'UP';
        $details = [];

        try {
            DB::select('SELECT 1');
            $details['database'] = ['status' => 'UP'];
        } catch (\Throwable $e) {
            $status = 'DOWN';
            $details['database'] = ['status' => 'DOWN'];
        }

        try {
            $key = 'health:'.bin2hex(random_bytes(8));
            Cache::put($key, 'OK', 10);
            $cacheHealthy = Cache::get($key) === 'OK';
            Cache::forget($key);

            if ($cacheHealthy) {
                $details['cache'] = ['status' => 'UP'];
            } else {
                if ($status !== 'DOWN') {
                    $status = 'DEGRADED';
                }
                $details['cache'] = ['status' => 'DOWN'];
            }
        } catch (\Throwable $e) {
            if ($status !== 'DOWN') {
                $status = 'DEGRADED';
            }
            $details['cache'] = ['status' => 'DOWN'];
        }

        try {
            Queue::connection()->size();
            $details['queue'] = ['status' => 'UP'];
        } catch (\Throwable $e) {
            if ($status !== 'DOWN') {
                $status = 'DEGRADED';
            }
            $details['queue'] = ['status' => 'DOWN'];
        }

        return response()->json([
            'status' => $status,
            'timestamp' => now()->toIso8601String(),
            'services' => $details,
        ], $status === 'DOWN' ? 503 : 200)
            ->header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0');
    }
}
