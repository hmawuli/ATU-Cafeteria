<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

class ConfigCheckCommand extends Command
{
    protected $signature = 'config:check {--fail-on-prod : Exit non-zero on production violations}';

    protected $description = 'Validate the runtime configuration (env, key, debug, CORS, Paystack, database).';

    public function handle(): int
    {
        $env = strtolower((string) config('app.env', 'local'));
        $isProdLike = in_array($env, ['production', 'staging'], true);
        $issues = [];

        if ($isProdLike) {
            $key = (string) config('app.key', '');
            if ($key === '' || ! str_starts_with($key, 'base64:')) {
                $issues[] = 'APP_KEY is missing or not base64-encoded.';
            }
            if ((bool) config('app.debug', false)) {
                $issues[] = 'APP_DEBUG must be false on '.$env.'.';
            }
            if (blank((string) env('CORS_ALLOWED_ORIGINS', ''))) {
                $issues[] = 'CORS_ALLOWED_ORIGINS is empty on '.$env.'.';
            }
            if (trim((string) config('services.paystack.secret', '')) === '') {
                $issues[] = 'PAYSTACK_SECRET_KEY is missing on '.$env.'.';
            }
            if ((bool) config('services.paystack.demo_mode', false)) {
                $issues[] = 'PAYSTACK_DEMO_MODE must be false on '.$env.'.';
            }
            if (strtolower((string) config('database.default', '')) !== 'pgsql') {
                $issues[] = 'DB_CONNECTION should be pgsql on '.$env.'.';
            }
        } else {
            $this->line("   env={$env} — production checks skipped.");
        }

        try {
            DB::connection()->getPdo();
        } catch (\Throwable $e) {
            $issues[] = 'Database not reachable: '.$e->getMessage();
        }

        if ($issues === []) {
            $this->info('✅ Configuration looks healthy.');

            return self::SUCCESS;
        }

        foreach ($issues as $issue) {
            $this->error('✗ '.$issue);
        }

        if ($isProdLike && (bool) $this->option('fail-on-prod')) {
            return self::FAILURE;
        }

        return self::SUCCESS;
    }
}
