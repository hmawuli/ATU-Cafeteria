<?php

namespace App\Services;

use App\Models\Payment;
use App\Models\Refund;
use Illuminate\Support\Facades\Http;
use RuntimeException;

class PaystackRefundService
{
    public function initiate(Payment $payment, Refund $refund): Refund
    {
        $secret = trim((string) config('services.paystack.secret', ''));
        $baseUrl = rtrim((string) config('services.paystack.base_url', 'https://api.paystack.co'), '/');
        $demoMode = (bool) config('services.paystack.demo_mode', false);

        if ($demoMode || $secret === '') {
            $refund->update([
                'status' => 'FAILED',
                'reason' => trim($refund->reason.' Paystack refund is not configured on this server.'),
            ]);

            throw new RuntimeException('Paystack is not configured for gateway refunds.');
        }

        $response = Http::timeout(20)
            ->withToken($secret)
            ->acceptJson()
            ->post($baseUrl.'/refund', [
                'transaction' => $payment->reference,
                'amount' => (int) round((float) $refund->amount * 100),
                'currency' => (string) ($payment->currency ?: 'GHS'),
                'customer_note' => $refund->reason,
                'merchant_note' => 'Refund for order '.optional($refund->order)->order_number,
            ]);

        if (! $response->successful() || data_get($response->json(), 'status') !== true) {
            $refund->update(['status' => 'FAILED']);
            throw new RuntimeException('Paystack could not initiate the refund.');
        }

        $data = $response->json('data', []);
        $gatewayStatus = strtolower((string) data_get($data, 'status', 'pending'));
        $localStatus = match ($gatewayStatus) {
            'processing' => 'PROCESSING',
            'processed' => 'SUCCESS',
            'failed' => 'FAILED',
            default => 'PENDING',
        };

        $refund->update([
            'status' => $localStatus,
            'gateway_reference' => (string) (data_get($data, 'id') ?? data_get($data, 'refund_reference') ?? ''),
        ]);

        $payment->update([
            'status' => $localStatus === 'FAILED'
                ? 'SUCCESS'
                : ($localStatus === 'SUCCESS' ? 'REFUNDED' : 'REFUND_PENDING'),
            'refunded_at' => $localStatus === 'SUCCESS' ? now() : $payment->refunded_at,
        ]);

        return $refund->fresh();
    }
}
