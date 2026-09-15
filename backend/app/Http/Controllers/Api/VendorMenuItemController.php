<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\MenuItem;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class VendorMenuItemController extends Controller
{
    /**
     * Display a listing of the vendor's menu items.
     */
    public function index(Request $request)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors can perform this action.',
            ], 403);
        }

        $items = MenuItem::where('vendor_id', $user->id)->get();

        return response()->json([
            'success' => true,
            'menu_items' => $items,
        ], 200);
    }

    /**
     * Store a newly created menu item in storage.
     */
    public function store(Request $request)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors can perform this action.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'food_name' => 'required|string|max:255',
            'price' => 'required|numeric|min:0',
            'description' => 'nullable|string',
            'category' => 'nullable|string|max:255',
            'is_available' => 'nullable|boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error.',
                'errors' => $validator->errors(),
            ], 400);
        }

        $item = DB::transaction(function () use ($request, $user) {
            $foodName = $request->input('food_name');

            $createdItem = MenuItem::create([
                'vendor_id' => $user->id,
                'food_name' => $foodName,
                'name' => $foodName,
                'price' => $request->input('price'),
                'description' => $request->input('description') ?? '',
                'category' => $request->input('category'),
                'is_available' => $request->input('is_available', true),
            ]);

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_CREATED',
                'details' => "Vendor added new food item '{$createdItem->food_name}' under category '{$createdItem->category}' at GH₵{$createdItem->price} through Vendor CRUD.",
            ]);

            return $createdItem;
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item successfully created.',
            'menu_item' => $item,
        ], 201);
    }

    /**
     * Display the specified menu item if owned by the vendor.
     */
    public function show(Request $request, $id)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors can perform this action.',
            ], 403);
        }

        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        if ($item->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu item.',
            ], 403);
        }

        return response()->json([
            'success' => true,
            'menu_item' => $item,
        ], 200);
    }

    /**
     * Update the specified menu item in storage.
     */
    public function update(Request $request, $id)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors can perform this action.',
            ], 403);
        }

        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        if ($item->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu item.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'food_name' => 'nullable|string|max:255',
            'price' => 'nullable|numeric|min:0',
            'description' => 'nullable|string',
            'category' => 'nullable|string|max:255',
            'is_available' => 'nullable|boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error.',
                'errors' => $validator->errors(),
            ], 400);
        }

        $updated = DB::transaction(function () use ($item, $request, $user) {
            $foodName = $request->input('food_name') ?? $item->food_name;

            $item->update([
                'food_name' => $foodName,
                'name' => $foodName,
                'price' => $request->has('price') ? $request->input('price') : $item->price,
                'description' => $request->has('description') ? $request->input('description') : $item->description,
                'category' => $request->has('category') ? $request->input('category') : $item->category,
                'is_available' => $request->has('is_available') ? $request->input('is_available') : $item->is_available,
            ]);

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_UPDATED',
                'details' => "Vendor updated food item '{$item->food_name}' via Vendor CRUD.",
            ]);

            return $item;
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item updated successfully.',
            'menu_item' => $updated,
        ], 200);
    }

    /**
     * Remove the specified menu item from storage.
     */
    public function destroy(Request $request, $id)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors can perform this action.',
            ], 403);
        }

        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        if ($item->vendor_id !== $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu item.',
            ], 403);
        }

        DB::transaction(function () use ($item, $user) {
            $item->delete();

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_DELETED',
                'details' => "Vendor deleted menu item ID: {$item->id} ('{$item->food_name}') via Vendor CRUD.",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item deleted successfully.',
        ], 200);
    }
}
