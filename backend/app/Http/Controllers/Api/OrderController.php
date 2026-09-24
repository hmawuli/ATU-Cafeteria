<?php

namespace App\Http\Controllers\Api;

use App\Events\OrderStatusCompleted;
use App\Events\OrderStatusReady;
use App\Events\OrderStatusUpdatedBroadcast;
use App\Http\Controllers\Controller;
use App\Http\Requests\StoreOrderRequest;
use App\Http\Resources\OrderResource;
use App\Models\AuditLog;
use App\Models\DeliveredOrderReview;
use App\Models\FoodItem;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\OrderItem;
use App\Models\Payment;
use App\Models\PaymentAllocation;
use App\Models\PromotionRedemption;
use App\Models\InventoryMovement;
use App\Models\User;
use App\Models\WalletTransaction;
use App\Notifications\NewIncomingOrderNotification;
use App\Notifications\OrderStatusChangedNotification;
use App\Services\ReceiptPdfWriter;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class OrderController extends Controller
{
    /**
     * Display all orders (Admin overview).
     */
    public function index(Request $request)
    {
        $query = Order::with(['customer', 'vendor', 'foodItem', 'menuItem', 'feedback'])->orderBy('order_timestamp', 'desc');
        $user = $request->user();
        if (strtoupper((string) $user->role) === 'STUDENT') {
            $query->where(fn ($q) => $q->where('customer_id', $user->id)->orWhere('student_id', $user->id)->orWhere('user_id', $user->id));
        } elseif (strtoupper((string) $user->role) === 'VENDOR') {
            $query->where('vendor_id', $user->id);
        } elseif (strtoupper((string) $user->role) !== 'ADMIN') {
            abort(403);
        }
        $orders = $query->get();

        return response()->json(OrderResource::collection($orders)->resolve(), 200);
    }

    /**
     * Display the specified order details and status.
     */
    public function show($id)
    {
        $order = Order::with(['customer', 'vendor', 'foodItem', 'menuItem', 'feedback'])->find($id);
        if (! $order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }
        $user = request()->user();
        $role = strtoupper((string) $user->role);
        $allowed = $role === 'ADMIN' || ($role === 'VENDOR' && (int) $order->vendor_id === (int) $user->id) || ($role === 'STUDENT' && in_array((int) $user->id, [(int) $order->customer_id, (int) $order->student_id, (int) $order->user_id], true));
        abort_unless($allowed, 403, 'You are not authorized to view this order.');

        return response()->json(new OrderResource($order), 200);
    }

    /**
     * Generate and download the printable PDF receipt for a completed order.
     */
    public function downloadReceipt($id)
    {
        $order = Order::with(['customer', 'vendor', 'foodItem', 'menuItem'])->find($id);
        if (! $order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }
        $user = request()->user();
        $role = strtoupper((string) $user->role);
        $allowed = $role === 'ADMIN' || ($role === 'VENDOR' && (int) $order->vendor_id === (int) $user->id) || ($role === 'STUDENT' && in_array((int) $user->id, [(int) $order->customer_id, (int) $order->student_id, (int) $order->user_id], true));
        abort_unless($allowed, 403, 'You are not authorized to access this receipt.');

        $pdfWriter = new ReceiptPdfWriter;
        $pdfContent = $pdfWriter->generate($order);

        $fileName = "receipt-order-{$id}.pdf";

        return response($pdfContent, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="'.$fileName.'"',
            'Content-Length' => strlen($pdfContent),
            'Cache-Control' => 'private, max-age=0, must-revalidate',
            'Pragma' => 'public',
        ]);
    }

    /**
     * Get orders placed by a specific student.
     * Supports filtering by status or searching by food name/status.
     */
    public function getPurchasedVendors(Request $request)
    {
        $user = $request->user();

        $orders = Order::where(function ($query) use ($user) {
            $query->where('customer_id', $user->id)
                ->orWhere('student_id', $user->id);
        })->whereNotNull('vendor_id')->orderByDesc('order_timestamp')->get();

        $vendorIds = $orders->pluck('vendor_id')->unique()->values();
        $reviewedOrderIds = DeliveredOrderReview::where('student_id', $user->id)
            ->whereIn('order_id', $orders->pluck('id'))
            ->pluck('order_id')
            ->all();

        $vendors = User::whereIn('id', $vendorIds)->get()->keyBy('id');

        $result = $vendorIds->map(function ($vendorId) use ($orders, $vendors, $reviewedOrderIds) {
            $vendorOrders = $orders->where('vendor_id', $vendorId);
            $latest = $vendorOrders->first();
            $vendor = $vendors->get($vendorId);
            $hasCompletedOrder = $vendorOrders->contains(function ($order) {
                return in_array(strtoupper((string) ($order->status ?? $order->order_status)), ['DELIVERED', 'COMPLETED'], true);
            });
            $hasUnreviewedCompletedOrder = $vendorOrders->contains(function ($order) use ($reviewedOrderIds) {
                $status = strtoupper((string) ($order->status ?? $order->order_status));

                return in_array($status, ['DELIVERED', 'COMPLETED'], true)
                    && ! in_array($order->id, $reviewedOrderIds, true);
            });

            return [
                'vendor_id' => (int) $vendorId,
                'name' => $vendor?->fullName ?: 'Campus Vendor',
                'store_name' => $vendor?->info ?: 'Campus Food Vendor',
                'order_count' => $vendorOrders->count(),
                'latest_order_id' => $latest?->id,
                'latest_status' => $latest?->status ?? $latest?->order_status ?? 'PENDING',
                'has_reviewed' => $vendorOrders->contains(function ($order) use ($reviewedOrderIds) {
                    return in_array($order->id, $reviewedOrderIds, true);
                }),
                'review_order_id' => $hasUnreviewedCompletedOrder
                    ? $vendorOrders->first(function ($order) use ($reviewedOrderIds) {
                        $status = strtoupper((string) ($order->status ?? $order->order_status));

                        return in_array($status, ['DELIVERED', 'COMPLETED'], true)
                            && ! in_array($order->id, $reviewedOrderIds, true);
                    })?->id
                    : null,
                'can_review' => $hasCompletedOrder && $hasUnreviewedCompletedOrder,
            ];
        })->values();

        return response()->json([
            'success' => true,
            'vendors' => $result,
        ], 200);
    }

    public function getCustomerOrders(Request $request, $customerId)
    {
        $user = $request->user();
        $role = strtoupper((string) $user->role);
        if ($role === 'STUDENT' && (int) $user->id !== (int) $customerId) {
            abort(403, 'You may only view your own orders.');
        }
        if (! in_array($role, ['STUDENT', 'ADMIN'], true)) {
            abort(403, 'You are not authorized to view student orders.');
        }
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
                $query->where('food_name', 'like', '%'.$search.'%')
                    ->orWhere('id', 'like', '%'.$search.'%')
                    ->orWhere('status', 'like', '%'.$search.'%');
            });
        }

        $orders = $ordersQuery->with(['items', 'customer'])->orderBy('order_timestamp', 'desc')->get();

        return response()->json($orders, 200);
    }

    /**
     * Get pre-orders received by a specific vendor.
     * Supports filtering by status, date range (start_date, end_date), or student identifier/ID.
     */
    public function getVendorOrders(Request $request, $vendorId = null)
    {
        $user = $request->user();
        if ($vendorId === null && $user && strtoupper((string) $user->role) === 'VENDOR') {
            $vendorId = $user->id;
        }
        if ($vendorId === null) {
            return response()->json(['success' => false, 'message' => 'Vendor ID is required.'], 400);
        }
        $role = strtoupper((string) $user->role);
        if ($role === 'VENDOR' && (int) $user->id !== (int) $vendorId) {
            abort(403, 'You may only view your own vendor orders.');
        }
        if (! in_array($role, ['VENDOR', 'ADMIN'], true)) {
            abort(403, 'You are not authorized to view vendor orders.');
        }
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
                $startMs = (float) $startDate;
                if ($startMs < 10000000000) {
                    $startMs *= 1000;
                }
            } else {
                $startMs = strtotime($startDate.' 00:00:00') * 1000;
            }
            if ($startMs) {
                $ordersQuery->where('order_timestamp', '>=', $startMs);
            }
        }

        $endDate = $request->input('end_date');
        if ($endDate && $endDate !== '') {
            if (is_numeric($endDate)) {
                $endMs = (float) $endDate;
                if ($endMs < 10000000001) {
                    $endMs *= 1000;
                }
            } else {
                $endMs = strtotime($endDate.' 23:59:59') * 1000;
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
                        $q->where('fullName', 'like', '%'.$studentIdentifier.'%')
                            ->orWhere('username', 'like', '%'.$studentIdentifier.'%')
                            ->orWhere('info', 'like', '%'.$studentIdentifier.'%');
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
    public function store(StoreOrderRequest $request)
    {
        // Fetch menu item to validate availability, ownership, and calculate total price
        $menuItem = MenuItem::find($request->input('menu_item_id'));
        if (! $menuItem) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item does not exist.',
            ], 404);
        }

        if (! $menuItem->is_available) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item is currently unavailable.',
            ], 400);
        }

        // Validate menu item owner matches vendor_id
        if ($menuItem->vendor_id != $request->input('vendor_id')) {
            return response()->json([
                'success' => false,
                'message' => 'The selected menu item does not belong to the specified vendor.',
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
                'received' => $unitPriceInput,
            ], 400);
        }

        if (abs($expectedTotalPrice - $totalPriceInput) > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error: total_price is incorrect based on menu item price and quantity.',
                'expected' => $expectedTotalPrice,
                'received' => $totalPriceInput,
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
            $vendor = User::find($order->vendor_id);
            if ($vendor) {
                try {
                    $vendor->notify(new NewIncomingOrderNotification($order));
                } catch (\Exception $e) {
                    \Illuminate\Support\Facades\Log::error("Failed to notify vendor {$order->vendor_id} of new order #{$order->id}: ".$e->getMessage());
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
        if (! $order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.',
            ], 404);
        }

        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors and administrators can perform this action.',
            ], 403);
        }

        if ($role === 'VENDOR' && $order->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'status' => 'required|string|max:50',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation failed.',
                'errors' => $validator->errors(),
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
                'message' => 'Invalid order status. Allowed values: preparing, ready, delivered, out_for_delivery, completed, cancelled, declined, pending.',
            ], 400);
        }

        $oldStatus = strtoupper((string) $order->status);
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
        if ($role !== 'ADMIN' && ! in_array($normalizedStatus, $allowedTransitions[$oldStatus] ?? [], true)) {
            return response()->json([
                'success' => false,
                'message' => "Invalid order transition from {$oldStatus} to {$normalizedStatus}.",
            ], 409);
        }
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
            if ($normalizedStatus === 'PREPARING' && ! $order->accepted_at) {
                $order->accepted_at = now();
            }
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
            $student = User::find($studentId);
            if ($student) {
                try {
                    $student->notify(new OrderStatusChangedNotification($updatedOrder, $oldStatus, $normalizedStatus));
                    event(new OrderStatusUpdatedBroadcast($updatedOrder, $oldStatus, $normalizedStatus));
                } catch (\Exception $e) {
                    \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} of order status patched to {$normalizedStatus}: ".$e->getMessage());
                }
            }
        }

        if ($normalizedStatus === 'COMPLETED') {
            event(new OrderStatusCompleted($updatedOrder));
        }

        // Never expose the pickup PIN to vendors; it is the student's secret.
        if ($role === 'VENDOR') {
            $updatedOrder->makeHidden('pickup_pin');
        }

        return response()->json([
            'success' => true,
            'message' => 'Order status updated successfully.',
            'order' => $updatedOrder,
        ], 200);
    }

    /**
     * Advancing order statuses (e.g. PENDING -> PREPARING -> READY -> DECLINED -> CANCELLED).
     * Secured with Sanctum and supports both strict enums and user-friendly labels.
     */
    public function updateStatus(Request $request, $id)
    {
        $order = Order::find($id);
        if (! $order) {
            return response()->json([
                'success' => false,
                'message' => 'Pre-order ticket not found.',
            ], 404);
        }

        // Authenticated vendor/admin security check
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN' && $role !== 'STUDENT') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires STUDENT, VENDOR or ADMIN privileges.',
            ], 403);
        }

        if ($role === 'VENDOR' && $order->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.',
            ], 403);
        }

        if ($role === 'STUDENT' && $order->customer_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this order.',
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
                'errors' => $validator->errors(),
            ], 400);
        }

        // Student can only cancel PENDING/ORDER_PLACED orders
        if ($role === 'STUDENT' && $request->input('status') !== 'CANCELLED') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized status transition. Students can only cancel orders.',
            ], 403);
        }

        if ($role === 'STUDENT' && $order->status !== 'PENDING' && $order->status !== 'ORDER_PLACED' && $request->input('status') === 'CANCELLED') {
            return response()->json([
                'success' => false,
                'message' => 'Completed or active orders cannot be cancelled.',
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
        if ($role !== 'ADMIN' && ! in_array($newStatus, $allowedTransitions[$oldStatus] ?? [], true)) {
            return response()->json([
                'success' => false,
                'message' => "Invalid order transition from {$oldStatus} to {$newStatus}.",
            ], 409);
        }

        $updatedOrder = DB::transaction(function () use ($order, $request, $oldStatus, $newStatus) {
            $order->status = $newStatus;
            $order->order_status = $newStatus;
            if ($newStatus === 'PREPARING') {
                $order->preparing_at = now();
                if (! $order->accepted_at) {
                    $order->accepted_at = now();
                }
            } elseif ($newStatus === 'READY') {
                $order->ready_at = now();
            } elseif ($newStatus === 'COMPLETED') {
                $order->collected_at = now();
            }
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
            $student = User::find($studentId);
            if ($student) {
                try {
                    $student->notify(new OrderStatusChangedNotification($updatedOrder, $oldStatus, $newStatus));
                    // Fire real-time broadcast event to students
                    event(new OrderStatusUpdatedBroadcast($updatedOrder, $oldStatus, $newStatus));
                } catch (\Exception $e) {
                    \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} of order status updated to {$newStatus}: ".$e->getMessage());
                }
            }
        }

        if (strtoupper($newStatus) === 'COMPLETED') {
            event(new OrderStatusCompleted($updatedOrder));
        }

        if ((strtoupper($oldStatus) === 'PENDING' || strtoupper($oldStatus) === 'ORDER_PLACED') && strtoupper($newStatus) === 'READY') {
            event(new OrderStatusReady($updatedOrder));
        }

        // The pickup PIN is the student's secret; vendors must verify it with the
        // student at the counter rather than reading it from the API response.
        if ($role === 'VENDOR') {
            $updatedOrder->makeHidden('pickup_pin');
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
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.',
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
                'errors' => $validator->errors(),
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
                        'message' => 'Unauthorized. One or more selected orders do not belong to you.',
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
                AuditLog::create([
                    'user_id' => $request->user()->id,
                    'timestamp' => time() * 1000,
                    'action' => 'ORDER_STATUS_CHANGED',
                    'details' => "Order #{$order->id} status moved from '{$oldStatus}' to '{$newStatus}' via bulk update by {$request->user()->fullName}.",
                ]);

                $updatedOrders[] = $order;

                // Notify student of status change
                $studentId = $order->customer_id ?? $order->student_id;
                if ($studentId) {
                    $student = User::find($studentId);
                    if ($student) {
                        try {
                            $student->notify(new OrderStatusChangedNotification($order, $oldStatus, $newStatus));
                            event(new OrderStatusUpdatedBroadcast($order, $oldStatus, $newStatus));
                        } catch (\Exception $e) {
                            \Illuminate\Support\Facades\Log::error("Failed to notify student {$studentId} in bulk status change: ".$e->getMessage());
                        }
                    }
                }

                if (strtoupper($newStatus) === 'COMPLETED') {
                    event(new OrderStatusCompleted($order));
                }

                if ((strtoupper($oldStatus) === 'PENDING' || strtoupper($oldStatus) === 'ORDER_PLACED') && strtoupper($newStatus) === 'READY') {
                    event(new OrderStatusReady($order));
                }
            }
        });

        // Keep the pickup PIN off vendor-facing responses.
        if ($role === 'VENDOR') {
            foreach ($updatedOrders as $updatedOrder) {
                $updatedOrder->makeHidden('pickup_pin');
            }
        }

        return response()->json([
            'success' => true,
            'message' => 'Bulk update completed successfully.',
            'updated_count' => count($updatedOrders),
            'orders' => $updatedOrders,
        ], 200);
    }

    /**
     * Conclude pre-order custody hand-offs by evaluating user PIN input.
     */
    public function verifyAndCompletePickup(Request $request, $id)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        if (! in_array(strtoupper($user->role), ['VENDOR', 'ADMIN'], true)) {
            return response()->json(['success' => false, 'message' => 'Only an authorized vendor can verify pickup.'], 403);
        }

        $order = Order::find($id);
        if (! $order) {
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

        if (! in_array($order->status, ['READY', 'OUT_FOR_DELIVERY'], true)) {
            return response()->json([
                'success' => false,
                'message' => 'This order is not ready for pickup.',
            ], 409);
        }

        $inputPin = (string) $request->input('pickup_pin');

        // Use a constant-time comparison and never accept a vendor_id supplied by the client.
        if (! hash_equals((string) $order->pickup_pin, $inputPin)) {
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

            if (! in_array($locked->status, ['READY', 'OUT_FOR_DELIVERY'], true)) {
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
                    $student->notify(new OrderStatusChangedNotification(
                        $updatedOrder, 'READY', 'COMPLETED'
                    ));
                    event(new OrderStatusUpdatedBroadcast(
                        $updatedOrder, 'READY', 'COMPLETED'
                    ));
                } catch (\Throwable $e) {
                    Log::error("Failed to notify student {$studentId} after pickup: ".$e->getMessage());
                }
            }
        }

        event(new OrderStatusCompleted($updatedOrder));

        if (strtoupper($user->role) === 'VENDOR') {
            $updatedOrder->makeHidden('pickup_pin');
        }

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
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
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
            $startTimestamp = strtotime($date.' 00:00:00') * 1000;
            $endTimestamp = strtotime($date.' 23:59:59') * 1000;
            if ($startTimestamp && $endTimestamp) {
                $ordersQuery->whereBetween('order_timestamp', [$startTimestamp, $endTimestamp]);
            }
        }

        // 2. Date Range filter (format: YYYY-MM-DD or milliseconds)
        $startDate = $request->input('start_date');
        if ($startDate && $startDate !== '') {
            if (is_numeric($startDate)) {
                $startMs = (float) $startDate;
                if ($startMs < 10000000000) {
                    $startMs *= 1000;
                }
            } else {
                $startMs = strtotime($startDate.' 00:00:00') * 1000;
            }
            if ($startMs) {
                $ordersQuery->where('order_timestamp', '>=', $startMs);
            }
        }

        $endDate = $request->input('end_date');
        if ($endDate && $endDate !== '') {
            if (is_numeric($endDate)) {
                $endMs = (float) $endDate;
                if ($endMs < 10000000001) {
                    $endMs *= 1000;
                }
            } else {
                $endMs = strtotime($endDate.' 23:59:59') * 1000;
            }
            if ($endMs) {
                $ordersQuery->where('order_timestamp', '<=', $endMs);
            }
        }

        // Order by order_timestamp descending
        $orders = $ordersQuery->orderBy('order_timestamp', 'desc')->get();

        return response()->json([
            'success' => true,
            'orders' => $orders,
        ], 200);
    }

    /**
     * Get orders placed by the currently authenticated student.
     */
    public function getAuthenticatedStudentOrders(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $orders = Order::with(['items', 'vendor'])
            ->where(function ($query) use ($user) {
                $query->where('customer_id', $user->id)
                    ->orWhere('student_id', $user->id)
                    ->orWhere('user_id', $user->id);
            })
            ->orderBy('order_timestamp', 'desc')
            ->get();

        return response()->json([
            'success' => true,
            'orders' => $orders,
        ], 200);
    }

    /**
     * Place a new secure order for the currently authenticated student.
     */
    /**
     * Legacy compatibility endpoint for the old single-item customer flow.
     * It deliberately delegates to the canonical production cart checkout so
     * pricing, promotion, loyalty, inventory, payments and idempotency use
     * exactly one business implementation.
     */
    public function storeAuthenticatedStudentOrder(Request $request)
    {
        $user = $request->user();
        if (! $user || ! $user->isActive()) {
            return response()->json([
                'success' => false,
                'message' => 'Your authenticated session is no longer valid.',
            ], 401);
        }

        $validator = Validator::make($request->all(), [
            'vendor_id' => 'nullable|integer',
            'food_item_id' => 'nullable|integer|exists:food_items,id',
            'menu_item_id' => 'nullable|integer|exists:menu_items,id',
            'food_name' => 'nullable|string|min:2',
            'quantity' => 'required|integer|min:1|max:50',
            'unit_price' => 'nullable|numeric|min:0.01',
            'total_price' => 'nullable|numeric|min:0.01',
            'points_to_redeem' => 'nullable|integer|min:0|max:100000',
            'order_type' => 'nullable|string|in:TAKEAWAY,PICKUP,DINE_IN,DELIVERY',
            'customer_note' => 'nullable|string|max:1000',
            'promotion_code' => 'nullable|string|max:50|alpha_dash',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation failed.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $hasMenuItem = $request->filled('menu_item_id');
        $hasFoodItem = $request->filled('food_item_id');

        if ($hasMenuItem === $hasFoodItem) {
            return response()->json([
                'success' => false,
                'message' => 'Provide exactly one menu item or food item.',
            ], 422);
        }

        $normalized = $request->all();
        $normalized['items'] = [[
            $hasMenuItem ? 'menu_item_id' : 'food_item_id' => (int) $request->input(
                $hasMenuItem ? 'menu_item_id' : 'food_item_id'
            ),
            'quantity' => (int) $request->input('quantity'),
        ]];
        $normalized['payment_method'] = 'WALLET';
        $normalized['points_to_redeem'] = (int) $request->input('points_to_redeem', 0);

        // Never trust client-supplied vendor, price, or food-name fields.
        unset(
            $normalized['vendor_id'],
            $normalized['food_item_id'],
            $normalized['menu_item_id'],
            $normalized['food_name'],
            $normalized['unit_price'],
            $normalized['total_price']
        );

        $request->replace($normalized);

        return app(ProductionCartCheckoutController::class)->store($request);
    }

    /**
     * Cancel an order.
     * Users can only cancel orders if the current status is 'PENDING' or 'ORDER_PLACED'.
     */
    public function cancel(Request $request, $id, \App\Services\PaystackRefundService $paystackRefunds)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $order = Order::withoutGlobalScopes()->find($id);
        if (! $order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }

        $role = strtoupper((string) $user->role);
        $owned = in_array((int) $user->id, [
            (int) $order->customer_id,
            (int) $order->student_id,
            (int) $order->user_id,
        ], true);

        if ($role === 'STUDENT' && ! $owned) {
            return response()->json(['success' => false, 'message' => 'You can only cancel your own orders.'], 403);
        }
        if ($role === 'VENDOR' && (int) $order->vendor_id !== (int) $user->id) {
            return response()->json(['success' => false, 'message' => 'You do not own this order.'], 403);
        }
        if (! in_array(strtoupper((string) $order->status), ['PENDING', 'ORDER_PLACED'], true)) {
            return response()->json(['success' => false, 'message' => 'This order can no longer be cancelled.'], 409);
        }

        $validator = Validator::make($request->all(), [
            'reason' => 'nullable|string|max:255',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid cancellation details.', 'errors' => $validator->errors()], 422);
        }

        try {
            $updatedOrder = DB::transaction(function () use ($request, $order, $user, $role) {
                $lockedOrder = Order::withoutGlobalScopes()
                    ->whereKey($order->id)
                    ->lockForUpdate()
                    ->firstOrFail();

                if (! in_array(strtoupper((string) $lockedOrder->status), ['PENDING', 'ORDER_PLACED'], true)) {
                    throw new \RuntimeException('This order can no longer be cancelled.');
                }

                $customerId = (int) ($lockedOrder->customer_id ?: $lockedOrder->user_id ?: $lockedOrder->student_id);
                if ($customerId <= 0) {
                    throw new \RuntimeException('The order has no refundable customer account.');
                }

                $customer = User::whereKey($customerId)->lockForUpdate()->firstOrFail();
                $refundAmount = round((float) ($lockedOrder->grand_total ?: $lockedOrder->total_price), 2);

                // Restore every order line for aggregate/multi-line orders.
                $orderItems = OrderItem::where('order_id', $lockedOrder->id)
                    ->lockForUpdate()
                    ->get();

                if ($orderItems->isNotEmpty()) {
                    foreach ($orderItems as $orderItem) {
                        $quantity = max(0, (int) $orderItem->quantity);

                        if ($orderItem->menu_item_id) {
                            $menuItem = MenuItem::whereKey($orderItem->menu_item_id)
                                ->lockForUpdate()
                                ->first();

                            if ($menuItem && $menuItem->current_stock !== null && $quantity > 0) {
                                $menuItem->current_stock = (int) $menuItem->current_stock + $quantity;
                                $menuItem->is_available = true;
                                $menuItem->save();
                            }
                        } elseif ($orderItem->food_item_id) {
                            $foodItem = FoodItem::whereKey($orderItem->food_item_id)
                                ->lockForUpdate()
                                ->first();

                            if ($foodItem && $foodItem->current_stock !== null && $quantity > 0) {
                                $foodItem->current_stock = (int) $foodItem->current_stock + $quantity;
                                $foodItem->is_available = true;
                                $foodItem->save();

                                InventoryMovement::create([
                                    'vendor_id' => $foodItem->vendor_id,
                                    'food_item_id' => $foodItem->id,
                                    'menu_item_id' => null,
                                    'order_id' => $lockedOrder->id,
                                    'type' => 'RESTOCK',
                                    'quantity' => $quantity,
                                    'balance_after' => $foodItem->current_stock,
                                    'reference' => 'CANCEL-'.$lockedOrder->order_number,
                                    'reason' => 'Stock restored after order cancellation.',
                                    'performed_by' => $user->id,
                                ]);
                            }
                        }
                    }
                } elseif ($lockedOrder->menu_item_id) {
                    // Legacy single-line menu order fallback.
                    $menuItem = MenuItem::whereKey($lockedOrder->menu_item_id)
                        ->lockForUpdate()
                        ->first();

                    if ($menuItem && $menuItem->current_stock !== null) {
                        $menuItem->current_stock = (int) $menuItem->current_stock + (int) ($lockedOrder->quantity ?? 0);
                        $menuItem->is_available = true;
                        $menuItem->save();
                    }
                } elseif ($lockedOrder->food_item_id) {
                    // Legacy single-line FoodItem order fallback.
                    $foodItem = FoodItem::whereKey($lockedOrder->food_item_id)
                        ->lockForUpdate()
                        ->first();

                    if ($foodItem && $foodItem->current_stock !== null) {
                        $quantity = max(0, (int) ($lockedOrder->quantity ?? 0));
                        $foodItem->current_stock = (int) $foodItem->current_stock + $quantity;
                        $foodItem->is_available = true;
                        $foodItem->save();

                        if ($quantity > 0) {
                            InventoryMovement::create([
                                'vendor_id' => $foodItem->vendor_id,
                                'food_item_id' => $foodItem->id,
                                'menu_item_id' => null,
                                'order_id' => $lockedOrder->id,
                                'type' => 'RESTOCK',
                                'quantity' => $quantity,
                                'balance_after' => $foodItem->current_stock,
                                'reference' => 'CANCEL-'.$lockedOrder->order_number,
                                'reason' => 'Stock restored after order cancellation.',
                                'performed_by' => $user->id,
                            ]);
                        }
                    }
                }

                $lockedOrder->status = 'CANCELLED';
                $lockedOrder->order_status = 'CANCELLED';
                $lockedOrder->cancelled_at = now();
                $lockedOrder->cancellation_reason = trim((string) $request->input(
                    'reason',
                    'Customer requested cancellation.'
                ));
                $lockedOrder->save();

                $payment = Payment::whereKey($lockedOrder->payment_id)
                    ->lockForUpdate()
                    ->first();

                $allocation = $payment
                    ? PaymentAllocation::where('payment_id', $payment->id)
                        ->where('order_id', $lockedOrder->id)
                        ->lockForUpdate()
                        ->first()
                    : null;

                $pointsToRestore = max(0, (int) ($lockedOrder->points_redeemed ?? 0));
                if ($pointsToRestore > 0) {
                    $customer->loyalty_points = (int) ($customer->loyalty_points ?? 0) + $pointsToRestore;
                }

                $walletPayment = strtoupper((string) ($lockedOrder->payment_method ?? '')) === 'WALLET';

                if ($walletPayment) {
                    $refundAmount = $allocation
                        ? max(0, round(
                            (float) $allocation->amount - (float) $allocation->refunded_amount,
                            2
                        ))
                        : $refundAmount;

                    if ($refundAmount <= 0) {
                        throw new \RuntimeException('This order has already been refunded.');
                    }

                    $before = round((float) $customer->balance, 2);
                    $customer->balance = round($before + $refundAmount, 2);
                    $customer->save();

                    $refund = \App\Models\Refund::create([
                        'order_id' => $lockedOrder->id,
                        'payment_id' => $payment?->id,
                        'customer_id' => $customer->id,
                        'requested_by' => $user->id,
                        'amount' => $refundAmount,
                        'reason' => trim((string) $request->input('reason', 'Order cancelled.')),
                        'status' => 'SUCCESS',
                        'processed_at' => now(),
                    ]);

                    WalletTransaction::create([
                        'user_id' => $customer->id,
                        'order_id' => $lockedOrder->id,
                        'payment_id' => $payment?->id,
                        'type' => 'REFUND',
                        'amount' => $refundAmount,
                        'status' => 'SUCCESS',
                        'source' => 'REFUND',
                        'performed_by' => $user->id,
                        'balance_before' => $before,
                        'balance_after' => $customer->balance,
                        'reference' => 'REF-'.strtoupper(Str::random(14)),
                        'details' => 'Wallet refund for cancelled order '.$lockedOrder->order_number,
                    ]);

                    if ($allocation) {
                        $allocation->refunded_amount = round(
                            min(
                                (float) $allocation->amount,
                                (float) $allocation->refunded_amount + $refundAmount
                            ),
                            2
                        );
                        $allocation->save();
                    }

                    if ($payment) {
                        $payment->load('allocations');
                        $allocated = (float) $payment->allocations->sum(
                            fn ($row) => (float) $row->amount
                        );
                        $refunded = (float) $payment->allocations->sum(
                            fn ($row) => (float) $row->refunded_amount
                        );

                        if ($allocated > 0) {
                            $payment->status = $refunded + 0.01 >= $allocated
                                ? 'REFUNDED'
                                : 'SUCCESS';
                            if ($payment->status === 'REFUNDED') {
                                $payment->refunded_at = now();
                            }
                        } else {
                            $payment->status = 'REFUNDED';
                            $payment->refunded_at = now();
                        }
                        $payment->save();
                    }
                } else {
                    if ($payment) {
                        $payment->update(['status' => 'REFUND_PENDING']);
                    }

                    \App\Models\Refund::create([
                        'order_id' => $lockedOrder->id,
                        'payment_id' => $payment?->id,
                        'customer_id' => $customer->id,
                        'requested_by' => $user->id,
                        'amount' => $refundAmount,
                        'reason' => trim((string) $request->input('reason', 'Order cancelled.')),
                        'status' => 'PENDING',
                    ]);
                }

                $customer->save();

                // A promotion belongs to the whole checkout session. Release it
                // only when every order in that session has been cancelled.
                $sessionId = $lockedOrder->checkout_session_id;
                if ($walletPayment) {
                    if ($sessionId) {
                        $hasOpenSibling = Order::withoutGlobalScopes()
                            ->where('checkout_session_id', $sessionId)
                            ->whereNotIn('status', ['CANCELLED', 'DECLINED'])
                            ->exists();

                        if (! $hasOpenSibling) {
                            PromotionRedemption::where('checkout_session_id', $sessionId)->delete();
                        }
                    } else {
                        PromotionRedemption::where('order_id', $lockedOrder->id)->delete();
                    }
                }

                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => now()->getTimestampMs(),
                    'action' => 'ORDER_CANCELLED',
                    'details' => "Order {$lockedOrder->order_number} cancelled. Refund workflow recorded.",
                ]);

                return $lockedOrder;
            }, 3);

            $gatewayRefund = null;
            if (strtoupper((string) $updatedOrder->payment_method) !== 'WALLET' && $updatedOrder->payment_id) {
                $payment = Payment::find($updatedOrder->payment_id);
                $refund = \App\Models\Refund::where('order_id', $updatedOrder->id)
                    ->where('status', 'PENDING')
                    ->latest()
                    ->first();

                if ($payment && $refund) {
                    try {
                        $gatewayRefund = $paystackRefunds->initiate($payment, $refund);
                    } catch (\Throwable $e) {
                        report($e);
                    }
                }
            }

            try {
                $customerId = (int) ($updatedOrder->customer_id ?: $updatedOrder->user_id);
                $customer = User::find($customerId);
                if ($customer && $role !== 'STUDENT') {
                    $customer->notify(
                        new OrderStatusChangedNotification($updatedOrder, 'PENDING', 'CANCELLED')
                    );
                }
            } catch (\Throwable $e) {
                report($e);
            }

            return response()->json([
                'success' => true,
                'message' => strtoupper((string) $updatedOrder->payment_method) === 'WALLET'
                    ? 'Order cancelled and wallet refund completed.'
                    : ($gatewayRefund && $gatewayRefund->status === 'FAILED'
                        ? 'Order cancelled, but the gateway refund could not be initiated. Please review the refund record.'
                        : 'Order cancelled. Refund has been submitted for processing.'),
                'order' => $updatedOrder,
            ], 200);
        } catch (\RuntimeException $e) {
            return response()->json(['success' => false, 'message' => $e->getMessage()], 409);
        } catch (\Throwable $e) {
            report($e);
            return response()->json([
                'success' => false,
                'message' => 'We could not cancel this order. Please try again.',
            ], 500);
        }
    }
    /**
     * Real-time order status tracking with SSE (Server-Sent Events) and JSON fallback.
     */
    public function trackOrderRealTime(Request $request, $id)
    {
        $order = Order::find($id);
        if (! $order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.',
            ], 404);
        }

        // If client requests text/event-stream or sets stream parameter of 1, stream in real-time
        if ($request->header('Accept') === 'text/event-stream' || $request->input('stream') == 1) {
            return response()->stream(function () use ($id) {
                $lastStatus = '';
                // Periodically check for updates for up to 15 cycles (~30 seconds)
                for ($i = 0; $i < 15; $i++) {
                    $order = Order::find($id);
                    if (! $order) {
                        echo "event: error\n";
                        echo 'data: '.json_encode(['message' => 'Order deleted'])."\n\n";
                        ob_flush();
                        flush();
                        break;
                    }

                    $currentStatus = $order->status;
                    $stages = $this->getTrackingStages($currentStatus);

                    if ($currentStatus !== $lastStatus) {
                        echo "event: status_update\n";
                        echo 'data: '.json_encode([
                            'id' => $order->id,
                            'status' => $currentStatus,
                            'estimated_pickup_time' => $order->estimated_pickup_time,
                            'stages' => $stages,
                            'updated_at' => $order->updated_at ? $order->updated_at->toIso8601String() : null,
                        ])."\n\n";
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
                'X-Accel-Buffering' => 'no',
            ]);
        }

        // Return direct single JSON snapshot
        $user = $request->user();
        $isOwner = in_array((int) $user->id, [
            (int) $order->customer_id,
            (int) $order->student_id,
            (int) $order->user_id,
        ], true);
        $canViewPin = $isOwner || strtoupper($user->role ?? '') === 'ADMIN';

        return response()->json([
            'success' => true,
            'id' => $order->id,
            'status' => $order->status,
            'estimated_pickup_time' => $order->estimated_pickup_time,
            'pickup_pin' => $canViewPin ? $order->pickup_pin : null,
            'stages' => $this->getTrackingStages($order->status),
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
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
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
            'polled_at' => date('c'),
        ], 200);
    }

    /**
     * Real-time SSE listener stream specifically alerting when active order statuses change to 'READY'.
     */
    public function streamOrderStatusReady(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
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
                    if (! in_array($order->id, $notifiedOrders)) {
                        echo "event: order_ready_push\n";
                        echo 'data: '.json_encode([
                            'order_id' => $order->id,
                            'food_name' => $order->food_name,
                            'pickup_pin' => $order->pickup_pin,
                            'message' => "Your order #{$order->id} ('{$order->food_name}') is ready for pickup!",
                            'timestamp' => date('c'),
                        ])."\n\n";
                        ob_flush();
                        flush();
                        $notifiedOrders[] = $order->id;
                    }
                }

                // Heartbeat to keep connection alive
                echo "event: heartbeat\n";
                echo 'data: '.json_encode(['status' => 'listening', 'cycle' => $cycle])."\n\n";
                ob_flush();
                flush();

                sleep(2);
            }
        }, 200, [
            'Content-Type' => 'text/event-stream',
            'Cache-Control' => 'no-cache',
            'Connection' => 'keep-alive',
            'X-Accel-Buffering' => 'no',
        ]);
    }

    /**
     * Submit multiple order requests (shopping cart checkout) for the authenticated user.
     */
    public function cartCheckout(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'items' => 'required|array|min:1|max:50',
            'items.*.menu_item_id' => 'nullable|integer|distinct|exists:menu_items,id',
            'items.*.food_item_id' => 'nullable|integer|distinct|exists:food_items,id',
            'items.*.quantity' => 'required|integer|min:1|max:50',
            'points_to_redeem' => 'nullable|integer|min:0|max:100000',
            'payment_method' => 'nullable|string|in:wallet,momo,card,WALLET,MOMO,CARD',
            'payment_reference' => 'nullable|string|max:120',
            'order_type' => 'nullable|string|in:TAKEAWAY,PICKUP,DINE_IN,DELIVERY',
            'customer_note' => 'nullable|string|max:1000',
        ]);

        $validator->after(function ($validator) use ($request) {
            foreach ((array) $request->input('items', []) as $index => $input) {
                if (! ($input['menu_item_id'] ?? null) && ! ($input['food_item_id'] ?? null)) {
                    $validator->errors()->add("items.{$index}", 'Each cart item must reference a menu item or food item.');
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
                $lockedUser = User::whereKey($user->id)->lockForUpdate()->firstOrFail();
                $pointsToRedeem = (int) $request->input('points_to_redeem', 0);
                $paymentMethod = strtoupper((string) $request->input('payment_method', 'WALLET'));
                $orderType = strtoupper((string) $request->input('order_type', 'TAKEAWAY'));
                $paymentReference = trim((string) $request->input('payment_reference', ''));
                $total = 0.0;
                $items = [];

                foreach ($request->input('items') as $index => $input) {
                    $menuItemId = $input['menu_item_id'] ?? null;
                    $foodItemId = $input['food_item_id'] ?? null;

                    if ($menuItemId) {
                        $catalogItem = MenuItem::whereKey((int) $menuItemId)->lockForUpdate()->first();
                        $isMenuItem = true;
                    } else {
                        $catalogItem = FoodItem::whereKey((int) $foodItemId)->lockForUpdate()->first();
                        $isMenuItem = false;
                    }

                    if (! $catalogItem || ! $catalogItem->is_available) {
                        throw new \RuntimeException('One of the selected menu items is no longer available.');
                    }

                    $quantity = (int) $input['quantity'];
                    if ($isMenuItem && $catalogItem->current_stock !== null && $catalogItem->current_stock < $quantity) {
                        throw new \RuntimeException("Only {$catalogItem->current_stock} unit(s) remain for '".($catalogItem->name ?: $catalogItem->food_name)."'.");
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

                $promotion = null;
                $promotionDiscount = 0.0;
                $promotionCode = strtoupper(trim((string) $request->input('promotion_code', '')));

                if ($promotionCode !== '') {
                    if ($pointsToRedeem > 0) {
                        throw new \RuntimeException('Use either loyalty points or a promotion code for this order, not both.');
                    }

                    $promotion = \App\Models\Promotion::whereRaw('UPPER(code) = ?', [$promotionCode])
                        ->lockForUpdate()
                        ->first();

                    if (! $promotion || ! $promotion->isCurrentlyActive()) {
                        throw new \RuntimeException('This promotion is not active or has expired.');
                    }

                    if ($total < (float) $promotion->minimum_order_amount) {
                        throw new \RuntimeException(
                            'This promotion requires a minimum order of GH₵ '.
                            number_format((float) $promotion->minimum_order_amount, 2).'.'
                        );
                    }

                    if ($promotion->usage_limit !== null &&
                        \App\Models\PromotionRedemption::where('promotion_id', $promotion->id)->count() >= $promotion->usage_limit) {
                        throw new \RuntimeException('This promotion has reached its usage limit.');
                    }

                    if ($promotion->per_customer_limit !== null &&
                        \App\Models\PromotionRedemption::where('promotion_id', $promotion->id)
                            ->where('customer_id', $lockedUser->id)->count() >= $promotion->per_customer_limit) {
                        throw new \RuntimeException('You have already used this promotion the maximum number of times allowed.');
                    }

                    $promotionDiscount = strtoupper((string) $promotion->type) === 'PERCENTAGE'
                        ? round($total * ((float) $promotion->value / 100), 2)
                        : round((float) $promotion->value, 2);

                    if ($promotion->maximum_discount_amount !== null) {
                        $promotionDiscount = min($promotionDiscount, (float) $promotion->maximum_discount_amount);
                    }

                    $promotionDiscount = min($promotionDiscount, $total);
                }

                $discount = round($promotion ? $promotionDiscount : ($pointsToRedeem * 0.10), 2);
                $finalTotal = max(0.0, round($total - $discount, 2));

                $payment = null;
                if ($paymentMethod === 'WALLET') {
                    if ((float) $lockedUser->balance < $finalTotal) {
                        throw new \RuntimeException('Insufficient wallet balance.');
                    }

                    $payment = \App\Models\Payment::create([
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
                    if ($paymentReference === '') {
                        throw new \RuntimeException('A verified online payment reference is required.');
                    }

                    $payment = \App\Models\Payment::where('customer_id', $lockedUser->id)
                        ->where('reference', $paymentReference)
                        ->where('purpose', 'DIRECT_ORDER_PAY')
                        ->where('status', 'SUCCESS')
                        ->lockForUpdate()
                        ->first();

                    if (! $payment || abs((float) $payment->amount - $finalTotal) > 0.01) {
                        throw new \RuntimeException('The online payment could not be verified for this order total.');
                    }

                    if (Order::withoutGlobalScopes()->where('payment_id', $payment->id)->exists()) {
                        throw new \RuntimeException('This payment has already been applied to an order.');
                    }
                }

                $beforeBalance = round((float) $lockedUser->balance, 2);
                $createdOrders = [];
                $remainingDiscount = $discount;
                $remainingTotal = $total;

                foreach ($items as $item) {
                    $catalogItem = $item['catalog_item'];
                    $itemName = $catalogItem->name ?: ($catalogItem->food_name ?? 'Meal');
                    $itemDiscount = $remainingTotal > 0
                        ? round(min($remainingDiscount, $discount * ($item['lineTotal'] / $total)), 2)
                        : 0.0;

                    $lineGrandTotal = max(0.0, round($item['lineTotal'] - $itemDiscount, 2));
                    $order = Order::create([
                        'order_number' => 'CAF-'.now()->format('ymdHis').'-'.strtoupper(Str::random(5)),
                        'order_type' => $orderType,
                        'payment_id' => $payment->id,
                        'payment_method' => $paymentMethod,
                        'payment_status' => 'PAID',
                        'subtotal' => $item['lineTotal'],
                        'discount_amount' => $itemDiscount,
                        'tax_amount' => 0,
                        'service_fee' => 0,
                        'delivery_fee' => 0,
                        'grand_total' => $lineGrandTotal,
                        'currency' => 'GHS',
                        'customer_note' => $request->input('customer_note'),
                        'customer_id' => $lockedUser->id,
                        'student_id' => $lockedUser->id,
                        'user_id' => $lockedUser->id,
                        'vendor_id' => $catalogItem->vendor_id,
                        'food_item_id' => $item['is_menu_item'] ? null : $catalogItem->id,
                        'menu_item_id' => $item['is_menu_item'] ? $catalogItem->id : null,
                        'food_name' => $itemName,
                        'quantity' => $item['quantity'],
                        'unit_price' => $item['unitPrice'],
                        'total_price' => $lineGrandTotal,
                        'order_timestamp' => now()->getTimestampMs(),
                        'placed_at' => now(),
                        'status' => 'PENDING',
                        'pickup_pin' => (string) random_int(1000, 9999),
                        'estimated_pickup_time' => $request->input('estimated_pickup_time', 'Calculating...'),
                        'points_redeemed' => 0,
                        'discount_applied' => $itemDiscount,
                    ]);

                    if ($item['is_menu_item'] && $catalogItem->current_stock !== null) {
                        $catalogItem->current_stock = max(0, (int) $catalogItem->current_stock - $item['quantity']);
                        $catalogItem->is_available = $catalogItem->current_stock > 0;
                        $catalogItem->save();
                    }

                    if ($promotion) {
                        \App\Models\PromotionRedemption::create([
                            'promotion_id' => $promotion->id,
                            'customer_id' => $lockedUser->id,
                            'order_id' => $order->id,
                            'discount_amount' => $itemDiscount,
                        ]);
                    }

                    \App\Models\OrderItem::create([
                        'order_id' => $order->id,
                        'food_item_id' => $item['is_menu_item'] ? null : $catalogItem->id,
                        'name' => $itemName,
                        'name_snapshot' => $itemName,
                        'quantity' => $item['quantity'],
                        'unit_price' => $item['unitPrice'],
                        'total_price' => $lineGrandTotal,
                        'discount_amount' => $itemDiscount,
                        'line_total' => $lineGrandTotal,
                    ]);

                    if ($paymentMethod === 'WALLET') {
                        $lockedUser->balance = round((float) $lockedUser->balance - $lineGrandTotal, 2);
                        \App\Models\WalletTransaction::create([
                            'user_id' => $lockedUser->id,
                            'order_id' => $order->id,
                            'payment_id' => $payment->id,
                            'type' => 'PAYMENT',
                            'amount' => -$lineGrandTotal,
                            'status' => 'SUCCESS',
                            'source' => 'ORDER',
                            'performed_by' => $lockedUser->id,
                            'balance_before' => $beforeBalance,
                            'balance_after' => $lockedUser->balance,
                            'reference' => 'CART-'.strtoupper(Str::random(14)),
                            'details' => 'Wallet payment for order '.$order->order_number,
                        ]);
                        $beforeBalance = round((float) $lockedUser->balance, 2);
                    }

                    $createdOrders[] = $order;
                    $remainingDiscount = max(0.0, round($remainingDiscount - $itemDiscount, 2));
                    $remainingTotal = max(0.0, round($remainingTotal - $item['lineTotal'], 2));
                }

                $lockedUser->loyalty_points = (int) ($lockedUser->loyalty_points ?? 0) - $pointsToRedeem;
                $lockedUser->total_spent = round((float) ($lockedUser->total_spent ?? 0) + $finalTotal, 2);
                $lockedUser->save();

                return [
                    'orders' => $createdOrders,
                    'total_cost' => $total,
                    'discount' => $discount,
                    'promotion_code' => $promotion?->code,
                    'promotion_discount' => $promotionDiscount,
                    'final_total' => $finalTotal,
                    'remaining_balance' => (float) $lockedUser->balance,
                    'payment_reference' => $payment->reference,
                ];
            }, 3);

            return response()->json([
                'success' => true,
                'message' => 'Order placed successfully.',
                'orders' => $result['orders'],
                'total_cost' => $result['total_cost'],
                'discount' => $result['discount'],
                'promotion_code' => $result['promotion_code'],
                'promotion_discount' => $result['promotion_discount'],
                'final_total' => $result['final_total'],
                'remaining_balance' => $result['remaining_balance'],
                'payment_reference' => $result['payment_reference'],
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
        if (! $order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.',
            ], 404);
        }

        $user = $request->user();
        if (! $user || strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only administrative personnel can delete orders.',
            ], 403);
        }

        DB::transaction(function () use ($order, $user) {
            $order->delete(); // Soft delete

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'ORDER_DELETED',
                'details' => "Administrator deleted order #{$order->id} ('{$order->food_name}').",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Order deleted successfully.',
        ], 200);
    }

    /**
     * Calculates and returns the estimated wait time for a vendor's queue.
     */
    public function getVendorWaitTime(Request $request, $vendorId = null)
    {
        // If not specified in path, check query param or authenticated user
        if (! $vendorId) {
            if ($request->has('vendor_id')) {
                $vendorId = (int) $request->input('vendor_id');
            } else {
                $user = $request->user();
                if ($user && (strtoupper($user->role) === 'VENDOR' || strtoupper($user->role) === 'ADMIN')) {
                    $vendorId = $user->id;
                } else {
                    return response()->json([
                        'success' => false,
                        'message' => 'Vendor ID is required.',
                    ], 400);
                }
            }
        }

        $vendor = User::find($vendorId);
        if (! $vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor not found.',
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
            'vendor_id' => (int) $vendorId,
            'vendor_name' => $vendor->fullName,
            'queue_metrics' => [
                'total_active_orders' => count($activeOrders),
                'preparing_orders_count' => $preparingCount,
                'pending_orders_count' => $pendingCount,
                'total_queue_quantity' => (int) $activeOrders->sum('quantity'),
            ],
            'estimated_wait_time_minutes' => (int) $estimatedMinutes,
            'formatted_wait_time' => "{$estimatedMinutes} mins",
            'congestion_level' => $preparingCount >= 8 ? 'CRITICAL' : ($preparingCount >= 4 ? 'HIGH' : ($preparingCount >= 1 ? 'MODERATE' : 'LOW')),
            'calculated_at' => date('c'),
        ], 200);
    }
}
