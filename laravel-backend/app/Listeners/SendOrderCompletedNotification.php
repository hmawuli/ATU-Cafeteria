<?php

namespace App\Listeners;

use App\Events\OrderStatusCompleted;
use App\Models\User;
use App\Notifications\OrderCompletedNotification;

class SendOrderCompletedNotification
{
    /**
     * Handle the event.
     *
     * @param  \App\Events\OrderStatusCompleted  $event
     * @return void
     */
    public function handle(OrderStatusCompleted $event)
    {
        $order = $event->order;

        // Retrieve the student user who placed the order and notify them
        $studentId = $order->user_id ?? $order->customer_id; // Standardized or legacy ID
        if ($studentId) {
            $student = User::find($studentId);
            if ($student) {
                $student->notify(new OrderCompletedNotification($order));
            }
        }
    }
}
