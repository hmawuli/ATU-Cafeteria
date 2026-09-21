<?php

namespace Tests\Unit;

use App\Http\Controllers\Api\WalletController;
use Illuminate\Http\Request;
use Tests\TestCase;

class WalletControllerSecurityTest extends TestCase
{
    public function test_legacy_manual_deposit_endpoint_cannot_credit_wallet(): void
    {
        $response = (new WalletController)->deposit(Request::create('/api/wallet/deposit', 'POST', [
            'amount' => 100,
        ]));

        $this->assertSame(410, $response->getStatusCode());
        $this->assertFalse($response->getData(true)['success']);
    }
}
