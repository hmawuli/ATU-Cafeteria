<?php

namespace App\Services;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\File;
use Illuminate\Support\Facades\Log;

class EnvironmentHealthService
{
    /**
     * Parse system logs and identify common configuration or database connection errors.
     */
    public function analyzeLogsAndDiagnose(): array
    {
        $logPath = storage_path('logs/laravel.log');
        $foundErrors = [];
        $recommendations = [];

        if (File::exists($logPath)) {
            $content = File::get($logPath);

            if (str_contains($content, 'SQLSTATE[HY000]') || str_contains($content, 'Connection refused') || str_contains($content, 'no such table')) {
                $foundErrors[] = 'Database Connection or Table Schema Mismatch detected in laravel.log.';
                $recommendations[] = 'Verify database connection settings in .env and run php artisan migrate.';
            }

            if (str_contains($content, 'No application encryption key has been specified')) {
                $foundErrors[] = 'Missing APP_KEY encryption secret.';
                $recommendations[] = 'Execute php artisan key:generate.';
            }

            if (str_contains($content, 'Permission denied') || str_contains($content, 'Could not open log file')) {
                $foundErrors[] = 'Storage or log directory permission failure.';
                $recommendations[] = 'Run chmod -R 775 storage bootstrap/cache.';
            }

            if (str_contains($content, 'GEMINI_API_KEY') || str_contains($content, 'cURL error 28')) {
                $foundErrors[] = 'Gemini API network timeout or missing GEMINI_API_KEY configuration.';
                $recommendations[] = 'Ensure valid GEMINI_API_KEY is supplied in .env or Secrets panel.';
            }
        }

        // Test Database connection dynamically
        $dbStatus = 'UNKNOWN';
        $dbDetails = '';
        try {
            DB::connection()->getPdo();
            $dbStatus = 'CONNECTED';
            $dbDetails = 'Database connection established successfully.';
        } catch (\Exception $e) {
            $dbStatus = 'ERROR';
            $dbDetails = $e->getMessage();
            $foundErrors[] = 'Live DB Connection Error: ' . $e->getMessage();
            $recommendations[] = 'Check DB_CONNECTION configuration in .env and verify database file or host availability.';
        }

        return [
            'status' => empty($foundErrors) && $dbStatus === 'CONNECTED' ? 'HEALTHY' : 'NEEDS_ATTENTION',
            'database_status' => $dbStatus,
            'database_message' => $dbDetails,
            'detected_issues' => $foundErrors,
            'recommended_resolutions' => array_unique($recommendations),
            'log_file_present' => File::exists($logPath),
            'environment' => config('app.env'),
            'app_key_set' => !empty(config('app.key')),
            'timestamp' => date('Y-m-d H:i:s'),
        ];
    }
}
