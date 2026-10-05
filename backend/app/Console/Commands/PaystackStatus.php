<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Illuminate\Support\Facades\Http;

class PaystackStatus extends Command
{
    protected $signature = 'paystack:status {--ping : Verify the secret against the live Paystack API}';

    protected $description = 'Report the Paystack payment configuration and optionally prove the key works.';

    public function handle(): int
    {
        $secret = trim((string) config('services.paystack.secret', ''));
        $demo = (bool) config('services.paystack.demo_mode', false);
        $base = trim((string) config('services.paystack.base_url', 'https://api.paystack.co'));
        $env = strtoupper((string) config('app.env', 'local'));

        $this->table(
            ['Setting', 'Value'],
            [
                ['Environment', $env],
                ['Demo mode', $demo ? 'YES — top-ups are simulated' : 'no — live gateway'],
                ['Secret key', $secret === '' ? 'not set' : 'set (…'.substr($secret, -4).')'],
                ['Base URL', $base],
                ['Webhook route', trim((string) config('app.url', 'https://api.example.com'), '/').'/api/paystack/webhook'],
            ]
        );

        if ($demo) {
            $this->warn('Demo mode is ON — initialization returns a simulated URL and verify credits without Paystack.');
        }
        if ($secret === '' && ! $demo) {
            $this->warn('PAYSTACK_SECRET_KEY is empty and demo mode is off — top-ups will fail with "Paystack is not configured".');
        }

        if ($secret !== '' && (bool) $this->option('ping')) {
            $this->info('Probing the live Paystack API with the configured secret…');
            try {
                $response = Http::timeout(20)->withToken($secret)->acceptJson()->get($base.'/balance');
                if ($response->successful()) {
                    $data = (array) $response->json('data');
                    $this->info('✅ Paystack secret is valid (HTTP '.$response->status().'), balance rows: '.count($data).'.');
                } else {
                    $this->error('✗ Paystack API returned HTTP '.$response->status().' — check the secret key.');

                    return self::FAILURE;
                }
            } catch (\Throwable $e) {
                $this->error('✗ Could not reach Paystack: '.$e->getMessage());

                return self::FAILURE;
            }
        }

        return self::SUCCESS;
    }
}
