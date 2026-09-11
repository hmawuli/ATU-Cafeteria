<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\MenuItem;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class MenuItemController extends Controller
{
    /**
     * List all menu items.
     */
    public function index(Request $request)
    {
        $query = MenuItem::with('vendor');
        if ($request->has('vendor_id')) {
            $query->where('vendor_id', $request->query('vendor_id'));
        }
        $items = $query->get();
        return response()->json([
            'success' => true,
            'menu_items' => $items
        ], 200);
    }

    /**
     * Show a single menu item.
     */
    public function show($id)
    {
        $item = MenuItem::with('vendor')->find($id);
        if (!$item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.'
            ], 404);
        }

        return response()->json([
            'success' => true,
            'menu_item' => $item
        ], 200);
    }

    /**
     * Store/create a new menu item.
     */
    public function store(\App\Http\Requests\StoreMenuItemRequest $request)
    {
        $user = $request->user();
        
        // Ensure user is VENDOR or ADMIN
        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Only vendors and admins are authorized to add menu items.'
            ], 403);
        }

        // Default vendor_id to log-in user's ID
        $vendorId = $user->id;
        if (strtoupper($user->role) === 'ADMIN' && $request->has('vendor_id')) {
            $vendorId = $request->input('vendor_id');
        }

        $item = DB::transaction(function () use ($request, $vendorId) {
            $foodName = $request->input('food_name');
            
            $createdItem = MenuItem::create([
                'vendor_id' => $vendorId,
                'food_name' => $foodName,
                'name' => $foodName, // duplicate to name for compatibility
                'price' => $request->input('price'),
                'description' => $request->input('description') ?? '',
                'category' => $request->input('category'),
                'is_available' => $request->input('is_available', true)
            ]);

            AuditLog::create([
                'user_id' => $request->user()->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_CREATED',
                'details' => "Vendor added new food item '{$createdItem->food_name}' under category '{$createdItem->category}' at GH₵{$createdItem->price}.",
            ]);

            return $createdItem;
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item successfully created.',
            'menu_item' => $item
        ], 201);
    }

    /**
     * Update an existing menu item.
     */
    public function update(Request $request, $id)
    {
        $item = MenuItem::find($id);
        if (!$item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.'
            ], 404);
        }

        $user = $request->user();
        if ($item->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu item.'
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'food_name' => 'nullable|string|max:255',
            'name' => 'nullable|string|max:255',
            'price' => 'nullable|numeric|min:0',
            'description' => 'nullable|string',
            'category' => 'nullable|string|max:255',
            'is_available' => 'nullable|boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'errors' => $validator->errors()
            ], 400);
        }

        $updated = DB::transaction(function () use ($item, $request) {
            $foodName = $request->input('food_name') ?? $request->input('name') ?? $item->food_name;

            $item->update([
                'food_name' => $foodName,
                'name' => $foodName,
                'price' => $request->has('price') ? $request->input('price') : $item->price,
                'description' => $request->has('description') ? $request->input('description') : $item->description,
                'category' => $request->has('category') ? $request->input('category') : $item->category,
                'is_available' => $request->has('is_available') ? $request->input('is_available') : $item->is_available,
            ]);

            AuditLog::create([
                'user_id' => $request->user()->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_UPDATED',
                'details' => "Vendor updated food item '{$item->food_name}' (price: GH₵{$item->price}, category: '{$item->category}').",
            ]);

            return $item;
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item updated successfully.',
            'menu_item' => $updated
        ], 200);
    }

    /**
     * Delete/remove an existing menu item.
     */
    public function destroy(Request $request, $id)
    {
        $item = MenuItem::find($id);
        if (!$item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.'
            ], 404);
        }

        $user = $request->user();
        if ($item->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu item.'
            ], 403);
        }

        DB::transaction(function () use ($item, $user) {
            $item->delete();

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_DELETED',
                'details' => "Vendor deleted menu item ID: {$item->id} ('{$item->food_name}').",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item deleted successfully.'
        ], 200);
    }

    /**
     * Delete/remove an existing menu item (Admin only).
     */
    public function destroyAdmin(Request $request, $id)
    {
        $item = MenuItem::find($id);
        if (!$item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.'
            ], 404);
        }

        $user = $request->user();
        if (!$user || strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only administrative personnel can delete menu items.'
            ], 403);
        }

        DB::transaction(function () use ($item, $user) {
            $item->delete(); // Soft delete

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_DELETED',
                'details' => "Administrator deleted menu item ID: {$item->id} ('{$item->food_name}').",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item deleted successfully.'
        ], 200);
    }
}
