<?php

namespace App\Observers;

use App\Models\Order;
use Illuminate\Support\Facades\Auth;
use LogicException;

/**
 * Central server-side guard for order creation and status changes.
 *
 * Controllers may expose more than one order endpoint, so the important
 * invariants are enforced again at the model boundary.
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

    public function creating(Order $order): void
    {
        $actor = Auth::user();
        if (! $actor || strtoupper((string) $actor->role) !== 'STUDENT') {
            return;
        }

        // Never trust customer/student/user IDs supplied by a student client.
        // The authenticated account is the sole owner of the new order.
        $order->customer_id = $actor->id;
        $order->student_id = $actor->id;
        $order->user_id = $actor->id;

        $status = strtoupper(trim((string) ($order->status ?: 'PENDING')));
        if (! in_array($status, ['PENDING', 'ORDER_PLACED'], true)) {
            throw new LogicException('A student order must start in PENDING or ORDER_PLACED status.');
        }
    }

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
