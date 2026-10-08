<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\StoreMenuItemRequest;
use App\Models\AuditLog;
use App\Models\MenuItem;
use App\Support\MenuBadges;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class MenuItemController extends Controller
{
    /**
     * Cache key/tag for the default public catalogue response.
     */
    public const CATALOG_CACHE_KEY = 'catalog.menu_items.v1';

    private const CATALOG_CACHE_TTL = 300;

    /**
     * List all menu items.
     */
    public function index(Request $request)
    {
        // Optional pagination: ?page=2&per_page=50. When omitted the full
        // catalogue is returned, preserving legacy client behaviour.
        $page = $request->integer('page', 0);
        $perPage = max(1, min(200, $request->integer('per_page', 50)));

        // The default catalogue is the hottest, most stable response — cache
        // it (invalidated on every menu-item create/update/delete/restore).
        if ($page === 0 && ! $request->has('vendor_id')) {
            $payload = Cache::remember(self::CATALOG_CACHE_KEY, self::CATALOG_CACHE_TTL, function () {
                return [
                    'success' => true,
                    'menu_items' => $this->catalogueRows(MenuItem::with('vendor')->get())->all(),
                ];
            });

            return response()->json($payload, 200);
        }

        $query = MenuItem::with('vendor');
        if ($request->has('vendor_id')) {
            $query->where('vendor_id', $request->query('vendor_id'));
        }

        if ($page > 0) {
            $paginated = $query->paginate($perPage, ['*'], 'page', $page);

            return response()->json([
                'success' => true,
                'menu_items' => $this->catalogueRows(collect($paginated->items()))->values(),
                'pagination' => [
                    'current_page' => $paginated->currentPage(),
                    'per_page' => $paginated->perPage(),
                    'last_page' => $paginated->lastPage(),
                    'total' => $paginated->total(),
                ],
            ], 200);
        }

        $items = $query->get();

        return response()->json([
            'success' => true,
            'menu_items' => $this->catalogueRows($items)->values(),
        ], 200);
    }

    /**
     * Serialize menu items with their derived health/sustainability badges.
     */
    private function catalogueRows($items)
    {
        return $items->map(function (MenuItem $item) {
            $data = $item->toArray();
            $data['vendor_id'] = $item->vendor_id;
            $data['badges'] = MenuBadges::for($item);

            return $data;
        });
    }

    /**
     * Show a single menu item.
     */
    public function show($id)
    {
        $item = MenuItem::with('vendor')->find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        return response()->json([
            'success' => true,
            'menu_item' => $item,
        ], 200);
    }

    /**
     * Store/create a new menu item.
     */
    public function store(StoreMenuItemRequest $request)
    {
        $user = $request->user();

        // Ensure user is VENDOR or ADMIN
        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Only vendors and admins are authorized to add menu items.',
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
                'is_available' => $request->input('is_available', true),
                'image_url' => $request->input('image_url'),
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
            'menu_item' => $item,
        ], 201);
    }

    /**
     * Update an existing menu item.
     */
    public function update(Request $request, $id)
    {
        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        $user = $request->user();
        if ($item->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu item.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'food_name' => 'nullable|string|max:255',
            'name' => 'nullable|string|max:255',
            'price' => 'nullable|numeric|min:0',
            'description' => 'nullable|string',
            'category' => 'nullable|string|max:255',
            'is_available' => 'nullable|boolean',
            'image_url' => 'nullable|string|max:3000000',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'errors' => $validator->errors(),
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
                'image_url' => $request->has('image_url') ? $request->input('image_url') : $item->image_url,
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
            'menu_item' => $updated,
        ], 200);
    }

    /**
     * Delete/remove an existing menu item.
     */
    public function destroy(Request $request, $id)
    {
        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        $user = $request->user();
        if ($item->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
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
                'details' => "Vendor deleted menu item ID: {$item->id} ('{$item->food_name}').",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item deleted successfully.',
        ], 200);
    }

    /**
     * Delete/remove an existing menu item (Admin only).
     */
    public function destroyAdmin(Request $request, $id)
    {
        $item = MenuItem::find($id);
        if (! $item) {
            return response()->json([
                'success' => false,
                'message' => 'Menu item not found.',
            ], 404);
        }

        $user = $request->user();
        if (! $user || strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only administrative personnel can delete menu items.',
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
            'message' => 'Menu item deleted successfully.',
        ], 200);
    }
}
