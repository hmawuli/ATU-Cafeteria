<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;
use App\Events\OrderStatusCompleted;
use App\Http\Resources\OrderResource;

class OrderController extends Controller
{
    /**
     * Display all orders (Admin overview).
     */
    public function index(Request $request)
    {
        $query = Order::with(['customer', 'vendor', 'foodItem', 'menuItem', 'feedback'])->orderBy('order_timestamp', 'desc');
        $user = $request->user();
        if (strtoupper((string)$user->role) === 'STUDENT') $query->where(fn($q) => $q->where('customer_id',$user->id)->orWhere('student_id',$user->id)->orWhere('user_id',$user->id));
        elseif (strtoupper((string)$user->role) === 'VENDOR') $query->where('vendor_id',$user->id);
        elseif (strtoupper((string)$user->role) !== 'ADMIN') abort(403);
        $orders = $query->get();
        return response()->json(OrderResource::collection($orders)->resolve(), 200);
    }

    /**
     * Display the specified order details and status.
     */
    public function show($id)
    {
        $order = Order::with(['customer', 'vendor', 'foodItem', 'menuItem', 'feedback'])->find($id);
        if (!$order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }
        $user = request()->user();
        $role = strtoupper((string)$user->role);
        $allowed = $role === 'ADMIN' || ($role === 'VENDOR' && (int)$order->vendor_id === (int)$user->id) || ($role === 'STUDENT' && in_array((int)$user->id, [(int)$order->customer_id,(int)$order->student_id,(int)$order->user_id], true));
        abort_unless($allowed, 403, 'You are not authorized to view this order.');
        return response()->json(new OrderResource($order), 200);
    }

    /**
     * Generate and download the printable PDF receipt for a completed order.
     */
    public function downloadReceipt($id)
    {
        $order = Order::with(['customer', 'vendor', 'foodItem', 'menuItem'])->find($id);
        if (!$order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }
        $user = request()->user();
        $role = strtoupper((string)$user->role);
        $allowed = $role === 'ADMIN' || ($role === 'VENDOR' && (int)$order->vendor_id === (int)$user->id) || ($role === 'STUDENT' && in_array((int)$user->id, [(int)$order->customer_id,(int)$order->student_id,(int)$order->user_id], true));
        abort_unless($allowed, 403, 'You are not authorized to access this receipt.');

        $pdfWriter = new \App\Services\ReceiptPdfWriter();
        $pdfContent = $pdfWriter->generate($order);

        $fileName = "receipt-order-{$id}.pdf";

        return response($pdfContent, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $fileName . '"',
            'Content-Length' => strlen($pdfContent),
            'Cache-Control' => 'private, max-age=0, must-revalidate',
            'Pragma' => 'public'
        ]);
    }

    /**
     * Get orders placed by a specific student.
     * Supports filtering by status or searching by food name/status.
     */
    public function getCustomerOrders(Request $request, $customerId)
    {
        $user = $request->user();
        $role = strtoupper((string)$user->role);
        if ($role === 'STUDENT' && (int)$user->id !== (int)$customerId) abort(403, 'You may only view your own orders.');
        if (!in_array($role, ['STUDENT','ADMIN'], true)) abort(403, 'You are not authorized to view student orders.');
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
        $user = $request->user();
        $role = strtoupper((string)$user->role);
        if ($role === 'VENDOR' && (int)$user->id !== (int)$vendorId) abort(403, 'You may only view your own vendor orders.');
        if (!in_array($role, ['VENDOR','ADMIN'], true)) abort(403, 'You are not authorized to view vendor orders.');
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
    public function store(\App\Http\Requests\StoreOrderRequest $request)
    {
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
        $securePin = (string) random_int(1000, 9999);

        $order = DB::transaction(function () use ($request, $securePin, $menuItem) {
            if ($menuItem->current_stock !== null) {
                $qty = intval($request->input('quantity'));
                if ($menuItem->current_stock < $qty) {
                    throw new \Exception("Insufficient stock for {$menuItem->food_name}. Only {$menuItem->current_stock} items remaining.");
                }
                $menuItem->current_stock -= $qty;
                $menuItem->save();
            }

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
                'estimated_pickup_time' => $request->input('estimated_pickup_time', 'Calculating...'),
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdOrder->customer_id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_CREATED',
                'details' => "Pre-order #{$createdOrder->id} created for '{$createdOrder->food_name}' (QTY: {$createdOrder->quantity}). Pickup credential issued securely.",
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
     * Patch route to allow vendors to update order status (e.g., 'preparing', 'ready', 'delivered') with validation.
     */
    public function patchStatus(Request $request, $id)
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
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors and administrators can perform this action.'
            ], 403);
        }

        if ($role === 'VENDOR' && $order->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.'
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'status' => 'required|string|max:50',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        $statusInput = strtoupper(trim($request->input('status')));
        
        // Map user/vendor friendly values to DB enum
        if ($statusInput === 'PREPARING') {
            $normalizedStatus = 'PREPARING';
        } elseif ($statusInput === 'READY' || $statusInput === 'READY_FOR_PICKUP' || $statusInput === 'READY FOR PICKUP') {
            $normalizedStatus = 'READY';
        } elseif ($statusInput === 'DELIVERED') {
            $normalizedStatus = 'DELIVERED';
        } elseif ($statusInput === 'OUT_FOR_DELIVERY' || $statusInput === 'OUT FOR DELIVERY') {
            $normalizedStatus = 'OUT_FOR_DELIVERY';
        } elseif ($statusInput === 'COMPLETED') {
            $normalizedStatus = 'COMPLETED';
        } elseif ($statusInput === 'CANCELLED' || $statusInput === 'CANCEL' || $statusInput === 'CANCELED') {
            $normalizedStatus = 'CANCELLED';
        } elseif ($statusInput === 'DECLINED') {
            $normalizedStatus = 'DECLINED';
        } elseif ($statusInput === 'PENDING' || $statusInput === 'ORDER_PLACED' || $statusInput === 'ORDER PLACED') {
            $normalizedStatus = 'ORDER_PLACED';
        } else {
            return response()->json([
                'success' => false,
                'message' => 'Invalid order status. Allowed values: preparing, ready, delivered, out_for_delivery, completed, cancelled, declined, pending.'
            ], 400);
        }

        $oldStatus = $order->status;
        $updatedOrder = DB::transaction(function () use ($order, $user, $normalizedStatus, $oldStatus) {
            $order->status = $normalizedStatus;
            $order->order_status = $normalizedStatus;
            $timestampColumns = [
                'ORDER_PLACED' => null, 'PENDING' => null, 'PREPARING' => 'preparing_at',
                'READY' => 'ready_at', 'COMPLETED' => 'collected_at',
            ];
            if (isset($timestampColumns[$normalizedStatus]) && $timestampColumns[$normalizedStatus]) {
                $order->{$timestampColumns[$normalizedStatus]} = now();
            }
            if ($normalizedStatus === 'PREPARING' && !$order->accepted_at) $order->accepted_at = now();
            $order->save();

            // Register Audit Log
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_STATUS_PATCHED',
                'details' => "Order #{$order->id} status patched from '{$oldStatus}' to '{$normalizedStatus}' by Vendor/Admin {$user->fullName}.",
            ]);

            return $order;
        });

        // Notify the student user of the status change
        $studentId = $updatedOrder->customer_id ?? $updatedOrder->student_id;
        if ($studentId) {
            $student = \App\Models\User::find($studentId);
            if ($student) {
                try {
                    $student->notify(new \App\Notifications\OrderStatusChangedNotification($updatedOrder, $oldStatus, $normalizedStatus));
                    event(new \App\Events\OrderStatusUpdatedBroadcast($updatedOrder, $oldStatus, $normalizedStatus));
                } catch (\Exception $e) {
                    \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} of order status patched to {$normalizedStatus}: " . $e->getMessage());
                }
            }
        }

        if ($normalizedStatus === 'COMPLETED') {
            event(new OrderStatusCompleted($updatedOrder));
        }

        return response()->json([
            'success' => true,
            'message' => 'Order status updated successfully.',
            'order' => $updatedOrder
        ], 200);
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

        // Enforce the order lifecycle on the server; clients cannot skip arbitrary states.
        $allowedTransitions = [
            'PENDING' => ['ORDER_PLACED', 'PREPARING', 'DECLINED', 'CANCELLED'],
            'ORDER_PLACED' => ['PREPARING', 'DECLINED', 'CANCELLED'],
            'PREPARING' => ['READY', 'CANCELLED'],
            'READY' => ['OUT_FOR_DELIVERY', 'COMPLETED'],
            'OUT_FOR_DELIVERY' => ['COMPLETED'],
            'DELIVERED' => ['COMPLETED'],
            'COMPLETED' => [],
            'DECLINED' => [],
            'CANCELLED' => [],
        ];
        if ($role !== 'ADMIN' && !in_array($newStatus, $allowedTransitions[$oldStatus] ?? [], true)) {
            return response()->json([
                'success' => false,
                'message' => "Invalid order transition from {$oldStatus} to {$newStatus}.",
            ], 409);
        }

        $updatedOrder = DB::transaction(function () use ($order, $request, $vendorId, $oldStatus, $newStatus) {
            $order->status = $newStatus;
            $order->order_status = $newStatus;
            if ($newStatus === 'PREPARING') { $order->preparing_at = now(); if (!$order->accepted_at) $order->accepted_at = now(); }
            elseif ($newStatus === 'READY') { $order->ready_at = now(); }
            elseif ($newStatus === 'COMPLETED') { $order->collected_at = now(); }
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
                    // Fire real-time broadcast event to students
                    event(new \App\Events\OrderStatusUpdatedBroadcast($updatedOrder, $oldStatus, $newStatus));
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
     * Bulk update statuses for multiple selected orders.
     * Secured with Sanctum and validated to ensure the authenticated vendor owns the selected orders.
     */
    public function bulkUpdateStatus(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

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
            'order_ids' => 'required|array',
            'order_ids.*' => 'integer|exists:orders,id',
            'status' => 'required|string|in:PENDING,ORDER_PLACED,PREPARING,READY,OUT_FOR_DELIVERY,DELIVERED,COMPLETED,DECLINED,CANCELLED',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Valid status and order IDs are required.',
                'errors' => $validator->errors()
            ], 400);
        }

        $orderIds = $request->input('order_ids');
        $newStatus = $request->input('status');

        $orders = Order::whereIn('id', $orderIds)->get();

        // Security check: ensure vendor owns all selected orders
        if ($role === 'VENDOR') {
            foreach ($orders as $order) {
                if ($order->vendor_id !== $user->id) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Unauthorized. One or more selected orders do not belong to you.'
                    ], 403);
                }
            }
        }

        $updatedOrders = [];
        DB::transaction(function () use ($orders, $request, $newStatus, &$updatedOrders) {
            foreach ($orders as $order) {
                $oldStatus = $order->status;
                $order->status = $newStatus;
                $order->order_status = $newStatus;
                $order->save();

                // Register Audit Log
                \App\Models\AuditLog::create([
                    'user_id' => $request->user()->id,
                    'timestamp' => time() * 1000,
                    'action' => 'ORDER_STATUS_CHANGED',
                    'details' => "Order #{$order->id} status moved from '{$oldStatus}' to '{$newStatus}' via bulk update by {$request->user()->fullName}.",
                ]);

                $updatedOrders[] = $order;

                // Notify student of status change
                $studentId = $order->customer_id ?? $order->student_id;
                if ($studentId) {
                    $student = \App\Models\User::find($studentId);
                    if ($student) {
                        try {
                            $student->notify(new \App\Notifications\OrderStatusChangedNotification($order, $oldStatus, $newStatus));
                            event(new \App\Events\OrderStatusUpdatedBroadcast($order, $oldStatus, $newStatus));
                        } catch (\Exception $e) {
                            \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} in bulk status change: " . $e->getMessage());
                        }
                    }
                }

                if (strtoupper($newStatus) === 'COMPLETED') {
                    event(new OrderStatusCompleted($order));
                }

                if ((strtoupper($oldStatus) === 'PENDING' || strtoupper($oldStatus) === 'ORDER_PLACED') && strtoupper($newStatus) === 'READY') {
                    event(new \App\Events\OrderStatusReady($order));
                }
            }
        });

        return response()->json([
            'success' => true,
            'message' => 'Bulk update completed successfully.',
            'updated_count' => count($updatedOrders),
            'orders' => $updatedOrders
        ], 200);
    }

    /**
     * Conclude pre-order custody hand-offs by evaluating user PIN input.
     */
    public function verifyAndCompletePickup(Request $request, $id)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        if (!in_array(strtoupper($user->role), ['VENDOR', 'ADMIN'], true)) {
            return response()->json(['success' => false, 'message' => 'Only an authorized vendor can verify pickup.'], 403);
        }

        $order = Order::find($id);
        if (!$order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }

        if (strtoupper($user->role) === 'VENDOR' && (int) $order->vendor_id !== (int) $user->id) {
            return response()->json(['success' => false, 'message' => 'You are not authorized to verify this order.'], 403);
        }

        $validator = Validator::make($request->all(), [
            'pickup_pin' => ['required', 'string', 'size:4', 'regex:/^\\d{4}$/'],
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'A valid 4-digit pickup PIN is required.',
                'errors' => $validator->errors(),
            ], 422);
        }

        if (!in_array($order->status, ['READY', 'OUT_FOR_DELIVERY'], true)) {
            return response()->json([
                'success' => false,
                'message' => 'This order is not ready for pickup.',
            ], 409);
        }

        $inputPin = (string) $request->input('pickup_pin');

        // Use a constant-time comparison and never accept a vendor_id supplied by the client.
        if (!hash_equals((string) $order->pickup_pin, $inputPin)) {
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'PICKUP_FAIL',
                'details' => "Invalid pickup PIN attempt for order #{$order->id}.",
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Invalid pickup PIN.',
            ], 401);
        }

        $updatedOrder = DB::transaction(function () use ($order, $user) {
            $locked = Order::whereKey($order->id)->lockForUpdate()->firstOrFail();

            if (!in_array($locked->status, ['READY', 'OUT_FOR_DELIVERY'], true)) {
                throw new \RuntimeException('This order is no longer ready for pickup.');
            }

            $locked->status = 'COMPLETED';
            $locked->order_status = 'COMPLETED';
            $locked->collected_at = now();
            $locked->save();

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'PICKUP_VALIDATED',
                'details' => "Pickup PIN validated successfully for order #{$locked->id}.",
            ]);

            return $locked;
        });

        $studentId = $updatedOrder->customer_id ?? $updatedOrder->student_id;
        if ($studentId) {
            $student = User::find($studentId);
            if ($student) {
                try {
                    $student->notify(new \App\Notifications\OrderStatusChangedNotification(
                        $updatedOrder, 'READY', 'COMPLETED'
                    ));
                    event(new \App\Events\OrderStatusUpdatedBroadcast(
                        $updatedOrder, 'READY', 'COMPLETED'
                    ));
                } catch (\Throwable $e) {
                    Log::error("Failed to notify student {$studentId} after pickup: ".$e->getMessage());
                }
            }
        }

        event(new OrderStatusCompleted($updatedOrder));

        return response()->json([
            'success' => true,
            'message' => 'Pickup verified and order completed successfully.',
            'order' => $updatedOrder,
        ], 200);
    }
    /**
     * Retrieve the authenticated student's personal order history, filtered by date.
     */
    public function getPersonalOrderHistory(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        // Base query for the authenticated user's orders
        $ordersQuery = Order::where(function ($query) use ($user) {
            $query->where('customer_id', $user->id)
                  ->orWhere('student_id', $user->id)
                  ->orWhere('user_id', $user->id);
        });

        // 1. Single Date filter (format: YYYY-MM-DD)
        $date = $request->input('date');
        if ($date && $date !== '') {
            $startTimestamp = strtotime($date . ' 00:00:00') * 1000;
            $endTimestamp = strtotime($date . ' 23:59:59') * 1000;
            if ($startTimestamp && $endTimestamp) {
                $ordersQuery->whereBetween('order_timestamp', [$startTimestamp, $endTimestamp]);
            }
        }

        // 2. Date Range filter (format: YYYY-MM-DD or milliseconds)
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

        // Order by order_timestamp descending
        $orders = $ordersQuery->orderBy('order_timestamp', 'desc')->get();

        return response()->json([
            'success' => true,
            'orders' => $orders
        ], 200);
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

        // Verify loyalty points redemption if requested
        $pointsToRedeem = intval($request->input('points_to_redeem', 0));
        $discount = 0.00;
        if ($pointsToRedeem > 0) {
            if (($user->loyalty_points ?? 0) < $pointsToRedeem) {
                return response()->json([
                    'success' => false,
                    'message' => 'Insufficient loyalty points balance. You have ' . ($user->loyalty_points ?? 0) . ' points.'
                ], 400);
            }
            // 10 points = 1.00 GHS discount
            $discount = round($pointsToRedeem * 0.10, 2);
        }

        $finalPrice = max(0.00, round($totalPriceInput - $discount, 2));

        // Verify balance
        if ($user->balance < $finalPrice) {
            return response()->json([
                'success' => false,
                'message' => 'Insufficient wallet balance. Please top up your wallet first.'
            ], 400);
        }

        // Generate a random secure 4-digit pickup PIN
        $securePin = (string) random_int(1000, 9999);

        try {
            $order = DB::transaction(function () use ($request, $user, $securePin, $totalPriceInput, $pointsToRedeem, $discount, $finalPrice, $menuItem) {
                // Check and decrement stock
                if ($menuItem->current_stock !== null) {
                    $qty = intval($request->input('quantity'));
                    if ($menuItem->current_stock < $qty) {
                        throw new \Exception("Insufficient stock for {$menuItem->food_name}. Only {$menuItem->current_stock} items remaining.");
                    }
                    $menuItem->current_stock -= $qty;
                    $menuItem->save();
                }

                // Deduct balance and loyalty points from user
                $user->balance = $user->balance - $finalPrice;
                if ($pointsToRedeem > 0) {
                    $user->loyalty_points = ($user->loyalty_points ?? 0) - $pointsToRedeem;
                }
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
                    'total_price' => $finalPrice,
                    'order_timestamp' => time() * 1000,
                    'status' => 'PENDING',
                    'pickup_pin' => $securePin,
                    'estimated_pickup_time' => $request->input('estimated_pickup_time', 'Calculating...'),
                    'points_redeemed' => $pointsToRedeem,
                    'discount_applied' => $discount,
                ]);

                // Create Wallet Transaction
                \App\Models\WalletTransaction::create([
                    'user_id' => $user->id,
                    'type' => 'PAYMENT',
                    'amount' => -$finalPrice,
                    'status' => 'SUCCESS',
                    'reference' => 'TXN-ORD-' . uniqid() . '-' . time(),
                    'details' => "Paid for Pre-order #{$createdOrder->id} ('{$createdOrder->food_name}')" . ($discount > 0 ? " with GHS " . number_format($discount, 2) . " loyalty discount" : "")
                ]);

                // Register Audit Log
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'ORDER_CREATED',
                    'details' => "Pre-order #{$createdOrder->id} created securely for '{$createdOrder->food_name}' by Student {$user->fullName} with verification PIN: {$securePin}. Wallet debited: GHS {$finalPrice}." . ($discount > 0 ? " Redeemed {$pointsToRedeem} loyalty points for GHS {$discount} discount." : ""),
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

        // Validate current order status (only pending / order_placed / ordered is cancellable)
        $currentStatus = strtoupper($order->status);
        $allowedCancelStatuses = ['PENDING', 'ORDER_PLACED', 'ORDERED'];
        if ($role === 'STUDENT' && !in_array($currentStatus, $allowedCancelStatuses)) {
            return response()->json([
                'success' => false,
                'message' => 'Students can only cancel orders if the current status is still pending or ordered.'
            ], 400);
        }

        if (!in_array($currentStatus, $allowedCancelStatuses)) {
            return response()->json([
                'success' => false,
                'message' => 'Orders can only be cancelled if the current status is pending or ordered.'
            ], 400);
        }

        // Perform cancellation in transaction
        $updatedOrder = DB::transaction(function () use ($order) {
            $order->status = 'CANCELLED';
            $order->order_status = 'CANCELLED';
            $order->save();

            // Restore stock if tracked
            if ($order->menu_item_id) {
                $menuItem = \App\Models\MenuItem::find($order->menu_item_id);
                if ($menuItem && $menuItem->current_stock !== null) {
                    $menuItem->current_stock += intval($order->quantity);
                    $menuItem->save();
                }
            }

            // Find student to refund
            $studentId = $order->customer_id ?? ($order->student_id ?? $order->user_id);
            $student = User::find($studentId);
            if ($student) {
                $student->balance = $student->balance + floatval($order->total_price);
                $student->save();

                // Create Refund Transaction
                \App\Models\WalletTransaction::create([
                    'user_id' => $student->id,
                    'type' => 'REFUND',
                    'amount' => floatval($order->total_price),
                    'status' => 'SUCCESS',
                    'reference' => 'TXN-REF-' . uniqid() . '-' . time(),
                    'details' => "Refund for cancelled Order #{$order->id} ('{$order->food_name}')"
                ]);
            }

            // Register Audit Log
            AuditLog::create([
                'user_id' => $order->customer_id ?? ($order->student_id ?? $order->user_id),
                'timestamp' => time() * 1000,
                'action' => 'ORDER_CANCELLED',
                'details' => "Order #{$order->id} was cancelled. Status updated to CANCELLED. Amount of GHS " . number_format($order->total_price, 2) . " was refunded to Student balance.",
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

    /**
     * Polling mechanism for students to fetch real-time updates and push alerts for READY orders.
     */
    public function pollOrderStatusReady(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $studentId = $user->id;
        
        // Find any active orders that are in 'READY' status
        $readyOrders = Order::where(function ($query) use ($studentId) {
                $query->where('customer_id', $studentId)
                      ->orWhere('student_id', $studentId)
                      ->orWhere('user_id', $studentId);
            })
            ->where(DB::raw('upper(status)'), 'READY')
            ->get();

        $alerts = [];
        foreach ($readyOrders as $order) {
            $alerts[] = [
                'order_id' => $order->id,
                'food_name' => $order->food_name,
                'vendor_id' => $order->vendor_id,
                'pickup_pin' => $order->pickup_pin,
                'title' => 'Order Ready for Pickup! 🍽️',
                'body' => "Your order #{$order->id} ('{$order->food_name}') is ready at the cafeteria. Hand-off PIN is {$order->pickup_pin}.",
                'alert_push' => true,
                'vibrate' => [100, 50, 100],
            ];
        }

        return response()->json([
            'success' => true,
            'student_id' => $studentId,
            'has_ready_orders' => count($alerts) > 0,
            'ready_alerts' => $alerts,
            'active_orders_count' => Order::where(function ($query) use ($studentId) {
                $query->where('customer_id', $studentId)
                      ->orWhere('student_id', $studentId)
                      ->orWhere('user_id', $studentId);
            })->whereNotIn(DB::raw('upper(status)'), ['COMPLETED', 'DELIVERED', 'CANCELLED', 'DECLINED'])->count(),
            'polled_at' => date('c')
        ], 200);
    }

    /**
     * Real-time SSE listener stream specifically alerting when active order statuses change to 'READY'.
     */
    public function streamOrderStatusReady(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $studentId = $user->id;

        return response()->stream(function () use ($studentId) {
            $notifiedOrders = []; // Track already emitted ready order IDs in this session
            
            // Loop for up to 20 cycles (approx 40 seconds) to maintain live stream connection
            for ($cycle = 0; $cycle < 20; $cycle++) {
                $readyOrders = Order::where(function ($query) use ($studentId) {
                        $query->where('customer_id', $studentId)
                              ->orWhere('student_id', $studentId)
                              ->orWhere('user_id', $studentId);
                    })
                    ->where(DB::raw('upper(status)'), 'READY')
                    ->get();

                foreach ($readyOrders as $order) {
                    if (!in_array($order->id, $notifiedOrders)) {
                        echo "event: order_ready_push\n";
                        echo "data: " . json_encode([
                            'order_id' => $order->id,
                            'food_name' => $order->food_name,
                            'pickup_pin' => $order->pickup_pin,
                            'message' => "Your order #{$order->id} ('{$order->food_name}') is ready for pickup!",
                            'timestamp' => date('c')
                        ]) . "\n\n";
                        ob_flush();
                        flush();
                        $notifiedOrders[] = $order->id;
                    }
                }

                // Heartbeat to keep connection alive
                echo "event: heartbeat\n";
                echo "data: " . json_encode(['status' => 'listening', 'cycle' => $cycle]) . "\n\n";
                ob_flush();
                flush();

                sleep(2);
            }
        }, 200, [
            'Content-Type' => 'text/event-stream',
            'Cache-Control' => 'no-cache',
            'Connection' => 'keep-alive',
            'X-Accel-Buffering' => 'no'
        ]);
    }

    /**
     * Submit multiple order requests (shopping cart checkout) for the authenticated user.
     */
    public function cartCheckout(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'items' => 'required|array|min:1|max:50',
            'items.*.menu_item_id' => 'required|integer|distinct|exists:menu_items,id',
            'items.*.quantity' => 'required|integer|min:1|max:50',
            'points_to_redeem' => 'nullable|integer|min:0|max:100000',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please review your cart and try again.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $user = $request->user();
        if (!$user || !$user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Your authenticated session is no longer valid.'], 401);
        }

        try {
            $result = DB::transaction(function () use ($request, $user) {
                // Lock the wallet owner row so two simultaneous checkouts cannot spend the same balance.
                $lockedUser = User::whereKey($user->id)->lockForUpdate()->firstOrFail();
                $pointsToRedeem = (int) $request->input('points_to_redeem', 0);
                $total = 0.0;
                $items = [];

                foreach ($request->input('items') as $index => $input) {
                    $menuItem = AppModelsMenuItem::whereKey((int) $input['menu_item_id'])
                        ->lockForUpdate()->first();

                    if (!$menuItem) {
                        throw new \RuntimeException("Menu item at index {$index} is no longer available.");
                    }
                    if (!$menuItem->is_available) {
                        throw new \RuntimeException("Menu item '{$menuItem->name}' is currently unavailable.");
                    }

                    $quantity = (int) $input['quantity'];
                    $stock = $menuItem->current_stock;
                    if ($stock !== null && $stock < $quantity) {
                        throw new \RuntimeException("Only {$stock} unit(s) of '{$menuItem->name}' remain.");
                    }

                    $unitPrice = round((float) $menuItem->price, 2);
                    $lineTotal = round($unitPrice * $quantity, 2);
                    $total = round($total + $lineTotal, 2);
                    $items[] = compact('menuItem', 'quantity', 'unitPrice', 'lineTotal');
                }

                if ($pointsToRedeem > (int) ($lockedUser->loyalty_points ?? 0)) {
                    throw new \RuntimeException('Insufficient loyalty points balance.');
                }

                $discount = round($pointsToRedeem * 0.10, 2);
                $finalTotal = max(0.0, round($total - $discount, 2));

                if ((float) $lockedUser->balance < $finalTotal) {
                    throw new \RuntimeException(
                        'Insufficient wallet balance. You need GH₵ '.number_format($finalTotal, 2).
                        ', but your balance is GH₵ '.number_format((float) $lockedUser->balance, 2).'.'
                    );
                }

                $pin = (string) random_int(1000, 9999);
                $createdOrders = [];

                foreach ($items as $item) {
                    $ratio = $total > 0 ? $item['lineTotal'] / $total : 0;
                    $itemDiscount = round($discount * $ratio, 2);

                    $order = Order::create([
                        'customer_id' => $lockedUser->id,
                        'student_id' => $lockedUser->id,
                        'user_id' => $lockedUser->id,
                        'vendor_id' => $item['menuItem']->vendor_id,
                        'menu_item_id' => $item['menuItem']->id,
                        'food_name' => $item['menuItem']->name,
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

                    if ($item['menuItem']->current_stock !== null) {
                        $item['menuItem']->decrement('current_stock', $item['quantity']);
                        if (($item['menuItem']->current_stock - $item['quantity']) <= 0) {
                            $item['menuItem']->update(['is_available' => false]);
                        }
                    }

                    AuditLog::create([
                        'user_id' => $lockedUser->id,
                        'action' => 'ORDER_CREATED',
                        'details' => "Placed order #{$order->id} for '{$item['menuItem']->name}' x {$item['quantity']}",
                    ]);
                    $createdOrders[] = $order;
                }

                $lockedUser->balance = round((float) $lockedUser->balance - $finalTotal, 2);
                $lockedUser->loyalty_points = (int) ($lockedUser->loyalty_points ?? 0) - $pointsToRedeem;
                $lockedUser->save();

                WalletTransaction::create([
                    'user_id' => $lockedUser->id,
                    'type' => 'PAYMENT',
                    'amount' => -$finalTotal,
                    'status' => 'SUCCESS',
                    'reference' => 'CART-ORD-'.Str::upper(Str::random(20)),
                    'details' => 'Cafeteria cart checkout payment.',
                ]);

                return [
                    'orders' => $createdOrders,
                    'total_cost' => $total,
                    'discount' => $discount,
                    'final_total' => $finalTotal,
                    'remaining_balance' => (float) $lockedUser->balance,
                    'pickup_pin' => $pin,
                ];
            }, 3);

            return response()->json([
                'success' => true,
                'message' => 'Order placed successfully.',
                'orders' => $result['orders'],
                'total_cost' => $result['total_cost'],
                'discount' => $result['discount'],
                'final_total' => $result['final_total'],
                'remaining_balance' => $result['remaining_balance'],
                'pickup_pin' => $result['pickup_pin'],
            ]);
        } catch (\RuntimeException $e) {
            return response()->json(['success' => false, 'message' => $e->getMessage()], 400);
        } catch (\Throwable $e) {
            report($e);
            return response()->json([
                'success' => false,
                'message' => 'We could not complete your order. No payment was taken. Please try again.',
            ], 500);
        }
    }
    /**
     * Remove the specified order from storage (Admin only).
     */
    public function destroy(Request $request, $id)
    {
        $order = Order::find($id);
        if (!$order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.'
            ], 404);
        }

        $user = $request->user();
        if (!$user || strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only administrative personnel can delete orders.'
            ], 403);
        }

        DB::transaction(function () use ($order, $user) {
            $order->delete(); // Soft delete

            \App\Models\AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_DELETED',
                'details' => "Administrator deleted order #{$order->id} ('{$order->food_name}').",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Order deleted successfully.'
        ], 200);
    }

    /**
     * Calculates and returns the estimated wait time for a vendor's queue.
     */
    public function getVendorWaitTime(Request $request, $vendorId = null)
    {
        // If not specified in path, check query param or authenticated user
        if (!$vendorId) {
            if ($request->has('vendor_id')) {
                $vendorId = (int)$request->input('vendor_id');
            } else {
                $user = $request->user();
                if ($user && (strtoupper($user->role) === 'VENDOR' || strtoupper($user->role) === 'ADMIN')) {
                    $vendorId = $user->id;
                } else {
                    return response()->json([
                        'success' => false,
                        'message' => 'Vendor ID is required.'
                    ], 400);
                }
            }
        }

        $vendor = \App\Models\User::find($vendorId);
        if (!$vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor not found.'
            ], 444);
        }

        // Fetch all preparing or pending orders for this vendor
        $activeOrders = Order::where('vendor_id', $vendorId)
            ->whereIn(DB::raw('upper(status)'), ['PREPARING', 'PENDING', 'ORDER_PLACED', 'ORDERED'])
            ->get();

        $preparingCount = 0;
        $pendingCount = 0;
        $totalMinutes = 0;

        foreach ($activeOrders as $order) {
            $status = strtoupper($order->status);
            $qty = intval($order->quantity ?: 1);

            if ($status === 'PREPARING') {
                $preparingCount++;
                // Base 5 mins for preparing + 2 mins per extra item
                $totalMinutes += 5 + (($qty - 1) * 2);
            } else {
                $pendingCount++;
                // Base 3 mins for pending/placed + 1 min per extra item
                $totalMinutes += 3 + (($qty - 1) * 1.5);
            }
        }

        // Concurrency factor (vendors usually have multiple stoves or prepare 2 orders in parallel)
        $concurrencyFactor = 2; // parallel processing capacity
        $estimatedMinutes = $totalMinutes > 0 ? ceil($totalMinutes / $concurrencyFactor) : 0;

        // Add a base buffer of 3 minutes if there are any active orders
        if ($estimatedMinutes > 0) {
            $estimatedMinutes += 3; // buffer time
        } else {
            $estimatedMinutes = 3; // minimum wait time (instant fulfillment prep)
        }

        return response()->json([
            'success' => true,
            'vendor_id' => (int)$vendorId,
            'vendor_name' => $vendor->fullName,
            'queue_metrics' => [
                'total_active_orders' => count($activeOrders),
                'preparing_orders_count' => $preparingCount,
                'pending_orders_count' => $pendingCount,
                'total_queue_quantity' => (int)$activeOrders->sum('quantity'),
            ],
            'estimated_wait_time_minutes' => (int)$estimatedMinutes,
            'formatted_wait_time' => "{$estimatedMinutes} mins",
            'congestion_level' => $preparingCount >= 8 ? 'CRITICAL' : ($preparingCount >= 4 ? 'HIGH' : ($preparingCount >= 1 ? 'MODERATE' : 'LOW')),
            'calculated_at' => date('c')
        ], 200);
    }
}
