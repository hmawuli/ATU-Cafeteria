<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Promotion;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class VendorPromotionController extends Controller
{
    private function vendor(Request $request)
    {
        $user = $request->user();
        abort_unless($user && strtoupper((string) $user->role) === 'VENDOR', 403);

        return $user;
    }

    public function index(Request $request)
    {
        $vendor = $this->vendor($request);

        return response()->json([
            'success' => true,
            'promotions' => Promotion::where('vendor_id', $vendor->id)
                ->latest()
                ->paginate(50),
        ]);
    }

    public function store(Request $request)
    {
        $vendor = $this->vendor($request);

        $validator = Validator::make($request->all(), [
            'code' => 'required|string|min:3|max:50|alpha_dash',
            'name' => 'required|string|max:160',
            'type' => 'required|string|in:PERCENTAGE,FIXED',
            'value' => 'required|numeric|min:0.01',
            'minimum_order_amount' => 'nullable|numeric|min:0',
            'maximum_discount_amount' => 'nullable|numeric|min:0',
            'usage_limit' => 'nullable|integer|min:1',
            'per_customer_limit' => 'nullable|integer|min:1',
            'starts_at' => 'nullable|date',
            'ends_at' => 'nullable|date|after_or_equal:starts_at',
            'is_active' => 'boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid promotion.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $promotion = Promotion::create([
            'vendor_id' => $vendor->id,
            'code' => strtoupper(trim($request->input('code'))),
            'name' => trim($request->input('name')),
            'type' => strtoupper($request->input('type')),
            'value' => $request->input('value'),
            'minimum_order_amount' => $request->input('minimum_order_amount', 0),
            'maximum_discount_amount' => $request->input('maximum_discount_amount'),
            'usage_limit' => $request->input('usage_limit'),
            'per_customer_limit' => $request->input('per_customer_limit'),
            'starts_at' => $request->input('starts_at'),
            'ends_at' => $request->input('ends_at'),
            'is_active' => $request->boolean('is_active', true),
        ]);

        return response()->json([
            'success' => true,
            'promotion' => $promotion,
        ], 201);
    }

    public function update(Request $request, Promotion $promotion)
    {
        $vendor = $this->vendor($request);
        abort_unless((int) $promotion->vendor_id === (int) $vendor->id, 403);

        $validator = Validator::make($request->all(), [
            'name' => 'sometimes|required|string|max:160',
            'type' => 'sometimes|required|string|in:PERCENTAGE,FIXED',
            'value' => 'sometimes|required|numeric|min:0.01',
            'minimum_order_amount' => 'nullable|numeric|min:0',
            'maximum_discount_amount' => 'nullable|numeric|min:0',
            'usage_limit' => 'nullable|integer|min:1',
            'per_customer_limit' => 'nullable|integer|min:1',
            'starts_at' => 'nullable|date',
            'ends_at' => 'nullable|date|after_or_equal:starts_at',
            'is_active' => 'boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid promotion update.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $promotion->fill($request->only([
            'name','type','value','minimum_order_amount',
            'maximum_discount_amount','usage_limit','per_customer_limit',
            'starts_at','ends_at','is_active',
        ]));
        $promotion->save();

        return response()->json([
            'success' => true,
            'promotion' => $promotion,
        ]);
    }

    public function destroy(Request $request, Promotion $promotion)
    {
        $vendor = $this->vendor($request);
        abort_unless((int) $promotion->vendor_id === (int) $vendor->id, 403);

        $promotion->is_active = false;
        $promotion->save();

        return response()->json([
            'success' => true,
            'message' => 'Promotion disabled.',
        ]);
    }
}
