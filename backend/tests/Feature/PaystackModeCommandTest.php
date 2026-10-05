<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PaystackModeCommandTest extends TestCase
{
    use RefreshDatabase;

    private string $envPath;

    protected function setUp(): void
    {
        parent::setUp();

        $this->envPath = storage_path('framework/testing-paystack.env');
        file_put_contents($this->envPath, "APP_ENV=testing\nPAYSTACK_DEMO_MODE=false\n");
    }

    protected function tearDown(): void
    {
        @unlink($this->envPath);
        parent::tearDown();
    }

    public function test_switches_to_demo_mode(): void
    {
        $this->artisan('paystack:mode', ['mode' => 'demo', '--env-path' => $this->envPath])
            ->assertExitCode(0);

        $this->assertStringContainsString('PAYSTACK_DEMO_MODE=true', file_get_contents($this->envPath));
    }

    public function test_live_mode_requires_a_secret(): void
    {
        $this->artisan('paystack:mode', ['mode' => 'live', '--env-path' => $this->envPath])
            ->assertExitCode(1);

        $this->assertStringContainsString('PAYSTACK_DEMO_MODE=false', file_get_contents($this->envPath));
    }

    public function test_live_mode_writes_secret_and_disables_demo(): void
    {
        $this->artisan('paystack:mode', [
            'mode' => 'live',
            '--secret' => 'sk_test_industrial_1234',
            '--env-path' => $this->envPath,
        ])->assertExitCode(0);

        $env = file_get_contents($this->envPath);
        $this->assertStringContainsString('PAYSTACK_SECRET_KEY=sk_test_industrial_1234', $env);
        $this->assertStringContainsString('PAYSTACK_DEMO_MODE=false', $env);
    }

    public function test_rejects_an_unknown_mode(): void
    {
        $this->artisan('paystack:mode', ['mode' => 'sometimes', '--env-path' => $this->envPath])
            ->assertExitCode(1);
    }
}
