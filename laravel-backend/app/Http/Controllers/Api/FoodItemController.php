<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\FoodItem;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class FoodItemController extends Controller
{
    /**
     * Display a listing of all food items.
     */
    public function index()
    {
        $foods = FoodItem::all();
        return response()->json($foods, 200);
    }

    /**
     * Get food items belonging to a single Vendor.
     */
    public function getVendorFoodItems($vendorId)
    {
        $foods = FoodItem::where('vendor_id', $vendorId)->get();
        return response()->json($foods, 200);
    }

    /**
     * Store a newly created menu dish.
     */
    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'vendor_id' => 'required|integer|exists:users,id',
            'name' => 'required|string|max:255',
            'price' => 'required|numeric|min:0',
            'category' => 'required|string',
            'description' => 'nullable|string',
            'image_url' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input validations failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        $food = DB::transaction(function () use ($request) {
            $createdFood = FoodItem::create([
                'vendor_id' => $request->input('vendor_id'),
                'name' => $request->input('name'),
                'price' => $request->input('price'),
                'category' => $request->input('category'),
                'description' => $request->input('description') ?? '',
                'image_url' => $request->input('image_url') ?? '',
                'is_available' => true,
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdFood->vendor_id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_CREATED',
                'details' => "Added dish: '{$createdFood->name}' to category '{$createdFood->category}' via Laravel API.",
            ]);

            return $createdFood;
        });

        return response()->json($food, 201);
    }

    /**
     * Update the details/availability of a single food item.
     */
    public function update(Request $request, $id)
    {
        $food = FoodItem::find($id);
        if (!$food) {
            return response()->json([
                'success' => false,
                'message' => 'Food item not found.'
            ], 404);
        }

        $updatedFood = DB::transaction(function () use ($food, $request) {
            // We allow partial updates
            $data = $request->only(['name', 'price', 'category', 'description', 'image_url', 'is_available']);
            $food->update($data);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $food->vendor_id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_UPDATED',
                'details' => "Updated details of '{$food->name}' (Availability: " . ($food->is_available ? 'Yes' : 'No') . ") on Laravel API.",
            ]);

            return $food;
        });

        return response()->json($updatedFood, 200);
    }

    /**
     * Remove the specified food item from DB.
     */
    public function destroy($id)
    {
        $food = FoodItem::find($id);
        if (!$food) {
            return response()->json([
                'success' => false,
                'message' => 'Food item not found.'
            ], 404);
        }

        $vendorId = $food->vendor_id;
        $name = $food->name;

        DB::transaction(function () use ($food, $vendorId, $name) {
            $food->delete();

            // Register Audit Log
            AuditLog::create([
                'user_id' => $vendorId,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_DELETED',
                'details' => "Eradicated menu item '{$name}' from vending list via Laravel API.",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Food item has been erased successfully.'
        ], 200);
    }
}
