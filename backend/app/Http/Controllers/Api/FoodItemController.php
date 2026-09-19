<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreFoodItemRequest;
use App\Http\Requests\UpdateFoodItemRequest;
use App\Models\AuditLog;
use App\Models\FoodItem;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class FoodItemController extends Controller
{
    /**
     * Display a listing of all food items.
     *
     * A vendor can accidentally have duplicate records with the same dish
     * name. The API keeps the earliest record and hides duplicates from the
     * customer catalogue without deleting database records.
     */
    public function index()
    {
        $foods = FoodItem::orderBy('id')->get()->unique(function ($food) {
            return $food->vendor_id . '|' . mb_strtolower(trim($food->name));
        })->values();

        return response()->json($foods, 200);
    }

    /**
     * Get food items belonging to a single Vendor.
     */
    public function getVendorFoodItems($vendorId)
    {
        $foods = FoodItem::where('vendor_id', $vendorId)
            ->orderBy('id')
            ->get()
            ->unique(function ($food) {
                return mb_strtolower(trim($food->name));
            })
            ->values();

        return response()->json($foods, 200);
    }

    /**
     * Store a newly created menu dish.
     */
    public function store(StoreFoodItemRequest $request)
    {
        $user = $request->user();
        $vendorId = $request->input('vendor_id');

        if ($vendorId != $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You cannot create food items for another vendor.',
            ], 403);
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
                'initial_stock' => $request->input('initial_stock', 50),
                'low_stock_threshold' => $request->input('low_stock_threshold', 10),
            ]);

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
    public function update(UpdateFoodItemRequest $request, $id)
    {
        $food = FoodItem::find($id);
        if (! $food) {
            return response()->json([
                'success' => false,
                'message' => 'Food item not found.',
            ], 404);
        }

        $user = $request->user();
        if ($food->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this food item.',
            ], 403);
        }

        $updatedFood = DB::transaction(function () use ($food, $request) {
            $data = $request->only(['name', 'price', 'category', 'description', 'image_url', 'is_available', 'initial_stock', 'low_stock_threshold']);
            $food->update($data);

            AuditLog::create([
                'user_id' => $food->vendor_id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_UPDATED',
                'details' => "Updated details of '{$food->name}' (Availability: ".($food->is_available ? 'Yes' : 'No').') on Laravel API.',
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
        if (! $food) {
            return response()->json([
                'success' => false,
                'message' => 'Food item not found.',
            ], 404);
        }

        $user = request()->user();
        if ($food->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this food item.',
            ], 403);
        }

        $vendorId = $food->vendor_id;
        $name = $food->name;

        DB::transaction(function () use ($food, $vendorId, $name) {
            $food->delete();

            AuditLog::create([
                'user_id' => $vendorId,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_DELETED',
                'details' => "Eradicated menu item '{$name}' from vending list via Laravel API.",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Food item has been erased successfully.',
        ], 200);
    }

    /**
     * Toggle availability of multiple menu items simultaneously.
     */
    public function bulkToggle(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'ids' => 'required|array',
            'ids.*' => 'integer|exists:food_items,id',
            'is_available' => 'required|boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'errors' => $validator->errors(),
            ], 400);
        }

        $user = $request->user();
        $ids = $request->input('ids');
        $isAvailable = $request->input('is_available');

        $foods = FoodItem::whereIn('id', $ids)->get();
        foreach ($foods as $food) {
            if ($food->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
                return response()->json([
                    'success' => false,
                    'message' => "Unauthorized. You do not own the food item '{$food->name}'.",
                ], 403);
            }
        }

        DB::transaction(function () use ($ids, $isAvailable, $foods, $user) {
            FoodItem::whereIn('id', $ids)->update(['is_available' => $isAvailable]);

            $names = $foods->pluck('name')->implode(', ');
            $statusStr = $isAvailable ? 'Available' : 'Unavailable';

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEMS_BULK_TOGGLED',
                'details' => "Bulk toggled items [{$names}] to {$statusStr} via Laravel API.",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu items updated successfully.',
        ], 200);
    }
}