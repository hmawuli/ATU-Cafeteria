<?php

namespace Tests\Feature;

use App\Models\Payment;
use App\Models\User;
use App\Models\VendorSettlement;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

class PaystackWebhookTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();

        // Webhooks only process in live mode with a secret.
        config()->set('services.paystack.secret', 'sk_test_industrial');
        config()->set('services.paystack.demo_mode', false);
    }

    private function signedPayload(string $event, string $reference): string
    {
        $payload = json_encode([
            'event' => $event,
            'data' => [
                'id' => 12345,
                'reference' => $reference,
                'status' => 'success',
            ],
        ]);

        return $payload;
    }

    public function test_webhook_rejects_a_bad_signature(): void
    {
        $this->postJson('/api/paystack/webhook', [
            'event' => 'charge.success',
            'data' => ['reference' => 'ATU-PAY-X'],
        ], ['x-paystack-signature' => 'wrong-signature'])
            ->assertStatus(401)
            ->assertJsonPath('message', 'Invalid webhook signature.');
    }

    private function webhookCall(string $payload, string $signature): TestResponse
    {
        $server = $this->transformHeadersToServerVars([
            'x-paystack-signature' => $signature,
            'Content-Type' => 'application/json',
            'Accept' => 'application/json',
        ]);

        return $this->call('POST', '/api/paystack/webhook', [], [], [], $server, $payload);
    }

    public function test_charge_success_is_acknowledged_and_marks_payment_seen(): void
    {
        $user = User::factory()->create(['role' => 'STUDENT']);
        $payment = Payment::create([
            'customer_id' => $user->id,
            'reference' => 'ATU-PAY-CHG-1',
            'gateway' => 'paystack',
            'amount' => 25.00,
            'currency' => 'GHS',
            'purpose' => 'WALLET_TOPUP',
            'status' => 'INITIATED',
            'initiated_at' => now(),
        ]);

        $payload = $this->signedPayload('charge.success', $payment->reference);
        $signature = hash_hmac('sha512', $payload, 'sk_test_industrial');

        $this->webhookCall($payload, $signature)
            ->assertOk()
            ->assertJsonPath('message', 'Charge webhook acknowledged.');

        $this->assertSame('PENDING', $payment->fresh()->status);
    }

    public function test_charge_webhook_does_not_credit_the_wallet_again(): void
    {
        $user = User::factory()->create(['role' => 'STUDENT', 'balance' => 0]);
        $payment = Payment::create([
            'customer_id' => $user->id,
            'reference' => 'ATU-PAY-CHG-2',
            'gateway' => 'paystack',
            'amount' => 25.00,
            'currency' => 'GHS',
            'purpose' => 'WALLET_TOPUP',
            'status' => 'INITIATED',
            'initiated_at' => now(),
        ]);

        $payload = $this->signedPayload('charge.success', $payment->reference);

        $this->webhookCall($payload, hash_hmac('sha512', $payload, 'sk_test_industrial'))
            ->assertOk();

        // Authoritative credit stays in the verify flow — webhook never mints.
        $this->assertSame(0.0, (float) $user->fresh()->balance);
    }

    public function test_transfer_success_marks_a_vendor_settlement_paid(): void
    {
        $vendor = User::factory()->create(['role' => 'VENDOR']);

        $settlement = VendorSettlement::create([
            'vendor_id' => $vendor->id,
            'period_start' => now()->subMonth()->toDateString(),
            'period_end' => now()->toDateString(),
            'gross_sales' => 500.00,
            'refunds' => 0,
            'fees' => 10.00,
            'net_amount' => 490.00,
            'status' => 'APPROVED',
            'payout_reference' => 'TRF-SETTLE-1',
        ]);

        $payload = json_encode([
            'event' => 'transfer.success',
            'data' => ['reference' => 'TRF-SETTLE-1', 'status' => 'success'],
        ]);

        $this->webhookCall($payload, hash_hmac('sha512', $payload, 'sk_test_industrial'))
            ->assertOk()
            ->assertJsonPath('message', 'Transfer webhook reconciled.');

        $this->assertSame('PAID', $settlement->fresh()->status);
    }
}
