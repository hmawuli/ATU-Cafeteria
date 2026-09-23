<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\InventoryMovement;
use App\Models\MenuItem;
use App\Services\InventoryService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class InventoryController extends Controller
{
    public function index(Request $request)
    {
        $user = $request->user();
        $query = InventoryMovement::with(['menuItem','performedBy'])->latest();

        if (strtoupper((string) $user->role) === 'VENDOR') {
            $query->where('vendor_id', $user->id);
        } elseif (strtoupper((string) $user->role) !== 'ADMIN') {
            abort(403);
        }

        return response()->json(['success' => true, 'movements' => $query->paginate(50)]);
    }

    public function adjust(Request $request, InventoryService $inventory)
    {
        $user = $request->user();

        $validator = Validator::make($request->all(), [
            'menu_item_id' => 'required|integer|exists:menu_items,id',
            'quantity' => 'required|integer|not_in:0',
            'reason' => 'nullable|string|max:255',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid inventory adjustment.', 'errors' => $validator->errors()], 422);
        }

        $item = MenuItem::findOrFail((int) $request->input('menu_item_id'));
        if (strtoupper((string) $user->role) === 'VENDOR' && (int) $item->vendor_id !== (int) $user->id) {
            abort(403);
        }

        $updated = $inventory->adjust(
            $item,
            (int) $request->input('quantity'),
            'ADJUSTMENT',
            (int) $user->id,
            null,
            $request->input('reason')
        );

        return response()->json(['success' => true, 'message' => 'Inventory updated.', 'menu_item' => $updated]);
    }
}
