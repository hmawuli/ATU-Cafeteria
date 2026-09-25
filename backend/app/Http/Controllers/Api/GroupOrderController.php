<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\GroupOrder;
use App\Models\GroupOrderItem;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class GroupOrderController extends Controller
{
    /**
     * Start a new group order session.
     */
    public function createSession(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        if (strtoupper($user->role) !== 'STUDENT') {
            return response()->json([
                'success' => false,
                'message' => 'Only registered students can initiate group order checkout sessions.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'vendor_id' => 'required|integer|exists:users,id',
            'payment_mode' => 'nullable|string|in:HOST_PAYS,INDIVIDUAL',
            'duration_minutes' => 'nullable|integer|min:5|max:120',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error.',
                'errors' => $validator->errors(),
            ], 400);
        }

        $vendorId = $request->input('vendor_id');
        $vendor = User::find($vendorId);
        if (! $vendor || strtoupper($vendor->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Selected vendor ID is invalid or not registered as a food vendor.',
            ], 400);
        }

        $paymentMode = $request->input('payment_mode', 'HOST_PAYS');
        $durationMinutes = intval($request->input('duration_minutes', 30));

        // Generate a clean unique alphanumeric session code (e.g., GP-F3K8A9)
        $code = '';
        $exists = true;
        while ($exists) {
            $code = 'GP-'.strtoupper(bin2hex(random_bytes(3)));
            $exists = GroupOrder::where('code', $code)->exists();
        }

        $expiresAt = now()->addMinutes($durationMinutes);

        $session = GroupOrder::create([
            'code' => $code,
            'creator_id' => $user->id,
            'vendor_id' => $vendorId,
            'status' => 'OPEN',
            'payment_mode' => $paymentMode,
            'expires_at' => $expiresAt,
        ]);

        // Register Audit Entry
        $vendorDisplayName = $vendor->fullName ?? $vendor->username;
        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'GROUP_ORDER_CREATED',
            'details' => "Student {$user->fullName} generated shared group order session {$code} for vendor '{$vendorDisplayName}' with checkout mode '{$paymentMode}'.",
        ]);

        // Construct a shareable invitation link using the app URL metadata or default host
        $baseUrl = url('/');
        $invitationLink = "{$baseUrl}/api/student/group-order/join/{$code}";

        return response()->json([
            'success' => true,
            'message' => 'Group order session created successfully.',
            'session_code' => $code,
            'payment_mode' => $paymentMode,
            'invitation_link' => $invitationLink,
            'expires_at' => $expiresAt->toIso8601String(),
            'session' => $session->load(['creator', 'vendor']),
        ], 201);
    }

    /**
     * Retrieve group order session details by code.
     */
    public function getSessionDetails(Request $request, $code)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $session = GroupOrder::where('code', $code)->first();
        if (! $session) {
            return response()->json([
                'success' => false,
                'message' => 'Group order session not found.',
            ], 404);
        }

        // Auto-expire session if time passed
        if ($session->status === 'OPEN' && $session->expires_at && now()->greaterThan($session->expires_at)) {
            $session->status = 'CANCELLED';
            $session->save();
        }

        // Fetch contributed items with item and user details
        $items = GroupOrderItem::where('group_order_id', $session->id)
            ->with(['user', 'menuItem'])
            ->get();

        // Calculate aggregates
        $totalItemsCount = 0;
        $totalSessionCost = 0.00;
        $contributorCosts = [];

        foreach ($items as $item) {
            $price = floatval($item->menuItem->price);
            $subtotal = round($price * $item->quantity, 2);

            $totalItemsCount += $item->quantity;
            $totalSessionCost += $subtotal;

            $cId = $item->user_id;
            $cName = $item->user->fullName ?: $item->user->username;

            if (! isset($contributorCosts[$cId])) {
                $contributorCosts[$cId] = [
                    'user_id' => $cId,
                    'username' => $cName,
                    'total_items' => 0,
                    'total_cost' => 0.00,
                    'is_creator' => ($cId === $session->creator_id),
                ];
            }
            $contributorCosts[$cId]['total_items'] += $item->quantity;
            $contributorCosts[$cId]['total_cost'] += $subtotal;
        }

        // Refine contributor summaries
        foreach ($contributorCosts as $uid => $data) {
            $contributorCosts[$uid]['total_cost'] = round($data['total_cost'], 2);
        }

        return response()->json([
            'success' => true,
            'session' => [
                'id' => $session->id,
                'code' => $session->code,
                'creator_id' => $session->creator_id,
                'creator_name' => $session->creator->fullName ?: $session->creator->username,
                'vendor_id' => $session->vendor_id,
                'vendor_name' => $session->vendor->fullName ?: $session->vendor->username,
                'status' => $session->status,
                'payment_mode' => $session->payment_mode,
                'expires_at' => $session->expires_at ? $session->expires_at->toIso8601String() : null,
                'time_remaining_seconds' => $session->expires_at ? max(0, $session->expires_at->diffInSeconds(now(), false) * -1) : null,
                'total_items' => $totalItemsCount,
                'total_cost' => round($totalSessionCost, 2),
            ],
            'contributors' => array_values($contributorCosts),
            'items' => $items->map(function ($item) {
                return [
                    'id' => $item->id,
                    'menu_item_id' => $item->menu_item_id,
                    'food_name' => $item->menuItem->name,
                    'unit_price' => (float) $item->menuItem->price,
                    'quantity' => $item->quantity,
                    'subtotal' => round($item->menuItem->price * $item->quantity, 2),
                    'custom_notes' => $item->custom_notes,
                    'added_by' => [
                        'user_id' => $item->user_id,
                        'name' => $item->user->fullName ?: $item->user->username,
                    ],
                ];
            }),
        ], 200);
    }

    /**
     * Add or update an item contribution in a group order session.
     */
    public function contributeItem(Request $request, $code)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $session = GroupOrder::where('code', $code)->first();
        if (! $session) {
            return response()->json([
                'success' => false,
                'message' => 'Group order session not found.',
            ], 404);
        }

        if ($session->status !== 'OPEN') {
            return response()->json([
                'success' => false,
                'message' => "Contributions are locked because this session is marked as {$session->status}.",
            ], 400);
        }

        if ($session->expires_at && now()->greaterThan($session->expires_at)) {
            $session->status = 'CANCELLED';
            $session->save();

            return response()->json([
                'success' => false,
                'message' => 'This group order session has expired.',
            ], 400);
        }

        $validator = Validator::make($request->all(), [
            'menu_item_id' => 'required|integer|exists:menu_items,id',
            'quantity' => 'required|integer|min:1',
            'custom_notes' => 'nullable|string|max:150',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation failed.',
                'errors' => $validator->errors(),
            ], 400);
        }

        $menuItemId = $request->input('menu_item_id');
        $quantity = intval($request->input('quantity'));
        $notes = $request->input('custom_notes');

        $menuItem = MenuItem::find($menuItemId);
        if ($menuItem->vendor_id !== $session->vendor_id) {
            return response()->json([
                'success' => false,
                'message' => "The selected menu item '{$menuItem->name}' is sold by another vendor. Group items must belong to vendor ID {$session->vendor_id}.",
            ], 400);
        }

        if (! $menuItem->is_available) {
            return response()->json([
                'success' => false,
                'message' => "Menu item '{$menuItem->name}' is currently out of stock.",
            ], 400);
        }

        // Check if user already contributed this specific menu item - if yes, aggregate
        $existingContribution = GroupOrderItem::where('group_order_id', $session->id)
            ->where('user_id', $user->id)
            ->where('menu_item_id', $menuItemId)
            ->first();

        if ($existingContribution) {
            $existingContribution->quantity += $quantity;
            if ($notes) {
                $existingContribution->custom_notes = $existingContribution->custom_notes
                    ? $existingContribution->custom_notes.' | '.$notes
                    : $notes;
            }
            $existingContribution->save();
            $contribution = $existingContribution;
        } else {
            $contribution = GroupOrderItem::create([
                'group_order_id' => $session->id,
                'user_id' => $user->id,
                'menu_item_id' => $menuItemId,
                'quantity' => $quantity,
                'custom_notes' => $notes,
            ]);
        }

        return response()->json([
            'success' => true,
            'message' => "Added {$quantity} portions of '{$menuItem->name}' to group session {$code}.",
            'contribution' => $contribution,
        ], 200);
    }

    /**
     * Remove or reduce item contribution.
     */
    public function removeContribution(Request $request, $code, $itemId)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $session = GroupOrder::where('code', $code)->first();
        if (! $session) {
            return response()->json(['success' => false, 'message' => 'Session not found.'], 404);
        }

        if ($session->status !== 'OPEN') {
            return response()->json([
                'success' => false,
                'message' => 'Cannot modify items. This group order session is closed or locked.',
            ], 400);
        }

        $item = GroupOrderItem::find($itemId);
        if (! $item || $item->group_order_id !== $session->id) {
            return response()->json(['success' => false, 'message' => 'Contribution item not found in this session.'], 404);
        }

        // Only the host or the contributor can delete/remove
        if ($user->id !== $session->creator_id && $user->id !== $item->user_id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only the session creator or the original contributor can remove this item.',
            ], 403);
        }

        $item->delete();

        return response()->json([
            'success' => true,
            'message' => 'Contributed item successfully removed from group order session.',
        ], 200);
    }

    /**
     * Lock group session so no more contributions can be made.
     */
    public function lockSession(Request $request, $code)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $session = GroupOrder::where('code', $code)->first();
        if (! $session) {
            return response()->json(['success' => false, 'message' => 'Session not found.'], 404);
        }

        if ($user->id !== $session->creator_id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only the group host can lock the checkout session.',
            ], 403);
        }

        $session->status = 'LOCKED';
        $session->save();

        return response()->json([
            'success' => true,
            'message' => "Group session {$code} is now LOCKED. Pre-order checkout compiles next.",
            'session' => $session,
        ], 200);
    }

    /**
     * Cancel / Close group session.
     */
    public function cancelSession(Request $request, $code)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $session = GroupOrder::where('code', $code)->first();
        if (! $session) {
            return response()->json(['success' => false, 'message' => 'Session not found.'], 404);
        }

        if ($user->id !== $session->creator_id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only the group host can cancel this session.',
            ], 403);
        }

        $session->status = 'CANCELLED';
        $session->save();

        return response()->json([
            'success' => true,
            'message' => 'Group order session cancelled successfully.',
            'session' => $session,
        ], 200);
    }

    /**
     * Execute consolidated group checkout.
     */
    public function checkoutSession(Request $request, $code)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $session = GroupOrder::where('code', $code)->first();
        if (! $session) {
            return response()->json(['success' => false, 'message' => 'Session not found.'], 404);
        }

        if ($user->id !== $session->creator_id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only the group session host can trigger the final consolidated vendor checkout.',
            ], 403);
        }

        if ($session->status === 'COMPLETED') {
            return response()->json([
                'success' => false,
                'message' => 'This group order checkout session has already been completed.',
            ], 400);
        }

        if ($session->status === 'CANCELLED') {
            return response()->json([
                'success' => false,
                'message' => 'Cannot checkout a cancelled or expired session.',
            ], 400);
        }

        $items = GroupOrderItem::where('group_order_id', $session->id)
            ->with(['user', 'menuItem'])
            ->get();

        if ($items->isEmpty()) {
            return response()->json([
                'success' => false,
                'message' => 'Cannot checkout an empty group order. Please contribute food items first.',
            ], 400);
        }

        // Compute costs & validate stock
        $totalCheckoutCost = 0.00;
        $contributorCosts = [];

        foreach ($items as $item) {
            $menuItem = $item->menuItem;
            if (! $menuItem->is_available) {
                return response()->json([
                    'success' => false,
                    'message' => "Menu item '{$menuItem->name}' is currently unavailable.",
                ], 400);
            }

            if ($menuItem->current_stock !== null && $menuItem->current_stock < $item->quantity) {
                return response()->json([
                    'success' => false,
                    'message' => "Insufficient stock for '{$menuItem->name}'. Requested {$item->quantity}, but only {$menuItem->current_stock} remaining.",
                ], 400);
            }

            $price = floatval($menuItem->price);
            $subtotal = round($price * $item->quantity, 2);

            $totalCheckoutCost += $subtotal;

            $uId = $item->user_id;
            if (! isset($contributorCosts[$uId])) {
                $contributorCosts[$uId] = 0.00;
            }
            $contributorCosts[$uId] += $subtotal;
        }

        // Perform balance validations
        if ($session->payment_mode === 'HOST_PAYS') {
            if ($session->creator->balance < $totalCheckoutCost) {
                return response()->json([
                    'success' => false,
                    'message' => 'Insufficient host wallet balance. Host balance is GH₵ '.number_format($session->creator->balance, 2).', but total group cost is GH₵ '.number_format($totalCheckoutCost, 2).'.',
                ], 400);
            }
        } else {
            // INDIVIDUAL split - verify each user has enough balance
            foreach ($contributorCosts as $uId => $reqCost) {
                $contributor = User::find($uId);
                if ($contributor->balance < $reqCost) {
                    return response()->json([
                        'success' => false,
                        'message' => "Checkout failed. Contributor '".($contributor->fullName ?: $contributor->username)."' has insufficient wallet balance. Required: GH₵ ".number_format($reqCost, 2).', Available: GH₵ '.number_format($contributor->balance, 2).'.',
                    ], 400);
                }
            }
        }

        // Let's also check if user wants to apply loyalty points to reduce host payment!
        $pointsToRedeem = intval($request->input('points_to_redeem', 0));
        $loyaltyDiscount = 0.00;
        if ($pointsToRedeem > 0) {
            if ($session->payment_mode !== 'HOST_PAYS') {
                return response()->json([
                    'success' => false,
                    'message' => 'Loyalty point discounts in group sessions are only available for Host Pays payment mode.',
                ], 400);
            }

            if (($session->creator->loyalty_points ?? 0) < $pointsToRedeem) {
                return response()->json([
                    'success' => false,
                    'message' => 'Insufficient loyalty points. Host has only '.($session->creator->loyalty_points ?? 0).' points.',
                ], 400);
            }

            $loyaltyDiscount = round($pointsToRedeem * 0.40, 2);
        }

        $finalCheckoutCost = max(0.00, round($totalCheckoutCost - $loyaltyDiscount, 2));

        // Re-check host balance after loyalty points deduction
        if ($session->payment_mode === 'HOST_PAYS' && $session->creator->balance < $finalCheckoutCost) {
            return response()->json([
                'success' => false,
                'message' => 'Insufficient wallet balance after loyalty points. Required: GH₵ '.number_format($finalCheckoutCost, 2).', host balance is GH₵ '.number_format($session->creator->balance, 2).'.',
            ], 400);
        }

        // Consolidated order creation transaction
        try {
            $createdOrders = DB::transaction(function () use ($session, $items, $contributorCosts, $totalCheckoutCost, $finalCheckoutCost, $pointsToRedeem, $loyaltyDiscount) {
                // 1. Deduct wallet balances and update user stats
                if ($session->payment_mode === 'HOST_PAYS') {
                    $host = $session->creator;
                    $host->balance = $host->balance - $finalCheckoutCost;
                    if ($pointsToRedeem > 0) {
                        $host->loyalty_points = ($host->loyalty_points ?? 0) - $pointsToRedeem;
                    }
                    $host->save();

                    // Generate one main Host payment transaction
                    WalletTransaction::create([
                        'user_id' => $host->id,
                        'type' => 'PAYMENT',
                        'amount' => -$finalCheckoutCost,
                        'status' => 'SUCCESS',
                        'reference' => 'GRP-HOST-'.$session->code.'-'.time(),
                        'details' => "Paid for Consolidated Group Order Pre-Order Session {$session->code}.".($loyaltyDiscount > 0 ? " Redeemed {$pointsToRedeem} loyalty points for GH₵ {$loyaltyDiscount} discount." : ''),
                    ]);
                } else {
                    // INDIVIDUAL payment deduction
                    foreach ($contributorCosts as $uId => $reqCost) {
                        $contributor = User::find($uId);
                        $contributor->balance = $contributor->balance - $reqCost;
                        $contributor->save();

                        WalletTransaction::create([
                            'user_id' => $contributor->id,
                            'type' => 'PAYMENT',
                            'amount' => -$reqCost,
                            'status' => 'SUCCESS',
                            'reference' => 'GRP-INDIV-'.$session->code.'-'.time(),
                            'details' => "Paid portion for Group Order Session {$session->code}.",
                        ]);
                    }
                }

                // 2. Generate Orders and update menu item stock
                $securePin = (string) rand(1000, 9999);
                $ordersList = [];

                $remainingPointsToDistribute = $pointsToRedeem;
                $remainingDiscountToDistribute = $loyaltyDiscount;
                $itemsCount = count($items);

                foreach ($items as $index => $item) {
                    $menuItem = $item->menuItem;

                    // Reduce stock
                    if ($menuItem->current_stock !== null) {
                        $menuItem->current_stock -= $item->quantity;
                        $menuItem->save();
                    }

                    // Apply loyalty discount proportionally if Host Pays
                    $itemPoints = 0;
                    $itemDiscount = 0.00;

                    if ($session->payment_mode === 'HOST_PAYS' && $pointsToRedeem > 0) {
                        if ($index === $itemsCount - 1) {
                            $itemPoints = $remainingPointsToDistribute;
                            $itemDiscount = $remainingDiscountToDistribute;
                        } else {
                            $ratio = ($menuItem->price * $item->quantity) / max(1.0, $totalCheckoutCost);
                            $itemPoints = intval(round($pointsToRedeem * $ratio));
                            $itemDiscount = round($loyaltyDiscount * $ratio, 2);

                            $remainingPointsToDistribute -= $itemPoints;
                            $remainingDiscountToDistribute -= $itemDiscount;
                        }
                    }

                    $itemRawTotalPrice = round($menuItem->price * $item->quantity, 2);
                    $itemFinalTotalPrice = max(0.00, round($itemRawTotalPrice - $itemDiscount, 2));

                    $createdOrder = Order::create([
                        'customer_id' => $item->user_id, // Contributor gets credit for their item
                        'student_id' => $item->user_id,
                        'user_id' => $item->user_id,
                        'vendor_id' => $session->vendor_id,
                        'menu_item_id' => $menuItem->id,
                        'food_name' => $menuItem->name,
                        'quantity' => $item->quantity,
                        'unit_price' => (float) $menuItem->price,
                        'total_price' => $itemFinalTotalPrice,
                        'order_timestamp' => time() * 1000,
                        'status' => 'PENDING',
                        'pickup_pin' => $securePin,
                        'estimated_pickup_time' => 'Calculating...',
                        'points_redeemed' => $itemPoints,
                        'discount_applied' => $itemDiscount,
                    ]);

                    // Add order_items table log entry
                    try {
                        DB::table('order_items')->insert([
                            'order_id' => $createdOrder->id,
                            'food_item_id' => $menuItem->id,
                            'quantity' => $item->quantity,
                            'price' => $menuItem->price,
                            'created_at' => now(),
                            'updated_at' => now(),
                        ]);
                    } catch (\Exception $e) {
                        // Suppress if table mismatch
                    }

                    // Log audit log
                    AuditLog::create([
                        'user_id' => $item->user_id,
                        'timestamp' => time() * 1000,
                        'action' => 'ORDER_CREATED',
                        'details' => "Placed order #{$createdOrder->id} via group checkout session {$session->code} for '{$menuItem->name}' x {$item->quantity}.",
                    ]);

                    $ordersList[] = $createdOrder;
                }

                // 3. Mark session as completed
                $session->status = 'COMPLETED';
                $session->save();

                return $ordersList;
            });

            return response()->json([
                'success' => true,
                'message' => 'Group order consolidated checkout completed successfully. Meal pickup PIN generated.',
                'session_code' => $session->code,
                'pickup_pin' => $createdOrders[0]->pickup_pin ?? 'N/A',
                'orders_count' => count($createdOrders),
                'total_cost' => round($totalCheckoutCost, 2),
                'loyalty_discount_applied' => round($loyaltyDiscount, 2),
                'final_checkout_paid' => round($finalCheckoutCost, 2),
                'orders' => $createdOrders,
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Group order checkout failed.',
                'error' => $e->getMessage(),
            ], 500);
        }
    }
}
