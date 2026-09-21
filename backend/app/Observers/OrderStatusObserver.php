<?php

namespace App\Observers;

use App\Models\Order;
use Illuminate\Support\Facades\Auth;
use LogicException;

/**
 * Central server-side guard for order creation and status changes.
 */
final class OrderStatusObserver
{
    private const TRANSITIONS = [
        'PENDING' => ['ORDER_PLACED', 'PREPARING', 'DECLINED', 'CANCELLED'],
        'ORDER_PLACED' => ['PREPARING', 'DECLINED', 'CANCELLED'],
        'PREPARING' => ['READY', 'READY_FOR_PICKUP', 'CANCELLED'],
        'READY' => ['OUT_FOR_DELIVERY', 'COMPLETED'],
        'READY_FOR_PICKUP' => ['COMPLETED', 'OUT_FOR_DELIVERY'],
        'OUT_FOR_DELIVERY' => ['COMPLETED', 'DELIVERED'],
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

        $actor = Auth::user();
        if ($actor && strtoupper((string) $actor->role) === 'ADMIN') {
            return;
        }

        if (! in_array($newStatus, self::TRANSITIONS[$oldStatus] ?? [], true)) {
            throw new LogicException("Invalid order status transition from {$oldStatus} to {$newStatus}.");
        }
    }
}
