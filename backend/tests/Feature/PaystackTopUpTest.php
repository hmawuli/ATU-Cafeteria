<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PaystackTopUpTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();

        config()->set('services.paystack.demo_mode', true);
        config()->set('services.paystack.secret', '');
    }

    private function auth(User $user): void
    {
        $token = $user->createToken('topup-test')->plainTextToken;
        $this->withHeader('Authorization', 'Bearer '.$token);
    }

    private function makeStudent(): User
    {
        return User::factory()->create([
            'role' => 'STUDENT',
            'balance' => 10.00,
            'profile_info' => ['email' => 'student@atu.edu.gh'],
        ]);
    }

    public function test_initialize_requires_an_idempotency_key(): void
    {
        $user = $this->makeStudent();
        $this->auth($user);

        $this->postJson('/api/paystack/initialize', [
            'amount' => 20.00,
            'email' => 'student@atu.edu.gh',
            'purpose' => 'WALLET_TOPUP',
        ])
            ->assertStatus(400)
            ->assertJsonPath('error_code', 'IDEMPOTENCY_KEY_REQUIRED');
    }

    public function test_demo_top_up_initializes_then_verifies_and_credits_wallet(): void
    {
        $user = $this->makeStudent();
        $this->auth($user);
        $this->withHeader('Idempotency-Key', 'topup-'.uniqid());

        $init = $this->postJson('/api/paystack/initialize', [
            'amount' => 20.00,
            'email' => 'student@atu.edu.gh',
            'purpose' => 'WALLET_TOPUP',
        ])
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('data.is_simulated', true);

        $reference = $init->json('data.reference');
        $this->assertNotNull($reference);
        $this->assertDatabaseHas('wallet_transactions', [
            'user_id' => $user->id,
            'reference' => $reference,
            'status' => 'PENDING',
        ]);

        // Completing the demo checkout verifies and credits the wallet.
        $this->getJson('/api/paystack/verify/'.$reference.'?amount=20.00&purpose=WALLET_TOPUP')
            ->assertOk()
            ->assertJsonPath('success', true);

        $this->assertSame(30.0, (float) $user->fresh()->balance);
        $this->assertDatabaseHas('wallet_transactions', [
            'user_id' => $user->id,
            'reference' => $reference,
            'status' => 'SUCCESS',
        ]);
    }
}
