<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Services\PerformanceAnalyticsService;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Cache;

class VendorController extends Controller
{
    /**
     * Fetch the authenticated vendor's cafeteria menu items.
     */
    public function getMyFoodItems(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $foodItems = $user->foodItems;

        return response()->json($foodItems, 200);
    }

    /**
     * Fetch the authenticated vendor's order history.
     */
    public function getMyOrders(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $orders = $user->vendorOrders()->orderBy('order_timestamp', 'desc')->get();

        return response()->json($orders, 200);
    }

    /**
     * Fetch authenticated vendor's performance report (aggregate completion times, ratings, order volumes).
     */
    public function getMyAnalytics(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $service = new PerformanceAnalyticsService();
        $report = $service->getVendorReport($user->id);

        return response()->json($report, 200);
    }

    /**
     * Fetch competitive vendor rankings and comparison report.
     */
    public function getComparativeAnalytics(Request $request)
    {
        $service = new PerformanceAnalyticsService();
        $report = $service->getComparativeVendorsReport();

        return response()->json($report, 200);
    }

    

    

    

    

    /**
     * Toggle or explicitly set the authenticated vendor's open status (is_open).
     */
    public function toggleStatus(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        // Check if explicit status was passed, otherwise toggle the current status
        if ($request->has('is_open')) {
            $user->is_open = filter_var($request->input('is_open'), FILTER_VALIDATE_BOOLEAN);
        } else {
            $user->is_open = !$user->is_open;
        }

        $user->save();

        return response()->json([
            'success' => true,
            'is_open' => $user->is_open,
            'message' => "Cafeteria open status updated to " . ($user->is_open ? 'OPEN' : 'CLOSED') . "."
        ], 200);
    }

    /**
     * Retrieve system-wide health and connectivity diagnostics (DB and Cache).
     */
    public function getSystemHealth()
    {
        $dbStatus = 'OK';
        $dbMessage = 'Successfully connected to database.';
        try {
            DB::connection()->getPdo();
        } catch (\Exception $e) {
            $dbStatus = 'FAILED';
            $dbMessage = $e->getMessage();
        }

        $cacheStatus = 'OK';
        $cacheMessage = 'Successfully verified cache store functionality.';
        try {
            Cache::store()->get('health_check_test_key');
            Cache::store()->put('health_check_test_key', 'OK', 10);
        } catch (\Exception $e) {
            $cacheStatus = 'FAILED';
            $cacheMessage = $e->getMessage();
        }

        $diskFree = 'N/A';
        try {
            $diskFree = round(disk_free_space('/') / 1024 / 1024 / 1024, 2) . ' GB';
        } catch (\Exception $e) {}

        $health = [
            'status' => ($dbStatus === 'OK' && $cacheStatus === 'OK') ? 'HEALTHY' : 'UNHEALTHY',
            'timestamp' => now()->toIso8601String(),
            'php_version' => PHP_VERSION,
            'environment' => config('app.env'),
            'debug_mode' => config('app.debug'),
            'services' => [
                'database' => [
                    'status' => $dbStatus,
                    'driver' => config('database.default'),
                    'message' => $dbMessage,
                ],
                'cache' => [
                    'status' => $cacheStatus,
                    'driver' => config('cache.default'),
                    'message' => $cacheMessage,
                ],
            ],
            'diagnostics' => [
                'memory_usage_mb' => round(memory_get_usage(true) / 1024 / 1024, 2),
                'disk_free_space_gb' => $diskFree,
            ]
        ];

        return response()->json($health, $health['status'] === 'HEALTHY' ? 200 : 503);
    }

    /**
     * Parse recent errors and stack traces from laravel.log file safely.
     */
    public function getDiagnosticLogs(Request $request)
    {
        $logPath = storage_path('logs/laravel.log');
        
        if (!file_exists($logPath)) {
            return response()->json([
                'success' => true,
                'logs' => [],
                'message' => 'No log file found at storage/logs/laravel.log yet.'
            ]);
        }
        
        $fileSize = filesize($logPath);
        $maxBytes = 256 * 1024; // 256KB max to avoid memory overload
        $handle = fopen($logPath, 'r');
        
        if ($fileSize > $maxBytes) {
            fseek($handle, -$maxBytes, SEEK_END);
        }
        
        $content = fread($handle, $maxBytes);
        fclose($handle);
        
        // Match standard [YYYY-MM-DD HH:MM:SS] level.ERROR: messages
        preg_match_all('/\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\]\s+([a-zA-Z0-9_-]+)\.([A-Z]+):\s+(.*?)(?=\n\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\]|\z)/s', $content, $matches, PREG_SET_ORDER);
        
        $logs = [];
        foreach ($matches as $match) {
            $timestamp = $match[1];
            $env = $match[2];
            $level = $match[3];
            $message = trim($match[4]);
            
            $shortMessage = strtok($message, "\n");
            $hasStack = strpos($message, "\n") !== false;
            
            $logs[] = [
                'timestamp' => $timestamp,
                'environment' => $env,
                'level' => $level,
                'short_message' => $shortMessage,
                'full_message' => $message,
                'has_stack' => $hasStack,
            ];
        }
        
        // Reverse logs to show the most recent entries first
        $logs = array_reverse($logs);
        
        return response()->json([
            'success' => true,
            'file_size_kb' => round($fileSize / 1024, 2),
            'logs' => array_slice($logs, 0, 100),
        ]);
    }

    /**
     * Clear current log file contents.
     */
    public function clearDiagnosticLogs(Request $request)
    {
        $logPath = storage_path('logs/laravel.log');
        if (file_exists($logPath)) {
            file_put_contents($logPath, '');
            return response()->json(['success' => true, 'message' => 'Log file cleared successfully.']);
        }
        return response()->json(['success' => false, 'message' => 'Log file does not exist.']);
    }
}
