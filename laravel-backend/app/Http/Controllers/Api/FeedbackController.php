<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Feedback;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class FeedbackController extends Controller
{
    /**
     * Display a comprehensive index of student feedback (Admin overview).
     */
    public function index()
    {
        $feedbacks = Feedback::orderBy('timestamp', 'desc')->get();
        return response()->json($feedbacks, 200);
    }

    /**
     * Get student evaluation logs specific to a single food vendor.
     */
    public function getVendorFeedback($vendorId)
    {
        $feedbacks = Feedback::where('vendor_id', $vendorId)
            ->orderBy('timestamp', 'desc')
            ->get();
        return response()->json($feedbacks, 200);
    }

    /**
     * Store post-purchase dynamic metrics and text comments.
     */
    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'order_id' => 'required|integer|exists:orders,id',
            'vendor_id' => 'required|integer|exists:users,id',
            'customer_id' => 'required|integer|exists:users,id',
            'food_quality' => 'required|integer|min:1|max:5',
            'cleanliness' => 'required|integer|min:1|max:5',
            'speed' => 'required|integer|min:1|max:5',
            'value' => 'required|integer|min:1|max:5',
            'comment' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input metric scores validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Execute feedback creation and auditing inside transactional bounds
        $feedback = DB::transaction(function () use ($request) {
            $createdFeedback = Feedback::create([
                'order_id' => $request->input('order_id'),
                'vendor_id' => $request->input('vendor_id'),
                'customer_id' => $request->input('customer_id'),
                'rating_food_quality' => $request->input('food_quality'),
                'rating_cleanliness' => $request->input('cleanliness'),
                'rating_service_speed' => $request->input('speed'),
                'rating_price_value' => $request->input('value'),
                'comment' => $request->input('comment') ?? '',
                'timestamp' => time() * 1000,
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdFeedback->customer_id,
                'timestamp' => time() * 1000,
                'action' => 'FEEDBACK_SUBMITTED',
                'details' => "Evaluated Order #{$createdFeedback->order_id}: Food Quality={$createdFeedback->rating_food_quality}, Cleanliness={$createdFeedback->rating_cleanliness}, Speed={$createdFeedback->rating_service_speed}, Value={$createdFeedback->rating_price_value}.",
            ]);

            return $createdFeedback;
        });

        return response()->json($feedback, 201);
    }
}
