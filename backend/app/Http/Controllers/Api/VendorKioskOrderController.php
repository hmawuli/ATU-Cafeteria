<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\FoodItem;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Payment;
use App\Models\InventoryMovement;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class VendorKioskOrderController extends Controller
{
    public function store(Request $request)
    {
        $vendor = $request->user();

        if (! $vendor || strtoupper((string) $vendor->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Vendor access required.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'items' => 'required|array|min:1|max:50',
            'items.*.food_item_id' => 'nullable|integer|distinct|exists:food_items,id',
            'items.*.menu_item_id' => 'nullable|integer|distinct|exists:menu_items,id',
            'items.*.quantity' => 'required|integer|min:1|max:50',
            'customer_name' => 'nullable|string|max:160',
            'customer_phone' => 'nullable|string|max:40',
            'payment_method' => 'required|string|in:CASH,MOMO,CARD',
            'payment_reference' => 'nullable|string|max:120',
            'customer_note' => 'nullable|string|max:500',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please review the kiosk sale.',
                'errors' => $validator->errors(),
            ], 422);
        }

        try {
            $result = DB::transaction(function () use ($request, $vendor) {
                $lockedVendor = User::whereKey($vendor->id)->lockForUpdate()->firstOrFail();

                $lines = [];
                $subtotal = 0.0;

                foreach ($request->input('items', []) as $input) {
                    $isMenu = ! empty($input['menu_item_id']);
                    $id = (int) ($input['menu_item_id'] ?? $input['food_item_id']);

                    $item = $isMenu
                        ? MenuItem::whereKey($id)->lockForUpdate()->first()
                        : FoodItem::whereKey($id)->lockForUpdate()->first();

                    if (! $item || (int) $item->vendor_id !== (int) $lockedVendor->id) {
                        throw new \RuntimeException('A selected meal does not belong to this vendor.');
                    }

                    if (! $item->is_available) {
                        throw new \RuntimeException('A selected meal is currently unavailable.');
                    }

                    $quantity = (int) $input['quantity'];
                    if ($item->current_stock !== null && (int) $item->current_stock < $quantity) {
                        throw new \RuntimeException("Only {$item->current_stock} unit(s) remain for '".($item->name ?: $item->food_name)."'.");
                    }

                    $unitPrice = round((float) $item->price, 2);
                    $lineTotal = round($unitPrice * $quantity, 2);
                    $subtotal = round($subtotal + $lineTotal, 2);

                    $lines[] = [
                        'item' => $item,
                        'is_menu' => $isMenu,
                        'quantity' => $quantity,
                        'unit_price' => $unitPrice,
                        'line_total' => $lineTotal,
                    ];
                }

                $order = Order::create([
                    'order_number' => 'KSK-'.now()->format('ymdHis').'-'.strtoupper(Str::random(5)),
                    'order_type' => 'TAKEAWAY',
                    'payment_method' => strtoupper($request->input('payment_method')),
                    'payment_status' => 'PAID',
                    'subtotal' => $subtotal,
                    'discount_amount' => 0,
                    'tax_amount' => 0,
                    'service_fee' => 0,
                    'delivery_fee' => 0,
                    'grand_total' => $subtotal,
                    'currency' => 'GHS',
                    'customer_note' => $request->input('customer_note'),
                    'customer_name' => trim((string) $request->input('customer_name', '')),
                    'customer_phone' => trim((string) $request->input('customer_phone', '')),
                    'sales_channel' => 'KIOSK',
                    'customer_id' => null,
                    'student_id' => null,
                    'user_id' => null,
                    'vendor_id' => $lockedVendor->id,
                    'food_item_id' => count($lines) === 1 && ! $lines[0]['is_menu'] ? $lines[0]['item']->id : null,
                    'menu_item_id' => count($lines) === 1 && $lines[0]['is_menu'] ? $lines[0]['item']->id : null,
                    'food_name' => count($lines) === 1
                        ? ($lines[0]['item']->name ?? $lines[0]['item']->food_name)
                        : count($lines).' items',
                    'quantity' => array_sum(array_column($lines, 'quantity')),
                    'unit_price' => count($lines) === 1 ? $lines[0]['unit_price'] : $subtotal,
                    'total_price' => $subtotal,
                    'order_timestamp' => now()->getTimestampMs(),
                    'placed_at' => now(),
                    'status' => 'COMPLETED',
                    'order_status' => 'COMPLETED',
                    'accepted_at' => now(),
                    'preparing_at' => now(),
                    'ready_at' => now(),
                    'collected_at' => now(),
                    'confirmed_at' => now(),
                    'pickup_pin' => (string) random_int(1000, 9999),
                    'estimated_pickup_time' => 'Collected at counter',
                ]);

                foreach ($lines as $line) {
                    $item = $line['item'];
                    $itemName = $item->name ?? $item->food_name;

                    if ($item->current_stock !== null) {
                        $item->current_stock = max(0, (int) $item->current_stock - $line['quantity']);
                        $item->is_available = $item->current_stock > 0;
                        $item->save();

                        InventoryMovement::create([
                            'vendor_id' => $item->vendor_id,
                            'food_item_id' => $line['is_menu'] ? null : $item->id,
                            'menu_item_id' => $line['is_menu'] ? $item->id : null,
                            'order_id' => $order->id,
                            'type' => 'SALE',
                            'quantity' => -$line['quantity'],
                            'balance_after' => $item->current_stock,
                            'reference' => 'KSK-'.$order->order_number,
                            'reason' => 'Stock consumed by walk-in kiosk sale.',
                            'performed_by' => $lockedVendor->id,
                        ]);
                    }

                    OrderItem::create([
                        'order_id' => $order->id,
                        'food_item_id' => $line['is_menu'] ? null : $item->id,
                        'menu_item_id' => $line['is_menu'] ? $item->id : null,
                        'name' => $itemName ?: 'Meal',
                        'name_snapshot' => $itemName ?: 'Meal',
                        'sku_snapshot' => $item->sku ?? null,
                        'quantity' => $line['quantity'],
                        'unit_price' => $line['unit_price'],
                        'total_price' => $line['line_total'],
                        'discount_amount' => 0,
                        'tax_amount' => 0,
                        'line_total' => $line['line_total'],
                    ]);
                }

                $payment = Payment::create([
                    'order_id' => $order->id,
                    'checkout_session_id' => null,
                    'customer_id' => null,
                    'reference' => trim((string) $request->input('payment_reference'))
                        ?: 'KSK-'.$order->order_number,
                    'gateway' => 'vendor-kiosk',
                    'amount' => $subtotal,
                    'currency' => 'GHS',
                    'purpose' => 'WALK_IN_SALE',
                    'method' => strtoupper($request->input('payment_method')),
                    'status' => 'SUCCESS',
                    'initiated_at' => now(),
                    'paid_at' => now(),
                ]);

                $order->payment_id = $payment->id;
                $order->save();

                return $order->load(['items', 'vendor']);
            }, 3);

            return response()->json([
                'success' => true,
                'message' => 'Walk-in sale recorded successfully.',
                'order' => $result,
                'pickup_pin' => $result->pickup_pin,
            ], 201);
        } catch (\RuntimeException $e) {
            return response()->json([
                'success' => false,
                'message' => $e->getMessage(),
            ], 400);
        } catch (\Throwable $e) {
            report($e);

            return response()->json([
                'success' => false,
                'message' => 'We could not record the kiosk sale.',
            ], 500);
        }
    }
}
