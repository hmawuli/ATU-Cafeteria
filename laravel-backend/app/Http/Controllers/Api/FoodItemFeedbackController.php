<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\FoodItemFeedback;
use App\Models\Order;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class FoodItemFeedbackController extends Controller
{
    /**
     * Display a complete index of food item feedbacks.
     */
    public function index()
    {
        $feedbacks = FoodItemFeedback::with(['customer', 'foodItem'])
            ->orderBy('timestamp', 'desc')
            ->get();
        return response()->json($feedbacks, 200);
    }

    /**
     * Get feedbacks for a specific food item.
     */
    public function getByFoodItem($foodItemId)
    {
        $feedbacks = FoodItemFeedback::where('food_item_id', $foodItemId)
            ->with(['customer'])
            ->orderBy('timestamp', 'desc')
            ->get();
        return response()->json($feedbacks, 200);
    }

    /**
     * Store feedback for a specific completed food item order.
     */
    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'order_id' => 'required|integer|exists:orders,id',
            'food_item_id' => 'required|integer|exists:food_items,id',
            'customer_id' => 'required|integer|exists:users,id',
            'rating' => 'required|integer|min:1|max:5',
            'comment' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input rating validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Verify order is completed before permitting food item feedback
        $order = Order::find($request->input('order_id'));
        if (!$order || strtoupper($order->status) !== 'COMPLETED') {
            return response()->json([
                'success' => false,
                'message' => 'Feedback can only be left on successfully COMPLETED orders.'
            ], 400);
        }

        // Verify order corresponds to this food item and customer
        if ($order->customer_id != $request->input('customer_id') || $order->food_item_id != $request->input('food_item_id')) {
            return response()->json([
                'success' => false,
                'message' => 'The specified order details do not match the food item or customer.'
            ], 400);
        }

        // Execute transaction to store feedback and record audit trails
        $feedback = DB::transaction(function () use ($request, $order) {
            $createdFeedback = FoodItemFeedback::create([
                'order_id' => $request->input('order_id'),
                'food_item_id' => $request->input('food_item_id'),
                'customer_id' => $request->input('customer_id'),
                'rating' => $request->input('rating'),
                'comment' => $request->input('comment') ?? '',
                'timestamp' => time() * 1000,
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdFeedback->customer_id,
                'timestamp' => time() * 1000,
                'action' => 'FOOD_FEEDBACK_SUBMITTED',
                'details' => "Rated food '{$order->food_name}' (ID: {$createdFeedback->food_item_id}) with rating={$createdFeedback->rating} stars.",
            ]);

            return $createdFeedback;
        });

        return response()->json($feedback, 201);
    }
}
