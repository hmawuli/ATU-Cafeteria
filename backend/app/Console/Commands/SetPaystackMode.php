<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;

class SetPaystackMode extends Command
{
    protected $signature = 'paystack:mode
        {mode : demo | live}
        {--secret= : Paystack secret key (required for live)}
        {--env-path= : Path to the env file (defaults to the app .env)}';

    protected $description = 'Safely switch the Paystack gateway between demo and live mode in the env file.';

    public function handle(): int
    {
        $mode = strtolower(trim((string) $this->argument('mode')));
        $path = (string) ($this->option('env-path') ?: base_path('.env'));

        if (! in_array($mode, ['demo', 'live'], true)) {
            $this->error('Mode must be either "demo" or "live".');

            return self::FAILURE;
        }
        if (! file_exists($path)) {
            $this->error("Env file not found: {$path}");

            return self::FAILURE;
        }

        if ($mode === 'demo') {
            $this->writeEnv($path, ['PAYSTACK_DEMO_MODE' => 'true']);
            $this->info('Paystack DEMO mode enabled — top-ups are simulated (no real charge).');
        } else {
            $secret = trim((string) $this->option('secret'));
            if (! str_starts_with($secret, 'sk_')) {
                $this->error('A live Paystack secret key is required: paystack:mode live --secret=sk_live_...');

                return self::FAILURE;
            }

            $this->writeEnv($path, [
                'PAYSTACK_SECRET_KEY' => $secret,
                'PAYSTACK_DEMO_MODE' => 'false',
            ]);

            $this->info('Paystack LIVE mode enabled.');
            $this->line('    secret : set (…'.substr($secret, -4).')');
            $this->line('    demo   : false');
        }

        $this->line('    file   : '.$path);
        $this->warn('Restart the backend (or `php artisan config:clear`) so the running process picks up the change.');

        return self::SUCCESS;
    }

    /**
     * Replace or append the given env keys, preserving every other line, and
     * lock the file to the owner (secrets live here).
     *
     * @param  array<string, string>  $values
     */
    private function writeEnv(string $path, array $values): void
    {
        $lines = file($path, FILE_IGNORE_NEW_LINES) ?: [];
        $remaining = $values;

        foreach ($lines as $index => $line) {
            foreach ($values as $key => $value) {
                if (preg_match('/^'.preg_quote($key, '/').'=/', $line) === 1) {
                    $lines[$index] = $key.'='.$value;
                    unset($remaining[$key]);
                }
            }
        }

        foreach ($remaining as $key => $value) {
            $lines[] = $key.'='.$value;
        }

        file_put_contents($path, implode(PHP_EOL, $lines).PHP_EOL);
        @chmod($path, 0600);
    }
}
