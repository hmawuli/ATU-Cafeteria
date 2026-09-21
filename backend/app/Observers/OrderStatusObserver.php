<?php

namespace App\Observers;

use App\Models\Order;
use Illuminate\Support\Facades\Auth;
use LogicException;

/**
 * Central server-side guard for order status changes.
 *
 * Controllers may expose more than one status endpoint, so the lifecycle is
 * enforced again at the model boundary. This prevents a newly added endpoint
 * or bulk operation from accidentally allowing a status to be skipped.
 */
final class OrderStatusObserver
{
    private const TRANSITIONS = [
        'PENDING' => ['ORDER_PLACED', 'PREPARING', 'DECLINED', 'CANCELLED'],
        'ORDER_PLACED' => ['PREPARING', 'DECLINED', 'CANCELLED'],
        'PREPARING' => ['READY', 'CANCELLED'],
        'READY' => ['OUT_FOR_DELIVERY', 'COMPLETED'],
        'OUT_FOR_DELIVERY' => ['COMPLETED'],
        'DELIVERED' => ['COMPLETED'],
        'COMPLETED' => [],
        'DECLINED' => [],
        'CANCELLED' => [],
    ];

    public function saving(Order $order): void
    {
        if (! $order->exists || ! $order->isDirty('status')) {
            return;
        }

        $oldStatus = strtoupper(trim((string) $order->getOriginal('status')));
        $newStatus = strtoupper(trim((string) $order->status));

        if ($oldStatus === $newStatus) {
            return;
        }

        // Administrative intervention is intentionally allowed to repair or
        // reconcile an order. All vendor/student transitions remain strict.
        $actor = Auth::user();
        if ($actor && strtoupper((string) $actor->role) === 'ADMIN') {
            return;
        }

        if (! in_array($newStatus, self::TRANSITIONS[$oldStatus] ?? [], true)) {
            throw new LogicException("Invalid order status transition from {$oldStatus} to {$newStatus}.");
        }
    }
}
