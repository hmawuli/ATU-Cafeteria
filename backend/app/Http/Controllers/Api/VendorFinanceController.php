<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use App\Models\Refund;
use App\Models\User;
use App\Models\VendorSettlement;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class VendorFinanceController extends Controller
{
    /**
     * Return financial information for the authenticated vendor.
     *
     * Vendors can only ever receive their own figures. Administrators may
     * inspect a specific vendor by supplying vendor_id.
     */
    public function index(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $role = strtoupper((string) $user->role);
        if (! in_array($role, ['VENDOR', 'ADMIN'], true)) {
            return response()->json([
                'success' => false,
                'message' => 'Only vendors and administrators can view vendor finance.',
            ], 403);
        }

        $vendorId = (int) $user->id;
        if ($role === 'ADMIN' && $request->filled('vendor_id')) {
            $vendorId = (int) $request->integer('vendor_id');
        }

        $vendor = User::whereKey($vendorId)
            ->whereRaw('upper(role) = ?', ['VENDOR'])
            ->first();

        if (! $vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor not found.',
            ], 404);
        }

        $days = (int) $request->input('days', 30);
        if (! in_array($days, [7, 30, 90], true)) {
            $days = 30;
        }

        $periodStart = now()->subDays($days - 1)->startOfDay();
        $periodEnd = now()->endOfDay();

        $completedOrders = Order::withoutGlobalScopes()
            ->where('vendor_id', $vendorId)
            ->whereBetween('created_at', [$periodStart, $periodEnd])
            ->whereIn(DB::raw('upper(status)'), ['COMPLETED', 'DELIVERED'])
            ->orderByDesc('created_at')
            ->get();

        $orderIds = $completedOrders->pluck('id');

        $refundMap = $orderIds->isEmpty()
            ? collect()
            : Refund::whereIn('order_id', $orderIds)
                ->where('status', 'SUCCESS')
                ->select('order_id', DB::raw('SUM(amount) as amount'))
                ->groupBy('order_id')
                ->pluck('amount', 'order_id');

        $grossFoodSales = 0.0;
        $discounts = 0.0;
        $refunds = 0.0;
        $customerCollected = 0.0;
        $taxes = 0.0;
        $serviceFees = 0.0;
        $deliveryFees = 0.0;

        $transactions = [];
        foreach ($completedOrders as $order) {
            $gross = (float) ($order->subtotal > 0 ? $order->subtotal : $order->total_price);
            $discount = (float) ($order->discount_amount ?: $order->discount_applied ?: 0);
            $customerPaid = (float) ($order->grand_total > 0 ? $order->grand_total : $order->total_price);
            $refund = (float) ($refundMap[$order->id] ?? 0);
            $net = max(0, round($gross - $discount - $refund, 2));

            $grossFoodSales += $gross;
            $discounts += $discount;
            $refunds += $refund;
            $customerCollected += $customerPaid;
            $taxes += (float) ($order->tax_amount ?? 0);
            $serviceFees += (float) ($order->service_fee ?? 0);
            $deliveryFees += (float) ($order->delivery_fee ?? 0);

            $transactions[] = [
                'order_id' => (int) $order->id,
                'order_number' => $order->order_number ?: 'ATU-'.$order->id,
                'date' => optional($order->created_at)->toIso8601String(),
                'status' => strtoupper((string) $order->status),
                'payment_method' => strtoupper((string) ($order->payment_method ?: 'UNKNOWN')),
                'payment_status' => strtoupper((string) ($order->payment_status ?: 'UNKNOWN')),
                'item' => $order->food_name,
                'quantity' => (int) ($order->quantity ?? 0),
                'gross_sales' => round($gross, 2),
                'discount' => round($discount, 2),
                'refund' => round($refund, 2),
                'net_sales' => $net,
                'customer_paid' => round($customerPaid, 2),
            ];
        }

        $netFoodSales = max(
            0,
            round($grossFoodSales - $discounts - $refunds, 2)
        );

        $settlements = VendorSettlement::where('vendor_id', $vendorId)
            ->orderByDesc('created_at')
            ->limit(12)
            ->get();

        $pendingSettlement = (float) $settlements
            ->whereIn('status', ['PENDING', 'PROCESSING'])
            ->sum(fn ($item) => (float) $item->net_amount);

        $settledAmount = (float) $settlements
            ->whereIn('status', ['PAID', 'SETTLED', 'COMPLETED'])
            ->sum(fn ($item) => (float) $item->net_amount);

        $trend = [];
        for ($offset = $days - 1; $offset >= 0; $offset--) {
            $date = now()->subDays($offset)->format('Y-m-d');
            $trend[$date] = [
                'date' => $date,
                'sales' => 0.0,
                'orders' => 0,
            ];
        }

        foreach ($completedOrders as $order) {
            $date = optional($order->created_at)->format('Y-m-d');
            if (! isset($trend[$date])) {
                continue;
            }

            $gross = (float) ($order->subtotal > 0 ? $order->subtotal : $order->total_price);
            $discount = (float) ($order->discount_amount ?: $order->discount_applied ?: 0);
            $refund = (float) ($refundMap[$order->id] ?? 0);

            $trend[$date]['sales'] = round(
                $trend[$date]['sales'] + max(0, $gross - $discount - $refund),
                2
            );
            $trend[$date]['orders']++;
        }

        $settlementPayload = $settlements->map(fn ($settlement) => [
            'id' => (int) $settlement->id,
            'period_start' => $settlement->period_start,
            'period_end' => $settlement->period_end,
            'gross_sales' => round((float) $settlement->gross_sales, 2),
            'refunds' => round((float) $settlement->refunds, 2),
            'fees' => round((float) $settlement->fees, 2),
            'net_amount' => round((float) $settlement->net_amount, 2),
            'status' => strtoupper((string) $settlement->status),
            'payout_reference' => $settlement->payout_reference,
            'settled_at' => optional($settlement->settled_at)->toIso8601String(),
        ])->values();

        return response()->json([
            'success' => true,
            'vendor' => [
                'id' => (int) $vendor->id,
                'name' => $vendor->fullName,
                'is_open' => (bool) $vendor->is_open,
            ],
            'period' => [
                'days' => $days,
                'start' => $periodStart->toIso8601String(),
                'end' => $periodEnd->toIso8601String(),
            ],
            'summary' => [
                'gross_food_sales' => round($grossFoodSales, 2),
                'discounts' => round($discounts, 2),
                'refunds' => round($refunds, 2),
                'net_food_sales' => $netFoodSales,
                'customer_collected' => round($customerCollected, 2),
                'taxes' => round($taxes, 2),
                'service_fees' => round($serviceFees, 2),
                'delivery_fees' => round($deliveryFees, 2),
                'completed_orders' => $completedOrders->count(),
                'average_order_value' => $completedOrders->count() > 0
                    ? round($netFoodSales / $completedOrders->count(), 2)
                    : 0.0,
                'pending_settlement' => round($pendingSettlement, 2),
                'settled_amount' => round($settledAmount, 2),
            ],
            'trend' => array_values($trend),
            'transactions' => array_slice($transactions, 0, 20),
            'settlements' => $settlementPayload,
            'generated_at' => now()->toIso8601String(),
        ]);
    }
}
