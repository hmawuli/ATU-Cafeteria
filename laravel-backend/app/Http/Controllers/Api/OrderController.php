<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;
use App\Events\OrderStatusCompleted;

class OrderController extends Controller
{
    /**
     * Display all orders (Admin overview).
     */
    public function index()
    {
        $orders = Order::orderBy('order_timestamp', 'desc')->get();
        return response()->json($orders, 200);
    }

    /**
     * Display the specified order details and status.
     */
    public function show($id)
    {
        $order = Order::find($id);
        if (!$order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.'
            ], 404);
        }
        return response()->json($order, 200);
    }

    /**
     * Get orders placed by a specific student.
     * Supports filtering by status or searching by food name/status.
     */
    public function getCustomerOrders(Request $request, $customerId)
    {
        $ordersQuery = Order::where(function ($query) use ($customerId) {
            $query->where('customer_id', $customerId)
                  ->orWhere('student_id', $customerId);
        });

        // 1. Filter by Status
        $status = $request->input('status');
        if ($status && $status !== '') {
            $normalizedStatus = strtoupper(trim($status));
            if ($normalizedStatus === 'READY FOR PICKUP' || $normalizedStatus === 'READY_FOR_PICKUP' || $normalizedStatus === 'READY') {
                $normalizedStatus = 'READY';
            } elseif ($normalizedStatus === 'CANCEL' || $normalizedStatus === 'CANCELED') {
                $normalizedStatus = 'CANCELLED';
            }
            $ordersQuery->where('status', $normalizedStatus);
        }

        // 2. Search filtering (food name, ID, or status)
        $search = $request->input('search');
        if ($search && $search !== '') {
            $ordersQuery->where(function ($query) use ($search) {
                $query->where('food_name', 'like', '%' . $search . '%')
                      ->orWhere('id', 'like', '%' . $search . '%')
                      ->orWhere('status', 'like', '%' . $search . '%');
            });
        }

        $orders = $ordersQuery->orderBy('order_timestamp', 'desc')->get();
        return response()->json($orders, 200);
    }

    /**
     * Get pre-orders received by a specific vendor.
     * Supports filtering by status, date range (start_date, end_date), or student identifier/ID.
     */
    public function getVendorOrders(Request $request, $vendorId)
    {
        $ordersQuery = Order::where('vendor_id', $vendorId);

        // 1. Filter by Status
        $status = $request->input('status');
        if ($status && $status !== '') {
            $normalizedStatus = strtoupper(trim($status));
            if ($normalizedStatus === 'READY FOR PICKUP' || $normalizedStatus === 'READY_FOR_PICKUP' || $normalizedStatus === 'READY') {
                $normalizedStatus = 'READY';
            } elseif ($normalizedStatus === 'CANCEL' || $normalizedStatus === 'CANCELED') {
                $normalizedStatus = 'CANCELLED';
            }
            $ordersQuery->where('status', $normalizedStatus);
        }

        // 2. Filter by Date Range (start_date, end_date)
        $startDate = $request->input('start_date');
        if ($startDate && $startDate !== '') {
            if (is_numeric($startDate)) {
                $startMs = (double) $startDate;
                if ($startMs < 10000000000) {
                    $startMs *= 1000;
                }
            } else {
                $startMs = strtotime($startDate . ' 00:00:00') * 1000;
            }
            if ($startMs) {
                $ordersQuery->where('order_timestamp', '>=', $startMs);
            }
        }

        $endDate = $request->input('end_date');
        if ($endDate && $endDate !== '') {
            if (is_numeric($endDate)) {
                $endMs = (double) $endDate;
                if ($endMs < 10000000001) {
                    $endMs *= 1000;
                }
            } else {
                $endMs = strtotime($endDate . ' 23:59:59') * 1000;
            }
            if ($endMs) {
                $ordersQuery->where('order_timestamp', '<=', $endMs);
            }
        }

        // 3. Filter by Student Identifier (ID, name, username, info)
        $studentIdentifier = $request->input('student_identifier') ?: $request->input('student') ?: $request->input('search') ?: $request->input('student_id');
        if ($studentIdentifier && $studentIdentifier !== '') {
            $ordersQuery->where(function ($query) use ($studentIdentifier) {
                if (is_numeric($studentIdentifier)) {
                    $query->where('customer_id', $studentIdentifier);
                } else {
                    $query->whereHas('customer', function ($q) use ($studentIdentifier) {
                        $q->where('fullName', 'like', '%' . $studentIdentifier . '%')
                          ->orWhere('username', 'like', '%' . $studentIdentifier . '%')
                          ->orWhere('info', 'like', '%' . $studentIdentifier . '%');
                    });
                }
            });
        }

        $orders = $ordersQuery->orderBy('order_timestamp', 'desc')->get();
        return response()->json($orders, 200);
    }

    /**
     * Place a new pre-order order.
     */
    public function store(Request $request)
    {
        // Support either student_id or customer_id
        if ($request->has('student_id') && !$request->has('customer_id')) {
            $request->merge(['customer_id' => $request->input('student_id')]);
        } elseif ($request->has('customer_id') && !$request->has('student_id')) {
            $request->merge(['student_id' => $request->input('customer_id')]);
        }

        $validator = Validator::make($request->all(), [
            'customer_id' => 'required|integer|exists:users,id',
            'student_id' => 'required|integer|exists:users,id',
            'vendor_id' => 'required|integer|exists:users,id',
            'food_item_id' => 'nullable|integer',
            'menu_item_id' => 'required|integer|exists:menu_items,id',
            'food_name' => 'required|string|min:2',
            'quantity' => 'required|integer|min:1',
            'unit_price' => 'required|numeric|min:0.01',
            'total_price' => 'required|numeric|min:0.01',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input parameters invalid or missing.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Fetch menu item to validate availability, ownership, and calculate total price
        $menuItem = \App\Models\MenuItem::find($request->input('menu_item_id'));
        if (!$menuItem) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item does not exist.'
            ], 404);
        }

        if (!$menuItem->is_available) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item is currently unavailable.'
            ], 400);
        }

        // Validate menu item owner matches vendor_id
        if ($menuItem->vendor_id != $request->input('vendor_id')) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item does not belong to the specified vendor.'
            ], 400);
        }

        // Calculate and validate order totals
        $quantity = intval($request->input('quantity'));
        $expectedUnitPrice = round($menuItem->price, 2);
        $expectedTotalPrice = round($expectedUnitPrice * $quantity, 2);
        
        $unitPriceInput = round($request->input('unit_price'), 2);
        $totalPriceInput = round($request->input('total_price'), 2);

        if (abs($expectedUnitPrice - $unitPriceInput) > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error: unit_price does not match the actual menu item price.',
                'expected' => $expectedUnitPrice,
                'received' => $unitPriceInput
            ], 400);
        }

        if (abs($expectedTotalPrice - $totalPriceInput) > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error: total_price is incorrect based on menu item price and quantity.',
                'expected' => $expectedTotalPrice,
                'received' => $totalPriceInput
            ], 400);
        }

        // Generate a secure, randomized 4-digit pickup PIN code
        $securePin = (string) rand(1000, 9999);

        $order = DB::transaction(function () use ($request, $securePin) {
            $createdOrder = Order::create([
                'customer_id' => $request->input('customer_id'),
                'student_id' => $request->input('student_id'),
                'user_id' => $request->input('customer_id'),
                'vendor_id' => $request->input('vendor_id'),
                'food_item_id' => $request->input('food_item_id'),
                'menu_item_id' => $request->input('menu_item_id'),
                'food_name' => $request->input('food_name'),
                'quantity' => $request->input('quantity'),
                'unit_price' => $request->input('unit_price'),
                'total_price' => $request->input('total_price'),
                'order_timestamp' => time() * 1000,
                'status' => 'ORDER_PLACED',
                'pickup_pin' => $securePin,
                'estimated_pickup_time' => 'Calculating...',
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdOrder->customer_id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_CREATED',
                'details' => "Pre-order #{$createdOrder->id} created for '{$createdOrder->food_name}' (QTY: {$createdOrder->quantity}) with PIN code: {$securePin}.",
            ]);

            return $createdOrder;
        });

        // Notify the vendor of the new incoming pre-order
        if ($order->vendor_id) {
            $vendor = \App\Models\User::find($order->vendor_id);
            if ($vendor) {
                try {
                    $vendor->notify(new \App\Notifications\NewIncomingOrderNotification($order));
                } catch (\Exception $e) {
                    \Illuminate\Support\Facades\Log::error("Failed to notify vendor {$order->vendor_id} of new order #{$order->id}: " . $e->getMessage());
                }
            }
        }

        return response()->json($order, 201);
    }

    /**
     * Advancing order statuses (e.g. PENDING -> PREPARING -> READY -> DECLINED -> CANCELLED).
     * Secured with Sanctum and supports both strict enums and user-friendly labels.
     */
    public function updateStatus(Request $request, $id)
    {
        $order = Order::find($id);
        if (!$order) {
            return response()->json([
                'success' => false,
                'message' => 'Pre-order ticket not found.'
            ], 404);
        }

        // Authenticated vendor/admin security check
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN' && $role !== 'STUDENT') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires STUDENT, VENDOR or ADMIN privileges.'
            ], 403);
        }

        if ($role === 'VENDOR' && $order->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.'
            ], 403);
        }

        if ($role === 'STUDENT' && $order->customer_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.'
            ], 403);
        }

        // Preprocess user-friendly labels to strict database statuses
        $statusInput = $request->input('status');
        if ($statusInput) {
            $normalized = strtoupper(trim($statusInput));
            if ($normalized === 'READY FOR PICKUP' || $normalized === 'READY_FOR_PICKUP' || $normalized === 'READY') {
                $statusInput = 'READY';
            } elseif ($normalized === 'PREPARING') {
                $statusInput = 'PREPARING';
            } elseif ($normalized === 'PENDING' || $normalized === 'ORDER PLACED' || $normalized === 'ORDER_PLACED') {
                $statusInput = 'ORDER_PLACED';
            } elseif ($normalized === 'OUT FOR DELIVERY' || $normalized === 'OUT_FOR_DELIVERY') {
                $statusInput = 'OUT_FOR_DELIVERY';
            } elseif ($normalized === 'DELIVERED') {
                $statusInput = 'DELIVERED';
            } elseif ($normalized === 'COMPLETED') {
                $statusInput = 'COMPLETED';
            } elseif ($normalized === 'DECLINED') {
                $statusInput = 'DECLINED';
            } elseif ($normalized === 'CANCELLED' || $normalized === 'CANCEL' || $normalized === 'CANCELED') {
                $statusInput = 'CANCELLED';
            }
            $request->merge(['status' => $statusInput]);
        }

        $validator = Validator::make($request->all(), [
            'status' => 'required|string|in:PENDING,ORDER_PLACED,PREPARING,READY,OUT_FOR_DELIVERY,DELIVERED,COMPLETED,DECLINED,CANCELLED',
            'estimated_pickup_time' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Valid status is required.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Student can only cancel PENDING/ORDER_PLACED orders
        if ($role === 'STUDENT' && $request->input('status') !== 'CANCELLED') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized status transition. Students can only cancel orders.'
            ], 403);
        }

        if ($role === 'STUDENT' && $order->status !== 'PENDING' && $order->status !== 'ORDER_PLACED' && $request->input('status') === 'CANCELLED') {
            return response()->json([
                'success' => false,
                'message' => 'Completed or active orders cannot be cancelled.'
            ], 400);
        }

        $vendorId = $order->vendor_id;
        $oldStatus = $order->status;
        $newStatus = $request->input('status');

        $updatedOrder = DB::transaction(function () use ($order, $request, $vendorId, $oldStatus, $newStatus) {
            $order->status = $newStatus;
            $order->order_status = $newStatus;
            if ($request->has('estimated_pickup_time')) {
                $order->estimated_pickup_time = $request->input('estimated_pickup_time');
            }
            $order->save();

            // Register Audit Log
            AuditLog::create([
                'user_id' => $request->user()->id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_STATUS_CHANGED',
                'details' => "Order #{$order->id} status moved from '{$oldStatus}' to '{$newStatus}' by {$request->user()->fullName} (ETA: {$order->estimated_pickup_time}).",
            ]);

            return $order;
        });

        // Notify the student user of the status change (e.g. preparation, readiness)
        $studentId = $updatedOrder->customer_id ?? $updatedOrder->student_id;
        if ($studentId) {
            $student = \App\Models\User::find($studentId);
            if ($student) {
                try {
                    $student->notify(new \App\Notifications\OrderStatusChangedNotification($updatedOrder, $oldStatus, $newStatus));
                } catch (\Exception $e) {
                    \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} of order status updated to {$newStatus}: " . $e->getMessage());
                }
            }
        }

        if (strtoupper($newStatus) === 'COMPLETED') {
            event(new OrderStatusCompleted($updatedOrder));
        }

        if ((strtoupper($oldStatus) === 'PENDING' || strtoupper($oldStatus) === 'ORDER_PLACED') && strtoupper($newStatus) === 'READY') {
            event(new \App\Events\OrderStatusReady($updatedOrder));
        }

        return response()->json($updatedOrder, 200);
    }

    /**
     * Conclude pre-order custody hand-offs by evaluating user PIN input.
     */
    public function verifyAndCompletePickup(Request $request, $id)
    {
        $order = Order::find($id);
        if (!$order) {
            return response()->json([
                'success' => false,
                'message' => 'Pre-order ticket not found.'
            ], 404);
        }

        $validator = Validator::make($request->all(), [
            'vendor_id' => 'required|integer',
            'pickup_pin' => 'required|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor ID and hand-off PIN are required.'
            ], 400);
        }

        $vendorId = $request->input('vendor_id');
        $inputPin = $request->input('pickup_pin');

        if ($order->vendor_id == $vendorId && $order->pickup_pin === $inputPin) {
            $updatedOrder = DB::transaction(function () use ($order, $vendorId) {
                $order->status = 'COMPLETED';
                $order->save();

                // Register Audit Log
                AuditLog::create([
                    'user_id' => $vendorId,
                    'timestamp' => time() * 1000,
                    'action' => 'PICKUP_VALIDATED',
                    'details' => "Order #{$order->id} verification PIN validated successfully. Released meal hand-off.",
                ]);

                return $order;
            });

            // Notify the student user of the status change to COMPLETED
            $studentId = $updatedOrder->customer_id ?? $updatedOrder->student_id;
            if ($studentId) {
                $student = \App\Models\User::find($studentId);
                if ($student) {
                    try {
                        $student->notify(new \App\Notifications\OrderStatusChangedNotification($updatedOrder, 'READY', 'COMPLETED'));
                    } catch (\Exception $e) {
                        \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} of order completed: " . $e->getMessage());
                    }
                }
            }

            // Dispatch event for order completion
            event(new OrderStatusCompleted($updatedOrder));

            return response()->json([
                'success' => true,
                'message' => 'Pre-order hand-off completed successfully.',
                'order' => $updatedOrder
            ], 200);
        }

        // Register Audit Log on failure
        DB::transaction(function () use ($order, $vendorId, $inputPin) {
            AuditLog::create([
                'user_id' => $vendorId,
                'timestamp' => time() * 1000,
                'action' => 'PICKUP_FAIL',
                'details' => "Arrested bad PIN input: '{$inputPin}' for Pre-order #{$order->id}.",
            ]);
        });

        return response()->json([
            'success' => false,
            'message' => 'Verification PIN mismatch. Access Denied.'
        ], 401);
    }

    /**
     * Get orders placed by the currently authenticated student.
     */
    public function getAuthenticatedStudentOrders(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $orders = Order::where('customer_id', $user->id)
            ->orWhere('student_id', $user->id)
            ->orWhere('user_id', $user->id)
            ->orderBy('order_timestamp', 'desc')
            ->get();

        return response()->json([
            'success' => true,
            'orders' => $orders
        ], 200);
    }

    /**
     * Place a new secure order for the currently authenticated student.
     */
    public function storeAuthenticatedStudentOrder(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $validator = Validator::make($request->all(), [
            'vendor_id' => 'required|integer|exists:users,id',
            'food_item_id' => 'nullable|integer',
            'menu_item_id' => 'required|integer|exists:menu_items,id',
            'food_name' => 'required|string|min:2',
            'quantity' => 'required|integer|min:1',
            'unit_price' => 'required|numeric|min:0.01',
            'total_price' => 'required|numeric|min:0.01',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Fetch menu item to validate availability, ownership, and calculate total price
        $menuItem = \App\Models\MenuItem::find($request->input('menu_item_id'));
        if (!$menuItem) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item does not exist.'
            ], 404);
        }

        if (!$menuItem->is_available) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item is currently unavailable.'
            ], 400);
        }

        // Validate menu item owner matches vendor_id
        if ($menuItem->vendor_id != $request->input('vendor_id')) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item does not belong to the specified vendor.'
            ], 400);
        }

        // Calculate and validate order totals
        $quantity = intval($request->input('quantity'));
        $expectedUnitPrice = round($menuItem->price, 2);
        $expectedTotalPrice = round($expectedUnitPrice * $quantity, 2);
        
        $unitPriceInput = round($request->input('unit_price'), 2);
        $totalPriceInput = round($request->input('total_price'), 2);

        if (abs($expectedUnitPrice - $unitPriceInput) > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error: unit_price does not match the actual menu item price.',
                'expected' => $expectedUnitPrice,
                'received' => $unitPriceInput
            ], 400);
        }

        if (abs($expectedTotalPrice - $totalPriceInput) > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error: total_price is incorrect based on menu item price and quantity.',
                'expected' => $expectedTotalPrice,
                'received' => $totalPriceInput
            ], 400);
        }

        // Verify balance
        if ($user->balance < $totalPriceInput) {
            return response()->json([
                'success' => false,
                'message' => 'Insufficient wallet balance. Please top up your wallet first.'
            ], 400);
        }

        // Generate a random secure 4-digit pickup PIN
        $securePin = (string) rand(1000, 9999);

        try {
            $order = DB::transaction(function () use ($request, $user, $securePin, $totalPriceInput) {
                // Deduct balance from user
                $user->balance = $user->balance - $totalPriceInput;
                $user->save();

                // Create Order
                $createdOrder = Order::create([
                    'customer_id' => $user->id,
                    'student_id' => $user->id,
                    'user_id' => $user->id,
                    'vendor_id' => $request->input('vendor_id'),
                    'food_item_id' => $request->input('food_item_id'),
                    'menu_item_id' => $request->input('menu_item_id'),
                    'food_name' => $request->input('food_name'),
                    'quantity' => $request->input('quantity'),
                    'unit_price' => $request->input('unit_price'),
                    'total_price' => $request->input('total_price'),
                    'order_timestamp' => time() * 1000,
                    'status' => 'PENDING',
                    'pickup_pin' => $securePin,
                    'estimated_pickup_time' => 'Calculating...',
                ]);

                // Create Wallet Transaction
                \App\Models\WalletTransaction::create([
                    'user_id' => $user->id,
                    'type' => 'PAYMENT',
                    'amount' => -$totalPriceInput,
                    'status' => 'SUCCESS',
                    'reference' => 'TXN-ORD-' . uniqid() . '-' . time(),
                    'details' => "Paid for Pre-order #{$createdOrder->id} ('{$createdOrder->food_name}')"
                ]);

                // Register Audit Log
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'ORDER_CREATED',
                    'details' => "Pre-order #{$createdOrder->id} created securely for '{$createdOrder->food_name}' by Student {$user->fullName} with verification PIN: {$securePin}. Wallet debited: GHS {$totalPriceInput}.",
                ]);

                return $createdOrder;
            });

            // Notify vendor
            if ($order->vendor_id) {
                $vendor = \App\Models\User::find($order->vendor_id);
                if ($vendor) {
                    try {
                        $vendor->notify(new \App\Notifications\NewIncomingOrderNotification($order));
                    } catch (\Exception $e) {
                        \Illuminate\Support\Facades\Log::error("Failed to notify vendor {$order->vendor_id} of secure order #{$order->id}: " . $e->getMessage());
                    }
                }
            }

            return response()->json([
                'success' => true,
                'message' => 'Order placed successfully.',
                'order' => $order
            ], 201);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Failed to place order securely on server.',
                'error' => $e->getMessage()
            ], 500);
        }
    }

    /**
     * Cancel an order.
     * Users can only cancel orders if the current status is 'PENDING' or 'ORDER_PLACED'.
     */
    public function cancel(Request $request, $id)
    {
        $order = Order::find($id);
        if (!$order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.'
            ], 404);
        }

        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $role = strtoupper($user->role);
        
        // Security check: Only the owning student, admin, or the assigned vendor can cancel.
        if ($role === 'STUDENT' && $order->customer_id !== $user->id && $order->user_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You can only cancel your own orders.'
            ], 403);
        }

        if ($role === 'VENDOR' && $order->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.'
            ], 403);
        }

        // Validate current order status (only pending / order_placed is cancellable)
        $currentStatus = strtoupper($order->status);
        if ($currentStatus !== 'PENDING' && $currentStatus !== 'ORDER_PLACED') {
            return response()->json([
                'success' => false,
                'message' => 'Orders can only be cancelled if the current status is pending.'
            ], 400);
        }

        // Perform cancellation in transaction
        $updatedOrder = DB::transaction(function () use ($order) {
            $order->status = 'CANCELLED';
            $order->order_status = 'CANCELLED';
            $order->save();

            // Register Audit Log
            AuditLog::create([
                'user_id' => $order->customer_id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_CANCELLED',
                'details' => "Order #{$order->id} was cancelled. Status updated to CANCELLED.",
            ]);

            return $order;
        });

        // Notify matching recipient via system notification triggers
        try {
            $studentId = $order->user_id ?: $order->customer_id;
            $student = User::find($studentId);
            if ($student && $role !== 'STUDENT') {
                $student->notify(new \App\Notifications\OrderStatusChangedNotification($updatedOrder, $currentStatus, 'CANCELLED'));
            }
        } catch (\Exception $e) {
            // Ignore notification fallback errors
        }

        return response()->json([
            'success' => true,
            'message' => 'Order cancelled successfully.',
            'order' => $updatedOrder
        ], 200);
    }

    /**
     * Real-time order status tracking with SSE (Server-Sent Events) and JSON fallback.
     */
    public function trackOrderRealTime(Request $request, $id)
    {
        $order = Order::find($id);
        if (!$order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.'
            ], 404);
        }

        // If client requests text/event-stream or sets stream parameter of 1, stream in real-time
        if ($request->header('Accept') === 'text/event-stream' || $request->input('stream') == 1) {
            return response()->stream(function () use ($id) {
                $lastStatus = '';
                // Periodically check for updates for up to 15 cycles (~30 seconds)
                for ($i = 0; $i < 15; $i++) {
                    $order = Order::find($id);
                    if (!$order) {
                        echo "event: error\n";
                        echo "data: " . json_encode(['message' => 'Order deleted']) . "\n\n";
                        ob_flush();
                        flush();
                        break;
                    }

                    $currentStatus = $order->status;
                    $stages = $this->getTrackingStages($currentStatus);

                    if ($currentStatus !== $lastStatus) {
                        echo "event: status_update\n";
                        echo "data: " . json_encode([
                            'id' => $order->id,
                            'status' => $currentStatus,
                            'estimated_pickup_time' => $order->estimated_pickup_time,
                            'stages' => $stages,
                            'updated_at' => $order->updated_at ? $order->updated_at->toIso8601String() : null
                        ]) . "\n\n";
                        ob_flush();
                        flush();
                        $lastStatus = $currentStatus;
                    }

                    if (in_array(strtoupper($currentStatus), ['DELIVERED', 'COMPLETED', 'CANCELLED', 'DECLINED'])) {
                        break;
                    }

                    sleep(2);
                }
            }, 200, [
                'Content-Type' => 'text/event-stream',
                'Cache-Control' => 'no-cache',
                'Connection' => 'keep-alive',
                'X-Accel-Buffering' => 'no'
            ]);
        }

        // Return direct single JSON snapshot
        return response()->json([
            'success' => true,
            'id' => $order->id,
            'status' => $order->status,
            'estimated_pickup_time' => $order->estimated_pickup_time,
            'stages' => $this->getTrackingStages($order->status)
        ]);
    }

    /**
     * Map order status to dynamic tracking stages.
     */
    private function getTrackingStages($status)
    {
        $statusUpper = strtoupper($status);
        
        $stages = [
            ['name' => 'Received', 'completed' => false, 'active' => false],
            ['name' => 'Preparing', 'completed' => false, 'active' => false],
            ['name' => 'Out for Delivery', 'completed' => false, 'active' => false],
            ['name' => 'Delivered', 'completed' => false, 'active' => false],
        ];

        $index = -1;
        if ($statusUpper === 'PENDING' || $statusUpper === 'ORDER_PLACED' || $statusUpper === 'RECEIVED') {
            $index = 0;
        } elseif ($statusUpper === 'PREPARING') {
            $index = 1;
        } elseif ($statusUpper === 'OUT_FOR_DELIVERY' || $statusUpper === 'READY' || $statusUpper === 'OUT FOR DELIVERY') {
            $index = 2;
        } elseif ($statusUpper === 'DELIVERED' || $statusUpper === 'COMPLETED') {
            $index = 3;
        }

        for ($i = 0; $i < 4; $i++) {
            if ($i < $index) {
                $stages[$i]['completed'] = true;
            } elseif ($i === $index) {
                $stages[$i]['active'] = true;
                $stages[$i]['completed'] = true;
            }
        }

        return $stages;
    }
}
