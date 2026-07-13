<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Menu;
use App\Models\MenuItem;
use App\Models\AuditLog;
use App\Http\Requests\StoreMenuRequest;
use App\Http\Requests\UpdateMenuRequest;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class MenuController extends Controller
{
    /**
     * List all custom menu assignments.
     */
    public function index(Request $request)
    {
        $query = Menu::with(['vendor', 'foodItem']);
        if ($request->has('vendor_id')) {
            $query->where('vendor_id', $request->query('vendor_id'));
        }
        $menus = $query->get();
        return response()->json([
            'success' => true,
            'menus' => $menus
        ], 200);
    }

    /**
     * Get menu assignments for a specific vendor.
     */
    public function getVendorMenu($vendorId)
    {
        $menus = Menu::where('vendor_id', $vendorId)
            ->with(['foodItem'])
            ->get();

        return response()->json([
            'success' => true,
            'vendor_id' => $vendorId,
            'menus' => $menus
        ], 200);
    }

    /**
     * Store/link a FoodItem to a Vendor Menu.
     */
    public function store(StoreMenuRequest $request)
    {
        $user = $request->user();
        $vendorId = $request->input('vendor_id');

        if ($vendorId != $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You cannot register menu items for another vendor.'
            ], 403);
        }

        $foodItem = \App\Models\FoodItem::find($request->input('food_item_id'));
        if (!$foodItem || ($foodItem->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN')) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This food item does not belong to you.'
            ], 403);
        }

        $menu = DB::transaction(function () use ($request) {
            $menuEntry = Menu::updateOrCreate(
                [
                    'vendor_id' => $request->input('vendor_id'),
                    'food_item_id' => $request->input('food_item_id')
                ],
                [
                    'price' => $request->input('price'),
                    'description' => $request->input('description') ?? '',
                    'is_available' => $request->input('is_available', true)
                ]
            );

            // Fetch relations
            $menuEntry->load('foodItem');

            AuditLog::create([
                'user_id' => $request->user()->id ?? $request->input('vendor_id'),
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_ASSIGNED',
                'details' => "Assigned food item '{$menuEntry->foodItem->name}' to vendor menu list at price GH₵{$menuEntry->price}.",
            ]);

            return $menuEntry;
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item successfully registered.',
            'menu' => $menu
        ], 201);
    }

    /**
     * Update an existing vendor menu entry.
     */
    public function update(UpdateMenuRequest $request, $id)
    {
        $menu = Menu::find($id);
        if (!$menu) {
            return response()->json([
                'success' => false,
                'message' => 'Menu entry not found.'
            ], 404);
        }

        $user = $request->user();
        if ($menu->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu entry.'
            ], 403);
        }

        $menu = DB::transaction(function () use ($menu, $request) {
            $data = $request->only(['price', 'description', 'is_available']);
            $menu->update($data);

            AuditLog::create([
                'user_id' => $request->user()->id ?? $menu->vendor_id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_UPDATED',
                'details' => "Updated vendor menu entry (availability: " . ($menu->is_available ? 'Yes' : 'No') . ", price: GH₵{$menu->price}).",
            ]);

            return $menu;
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu item updated successfully.',
            'menu' => $menu
        ], 200);
    }

    /**
     * Remove a pivot menu link.
     */
    public function destroy($id)
    {
        $menu = Menu::find($id);
        if (!$menu) {
            return response()->json([
                'success' => false,
                'message' => 'Menu entry not found.'
            ], 404);
        }

        $user = request()->user();
        if ($menu->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You do not own this menu entry.'
            ], 403);
        }

        DB::transaction(function () use ($menu) {
            $menu->delete();

            AuditLog::create([
                'user_id' => $menu->vendor_id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_ITEM_REMOVED',
                'details' => "Eradicated menu entry ID: {$menu->id} from vendor menu list.",
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Menu entry deleted from vendor configuration.'
        ], 200);
    }

    // ==========================================
    // EXTRA OPERATIONS FOR GENERIC MENU ITEMS SYSTEM
    // ==========================================

    /**
     * List all items from the standalone menu_items table.
     */
    public function listMenuItems(Request $request)
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
     * Get standalone menu items for a specific vendor user.
     */
    public function getVendorMenuItems($vendorId)
    {
        $items = MenuItem::where('vendor_id', $vendorId)->get();
        return response()->json([
            'success' => true,
            'vendor_id' => $vendorId,
            'menu_items' => $items
        ], 200);
    }

    /**
     * Store a standalone menu item in menu_items table.
     */
    public function storeMenuItem(Request $request)
    {
        $user = $request->user();
        $vendorId = $request->input('vendor_id');

        if ($vendorId != $user->id && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. You cannot create standalone menu items for another vendor.'
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'vendor_id' => 'required|integer|exists:users,id',
            'name' => 'required|string|max:255',
            'price' => 'required|numeric|min:0',
            'description' => 'nullable|string',
            'is_available' => 'nullable|boolean',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'errors' => $validator->errors()
            ], 400);
        }

        $item = DB::transaction(function () use ($request) {
            $createdItem = MenuItem::create([
                'vendor_id' => $request->input('vendor_id'),
                'name' => $request->input('name'),
                'price' => $request->input('price'),
                'description' => $request->input('description') ?? '',
                'is_available' => $request->input('is_available', true)
            ]);

            AuditLog::create([
                'user_id' => $createdItem->vendor_id,
                'timestamp' => time() * 1000,
                'action' => 'STANDALONE_MENU_ITEM_CREATED',
                'details' => "Created standalone menu item '{$createdItem->name}' at GH₵{$createdItem->price}.",
            ]);

            return $createdItem;
        });

        return response()->json([
            'success' => true,
            'menu_item' => $item
        ], 201);
    }

    /**
     * Search and filter standalone menu items by category or price range.
     */
    public function search(Request $request)
    {
        $query = MenuItem::with('vendor');

        // Filter by Category
        if ($request->has('category') && $request->input('category') !== '') {
            $query->where('category', 'like', '%' . $request->input('category') . '%');
        }

        // Filter by Min Price
        if ($request->has('min_price') && is_numeric($request->input('min_price'))) {
            $query->where('price', '>=', (float) $request->input('min_price'));
        }

        // Filter by Max Price
        if ($request->has('max_price') && is_numeric($request->input('max_price'))) {
            $query->where('price', '<=', (float) $request->input('max_price'));
        }

        // General search query (on food_name, description, category)
        if ($request->has('q') && $request->input('q') !== '') {
            $search = $request->input('q');
            $query->where(function($q) use ($search) {
                $q->where('food_name', 'like', '%' . $search . '%')
                  ->orWhere('name', 'like', '%' . $search . '%')
                  ->orWhere('description', 'like', '%' . $search . '%')
                  ->orWhere('category', 'like', '%' . $search . '%');
            });
        }

        $items = $query->get();

        return response()->json([
            'success' => true,
            'menu_items' => $items
        ], 200);
    }
}
