<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Order;
use App\Models\Payment;
use App\Models\Promotion;
use App\Models\Refund;
use App\Models\SupportTicket;
use App\Models\User;
use App\Models\VendorSettlement;
use App\Models\WalletTransaction;
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

        $promotion = Promotion::create([
            ...$request->only([
                'name','type','value','minimum_order_amount','maximum_discount_amount',
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

            $alreadyRefunded = (float) Refund::where('order_id', $lockedOrder->id)
                ->whereIn('status', ['SUCCESS','PROCESSING'])
                ->sum('amount');
            $maxRefund = max(0, (float) $lockedOrder->grand_total ?: (float) $lockedOrder->total_price);
            $remaining = round($maxRefund - $alreadyRefunded, 2);
            $amount = round((float) ($request->input('amount') ?? $remaining), 2);

            if ($amount <= 0 || $amount > $remaining) {
                throw new \RuntimeException('Refund amount exceeds the remaining refundable order value.');
            }

            $payment = Payment::where('order_id', $lockedOrder->id)->latest()->lockForUpdate()->first();

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

                if ($payment) {
                    $payment->status = 'REFUNDED';
                    $payment->refunded_at = now();
                    $payment->save();
                }

                return $refund;
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
