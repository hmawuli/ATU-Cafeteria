<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\DeliveredOrderReview;
use App\Models\Order;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class DeliveredOrderReviewController extends Controller
{
    /**
     * Store rating and review for food items or vendors after an order is marked as 'delivered' or 'completed'.
     */
    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'order_id' => 'required|integer|exists:orders,id',
            'vendor_rating' => 'nullable|integer|min:1|max:5',
            'vendor_comment' => 'nullable|string',
            'food_rating' => 'nullable|integer|min:1|max:5',
            'food_comment' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Review validation failed.',
                'errors' => $validator->errors(),
            ], 400);
        }

        $orderId = $request->input('order_id');
        $order = Order::find($orderId);

        // Check if order exists (already verified by validator, but safe-keeping)
        if (! $order) {
            return response()->json([
                'success' => false,
                'message' => 'Order not found.',
            ], 404);
        }

        // Verify status - MUST be marked as DELIVERED or COMPLETED to rate/review
        $allowedStatuses = ['DELIVERED', 'COMPLETED'];
        $currentStatus = strtoupper($order->status ?? '');
        $currentOrderStatus = strtoupper($order->order_status ?? '');

        $isEligible = in_array($currentStatus, $allowedStatuses) || in_array($currentOrderStatus, $allowedStatuses);

        if (! $isEligible) {
            return response()->json([
                'success' => false,
                'message' => "Order #{$orderId} cannot be reviewed yet. It must be delivered or completed first. Current status: ".($order->status ?: 'PENDING'),
            ], 400);
        }

        // Check if student has already reviewed this order
        $existingReview = DeliveredOrderReview::where('order_id', $orderId)->first();
        if ($existingReview) {
            return response()->json([
                'success' => false,
                'message' => 'You have already submitted a review for this order.',
            ], 400);
        }

        // Create the review in database transaction
        $review = DB::transaction(function () use ($request, $order) {
            $createdReview = DeliveredOrderReview::create([
                'order_id' => $order->id,
                'student_id' => $order->customer_id ?? $order->student_id,
                'vendor_id' => $order->vendor_id,
                'food_item_id' => $order->food_item_id ?: ($order->menu_item_id ?: null),
                'vendor_rating' => $request->input('vendor_rating'),
                'vendor_comment' => $request->input('vendor_comment'),
                'food_rating' => $request->input('food_rating'),
                'food_comment' => $request->input('food_comment'),
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdReview->student_id,
                'timestamp' => time() * 1000,
                'action' => 'DELIVERED_ORDER_REVIEWED',
                'details' => "Student #{$createdReview->student_id} rated Order #{$order->id}: Vendor Rating={$createdReview->vendor_rating}, Food Rating={$createdReview->food_rating}.",
            ]);

            return $createdReview;
        });

        return response()->json([
            'success' => true,
            'message' => 'Your review and rating have been registered successfully.',
            'review' => $review,
        ], 201);
    }

    /**
     * Get reviews and ratings for a specific vendor.
     */
    public function getVendorReviews($vendorId)
    {
        $reviews = DeliveredOrderReview::with(['student:id,fullName,username', 'foodItem:id,name'])
            ->where('vendor_id', $vendorId)
            ->orderBy('created_at', 'desc')
            ->get();

        $averageRating = round($reviews->avg('vendor_rating') ?? 0, 1);

        return response()->json([
            'success' => true,
            'vendor_id' => (int) $vendorId,
            'average_vendor_rating' => $averageRating,
            'reviews_count' => count($reviews),
            'reviews' => $reviews,
        ], 200);
    }

    /**
     * Get reviews and ratings for a specific food/menu item.
     */
    public function getFoodItemReviews($foodItemId)
    {
        $reviews = DeliveredOrderReview::with(['student:id,fullName,username'])
            ->where('food_item_id', $foodItemId)
            ->whereNotNull('food_rating')
            ->orderBy('created_at', 'desc')
            ->get();

        $averageRating = round($reviews->avg('food_rating') ?? 0, 1);

        return response()->json([
            'success' => true,
            'food_item_id' => (int) $foodItemId,
            'average_food_rating' => $averageRating,
            'reviews_count' => count($reviews),
            'reviews' => $reviews,
        ], 200);
    }

    /**
     * Get all review records (Admin/Audit view).
     */
    public function index()
    {
        $reviews = DeliveredOrderReview::with(['student:id,fullName,username', 'vendor:id,fullName', 'order:id,food_name'])
            ->orderBy('created_at', 'desc')
            ->get();

        return response()->json([
            'success' => true,
            'reviews' => $reviews,
        ], 200);
    }
}
