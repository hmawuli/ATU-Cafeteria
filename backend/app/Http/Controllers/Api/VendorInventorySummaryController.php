<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\FoodItem;
use App\Models\MenuItem;
use Illuminate\Http\Request;

class VendorInventorySummaryController extends Controller
{
    public function index(Request $request)
    {
        $vendor = $request->user();
        if (! $vendor || strtoupper((string) $vendor->role) !== 'VENDOR') {
            return response()->json(['success' => false, 'message' => 'Vendor access required.'], 403);
        }

        $menu = MenuItem::where('vendor_id', $vendor->id)->get();
        $food = FoodItem::where('vendor_id', $vendor->id)->get();

        $rows = [];
        foreach ($menu as $item) {
            $stock = $item->current_stock;
            $threshold = (int) $item->low_stock_threshold;
            $rows[] = [
                'type' => 'MENU',
                'id' => $item->id,
                'name' => $item->name ?: $item->food_name,
                'stock' => $stock === null ? null : (int) $stock,
                'threshold' => $threshold,
                'status' => $stock === null ? 'UNTRACKED' : ($stock <= 0 ? 'OUT_OF_STOCK' : ($stock <= $threshold ? 'LOW_STOCK' : 'HEALTHY')),
                'available' => (bool) $item->is_available,
            ];
        }

        foreach ($food as $item) {
            $rows[] = [
                'type' => 'FOOD',
                'id' => $item->id,
                'name' => $item->name,
                'stock' => null,
                'threshold' => (int) $item->low_stock_threshold,
                'status' => $item->is_available ? 'AVAILABLE' : 'UNAVAILABLE',
                'available' => (bool) $item->is_available,
            ];
        }

        $tracked = collect($rows)->whereNotNull('stock');
        $out = $tracked->where('stock', '<=', 0)->count();
        $low = $tracked->where('stock', '>', 0)->filter(fn ($r) => $r['stock'] <= $r['threshold'])->count();

        return response()->json([
            'success' => true,
            'summary' => [
                'total_items' => count($rows),
                'tracked_items' => $tracked->count(),
                'low_stock' => $low,
                'out_of_stock' => $out,
                'available_items' => collect($rows)->where('available', true)->count(),
            ],
            'items' => $rows,
            'generated_at' => now()->toIso8601String(),
        ]);
    }
}
