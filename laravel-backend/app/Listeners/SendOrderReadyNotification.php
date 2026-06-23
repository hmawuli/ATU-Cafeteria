<?php

namespace App\Listeners;

use App\Events\OrderStatusReady;
use App\Models\User;
use App\Notifications\OrderReadyNotification;

class SendOrderReadyNotification
{
    /**
     * Handle the event.
     *
     * @param  \App\Events\OrderStatusReady  $event
     * @return void
     */
    public function handle(OrderStatusReady $event)
    {
        $order = $event->order;

        // Retrieve the student user who placed the order and notify them
        $studentId = $order->user_id ?? $order->customer_id; // Standardized or legacy ID
        if ($studentId) {
            $student = User::find($studentId);
            if ($student) {
                $student->notify(new OrderReadyNotification($order));
            }
        }
    }
}
