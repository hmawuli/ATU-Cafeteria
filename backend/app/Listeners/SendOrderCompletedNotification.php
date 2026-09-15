<?php

namespace App\Listeners;

use App\Events\OrderStatusCompleted;
use App\Models\AuditLog;
use App\Models\User;
use App\Notifications\OrderCompletedNotification;

class SendOrderCompletedNotification
{
    /**
     * Handle the event.
     *
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

                // Award loyalty points based on total spending
                // Let's award 1 point for every 1.00 GHS of the total price
                $pointsEarned = intval(floor($order->total_price));
                if ($pointsEarned > 0) {
                    $student->loyalty_points = ($student->loyalty_points ?? 0) + $pointsEarned;
                    $student->total_spent = ($student->total_spent ?? 0.00) + floatval($order->total_price);
                    $student->save();

                    // Log audit entry
                    AuditLog::create([
                        'user_id' => $student->id,
                        'timestamp' => time() * 1000,
                        'action' => 'LOYALTY_POINTS_EARNED',
                        'details' => "Earned {$pointsEarned} loyalty points for completed Order #{$order->id} (Total paid: GHS {$order->total_price}). Current points balance: {$student->loyalty_points}.",
                    ]);
                }
            }
        }
    }
}
