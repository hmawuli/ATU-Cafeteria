<?php

namespace App\Services;

use App\Models\VendorPayoutAccount;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Str;

class PaystackPayoutService
{
    protected function secretKey(): string
    {
        return trim((string) config('services.paystack.secret', ''));
    }

    protected function baseUrl(): string
    {
        return rtrim((string) config('services.paystack.base_url', 'https://api.paystack.co'), '/');
    }

    protected function request(string $method, string $path, array $payload = [], array $query = [])
    {
        $secret = $this->secretKey();
        if ($secret === '') {
            throw new \RuntimeException('Paystack transfers are not configured.');
        }

        $request = Http::timeout(20)->withToken($secret)->acceptJson();
        $url = $this->baseUrl() . $path;

        return $method === 'GET'
            ? $request->get($url, $query)
            : $request->post($url, $payload);
    }

    public function listGhanaBanks(?string $type = null): array
    {
        $query = ['country' => 'ghana', 'currency' => 'GHS'];
        if ($type !== null) $query['type'] = $type;

        $response = $this->request('GET', '/bank', [], $query);
        if (! $response->successful() || data_get($response->json(), 'status') !== true) {
            throw new \RuntimeException('Unable to load Paystack payout channels.');
        }

        return (array) $response->json('data', []);
    }

    public function resolveBankAccount(string $accountNumber, string $bankCode): array
    {
        $response = $this->request('GET', '/bank/resolve', [], [
            'account_number' => $accountNumber,
            'bank_code' => $bankCode,
        ]);

        if (! $response->successful() || data_get($response->json(), 'status') !== true) {
            throw new \RuntimeException((string) ($response->json('message') ?: 'Unable to verify the bank account.'));
        }

        return (array) $response->json('data', []);
    }

    public function createRecipient(VendorPayoutAccount $account): array
    {
        $type = strtoupper($account->type) === 'MOBILE_MONEY' ? 'mobile_money' : 'ghipss';
        $response = $this->request('POST', '/transferrecipient', [
            'type' => $type,
            'name' => $account->account_name,
            'account_number' => $account->account_number,
            'bank_code' => $account->bank_code,
            'currency' => $account->currency ?: 'GHS',
            'description' => 'ATU Cafeteria vendor ' . $account->vendor_id,
            'metadata' => ['vendor_id' => $account->vendor_id],
        ]);

        if (! $response->successful() || data_get($response->json(), 'status') !== true) {
            throw new \RuntimeException((string) ($response->json('message') ?: 'Unable to create payout recipient.'));
        }

        return (array) $response->json('data', []);
    }

    public function initiateTransfer(VendorPayoutAccount $account, float $amount, string $reason): array
    {
        if (! $account->recipient_code) {
            throw new \RuntimeException('The vendor payout recipient has not been created.');
        }

        $amountPesewas = (int) round($amount * 100);
        if ($amountPesewas < 1) throw new \RuntimeException('Settlement amount must be greater than zero.');

        $reference = 'atu_settle_' . Str::lower(str_replace('-', '', (string) Str::uuid()));
        $response = $this->request('POST', '/transfer', [
            'source' => 'balance',
            'amount' => $amountPesewas,
            'recipient' => $account->recipient_code,
            'reference' => $reference,
            'reason' => $reason,
            'currency' => 'GHS',
        ]);

        if (! $response->successful() || data_get($response->json(), 'status') !== true) {
            throw new \RuntimeException((string) ($response->json('message') ?: 'Unable to initiate vendor payout.'));
        }

        return (array) $response->json('data', []);
    }

    public function finalizeTransfer(string $transferCode, string $otp): array
    {
        $response = $this->request('POST', '/transfer/finalize_transfer', [
            'transfer_code' => $transferCode,
            'otp' => $otp,
        ]);

        if (! $response->successful() || data_get($response->json(), 'status') !== true) {
            throw new \RuntimeException((string) ($response->json('message') ?: 'Unable to finalize transfer.'));
        }

        return (array) $response->json('data', []);
    }
}