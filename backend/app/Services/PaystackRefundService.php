<?php

namespace App\Services;

use App\Models\Order;
use App\Models\Payment;
use App\Models\PaymentAllocation;
use App\Models\PromotionRedemption;
use App\Models\Refund;
use Illuminate\Support\Facades\DB;
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

        DB::transaction(function () use ($payment, $refund, $localStatus) {
            $refund->update([
                'status' => $localStatus,
                'processed_at' => $localStatus === 'SUCCESS' ? now() : $refund->processed_at,
            ]);

            if ($localStatus === 'SUCCESS' && $refund->payment_id && $refund->order_id) {
                $allocation = PaymentAllocation::where('payment_id', $refund->payment_id)
                    ->where('order_id', $refund->order_id)
                    ->lockForUpdate()
                    ->first();

                if ($allocation) {
                    $allocation->refunded_amount = round(
                        min(
                            (float) $allocation->amount,
                            (float) $allocation->refunded_amount + (float) $refund->amount
                        ),
                        2
                    );
                    $allocation->save();
                }
            }

            if ($localStatus === 'SUCCESS') {
                $payment->load('allocations');
                $allocated = (float) $payment->allocations->sum(fn ($row) => (float) $row->amount);
                $refunded = (float) $payment->allocations->sum(fn ($row) => (float) $row->refunded_amount);
                $paymentStatus = $allocated > 0 && $refunded + 0.01 >= $allocated
                    ? 'REFUNDED'
                    : 'SUCCESS';
            } else {
                $paymentStatus = $localStatus === 'FAILED' ? 'SUCCESS' : 'REFUND_PENDING';
            }

            $payment->update([
                'status' => $paymentStatus,
                'refunded_at' => $paymentStatus === 'REFUNDED' ? now() : $payment->refunded_at,
            ]);

            if ($localStatus === 'SUCCESS' && $refund->order_id) {
                $order = Order::withoutGlobalScopes()->find($refund->order_id);
                if ($order && $order->checkout_session_id) {
                    $hasOpenSibling = Order::withoutGlobalScopes()
                        ->where('checkout_session_id', $order->checkout_session_id)
                        ->whereNotIn('status', ['CANCELLED', 'DECLINED'])
                        ->exists();

                    if (! $hasOpenSibling) {
                        PromotionRedemption::where('checkout_session_id', $order->checkout_session_id)->delete();
                    }
                } elseif ($order) {
                    PromotionRedemption::where('order_id', $order->id)->delete();
                }
            }
        });

        return $refund->fresh();
    }
}
