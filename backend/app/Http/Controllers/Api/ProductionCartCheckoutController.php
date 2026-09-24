<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\CheckoutSession;
use App\Models\FoodItem;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\InventoryMovement;
use App\Models\PaymentAllocation;
use App\Models\Promotion;
use App\Models\PromotionRedemption;
use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class ProductionCartCheckoutController extends Controller
{
    public function preview(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'items' => 'required|array|min:1|max:50',
            'items.*.menu_item_id' => 'nullable|integer|distinct|exists:menu_items,id',
            'items.*.food_item_id' => 'nullable|integer|distinct|exists:food_items,id',
            'items.*.quantity' => 'required|integer|min:1|max:50',
            'points_to_redeem' => 'nullable|integer|min:0|max:100000',
            'promotion_code' => 'nullable|string|max:50|alpha_dash',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please review your cart.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $user = $request->user();
        if (! $user || ! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Your session is no longer valid.'], 401);
        }

        try {
            $points = (int) $request->input('points_to_redeem', 0);
            $promotionCode = strtoupper(trim((string) $request->input('promotion_code', '')));
            $subtotal = 0.0;
            $vendorIds = [];

            foreach ($request->input('items', []) as $input) {
                $isMenu = ! empty($input['menu_item_id']);
                $id = (int) ($input['menu_item_id'] ?? $input['food_item_id']);
                $item = $isMenu
                    ? MenuItem::find($id)
                    : FoodItem::find($id);

                if (! $item || ! $item->is_available) {
                    throw new \RuntimeException('One of the selected meals is no longer available.');
                }

                $qty = (int) $input['quantity'];
                if ($item->current_stock !== null && (int) $item->current_stock < $qty) {
                    throw new RuntimeException("Only {$item->current_stock} unit(s) remain for '".($item->name ?: $item->food_name)."'.");
                }

                $vendorIds[] = (int) $item->vendor_id;
                $subtotal = round($subtotal + ((float) $item->price * $qty), 2);
            }

            if ($points > (int) ($user->loyalty_points ?? 0)) {
                throw new RuntimeException('Insufficient loyalty points balance.');
            }

            $discount = 0.0;
            $promotion = null;

            if ($promotionCode !== '') {
                if ($points > 0) {
                    throw new RuntimeException('Use either loyalty points or a promotion code, not both.');
                }

                $promotion = Promotion::whereRaw('UPPER(code) = ?', [$promotionCode])->first();

                if (! $promotion || ! $promotion->isCurrentlyActive()) {
                    throw new RuntimeException('This promotion is not active or has expired.');
                }

                $uniqueVendors = array_values(array_unique($vendorIds));
                if ($promotion->vendor_id !== null &&
                    (count($uniqueVendors) !== 1 || (int) $promotion->vendor_id !== (int) $uniqueVendors[0])) {
                    throw new RuntimeException('This promotion applies only to that vendor’s menu.');
                }

                if ($subtotal < (float) $promotion->minimum_order_amount) {
                    throw new RuntimeException(
                        'This promotion requires a minimum order of GH₵ '.
                        number_format((float) $promotion->minimum_order_amount, 2).'.'
                    );
                }

                $discount = strtoupper((string) $promotion->type) === 'PERCENTAGE'
                    ? round($subtotal * ((float) $promotion->value / 100), 2)
                    : round((float) $promotion->value, 2);

                if ($promotion->maximum_discount_amount !== null) {
                    $discount = min($discount, (float) $promotion->maximum_discount_amount);
                }

                $discount = min($discount, $subtotal);
            } else {
                $discount = min($subtotal, round($points * 0.10, 2));
            }

            return response()->json([
                'success' => true,
                'subtotal' => round($subtotal, 2),
                'discount' => round($discount, 2),
                'final_total' => max(0, round($subtotal - $discount, 2)),
                'currency' => 'GHS',
                'promotion_code' => $promotion?->code,
            ]);
        } catch (\RuntimeException $e) {
            return response()->json(['success' => false, 'message' => $e->getMessage()], 400);
        }
    }

    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'items' => 'required|array|min:1|max:50',
            'items.*.menu_item_id' => 'nullable|integer|distinct|exists:menu_items,id',
            'items.*.food_item_id' => 'nullable|integer|distinct|exists:food_items,id',
            'items.*.quantity' => 'required|integer|min:1|max:50',
            'points_to_redeem' => 'nullable|integer|min:0|max:100000',
            'payment_method' => 'nullable|string|in:wallet,momo,card,WALLET,MOMO,CARD',
            'payment_reference' => 'nullable|string|max:120',
            'promotion_code' => 'nullable|string|max:50|alpha_dash',
            'order_type' => 'nullable|string|in:TAKEAWAY,PICKUP,DINE_IN,DELIVERY',
            'customer_note' => 'nullable|string|max:1000',
        ]);

        $validator->after(function ($validator) use ($request) {
            foreach ((array) $request->input('items', []) as $index => $item) {
                if (! ($item['menu_item_id'] ?? null) && ! ($item['food_item_id'] ?? null)) {
                    $validator->errors()->add(
                        "items.$index",
                        'Each cart item must reference a menu item or food item.'
                    );
                }

                if (($item['menu_item_id'] ?? null) && ($item['food_item_id'] ?? null)) {
                    $validator->errors()->add(
                        "items.$index",
                        'A cart line may reference only one catalogue item.'
                    );
                }
            }
        });

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please review your cart and try again.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $customer = $request->user();
        if (! $customer || ! $customer->isActive()) {
            return response()->json([
                'success' => false,
                'message' => 'Your authenticated session is no longer valid.',
            ], 401);
        }

        try {
            $result = DB::transaction(function () use ($request, $customer) {
                $user = User::whereKey($customer->id)->lockForUpdate()->firstOrFail();

                $paymentMethod = strtoupper((string) $request->input('payment_method', 'WALLET'));
                $orderType = strtoupper((string) $request->input('order_type', 'TAKEAWAY'));
                $points = (int) $request->input('points_to_redeem', 0);
                $paymentReference = trim((string) $request->input('payment_reference', ''));
                $promotionCode = strtoupper(trim((string) $request->input('promotion_code', '')));

                $lines = [];
                $subtotal = 0.0;
                $vendorIds = [];

                foreach ($request->input('items', []) as $input) {
                    $isMenu = ! empty($input['menu_item_id']);
                    $id = (int) ($input['menu_item_id'] ?? $input['food_item_id']);
                    $item = $isMenu
                        ? MenuItem::whereKey($id)->lockForUpdate()->first()
                        : FoodItem::whereKey($id)->lockForUpdate()->first();

                    if (! $item || ! $item->is_available) {
                        throw new \RuntimeException('One of the selected meals is no longer available.');
                    }

                    if ($item->current_stock !== null && (int) $item->current_stock < (int) $input['quantity']) {
                        throw new \RuntimeException(
                            "Only {$item->current_stock} unit(s) remain for '".($item->name ?: $item->food_name)."'."
                        );
                    }

                    $quantity = (int) $input['quantity'];
                    $unitPrice = round((float) $item->price, 2);
                    $lineTotal = round($unitPrice * $quantity, 2);

                    $vendorIds[] = (int) $item->vendor_id;
                    $subtotal = round($subtotal + $lineTotal, 2);

                    $lines[] = [
                        'item' => $item,
                        'is_menu' => $isMenu,
                        'quantity' => $quantity,
                        'unit_price' => $unitPrice,
                        'line_total' => $lineTotal,
                    ];
                }

                if ($points > 0 && $points > (int) ($user->loyalty_points ?? 0)) {
                    throw new \RuntimeException('Insufficient loyalty points balance.');
                }

                $promotion = null;
                $promotionDiscount = 0.0;

                if ($promotionCode !== '') {
                    if ($points > 0) {
                        throw new \RuntimeException('Use either loyalty points or a promotion code, not both.');
                    }

                    $promotion = Promotion::whereRaw('UPPER(code) = ?', [$promotionCode])
                        ->lockForUpdate()
                        ->first();

                    if (! $promotion || ! $promotion->isCurrentlyActive()) {
                        throw new \RuntimeException('This promotion is not active or has expired.');
                    }

                    $cartVendorIds = array_values(array_unique($vendorIds));
                    if ($promotion->vendor_id !== null &&
                        (count($cartVendorIds) !== 1 || (int) $promotion->vendor_id !== (int) $cartVendorIds[0])) {
                        throw new \RuntimeException('This promotion applies only to that vendor’s menu.');
                    }

                    if ($subtotal < (float) $promotion->minimum_order_amount) {
                        throw new \RuntimeException(
                            'This promotion requires a minimum order of GH₵ '.
                            number_format((float) $promotion->minimum_order_amount, 2).'.'
                        );
                    }

                    if ($promotion->usage_limit !== null &&
                        PromotionRedemption::where('promotion_id', $promotion->id)->count() >= $promotion->usage_limit) {
                        throw new \RuntimeException('This promotion has reached its usage limit.');
                    }

                    if ($promotion->per_customer_limit !== null &&
                        PromotionRedemption::where('promotion_id', $promotion->id)
                            ->where('customer_id', $user->id)
                            ->count() >= $promotion->per_customer_limit) {
                        throw new \RuntimeException('You have reached this promotion’s usage limit.');
                    }

                    $promotionDiscount = strtoupper((string) $promotion->type) === 'PERCENTAGE'
                        ? round($subtotal * ((float) $promotion->value / 100), 2)
                        : round((float) $promotion->value, 2);

                    if ($promotion->maximum_discount_amount !== null) {
                        $promotionDiscount = min(
                            $promotionDiscount,
                            (float) $promotion->maximum_discount_amount
                        );
                    }

                    $promotionDiscount = min($promotionDiscount, $subtotal);
                }

                $discount = round(
                    $promotion
                        ? $promotionDiscount
                        : ($points * 0.10),
                    2
                );
                $grandTotal = max(0, round($subtotal - $discount, 2));

                $session = CheckoutSession::create([
                    'customer_id' => $user->id,
                    'reference' => 'CHK-'.now()->format('ymdHis').'-'.strtoupper(Str::random(8)),
                    'total_amount' => $grandTotal,
                    'currency' => 'GHS',
                    'payment_method' => $paymentMethod,
                    'status' => 'PAID',
                ]);

                if ($paymentMethod === 'WALLET') {
                    if ((float) $user->balance < $grandTotal) {
                        throw new \RuntimeException('Insufficient wallet balance.');
                    }

                    $payment = \App\Models\Payment::create([
                        'order_id' => null,
                        'checkout_session_id' => $session->id,
                        'customer_id' => $user->id,
                        'reference' => 'WAL-CHECKOUT-'.strtoupper(Str::random(14)),
                        'gateway' => 'internal-wallet',
                        'amount' => $grandTotal,
                        'currency' => 'GHS',
                        'purpose' => 'DIRECT_ORDER_PAY',
                        'method' => 'WALLET',
                        'status' => 'SUCCESS',
                        'initiated_at' => now(),
                        'paid_at' => now(),
                    ]);
                } else {
                    if ($paymentReference === '') {
                        throw new \RuntimeException('A verified online payment reference is required.');
                    }

                    $payment = \App\Models\Payment::where('customer_id', $user->id)
                        ->where('reference', $paymentReference)
                        ->where('purpose', 'DIRECT_ORDER_PAY')
                        ->where('status', 'SUCCESS')
                        ->lockForUpdate()
                        ->first();

                    if (! $payment || abs((float) $payment->amount - $grandTotal) > 0.01) {
                        throw new \RuntimeException('The online payment could not be verified for this order total.');
                    }

                    if ($payment->checkout_session_id !== null) {
                        throw new \RuntimeException('This payment has already been attached to a checkout.');
                    }

                    $payment->checkout_session_id = $session->id;
                    $payment->save();
                }

                $groups = [];
                foreach ($lines as $line) {
                    $vendorId = (int) $line['item']->vendor_id;
                    $groups[$vendorId][] = $line;
                }

                $createdOrders = [];
                $remainingDiscount = $discount;
                $remainingSubtotal = $subtotal;
                $remainingPoints = $points;
                $balanceBefore = round((float) $user->balance, 2);

                foreach ($groups as $vendorId => $vendorLines) {
                    $vendorSubtotal = round(array_sum(array_column($vendorLines, 'line_total')), 2);
                    $isLastVendor = $vendorId === array_key_last($groups);
                    $vendorPoints = $points > 0
                        ? ($isLastVendor
                            ? $remainingPoints
                            : (int) floor($remainingPoints * ($vendorSubtotal / max(0.01, $remainingSubtotal))))
                        : 0;
                    $vendorPoints = min($vendorPoints, $remainingPoints);

                    $vendorDiscount = $points > 0
                        ? round($vendorPoints * 0.10, 2)
                        : ($isLastVendor
                            ? $remainingDiscount
                            : ($remainingSubtotal > 0
                                ? round($discount * ($vendorSubtotal / $remainingSubtotal), 2)
                                : 0.0));

                    $vendorDiscount = min($vendorDiscount, $vendorSubtotal);
                    $vendorGrandTotal = max(0, round($vendorSubtotal - $vendorDiscount, 2));
                    if ($points === 0) {
                        $remainingDiscount = max(0, round($remainingDiscount - $vendorDiscount, 2));
                    }
                    $remainingPoints = max(0, $remainingPoints - $vendorPoints);
                    $remainingSubtotal = max(0, round($remainingSubtotal - $vendorSubtotal, 2));

                    $order = Order::create([
                        'order_number' => 'CAF-'.now()->format('ymdHis').'-'.strtoupper(Str::random(5)),
                        'checkout_session_id' => $session->id,
                        'order_type' => $orderType,
                        'payment_id' => $payment->id,
                        'payment_method' => $paymentMethod,
                        'payment_status' => 'PAID',
                        'subtotal' => $vendorSubtotal,
                        'discount_amount' => $vendorDiscount,
                        'tax_amount' => 0,
                        'service_fee' => 0,
                        'delivery_fee' => 0,
                        'grand_total' => $vendorGrandTotal,
                        'currency' => 'GHS',
                        'customer_note' => $request->input('customer_note'),
                        'customer_id' => $user->id,
                        'student_id' => $user->id,
                        'user_id' => $user->id,
                        'vendor_id' => $vendorId,
                        'food_item_id' => null,
                        'menu_item_id' => null,
                        'food_name' => count($vendorLines) === 1
                            ? (($vendorLines[0]['item']->name ?? $vendorLines[0]['item']->food_name) ?: 'Meal')
                            : count($vendorLines).' items',
                        'quantity' => array_sum(array_column($vendorLines, 'quantity')),
                        'unit_price' => $vendorSubtotal,
                        'total_price' => $vendorGrandTotal,
                        'order_timestamp' => now()->getTimestampMs(),
                        'placed_at' => now(),
                        'status' => 'PENDING',
                        'pickup_pin' => (string) random_int(1000, 9999),
                        'estimated_pickup_time' => $request->input('estimated_pickup_time', 'Calculating...'),
                        'points_redeemed' => $vendorPoints,
                        'discount_applied' => $vendorDiscount,
                    ]);

                    $remainingVendorDiscount = $vendorDiscount;
                    $remainingVendorSubtotal = $vendorSubtotal;

                    foreach ($vendorLines as $lineIndex => $line) {
                        $item = $line['item'];
                        $itemName = ($item->name ?? $item->food_name) ?: 'Meal';
                        $isLastLine = $lineIndex === array_key_last($vendorLines);
                        $lineDiscount = $isLastLine
                            ? $remainingVendorDiscount
                            : ($remainingVendorSubtotal > 0
                                ? round($vendorDiscount * ($line['line_total'] / $vendorSubtotal), 2)
                                : 0.0);
                        $lineDiscount = min($lineDiscount, $line['line_total']);
                        $remainingVendorDiscount = max(0, round($remainingVendorDiscount - $lineDiscount, 2));
                        $remainingVendorSubtotal = max(0, round($remainingVendorSubtotal - $line['line_total'], 2));

                        if ($item->current_stock !== null) {
                            $item->current_stock = max(0, (int) $item->current_stock - $line['quantity']);
                            $item->is_available = $item->current_stock > 0;
                            $item->save();

                            if (! $line['is_menu']) {
                                InventoryMovement::create([
                                    'vendor_id' => $item->vendor_id,
                                    'food_item_id' => $item->id,
                                    'menu_item_id' => null,
                                    'order_id' => $order->id,
                                    'type' => 'SALE',
                                    'quantity' => -$line['quantity'],
                                    'balance_after' => $item->current_stock,
                                    'reference' => 'ORD-'.$order->order_number,
                                    'reason' => 'Stock consumed by customer checkout.',
                                    'performed_by' => $user->id,
                                ]);
                            }
                        }

                        OrderItem::create([
                            'order_id' => $order->id,
                            'food_item_id' => $line['is_menu'] ? null : $item->id,
                            'menu_item_id' => $line['is_menu'] ? $item->id : null,
                            'name' => $itemName,
                            'name_snapshot' => $itemName,
                            'sku_snapshot' => $item->sku ?? null,
                            'quantity' => $line['quantity'],
                            'unit_price' => $line['unit_price'],
                            'total_price' => max(0, round($line['line_total'] - $lineDiscount, 2)),
                            'discount_amount' => $lineDiscount,
                            'tax_amount' => 0,
                            'line_total' => max(0, round($line['line_total'] - $lineDiscount, 2)),
                ]);

                    }

                    if ($paymentMethod === 'WALLET') {
                        $newBalance = round((float) $user->balance - $vendorGrandTotal, 2);
                        WalletTransaction::create([
                            'user_id' => $user->id,
                            'order_id' => $order->id,
                            'payment_id' => $payment->id,
                            'type' => 'PAYMENT',
                            'amount' => -$vendorGrandTotal,
                            'status' => 'SUCCESS',
                            'source' => 'ORDER',
                            'performed_by' => $user->id,
                            'balance_before' => $balanceBefore,
                            'balance_after' => $newBalance,
                            'reference' => 'CHK-'.strtoupper(Str::random(14)),
                            'details' => 'Wallet payment for checkout '.$session->reference.' / '.$order->order_number,
                        ]);
                        $user->balance = $newBalance;
                        $balanceBefore = $newBalance;
                    }

                    PaymentAllocation::create([
                        'payment_id' => $payment->id,
                        'order_id' => $order->id,
                        'amount' => $vendorGrandTotal,
                        'refunded_amount' => 0,
                    ]);

                    $createdOrders[] = $order->load(['items', 'vendor']);
                }

                if ($promotion) {
                    PromotionRedemption::create([
                        'promotion_id' => $promotion->id,
                        'customer_id' => $user->id,
                        'order_id' => $createdOrders[0]->id ?? null,
                        'checkout_session_id' => $session->id,
                        'discount_amount' => $discount,
                    ]);
                }

                $user->loyalty_points = max(0, (int) ($user->loyalty_points ?? 0) - $points);
                // Lifetime spend is finalized only when each order reaches
                // COMPLETED, preventing cancelled/failed orders from being
                // counted and avoiding double-counting split checkouts.
                $user->save();

                $session->status = 'COMPLETED';
                $session->save();

                return [
                    'session' => $session,
                    'payment' => $payment,
                    'orders' => $createdOrders,
                    'subtotal' => $subtotal,
                    'discount' => $discount,
                    'grand_total' => $grandTotal,
                    'remaining_balance' => (float) $user->balance,
                ];
            }, 3);

            return response()->json([
                'success' => true,
                'message' => count($result['orders']) > 1
                    ? 'Checkout completed. Separate pickup orders were created for each vendor.'
                    : 'Order placed successfully.',
                'checkout_session' => [
                    'id' => $result['session']->id,
                    'reference' => $result['session']->reference,
                    'total_amount' => (float) $result['session']->total_amount,
                    'status' => $result['session']->status,
                ],
                'payment_reference' => $result['payment']->reference,
                'orders' => $result['orders'],
                'subtotal' => $result['subtotal'],
                'discount' => $result['discount'],
                'final_total' => $result['grand_total'],
                'remaining_balance' => $result['remaining_balance'],
            ]);
        } catch (\RuntimeException $e) {
            return response()->json([
                'success' => false,
                'message' => $e->getMessage(),
            ], 400);
        } catch (\Throwable $e) {
            report($e);

            return response()->json([
                'success' => false,
                'message' => 'We could not complete your checkout. No order was committed.',
            ], 500);
        }
    }
}
