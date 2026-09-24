<?php

namespace Tests\Feature;

use App\Models\VendorPayoutAccount;
use App\Services\PaystackPayoutService;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Http;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorPayoutTest extends TestCase
{
    use RefreshDatabase;

    public function test_vendor_can_verify_and_save_payout_account(): void
    {
        config(['services.paystack.secret' => 'sk_test_fake']);
        $vendor = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);

        Http::fake(function ($request) {
            $url = $request->url();
            if (str_contains($url, '/bank/resolve')) {
                return Http::response([
                    'status' => true,
                    'message' => 'Account resolved',
                    'data' => ['account_number' => '0123456789', 'account_name' => 'Test Vendor Ltd'],
                ], 200);
            }
            if (str_contains($url, '/transferrecipient')) {
                return Http::response([
                    'status' => true,
                    'data' => ['recipient_code' => 'RCP_TEST_001'],
                ], 201);
            }
            return Http::response(['status' => false], 404);
        });

        Sanctum::actingAs($vendor, ['vendor']);

        $response = $this->postJson('/api/vendor/payout-account', [
            'type' => 'BANK',
            'bank_code' => '044',
            'bank_name' => 'Test Bank',
            'account_number' => '0123456789',
        ], [
            'Idempotency-Key' => 'payout-account-test-001',
        ]);

        $response->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('account.status', 'ACTIVE')
            ->assertJsonPath('account.account_number', '••••6789');

        $this->assertDatabaseHas('vendor_payout_accounts', [
            'vendor_id' => $vendor->id,
            'status' => 'ACTIVE',
            'recipient_code' => 'RCP_TEST_001',
            'account_number_last4' => '6789',
            'account_name' => 'Test Vendor Ltd',
        ]);

        $stored = VendorPayoutAccount::where('vendor_id', $vendor->id)->firstOrFail();
        $this->assertSame('0123456789', $stored->account_number);
    }

    public function test_paystack_payout_service_initiates_transfer_with_ghana_pesewas(): void
    {
        config(['services.paystack.secret' => 'sk_test_fake']);
        $vendor = User::factory()->create(['role' => 'VENDOR']);
        $account = VendorPayoutAccount::create([
            'vendor_id' => $vendor->id,
            'type' => 'BANK',
            'bank_code' => '044',
            'bank_name' => 'Test Bank',
            'account_number' => '0123456789',
            'account_number_last4' => '6789',
            'account_name' => 'Test Vendor Ltd',
            'currency' => 'GHS',
            'recipient_code' => 'RCP_TEST_001',
            'status' => 'ACTIVE',
            'verified_at' => now(),
        ]);

        Http::fake([
            '*/transfer' => Http::response([
                'status' => true,
                'data' => [
                    'transfer_code' => 'TRF_TEST_001',
                    'status' => 'pending',
                    'reference' => 'atu_settle_test_001',
                ],
            ], 200),
        ]);

        $data = app(PaystackPayoutService::class)->initiateTransfer(
            $account,
            123.45,
            'Test settlement',
            'atu_settle_test_001',
        );

        $this->assertSame('TRF_TEST_001', $data['transfer_code']);
        Http::assertSent(function ($request) {
            return str_ends_with($request->url(), '/transfer')
                && data_get($request->data(), 'amount') === 12345
                && data_get($request->data(), 'recipient') === 'RCP_TEST_001'
                && data_get($request->data(), 'currency') === 'GHS'
                && data_get($request->data(), 'reference') === 'atu_settle_test_001';
        });
    }
}