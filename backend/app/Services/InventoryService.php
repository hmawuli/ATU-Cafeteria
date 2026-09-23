<?php

namespace App\Services;

use App\Models\InventoryMovement;
use App\Models\MenuItem;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use RuntimeException;

class InventoryService
{
    public function adjust(MenuItem $item, int $delta, string $type, ?int $performedBy = null, ?int $orderId = null, ?string $reason = null): MenuItem
    {
        if ($delta === 0) return $item;

        return DB::transaction(function () use ($item, $delta, $type, $performedBy, $orderId, $reason) {
            $locked = MenuItem::whereKey($item->id)->lockForUpdate()->firstOrFail();
            $current = (int) ($locked->current_stock ?? 0);
            $next = $current + $delta;

            if ($next < 0) {
                throw new RuntimeException('Stock cannot become negative.');
            }

            $locked->current_stock = $next;
            if ($next > 0) $locked->is_available = true;
            if ($next === 0) $locked->is_available = false;
            $locked->save();

            InventoryMovement::create([
                'vendor_id' => $locked->vendor_id,
                'menu_item_id' => $locked->id,
                'order_id' => $orderId,
                'type' => strtoupper($type),
                'quantity' => $delta,
                'balance_after' => $next,
                'reference' => 'INV-'.strtoupper(Str::random(12)),
                'reason' => $reason,
                'performed_by' => $performedBy,
            ]);

            return $locked;
        });
    }
}
