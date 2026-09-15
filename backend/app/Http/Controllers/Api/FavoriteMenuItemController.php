<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\FavoriteMenuItem;
use App\Models\MenuItem;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class FavoriteMenuItemController extends Controller
{
    /**
     * Get all favorite menu items of the authenticated student.
     */
    public function index(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $favorites = FavoriteMenuItem::where('user_id', $user->id)
            ->with(['menuItem.vendor'])
            ->get();

        // Map and extract menu items with safety checks for deleted items
        $items = $favorites->map(function ($fav) {
            if (! $fav->menuItem) {
                return null;
            }

            return [
                'favorite_id' => $fav->id,
                'menu_item_id' => $fav->menu_item_id,
                'name' => $fav->menuItem->name ?: $fav->menuItem->food_name,
                'price' => $fav->menuItem->price,
                'description' => $fav->menuItem->description,
                'category' => $fav->menuItem->category,
                'is_available' => $fav->menuItem->is_available,
                'vendor_id' => $fav->menuItem->vendor_id,
                'vendor_name' => $fav->menuItem->vendor ? $fav->menuItem->vendor->fullName : 'Unknown Vendor',
                'current_stock' => $fav->menuItem->current_stock,
                'favorited_at' => $fav->created_at->toIso8601String(),
            ];
        })->filter()->values();

        return response()->json([
            'success' => true,
            'favorites' => $items,
            'count' => count($items),
        ], 200);
    }

    /**
     * Mark a MenuItem as favorite.
     */
    public function store(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $validator = Validator::make($request->all(), [
            'menu_item_id' => 'required|integer',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $menuItemId = $request->input('menu_item_id');

        // Verify MenuItem exists
        $menuItem = MenuItem::find($menuItemId);
        if (! $menuItem) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 444); // Consistent with 404 but using standard error message
        }

        // Check if already favorited
        $existing = FavoriteMenuItem::where('user_id', $user->id)
            ->where('menu_item_id', $menuItemId)
            ->first();

        if ($existing) {
            return response()->json([
                'success' => true,
                'message' => 'Menu item is already in favorites.',
                'favorite_id' => $existing->id,
            ], 200);
        }

        $fav = FavoriteMenuItem::create([
            'user_id' => $user->id,
            'menu_item_id' => $menuItemId,
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Menu item marked as favorite successfully.',
            'favorite_id' => $fav->id,
            'menu_item' => [
                'id' => $menuItem->id,
                'name' => $menuItem->name ?: $menuItem->food_name,
                'price' => $menuItem->price,
            ],
        ], 201);
    }

    /**
     * Unfavorite a MenuItem.
     */
    public function destroy(Request $request, $id)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        // We support unfavoriting either by favorite_id (primary key) OR by menu_item_id
        $favorite = FavoriteMenuItem::where('user_id', $user->id)
            ->where(function ($query) use ($id) {
                $query->where('id', $id)
                    ->orWhere('menu_item_id', $id);
            })
            ->first();

        if (! $favorite) {
            return response()->json([
                'success' => false,
                'message' => 'Favorite record not found.',
            ], 404);
        }

        $favorite->delete();

        return response()->json([
            'success' => true,
            'message' => 'Menu item removed from favorites successfully.',
        ], 200);
    }
}
