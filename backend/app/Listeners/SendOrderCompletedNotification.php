<?php

namespace App\Listeners;

use App\Events\OrderStatusCompleted;
use App\Models\AuditLog;
use App\Models\CustomerDevice;
use App\Models\User;
use App\Services\FcmService;
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

                $alreadyAwarded = AuditLog::where('user_id', $student->id)
                    ->where('action', 'LOYALTY_POINTS_EARNED')
                    ->where('details', 'like', '%completed Order #'.$order->id.'%')
                    ->exists();

                if (! $alreadyAwarded) {
                    $pointsEarned = intval(floor((float) ($order->grand_total ?: $order->total_price)));
                    if ($pointsEarned > 0) {
                        $student->loyalty_points = ($student->loyalty_points ?? 0) + $pointsEarned;
                        $student->total_spent = ($student->total_spent ?? 0.00) + floatval($order->grand_total ?: $order->total_price);
                        $student->save();

                        AuditLog::create([
                            'user_id' => $student->id,
                            'timestamp' => time() * 1000,
                            'action' => 'LOYALTY_POINTS_EARNED',
                            'details' => "Earned {$pointsEarned} loyalty points for completed Order #{$order->id} (Total paid: GHS ".($order->grand_total ?: $order->total_price)."). Current points balance: {$student->loyalty_points}.",
                        ]);
                    }
                }

                $title = 'Order completed';
                $body = "Order #".($order->order_number ?: $order->id)." has been picked up. Thank you for ordering with ATU Cafeteria.";
                $data = [
                    'order_id' => (string) $order->id,
                    'order_number' => (string) ($order->order_number ?: $order->id),
                    'status' => 'COMPLETED',
                ];

                CustomerDevice::where('customer_id', $student->id)
                    ->active()
                    ->whereNotNull('push_token')
                    ->get()
                    ->each(function (CustomerDevice $device) use ($title, $body, $data) {
                        $delivery = FcmService::sendPushResult($device->push_token, $title, $body, $data);
                        if (($delivery['invalid_token'] ?? false) === true) {
                            $device->update([
                                'revoked_at' => now(),
                                'push_token' => null,
                            ]);
                        }
                    });
            }
        }
    }
}
