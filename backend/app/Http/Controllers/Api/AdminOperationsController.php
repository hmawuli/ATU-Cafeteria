<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Order;
use App\Models\Payment;
use App\Models\PaymentAllocation;
use App\Models\Promotion;
use App\Models\Refund;
use App\Models\SupportTicket;
use App\Models\User;
use App\Models\VendorSettlement;
use App\Models\VendorPayoutAccount;
use App\Models\WalletTransaction;
use App\Services\PaystackPayoutService;
use App\Services\PaystackRefundService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class AdminOperationsController extends Controller
{
    public function promotions()
    {
        return response()->json(['success' => true, 'promotions' => Promotion::latest()->paginate(50)]);
    }

    public function createPromotion(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'vendor_id' => 'nullable|integer|exists:users,id',
            'code' => 'required|string|min:3|max:50|alpha_dash',
            'name' => 'required|string|max:160',
            'type' => 'required|string|in:PERCENTAGE,FIXED',
            'value' => 'required|numeric|min:0.01',
            'minimum_order_amount' => 'nullable|numeric|min:0',
            'maximum_discount_amount' => 'nullable|numeric|min:0',
            'usage_limit' => 'nullable|integer|min:1',
            'per_customer_limit' => 'nullable|integer|min:1',
            'starts_at' => 'nullable|date',
            'ends_at' => 'nullable|date|after_or_equal:starts_at',
            'is_active' => 'boolean',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid promotion.', 'errors' => $validator->errors()], 422);
        }

        if ($request->filled('vendor_id')) {
            $vendor = User::find((int) $request->input('vendor_id'));
            if (! $vendor || strtoupper((string) $vendor->role) !== 'VENDOR') {
                return response()->json(['success' => false, 'message' => 'The promotion vendor must be a valid vendor account.'], 422);
            }
        }

        $promotion = Promotion::create([
            ...$request->only([
                'vendor_id','name','type','value','minimum_order_amount','maximum_discount_amount',
                'usage_limit','per_customer_limit','starts_at','ends_at',
            ]),
            'code' => strtoupper(trim($request->input('code'))),
            'is_active' => $request->boolean('is_active', true),
        ]);

        AuditLog::create([
            'user_id' => $request->user()->id,
            'timestamp' => now()->getTimestampMs(),
            'action' => 'PROMOTION_CREATED',
            'details' => "Promotion {$promotion->code} created.",
        ]);

        return response()->json(['success' => true, 'promotion' => $promotion], 201);
    }

    public function updatePromotion(Request $request, Promotion $promotion)
    {
        $validator = Validator::make($request->all(), [
            'name' => 'sometimes|required|string|max:160',
            'type' => 'sometimes|required|string|in:PERCENTAGE,FIXED',
            'value' => 'sometimes|required|numeric|min:0.01',
            'minimum_order_amount' => 'nullable|numeric|min:0',
            'maximum_discount_amount' => 'nullable|numeric|min:0',
            'usage_limit' => 'nullable|integer|min:1',
            'per_customer_limit' => 'nullable|integer|min:1',
            'starts_at' => 'nullable|date',
            'ends_at' => 'nullable|date|after_or_equal:starts_at',
            'is_active' => 'boolean',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid promotion update.', 'errors' => $validator->errors()], 422);
        }

        $promotion->fill($request->only([
            'name','type','value','minimum_order_amount','maximum_discount_amount',
            'usage_limit','per_customer_limit','starts_at','ends_at','is_active',
        ]));
        $promotion->save();

        return response()->json(['success' => true, 'promotion' => $promotion]);
    }

    public function refunds(Request $request)
    {
        $query = Refund::with(['order','customer','payment'])->latest();
        return response()->json(['success' => true, 'refunds' => $query->paginate(50)]);
    }

    public function refundOrder(Request $request, Order $order, PaystackRefundService $paystackRefunds)
    {
        $validator = Validator::make($request->all(), [
            'amount' => 'nullable|numeric|min:0.01',
            'reason' => 'required|string|min:3|max:255',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid refund request.', 'errors' => $validator->errors()], 422);
        }

        $result = DB::transaction(function () use ($request, $order) {
            $lockedOrder = Order::whereKey($order->id)->lockForUpdate()->firstOrFail();
            $customer = User::whereKey($lockedOrder->customer_id ?: $lockedOrder->user_id ?: $lockedOrder->student_id)
                ->lockForUpdate()->firstOrFail();

            $allocation = $lockedOrder->payment_id
                ? PaymentAllocation::where('payment_id', $lockedOrder->payment_id)
                    ->where('order_id', $lockedOrder->id)
                    ->lockForUpdate()
                    ->first()
                : null;

            $alreadyRefunded = (float) Refund::where('order_id', $lockedOrder->id)
                ->whereIn('status', ['SUCCESS','PROCESSING','PENDING'])
                ->sum('amount');
            $maxRefund = $allocation
                ? (float) $allocation->amount
                : max(0, (float) $lockedOrder->grand_total ?: (float) $lockedOrder->total_price);
            $remaining = $allocation
                ? round($allocation->refundableAmount() - (max(0, $alreadyRefunded - (float) $allocation->refunded_amount)), 2)
                : round($maxRefund - $alreadyRefunded, 2);
            $remaining = max(0, $remaining);
            $amount = round((float) ($request->input('amount') ?? $remaining), 2);

            if ($amount <= 0 || $amount > $remaining) {
                throw new \RuntimeException('Refund amount exceeds the remaining refundable order value.');
            }

            $payment = $lockedOrder->payment_id
                ? Payment::whereKey($lockedOrder->payment_id)->lockForUpdate()->first()
                : Payment::where('order_id', $lockedOrder->id)->latest()->lockForUpdate()->first();

            if ($lockedOrder->payment_method === 'WALLET') {
                $before = round((float) $customer->balance, 2);
                $customer->balance = round($before + $amount, 2);
                $customer->save();

                $refund = Refund::create([
                    'order_id' => $lockedOrder->id,
                    'payment_id' => $payment?->id,
                    'customer_id' => $customer->id,
                    'requested_by' => $request->user()->id,
                    'amount' => $amount,
                    'reason' => trim($request->input('reason')),
                    'status' => 'SUCCESS',
                    'processed_at' => now(),
                ]);

                WalletTransaction::create([
                    'user_id' => $customer->id,
                    'order_id' => $lockedOrder->id,
                    'payment_id' => $payment?->id,
                    'type' => 'REFUND',
                    'amount' => $amount,
                    'status' => 'SUCCESS',
                    'source' => 'REFUND',
                    'performed_by' => $request->user()->id,
                    'balance_before' => $before,
                    'balance_after' => $customer->balance,
                    'reference' => 'REF-'.strtoupper(Str::random(14)),
                    'details' => 'Admin refund for order '.$lockedOrder->order_number,
                ]);

                if ($allocation) {
                    $allocation->refunded_amount = round(
                        min((float) $allocation->amount, (float) $allocation->refunded_amount + $amount),
                        2
                    );
                    $allocation->save();
                }

                if ($payment) {
                    $payment->load('allocations');
                    $allAllocated = $payment->allocations->sum(fn ($row) => (float) $row->amount);
                    $allRefunded = $payment->allocations->sum(fn ($row) => (float) $row->refunded_amount);
                    $pendingRefunds = Refund::where('payment_id', $payment->id)
                        ->whereIn('status', ['PENDING', 'PROCESSING'])
                        ->exists();

                    if ($allAllocated > 0 && $allRefunded + 0.01 >= $allAllocated) {
                        $payment->status = 'REFUNDED';
                    } elseif ($pendingRefunds) {
                        $payment->status = 'REFUND_PENDING';
                    } else {
                        $payment->status = 'SUCCESS';
                    }

                    $payment->refunded_at = $payment->status === 'REFUNDED'
                        ? now()
                        : $payment->refunded_at;
                    $payment->save();
                }

                return $refund;
            }

            if (! $payment) {
                throw new \RuntimeException('No payment record is attached to this order.');
            }

            return Refund::create([
                'order_id' => $lockedOrder->id,
                'payment_id' => $payment->id,
                'customer_id' => $customer->id,
                'requested_by' => $request->user()->id,
                'amount' => $amount,
                'reason' => trim($request->input('reason')),
                'status' => 'PENDING',
            ]);
        });

        if ($result->status === 'PENDING' && $result->payment_id) {
            $payment = Payment::find($result->payment_id);
            if ($payment) {
                try {
                    $result = $paystackRefunds->initiate($payment, $result);
                } catch (\Throwable $e) {
                    report($e);
                }
            }
        }

        AuditLog::create([
            'user_id' => $request->user()->id,
            'timestamp' => now()->getTimestampMs(),
            'action' => 'REFUND_CREATED',
            'details' => "Refund #{$result->id} created for order {$order->order_number}.",
        ]);

        $message = match ($result->status) {
            'SUCCESS' => 'Refund completed.',
            'FAILED' => 'Refund could not be initiated. Please review the payment record.',
            default => 'Refund has been submitted and is being processed.',
        };

        return response()->json([
            'success' => $result->status !== 'FAILED',
            'message' => $message,
            'refund' => $result,
        ], $result->status === 'FAILED' ? 502 : ($result->status === 'SUCCESS' ? 200 : 202));
    }

    public function settlements(Request $request)
    {
        $query = VendorSettlement::with('vendor')->latest();
        return response()->json(['success' => true, 'settlements' => $query->paginate(50)]);
    }

    public function generateSettlement(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'vendor_id' => 'required|integer|exists:users,id',
            'period_start' => 'required|date_format:Y-m-d',
            'period_end' => 'required|date_format:Y-m-d|after_or_equal:period_start',
            'fees' => 'nullable|numeric|min:0',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid settlement period.', 'errors' => $validator->errors()], 422);
        }

        $start = $request->input('period_start');
        $end = $request->input('period_end');
        $vendorId = (int) $request->input('vendor_id');
        $fees = round((float) $request->input('fees', 0), 2);

        $vendor = User::whereKey($vendorId)
            ->whereRaw('upper(role) = ?', ['VENDOR'])
            ->first();

        if (! $vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Settlement vendor must be a valid vendor account.',
            ], 422);
        }

        $gross = (float) Order::withoutGlobalScopes()
            ->where('vendor_id', $vendorId)
            ->whereIn('status', ['COMPLETED','DELIVERED'])
            ->whereBetween('created_at', [$start.' 00:00:00', $end.' 23:59:59'])
            ->sum(DB::raw('COALESCE(grand_total, total_price)'));

        $refunds = (float) Refund::whereNotNull('requested_by')
            ->whereHas('order', fn ($q) => $q->where('vendor_id', $vendorId))
            ->where('status', 'SUCCESS')
            ->whereBetween('created_at', [$start.' 00:00:00', $end.' 23:59:59'])
            ->sum('amount');

        $net = round(max(0, $gross - $refunds - $fees), 2);

        $settlement = VendorSettlement::updateOrCreate(
            ['vendor_id' => $vendorId, 'period_start' => $start, 'period_end' => $end],
            ['gross_sales' => $gross, 'refunds' => $refunds, 'fees' => $fees, 'net_amount' => $net, 'status' => 'PENDING']
        );

        return response()->json(['success' => true, 'settlement' => $settlement], 201);
    }

    public function payoutSettlement(VendorSettlement $settlement, PaystackPayoutService $payouts)
    {
        try {
            $locked = DB::transaction(function () use ($settlement) {
                $row = VendorSettlement::whereKey($settlement->id)->lockForUpdate()->firstOrFail();
                if (in_array(strtoupper((string) $row->status), ['PAID', 'SETTLED', 'COMPLETED'], true)) {
                    throw new \RuntimeException('This settlement has already been paid.');
                }
                if (strtoupper((string) $row->status) === 'PROCESSING') {
                    throw new \RuntimeException('This settlement already has a payout in progress.');
                }
                if ((float) $row->net_amount <= 0) {
                    throw new \RuntimeException('Settlement amount must be greater than zero.');
                }

                $wasFailed = strtoupper((string) $row->status) === 'FAILED';
                $row->status = 'PROCESSING';
                $row->payout_attempted_at = now();
                $row->failure_reason = null;
                $row->gateway_status = null;
                $row->transfer_code = null;

                if (! $row->payout_reference || $wasFailed) {
                    $row->payout_reference = 'atu_settle_' . \Illuminate\Support\Str::lower(
                        str_replace('-', '', (string) \Illuminate\Support\Str::uuid())
                    );
                }

                $row->save();
                return $row->fresh();
            });
        } catch (\RuntimeException $e) {
            return response()->json([
                'success' => false,
                'message' => $e->getMessage(),
            ], 409);
        }

        $account = VendorPayoutAccount::where('vendor_id', $locked->vendor_id)->where('status', 'ACTIVE')->first();
        if (! $account) {
            $locked->update(['status' => 'FAILED', 'failure_reason' => 'Vendor has no active payout account.']);
            return response()->json(['success' => false, 'message' => 'Vendor has no active payout account.'], 422);
        }

        try {
            $data = $payouts->initiateTransfer(
                $account,
                (float) $locked->net_amount,
                'ATU Cafeteria settlement ' . $locked->period_start . ' to ' . $locked->period_end,
                $locked->payout_reference,
            );
            $gatewayStatus = strtolower((string) ($data['status'] ?? 'pending'));
            $locked->update([
                'gateway_status' => strtoupper($gatewayStatus),
                'transfer_code' => $data['transfer_code'] ?? null,
                'status' => $gatewayStatus === 'success' ? 'PAID' : 'PROCESSING',
                'settled_at' => $gatewayStatus === 'success' ? now() : $locked->settled_at,
            ]);
        } catch (\Throwable $e) {
            report($e);
            $locked->update(['status' => 'FAILED', 'gateway_status' => 'FAILED', 'failure_reason' => $e->getMessage()]);
            return response()->json(['success' => false, 'message' => 'Vendor payout could not be initiated.'], 502);
        }

        AuditLog::create([
            'user_id' => request()->user()->id,
            'timestamp' => now()->getTimestampMs(),
            'action' => 'VENDOR_SETTLEMENT_PAYOUT_INITIATED',
            'details' => 'Settlement #' . $locked->id . ' payout reference ' . $locked->payout_reference . '.',
        ]);

        return response()->json([
            'success' => true,
            'message' => $locked->status === 'PAID' ? 'Vendor payout completed.' : 'Vendor payout queued for processing.',
            'settlement' => $locked->fresh(),
        ], $locked->status === 'PAID' ? 200 : 202);
    }

    public function finalizeSettlementPayout(Request $request, VendorSettlement $settlement, PaystackPayoutService $payouts)
    {
        $validator = Validator::make($request->all(), [
            'otp' => 'required|string|min:4|max:12',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'A valid transfer OTP is required.', 'errors' => $validator->errors()], 422);
        }

        $locked = DB::transaction(fn () => VendorSettlement::whereKey($settlement->id)->lockForUpdate()->firstOrFail());
        if (strtoupper((string) $locked->status) !== 'PROCESSING' || ! $locked->transfer_code) {
            return response()->json(['success' => false, 'message' => 'This settlement is not awaiting transfer authorization.'], 409);
        }

        try {
            $data = $payouts->finalizeTransfer($locked->transfer_code, trim((string) $request->input('otp')));
            $gatewayStatus = strtolower((string) ($data['status'] ?? 'pending'));
            $locked->update([
                'gateway_status' => strtoupper($gatewayStatus),
                'status' => $gatewayStatus === 'success' ? 'PAID' : 'PROCESSING',
                'settled_at' => $gatewayStatus === 'success' ? now() : $locked->settled_at,
            ]);
        } catch (\Throwable $e) {
            report($e);
            $locked->update(['gateway_status' => 'FAILED', 'failure_reason' => $e->getMessage()]);
            return response()->json(['success' => false, 'message' => 'Transfer authorization failed.'], 502);
        }

        return response()->json([
            'success' => true,
            'message' => $locked->status === 'PAID' ? 'Vendor payout finalized.' : 'Vendor payout remains processing.',
            'settlement' => $locked->fresh(),
        ], $locked->status === 'PAID' ? 200 : 202);
    }

    public function supportTickets()
    {
        return response()->json(['success' => true, 'tickets' => SupportTicket::with(['customer','order','assignedTo'])->latest()->paginate(50)]);
    }

    public function updateSupportTicket(Request $request, SupportTicket $ticket)
    {
        $validator = Validator::make($request->all(), [
            'status' => 'sometimes|string|in:OPEN,IN_PROGRESS,WAITING_CUSTOMER,RESOLVED,CLOSED',
            'priority' => 'sometimes|string|in:LOW,NORMAL,HIGH,URGENT',
            'assigned_to' => 'nullable|integer|exists:users,id',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid support ticket update.', 'errors' => $validator->errors()], 422);
        }

        $ticket->fill($request->only(['status','priority','assigned_to']));
        if (($request->input('status') ?? '') === 'RESOLVED') $ticket->resolved_at = now();
        $ticket->save();

        return response()->json(['success' => true, 'ticket' => $ticket]);
    }
}
