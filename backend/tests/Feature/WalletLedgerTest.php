<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class WalletLedgerTest extends TestCase
{
    use RefreshDatabase;

    private User $student;

    protected function setUp(): void
    {
        parent::setUp();

        $this->student = User::create([
            'username' => 'ledger-student',
            'password' => Hash::make('aStr0ngPin9!'),
            'role' => 'STUDENT',
            'fullName' => 'Ledger Student',
            'balance' => 0.00,
            'profile_info' => ['email' => 'ledger@atu.edu.gh'],
        ]);
    }

    private function auth(): void
    {
        $token = $this->student->createToken('ledger-test')->plainTextToken;
        $this->withHeader('Authorization', 'Bearer '.$token);
    }

    private function tx(string $type, float $amount, string $status = 'SUCCESS'): void
    {
        WalletTransaction::create([
            'user_id' => $this->student->id,
            'type' => $type,
            'amount' => $amount,
            'status' => $status,
            'reference' => 'tx-'.uniqid(),
            // The append-only guard requires SUCCESS credits to be verifiable.
            'details' => $type === 'DEPOSIT' ? 'paystack verified payment' : null,
        ]);
    }

    public function test_reconcile_reports_an_consistent_ledger(): void
    {
        $this->tx('DEPOSIT', 100.00);
        $this->tx('PAYMENT', -40.00);
        $this->tx('REFUND', 10.00);
        $this->student->update(['balance' => 70.00]);
        $this->auth();

        $this->getJson('/api/wallet/reconcile')
            ->assertOk()
            ->assertJsonPath('data.consistent', true)
            ->assertJsonPath('data.stored_balance', 70)
            ->assertJsonPath('data.ledger_balance', 70)
            ->assertJsonPath('data.delta', 0);
    }

    public function test_reconcile_detects_balance_drift(): void
    {
        $this->tx('DEPOSIT', 100.00);
        // Simulate drift: stored balance differs from what the ledger implies.
        $this->student->update(['balance' => 15.00]);
        $this->auth();

        $this->getJson('/api/wallet/reconcile')
            ->assertOk()
            ->assertJsonPath('data.consistent', false)
            ->assertJsonPath('data.delta', -85);
    }

    public function test_statement_returns_json_ledger(): void
    {
        $this->tx('DEPOSIT', 100.00);
        $this->auth();

        $this->getJson('/api/wallet/statement')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data' => ['from', 'to', 'balance', 'transactions']])
            ->assertJsonCount(1, 'data.transactions');
    }

    public function test_statement_returns_pdf(): void
    {
        $this->tx('DEPOSIT', 100.00);
        $this->auth();

        $this->get('/api/wallet/statement?pdf=1')
            ->assertOk()
            ->assertHeader('Content-Type', 'application/pdf');
    }
}
