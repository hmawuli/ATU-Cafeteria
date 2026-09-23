<?php

namespace App\Services;

use App\Models\MenuItem;
use Illuminate\Support\Facades\DB;
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



            return $locked;
        });
    }
}
