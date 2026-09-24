<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\InventoryMovement;
use App\Models\MenuItem;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class VendorMenuItemController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json(['success' => false, 'message' => 'Unauthorized. Only vendors can perform this action.'], 403);
        }

        return response()->json([
            'success' => true,
            'menu_items' => MenuItem::where('vendor_id', $user->id)->orderBy('name')->get(),
        ], 200);
    }

    public function store(Request $request)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json(['success' => false, 'message' => 'Unauthorized. Only vendors can perform this action.'], 403);
        }

        $validator = Validator::make($request->all(), [
            'food_name' => 'required|string|max:255',
            'price' => 'required|numeric|min:0',
            'description' => 'nullable|string|max:5000',
            'category' => 'nullable|string|max:255',
            'is_available' => 'nullable|boolean',
            'initial_stock' => 'nullable|integer|min:0|max:1000000',
            'low_stock_threshold' => 'nullable|integer|min:0|max:1000000',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Validation error.', 'errors' => $validator->errors()], 422);
        }

        $item = DB::transaction(function () use ($request, $user) {
            $foodName = trim((string) $request->input('food_name'));
            $initialStock = $request->has('initial_stock') ? (int) $request->input('initial_stock') : null;
            $threshold = $request->has('low_stock_threshold') ? (int) $request->input('low_stock_threshold') : 0;

            $createdItem = MenuItem::create([
                'vendor_id' => $user->id,
                'food_name' => $foodName,
                'name' => $foodName,
                'price' => $request->input('price'),
                'description' => $request->input('description') ?? '',
                'category' => $request->input('category') ?: 'General',
                'is_available' => $request->input('is_available', true),
                'initial_stock' => $initialStock,
                'current_stock' => $initialStock,
                'low_stock_threshold' => $threshold,
            ]);

            if ($initialStock !== null && $initialStock > 0) {
                InventoryMovement::create([
                    'vendor_id' => $user->id,
                    'menu_item_id' => $createdItem->id,
                    'type' => 'RESTOCK',
                    'quantity' => $initialStock,
                    'balance_after' => $initialStock,
                    'reference' => 'OPEN-'.strtoupper(bin2hex(random_bytes(6))),
                    'reason' => 'Opening stock recorded when menu item was created.',
                    'performed_by' => $user->id,
                ]);
            }

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_CREATED',
                'details' => "Vendor added menu item '{$createdItem->food_name}' under category '{$createdItem->category}' at GH₵{$createdItem->price}.",
            ]);

            return $createdItem;
        });

        return response()->json(['success' => true, 'message' => 'Menu item successfully created.', 'menu_item' => $item], 201);
    }

    public function show(Request $request, $id)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json(['success' => false, 'message' => 'Unauthorized. Only vendors can perform this action.'], 403);
        }

        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json(['success' => false, 'message' => 'Menu item not found.'], 404);
        }
        if ((int) $item->vendor_id !== (int) $user->id) {
            return response()->json(['success' => false, 'message' => 'Unauthorized. You do not own this menu item.'], 403);
        }

        return response()->json(['success' => true, 'menu_item' => $item], 200);
    }

    public function update(Request $request, $id)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json(['success' => false, 'message' => 'Unauthorized. Only vendors can perform this action.'], 403);
        }

        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json(['success' => false, 'message' => 'Menu item not found.'], 404);
        }
        if ((int) $item->vendor_id !== (int) $user->id) {
            return response()->json(['success' => false, 'message' => 'Unauthorized. You do not own this menu item.'], 403);
        }

        $validator = Validator::make($request->all(), [
            'food_name' => 'nullable|string|max:255',
            'price' => 'nullable|numeric|min:0',
            'description' => 'nullable|string|max:5000',
            'category' => 'nullable|string|max:255',
            'is_available' => 'nullable|boolean',
            'low_stock_threshold' => 'nullable|integer|min:0|max:1000000',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Validation error.', 'errors' => $validator->errors()], 422);
        }

        $updated = DB::transaction(function () use ($item, $request, $user) {
            $foodName = trim((string) ($request->input('food_name') ?? $item->food_name));
            $item->update([
                'food_name' => $foodName,
                'name' => $foodName,
                'price' => $request->has('price') ? $request->input('price') : $item->price,
                'description' => $request->has('description') ? $request->input('description') : $item->description,
                'category' => $request->has('category') ? ($request->input('category') ?: 'General') : $item->category,
                'is_available' => $request->has('is_available') ? $request->input('is_available') : $item->is_available,
                'low_stock_threshold' => $request->has('low_stock_threshold') ? $request->input('low_stock_threshold') : $item->low_stock_threshold,
            ]);

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_UPDATED',
                'details' => "Vendor updated menu item '{$item->food_name}'.",
            ]);

            return $item;
        });

        return response()->json(['success' => true, 'message' => 'Menu item updated successfully.', 'menu_item' => $updated], 200);
    }

    public function destroy(Request $request, $id)
    {
        $user = $request->user();
        if (strtoupper($user->role) !== 'VENDOR') {
            return response()->json(['success' => false, 'message' => 'Unauthorized. Only vendors can perform this action.'], 403);
        }

        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json(['success' => false, 'message' => 'Menu item not found.'], 404);
        }
        if ((int) $item->vendor_id !== (int) $user->id) {
            return response()->json(['success' => false, 'message' => 'Unauthorized. You do not own this menu item.'], 403);
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

        return response()->json(['success' => true, 'message' => 'Menu item deleted successfully.'], 200);
    }
}
