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
     * Get orders placed by a specific student.
     */
    public function getCustomerOrders($customerId)
    {
        $orders = Order::where('customer_id', $customerId)
            ->orderBy('order_timestamp', 'desc')
            ->get();
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
            'food_item_id' => 'required|integer',
            'food_name' => 'required|string',
            'quantity' => 'required|integer|min:1',
            'unit_price' => 'required|numeric',
            'total_price' => 'required|numeric',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input parameters invalid.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Generate a secure, randomized 4-digit pickup PIN code
        $securePin = (string) rand(1000, 9999);

        $order = DB::transaction(function () use ($request, $securePin) {
            $createdOrder = Order::create([
                'customer_id' => $request->input('customer_id'),
                'student_id' => $request->input('student_id'),
                'vendor_id' => $request->input('vendor_id'),
                'food_item_id' => $request->input('food_item_id'),
                'food_name' => $request->input('food_name'),
                'quantity' => $request->input('quantity'),
                'unit_price' => $request->input('unit_price'),
                'total_price' => $request->input('total_price'),
                'order_timestamp' => time() * 1000,
                'status' => 'PENDING',
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
            } elseif ($normalized === 'PENDING') {
                $statusInput = 'PENDING';
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
            'status' => 'required|string|in:PENDING,PREPARING,READY,COMPLETED,DECLINED,CANCELLED',
            'estimated_pickup_time' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Valid status is required.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Student can only cancel PENDING orders
        if ($role === 'STUDENT' && $request->input('status') !== 'CANCELLED') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized status transition. Students can only cancel orders.'
            ], 403);
        }

        if ($role === 'STUDENT' && $order->status !== 'PENDING' && $request->input('status') === 'CANCELLED') {
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

        if (strtoupper($oldStatus) === 'PENDING' && strtoupper($newStatus) === 'COMPLETED') {
            event(new OrderStatusCompleted($updatedOrder));
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
}
