<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ConfigCheckTest extends TestCase
{
    use RefreshDatabase;

    public function test_production_violations_fail_the_gate(): void
    {
        $previous = config('app.env');
        try {
            config()->set('app.env', 'production');
            config()->set('app.debug', true);
            config()->set('services.paystack.secret', '');
            config()->set('services.paystack.demo_mode', false);
            config()->set('app.key', '');
            putenv('CORS_ALLOWED_ORIGINS=');

            $this->artisan('config:check', ['--fail-on-prod' => true])
                ->expectsOutputToContain('APP_DEBUG')
                ->expectsOutputToContain('PAYSTACK_SECRET_KEY')
                ->assertExitCode(1);
        } finally {
            config()->set('app.env', $previous);
            config()->set('app.debug', false);
            config()->set('services.paystack.secret', null);
            config()->set('services.paystack.demo_mode', false);
            config()->set('app.key', 'base64:'.base64_encode(random_bytes(32)));
            putenv('CORS_ALLOWED_ORIGINS=http://localhost');
        }
    }

    public function test_non_production_environment_passes(): void
    {
        config()->set('app.env', 'local');

        $this->artisan('config:check')
            ->expectsOutputToContain('healthy')
            ->assertExitCode(0);
    }
}
