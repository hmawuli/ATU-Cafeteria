<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class LoyaltyController extends Controller
{
    /**
     * Retrieve the authenticated user's loyalty point status and history.
     */
    public function getLoyaltySummary(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'STUDENT' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Loyalty rewards program is restricted to students.',
            ], 403);
        }

        $points = (int) ($user->loyalty_points ?? 0);
        $totalSpent = (float) ($user->total_spent ?? 0.00);

        // Calculate cashback conversion: 10 points = GH₵ 4.00 (i.e. 1 point = GH₵ 0.40)
        $cashbackRate = 0.40;
        $equivalentCashbackVal = round($points * $cashbackRate, 2);

        // Tier thresholds
        $tier = 'Bronze member';
        $nextTier = 'Silver Elite';
        $pointsNeededForNext = 100 - $points;

        if ($points >= 500) {
            $tier = 'Platinum Legends';
            $nextTier = 'Max Tier Reached';
            $pointsNeededForNext = 0;
        } elseif ($points >= 250) {
            $tier = 'Gold Star';
            $nextTier = 'Platinum Legends';
            $pointsNeededForNext = 500 - $points;
        } elseif ($points >= 100) {
            $tier = 'Silver Elite';
            $nextTier = 'Gold Star';
            $pointsNeededForNext = 250 - $points;
        }

        // Fetch user orders to compile a dynamic history of earning and redemption events
        $orders = Order::where(function ($query) use ($user) {
            $query->where('customer_id', $user->id)
                ->orWhere('student_id', $user->id)
                ->orWhere('user_id', $user->id);
        })
            ->whereIn(DB::raw('upper(status)'), ['COMPLETED', 'DELIVERED', 'READY', 'PENDING'])
            ->orderBy('order_timestamp', 'desc')
            ->get();

        $history = [];

        foreach ($orders as $order) {
            $timestampSec = $order->order_timestamp / 1000;
            $formattedDate = date('M d, Y h:i A', $timestampSec);

            // 1. Redemption Transaction (occurred when order was placed/processed)
            if ($order->points_redeemed > 0) {
                $history[] = [
                    'order_id' => $order->id,
                    'food_name' => $order->food_name,
                    'type' => 'REDEMPTION',
                    'points' => -$order->points_redeemed,
                    'discount_applied' => (float) $order->discount_applied,
                    'description' => "Redeemed {$order->points_redeemed} points for GH₵ ".number_format($order->discount_applied, 2)." discount on '{$order->food_name}'",
                    'status' => 'COMPLETED',
                    'date' => $formattedDate,
                    'timestamp_ms' => $order->order_timestamp,
                ];
            }

            // 2. Earning Transaction (granted only on completed/delivered status)
            if (in_array(strtoupper($order->status), ['COMPLETED', 'DELIVERED'])) {
                $earned = intval(floor($order->total_price));
                if ($earned > 0) {
                    $history[] = [
                        'order_id' => $order->id,
                        'food_name' => $order->food_name,
                        'type' => 'EARNED',
                        'points' => $earned,
                        'discount_applied' => 0.00,
                        'description' => "Earned {$earned} points from order #{$order->id} (Paid GH₵ ".number_format($order->total_price, 2).')',
                        'status' => 'COMPLETED',
                        'date' => $formattedDate,
                        'timestamp_ms' => $order->order_timestamp,
                    ];
                }
            }
        }

        // Sort history by timestamp descending
        usort($history, function ($a, $b) {
            return $b['timestamp_ms'] <=> $a['timestamp_ms'];
        });

        return response()->json([
            'success' => true,
            'loyalty_points_balance' => $points,
            'equivalent_cashback_value' => $equivalentCashbackVal,
            'tier' => $tier,
            'next_tier' => $nextTier,
            'points_needed_for_next_tier' => max(0, $pointsNeededForNext),
            'total_spent_all_time' => $totalSpent,
            'conversion_rule' => '10 Loyalty Points = GH₵ 4.00 Discount',
            'earning_rule' => 'Earn 1 Loyalty Point for every GH₵ 1.00 spent on completed orders',
            'history' => $history,
            'generated_at' => date('c'),
        ], 200);
    }

    /**
     * Preview discount before placing order.
     */
    public function previewDiscount(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $request->validate([
            'points_to_redeem' => 'required|integer|min:1',
        ]);

        $pointsToRedeem = intval($request->input('points_to_redeem'));
        $balance = (int) ($user->loyalty_points ?? 0);

        if ($balance < $pointsToRedeem) {
            return response()->json([
                'success' => false,
                'message' => "Insufficient loyalty points balance. You have {$balance} points, but requested to redeem {$pointsToRedeem}.",
            ], 400);
        }

        // 10 points = 4.00 GHS
        $discount = round($pointsToRedeem * 0.40, 2);

        return response()->json([
            'success' => true,
            'points_to_redeem' => $pointsToRedeem,
            'discount_value' => $discount,
            'currency' => 'GH₵',
            'remaining_points' => $balance - $pointsToRedeem,
        ], 200);
    }
}
