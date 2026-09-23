                    $validator->errors()->add(
                        "items.{$index}",
                        'Each cart item must reference a menu item or food item.'
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

        $user = $request->user();
        if (! $user || ! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Your authenticated session is no longer valid.'], 401);
        }

        try {
            $result = DB::transaction(function () use ($request, $user) {
                // Lock the wallet owner row so two simultaneous checkouts cannot spend the same balance.
                $lockedUser = User::whereKey($user->id)->lockForUpdate()->firstOrFail();
                $pointsToRedeem = (int) $request->input('points_to_redeem', 0);
                $paymentMethod = strtoupper((string) $request->input('payment_method', 'wallet'));
                $orderType = strtoupper((string) $request->input('order_type', 'TAKEAWAY'));
                $paymentReference = trim((string) $request->input('payment_reference', ''));
                $total = 0.0;
                $items = [];

                foreach ($request->input('items') as $index => $input) {
                    $menuItemId = $input['menu_item_id'] ?? null;
                    $foodItemId = $input['food_item_id'] ?? null;

                    if ($menuItemId) {
                        $catalogItem = MenuItem::whereKey((int) $menuItemId)
                            ->lockForUpdate()->first();
                        $isMenuItem = true;
                    } else {
                        $catalogItem = FoodItem::whereKey((int) $foodItemId)
                            ->lockForUpdate()->first();
                        $isMenuItem = false;
                    }

                    if (! $catalogItem) {
                        throw new \RuntimeException("Cart item at index {$index} is no longer available.");
                    }

                    if (! $catalogItem->is_available) {
                        $itemName = $catalogItem->name ?: ($catalogItem->food_name ?? 'This item');
                        throw new \RuntimeException("Menu item '{$itemName}' is currently unavailable.");
                    }

                    $quantity = (int) $input['quantity'];

                    // Standalone menu_items track current stock. The legacy food_items
                    // catalog exposes initial_stock but not a live current_stock field,
                    // so availability is enforced there without decrementing a
                    // non-live stock value.
                    if ($isMenuItem) {
                        $stock = $catalogItem->current_stock;
                        if ($stock !== null && $stock < $quantity) {
                            throw new \RuntimeException("Only {$stock} unit(s) of '{$catalogItem->name}' remain.");
                        }
                    }

                    $unitPrice = round((float) $catalogItem->price, 2);
                    $lineTotal = round($unitPrice * $quantity, 2);
                    $total = round($total + $lineTotal, 2);

                    $items[] = [
                        'catalog_item' => $catalogItem,
                        'quantity' => $quantity,
                        'unitPrice' => $unitPrice,
                        'lineTotal' => $lineTotal,
                        'is_menu_item' => $isMenuItem,
                    ];
                }

                if ($pointsToRedeem > (int) ($lockedUser->loyalty_points ?? 0)) {
                    throw new \RuntimeException('Insufficient loyalty points balance.');
                }

                $discount = round($pointsToRedeem * 0.10, 2);
                $finalTotal = max(0.0, round($total - $discount, 2));

                $verifiedPayment = null;
                if ($paymentMethod !== 'WALLET') {
                    if ($paymentReference === '') {
                        throw new \RuntimeException('A verified online payment reference is required.');
                    }

                    $verifiedPayment = \App\Models\Payment::where('customer_id', $lockedUser->id)
                        ->where('reference', $paymentReference)
                        ->where('purpose', 'DIRECT_ORDER_PAY')
                        ->where('status', 'SUCCESS')
                        ->lockForUpdate()
                        ->first();

                    if (! $verifiedPayment || abs((float) $verifiedPayment->amount - $finalTotal) > 0.01) {
                        throw new \RuntimeException('The online payment could not be verified for this order total.');
                    }
                } else {
                    if ((float) $lockedUser->balance < $finalTotal) {
                        throw new \RuntimeException(
                            'Insufficient wallet balance. You need GH₵ '.number_format($finalTotal, 2).
                            ', but your balance is GH₵'.number_format((float) $lockedUser->balance, 2).'.'
                        );
                    }
                }

                $checkoutPayment = null;
                if ($paymentMethod === 'WALLET') {
                    $checkoutPayment = \App\Models\Payment::create([
                        'customer_id' => $lockedUser->id,
                        'reference' => 'WAL-CART-'.strtoupper(Str::random(14)),
                        'gateway' => 'internal-wallet',
                        'amount' => $finalTotal,
                        'currency' => 'GHS',
                        'purpose' => 'DIRECT_ORDER_PAY',
                        'method' => 'WALLET',
                        'status' => 'SUCCESS',
                        'initiated_at' => now(),
                        'paid_at' => now(),
                    ]);
                } else {
                    $checkoutPayment = $verifiedPayment;
                }

                if ($paymentMethod !== 'WALLET') {
                    if ($paymentReference === '') {
                        throw new \RuntimeException('A verified online payment reference is required.');
                    }
                    $verifiedPayment = AuditLog::where('user_id', $lockedUser->id)
                        ->where('action', 'PAYSTACK_DIRECT_PAY')
                        ->where('details', 'like', '%'.$paymentReference.'%')
                        ->where('details', 'like', 'Cleared GH₵ '.number_format($finalTotal, 2).'%')
                        ->exists();
                    if (! $verifiedPayment) {
                        throw new \RuntimeException('The online payment could not be verified for this order total.');
                    }
                } elseif ((float) $lockedUser->balance < $finalTotal) {
                    throw new \RuntimeException(
                        'Insufficient wallet balance. You need GH₵ '.number_format($finalTotal, 2).
                        ', but your balance is GH₵'.number_format((float) $lockedUser->balance, 2).'.'
                    );
                }

                $pin = (string) random_int(1000, 9999);
                $createdOrders = [];

                foreach ($items as $item) {
                    $ratio = $total > 0 ? $item['lineTotal'] / $total : 0;
                    $itemDiscount = round($discount * $ratio, 2);
                    $catalogItem = $item['catalog_item'];
                    $itemName = $catalogItem->name ?: ($catalogItem->food_name ?? 'Meal');

                    $order = Order::create([
                        'order_number' => 'CAF-'.now()->format('ymdHis').'-'.strtoupper(Str::random(5)),
                        'order_type' => $orderType,
                        'payment_id' => $checkoutPayment->id,
                        'payment_method' => $paymentMethod,
                        'payment_status' => 'PAID',
                        'subtotal' => $item['lineTotal'],
                        'discount_amount' => $itemDiscount,
                        'tax_amount' => 0,
                        'service_fee' => 0,
                        'delivery_fee' => 0,
                        'grand_total' => max(0.0, round($item['lineTotal'] - $itemDiscount, 2)),
                        'currency' => 'GHS',
                        'customer_note' => $request->input('customer_note'),
                        'placed_at' => now(),
                        'customer_id' => $lockedUser->id,
                        'student_id' => $lockedUser->id,
                        'user_id' => $lockedUser->id,
                        'vendor_id' => $catalogItem->vendor_id,
                        'food_item_id' => $item['is_menu_item'] ? null : $catalogItem->id,
                        'menu_item_id' => $item['is_menu_item'] ? $catalogItem->id : null,
                        'food_name' => $itemName,
                        'quantity' => $item['quantity'],
                        'unit_price' => $item['unitPrice'],
                        'total_price' => max(0.0, round($item['lineTotal'] - $itemDiscount, 2)),
                        'order_timestamp' => now()->getTimestampMs(),
                        'status' => 'PENDING',
                        'pickup_pin' => $pin,
                        'estimated_pickup_time' => $request->input('estimated_pickup_time', 'Calculating...'),
                        'points_redeemed' => 0,
                        'discount_applied' => $itemDiscount,
                    ]);

                    if ($item['is_menu_item'] && $catalogItem->current_stock !== null) {
                        $catalogItem->current_stock = max(0, (int) $catalogItem->current_stock - $item['quantity']);
                        $catalogItem->is_available = $catalogItem->current_stock > 0;
                        $catalogItem->save();
                    }

                    \App\Models\OrderItem::create([
                        'order_id' => $order->id,
                        'food_item_id' => $item['is_menu_item'] && ! empty($catalogItem->food_item_id) ? (int) $catalogItem->food_item_id : (! $item['is_menu_item'] ? (int) $catalogItem->id : null),
                        'name' => $itemName,
                        'name_snapshot' => $itemName,
                        'quantity' => $item['quantity'],
                        'unit_price' => $item['unitPrice'],
                        'total_price' => $order->total_price,
                        'discount_amount' => $itemDiscount,
                        'line_total' => $order->total_price,
                    ]);

                    AuditLog::create([
                        'user_id' => $lockedUser->id,
                        'timestamp' => now()->getTimestampMs(),
                        'action' => 'ORDER_CREATED',
                        'details' => "Placed order #{$order->id} for '{$itemName}' x {$item['quantity']}",
                    ]);
                    $createdOrders[] = $order;
                }

                if ($paymentMethod === 'WALLET') {
                    $lockedUser->balance = round((float) $lockedUser->balance - $finalTotal, 2);
                }
                $lockedUser->loyalty_points = (int) ($lockedUser->loyalty_points ?? 0) - $pointsToRedeem;
                $lockedUser->save();

                if ($paymentMethod === 'WALLET') {
                    $beforeBalance = round((float) $lockedUser->balance + $finalTotal, 2);
                    foreach ($createdOrders as $createdOrder) {
                        WalletTransaction::create([
                            'user_id' => $lockedUser->id,