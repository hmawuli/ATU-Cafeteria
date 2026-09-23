<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\SupportTicket;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class CustomerSupportController extends Controller
{
    public function index(Request $request)
    {
        return response()->json([
            'success' => true,
            'tickets' => SupportTicket::where('customer_id', $request->user()->id)->latest()->paginate(20),
        ]);
    }

    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'order_id' => 'nullable|integer|exists:orders,id',
            'category' => 'required|string|in:ORDER,PAYMENT,REFUND,ACCOUNT,TECHNICAL,OTHER',
            'subject' => 'required|string|min:3|max:180',
            'description' => 'required|string|min:5|max:5000',
            'priority' => 'nullable|string|in:LOW,NORMAL,HIGH,URGENT',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid support request.', 'errors' => $validator->errors()], 422);
        }

        $orderId = $request->input('order_id');
        if ($orderId !== null) {
            $ownsOrder = \App\Models\Order::query()
                ->whereKey($orderId)
                ->where(function ($query) use ($request) {
                    $query->where('customer_id', $request->user()->id)
                        ->orWhere('student_id', $request->user()->id)
                        ->orWhere('user_id', $request->user()->id);
                })
                ->exists();

            if (! $ownsOrder) {
                return response()->json([
                    'success' => false,
                    'message' => 'The selected order does not belong to your account.',
                    'error_code' => 'ORDER_NOT_OWNED',
                ], 403);
            }
        }

        $ticket = SupportTicket::create([
            'customer_id' => $request->user()->id,
            'order_id' => $request->input('order_id'),
            'category' => strtoupper($request->input('category')),
            'subject' => trim($request->input('subject')),
            'description' => trim($request->input('description')),
            'priority' => strtoupper($request->input('priority', 'NORMAL')),
            'status' => 'OPEN',
        ]);

        return response()->json(['success' => true, 'message' => 'Support ticket created.', 'ticket' => $ticket], 201);
    }

    public function show(Request $request, SupportTicket $ticket)
    {
        abort_unless((int) $ticket->customer_id === (int) $request->user()->id, 403);
        return response()->json(['success' => true, 'ticket' => $ticket]);
    }
}
