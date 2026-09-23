<?php

use App\Models\Feedback;
use App\Models\GroupOrder;
use App\Models\GroupOrderItem;
use App\Models\Order;
use App\Models\RequestPerformanceLog;
use App\Models\SystemLog;
use App\Models\User;
use App\Models\VendorPerformanceMetric;
use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\Schedule;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote');

/**
 * Custom console command to clean up system logs, performance logs,
 * and group order sessions older than 30 days.
 */
Artisan::command('cleanup:database', function () {
    $this->info('Starting production database cleanup of records older than 30 days...');

    $cutoffDate = now()->subDays(30);

    // 1. Delete associated group order items first to prevent constraint violations, then delete group orders
    $expiredGroupOrdersCount = GroupOrder::where('created_at', '<', $cutoffDate)->count();

    if ($expiredGroupOrdersCount > 0) {
        GroupOrderItem::whereIn('group_order_id', function ($query) use ($cutoffDate) {
            $query->select('id')->from('group_orders')->where('created_at', '<', $cutoffDate);
        })->delete();

        GroupOrder::where('created_at', '<', $cutoffDate)->delete();
        $this->info("Successfully cleared {$expiredGroupOrdersCount} old group order sessions and their items.");
    } else {
        $this->comment('No old group orders found to clear.');
    }

    // 2. Clear old request performance logs
    $performanceLogsCleared = RequestPerformanceLog::where('created_at', '<', $cutoffDate)->delete();
    $this->info("Successfully cleared {$performanceLogsCleared} old request performance logs.");

    // 3. Clear old system logs
    $systemLogsCleared = SystemLog::where('created_at', '<', $cutoffDate)->delete();
    $this->info("Successfully cleared {$systemLogsCleared} old system logs.");

    Log::info(sprintf(
        'Database Scheduled Cleanup: Cleared %d group orders, %d performance logs, and %d system logs older than 30 days.',
        $expiredGroupOrdersCount,
        $performanceLogsCleared,
        $systemLogsCleared
    ));
})->purpose('Clear logs and expired group order sessions older than 30 days');

// Schedule the database cleanup command to run daily
Schedule::command('cleanup:database')->daily();

// Schedule the database backup command to run daily
Schedule::command('database:backup')->daily();

/**
 * Custom console command to safely dump the active database and save it to both
 * local storage backups directory and external cloud storage.
 */
Artisan::command('database:backup', function () {
    $this->info('Starting database backup process...');

    $connection = config('database.default');
    $config = config("database.connections.{$connection}");

    $timestamp = now()->format('Y-m-d_H-i-s');
    $backupDir = storage_path('app/backups');

    if (! file_exists($backupDir)) {
        mkdir($backupDir, 0755, true);
    }

    $fileName = "backup_{$connection}_{$timestamp}";
    $filePath = '';

    $this->info("Current active connection: {$connection}");

    switch ($connection) {
        case 'sqlite':
            $dbPath = $config['database'];
            if (! file_exists($dbPath)) {
                $this->error("SQLite database file not found at: {$dbPath}");

                return 1;
            }
            $fileName .= '.sqlite';
            $filePath = "{$backupDir}/{$fileName}";
            copy($dbPath, $filePath);
            $this->info("SQLite database backed up to local path: {$filePath}");
            break;

        case 'pgsql':
            $fileName .= '.sql';
            $filePath = "{$backupDir}/{$fileName}";

            $host = $config['host'] ?? '127.0.0.1';
            $port = $config['port'] ?? '5432';
            $db = $config['database'] ?? 'laravel';
            $user = $config['username'] ?? 'root';
            $password = $config['password'] ?? '';

            // Set PGPASSWORD environment variable for pg_dump to avoid password prompt
            putenv("PGPASSWORD={$password}");
            $cmd = "pg_dump -h {$host} -p {$port} -U {$user} -F c -b -v -f ".escapeshellarg($filePath).' '.escapeshellarg($db);

            $this->info('Running pg_dump command...');
            exec($cmd, $output, $resultCode);
            putenv('PGPASSWORD'); // Unset for security

            if ($resultCode !== 0) {
                $this->error("pg_dump failed with exit code: {$resultCode}");
                Log::error("Database backup failed for pgsql. Exit code: {$resultCode}");

                return 1;
            }
            break;

        case 'mysql':
            $fileName .= '.sql';
            $filePath = "{$backupDir}/{$fileName}";

            $host = $config['host'] ?? '127.0.0.1';
            $port = $config['port'] ?? '3306';
            $db = $config['database'] ?? 'laravel';
            $user = $config['username'] ?? 'root';
            $password = $config['password'] ?? '';

            $cmd = "mysqldump -h {$host} -P {$port} -u {$user} -p".escapeshellarg($password).' '.escapeshellarg($db).' > '.escapeshellarg($filePath);

            $this->info('Running mysqldump command...');
            exec($cmd, $output, $resultCode);

            if ($resultCode !== 0) {
                $this->error("mysqldump failed with exit code: {$resultCode}");
                Log::error("Database backup failed for mysql. Exit code: {$resultCode}");

                return 1;
            }
            break;

        default:
            $this->error("Unsupported database driver: {$connection}");

            return 1;
    }

    // Check if backup file exists and has content
    if (file_exists($filePath) && filesize($filePath) > 0) {
        $fileSizeKb = round(filesize($filePath) / 1024, 2);
        $this->info("Backup file successfully generated: {$fileName} ({$fileSizeKb} KB)");

        // Export to External Storage if configured or standard S3 disk
        $disk = env('BACKUP_DISK', 's3');
        $this->info("Checking upload availability to backup disk: {$disk}...");

        try {
            if (config("filesystems.disks.{$disk}")) {
                $fileStream = fopen($filePath, 'r');
                Storage::disk($disk)->put("backups/{$fileName}", $fileStream);
                fclose($fileStream);
                $this->info("Uploaded backup file to cloud storage: disk '{$disk}' / path: backups/{$fileName}");
            } else {
                throw new \RuntimeException("Backup disk '{$disk}' is not configured.");
            }

            Log::info("Database daily backup completed successfully. File: {$fileName}, Size: {$fileSizeKb} KB, Transmitted to Cloud Storage: true");
        } catch (Throwable $e) {
            $this->error('Error transferring to cloud storage: '.$e->getMessage());
            Log::error('Backup cloud transfer failed: '.$e->getMessage());
        }

    } else {
        $this->error('Backup file was not created or is empty.');

        return 1;
    }

    return 0;
})->purpose('Backup active database schema and dump data to local folder and cloud storage');

/**
 * Custom console command to calculate weekly vendor performance ratings based on order completion speed and customer feedback.
 */
Artisan::command('vendor:calculate-weekly-metrics', function () {
    $this->info('Starting weekly vendor performance rating calculation...');
    Log::info('Vendor Weekly Metrics Cron: Started calculations.');

    $vendors = User::where('role', 'VENDOR')->orWhere('role', 'vendor')->get();
    $cutoffDate = now()->subDays(7);
    $processedCount = 0;

    foreach ($vendors as $vendor) {
        // 1. Gather Orders in the last 7 days
        $ordersQuery = Order::where('vendor_id', $vendor->id)
            ->where('created_at', '>=', $cutoffDate);

        $totalOrders = $ordersQuery->count();
        $completedOrdersCount = (clone $ordersQuery)->where('status', 'COMPLETED')->count();
        $totalSales = (clone $ordersQuery)->where('status', 'COMPLETED')->sum('total_price');

        // Fallback to all-time stats if zero activity in the last 7 days to avoid blank metric fields
        if ($totalOrders === 0) {
            $ordersQueryAllTime = Order::where('vendor_id', $vendor->id);
            $totalOrders = $ordersQueryAllTime->count();
            $completedOrdersCount = (clone $ordersQueryAllTime)->where('status', 'COMPLETED')->count();
            $totalSales = (clone $ordersQueryAllTime)->where('status', 'COMPLETED')->sum('total_price');
        }

        // Calculate order fulfillment rate
        $fulfillmentRate = $totalOrders > 0 ? round(($completedOrdersCount / $totalOrders) * 100, 2) : 100.0;

        // Calculate average completion time in minutes (updated_at - created_at)
        $completedOrders = Order::where('vendor_id', $vendor->id)
            ->where('status', 'COMPLETED')
            ->where('created_at', '>=', $cutoffDate)
            ->get();

        if ($completedOrders->isEmpty()) {
            $completedOrders = Order::where('vendor_id', $vendor->id)
                ->where('status', 'COMPLETED')
                ->get();
        }

        $totalMinutes = 0;
        $completionCount = 0;
        foreach ($completedOrders as $order) {
            if ($order->created_at && $order->updated_at) {
                $diff = $order->updated_at->diffInMinutes($order->created_at);
                $totalMinutes += $diff;
                $completionCount++;
            }
        }
        // Base fallback of 10-15 minutes if no time delta can be calculated
        $avgCompletionTime = $completionCount > 0 ? round($totalMinutes / $completionCount, 2) : 12.5;

        // 2. Gather Customer Feedback in the last 7 days (fallback to all time)
        $feedbackQuery = Feedback::where('vendor_id', $vendor->id)
            ->where('created_at', '>=', $cutoffDate);

        if ($feedbackQuery->count() === 0) {
            $feedbackQuery = Feedback::where('vendor_id', $vendor->id);
        }

        $avgFoodQuality = round($feedbackQuery->avg('rating_food_quality') ?? 4.0, 1);
        $avgCleanliness = round($feedbackQuery->avg('rating_cleanliness') ?? 4.0, 1);
        $avgServiceSpeed = round($feedbackQuery->avg('rating_service_speed') ?? 4.0, 1);
        $avgPriceValue = round($feedbackQuery->avg('rating_price_value') ?? 4.0, 1);

        // Overall rating is the average of the four categories
        $avgOverall = round(($avgFoodQuality + $avgCleanliness + $avgServiceSpeed + $avgPriceValue) / 4, 1);

        // 3. Gather top popular menu items
        $popularItems = Order::where('vendor_id', $vendor->id)
            ->where('status', 'COMPLETED')
            ->select('food_name', DB::raw('COUNT(*) as order_count'))
            ->groupBy('food_name')
            ->orderBy('order_count', 'desc')
            ->limit(3)
            ->pluck('food_name')
            ->toArray();

        // 4. Save metrics report record
        VendorPerformanceMetric::create([
            'vendor_id' => $vendor->id,
            'total_orders' => $totalOrders,
            'total_completed_orders' => $completedOrdersCount,
            'total_sales' => $totalSales,
            'avg_completion_time_minutes' => $avgCompletionTime,
            'order_fulfillment_rate' => $fulfillmentRate,
            'rating_food_quality' => $avgFoodQuality,
            'rating_cleanliness' => $avgCleanliness,
            'rating_service_speed' => $avgServiceSpeed,
            'rating_price_value' => $avgPriceValue,
            'rating_overall' => $avgOverall,
            'popular_menu_items' => $popularItems,
            'calculated_at' => now(),
        ]);

        $this->info("Calculated performance ratings for vendor '{$vendor->fullName}' (ID: {$vendor->id}) -> Star Rating: {$avgOverall} | Speed: {$avgCompletionTime} mins.");
        $processedCount++;
    }

    $this->info("Completed weekly performance calculations for {$processedCount} vendors.");
    Log::info("Vendor Weekly Metrics Cron: Successfully updated metrics for {$processedCount} vendors.");
})->purpose('Calculate weekly vendor performance ratings based on completion speeds and customer feedback scores');

// Schedule the weekly performance rating calculator to run every week
Schedule::command('vendor:calculate-weekly-metrics')->weekly();


Artisan::command('cafeteria:cleanup-production', function () {
    $codes = DB::table('auth_verification_codes')
        ->where(function ($query) {
            $query->whereNotNull('used_at')
                ->orWhere('expires_at', '<', now()->subDay());
        })
        ->delete();

    $reservations = DB::table('inventory_reservations')
        ->where('status', 'RESERVED')
        ->where('reserved_until', '<', now())
        ->update([
            'status' => 'EXPIRED',
            'released_at' => now(),
            'updated_at' => now(),
        ]);

    $this->info("Production cleanup completed: {$codes} auth codes removed, {$reservations} inventory reservations expired.");
})->purpose('Clean expired authentication codes and inventory reservations');

Schedule::command('cafeteria:cleanup-production')->hourly();
