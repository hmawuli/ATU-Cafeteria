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

                // Dispatch FCM Push notification
                $fcmToken = null;
                if ($student->profile_info && is_array($student->profile_info)) {
                    $fcmToken = $student->profile_info['fcm_token'] ?? null;
                }

                // Fallback token for simulation/development testing
                if (!$fcmToken) {
                    $fcmToken = "simulated-fcm-token-student-id-{$student->id}";
                }

                $title = "Order Ready for Pickup! 🍔";
                $body = "Your order #{$order->id} ('{$order->food_name}') is ready! Pickup PIN: {$order->pickup_pin}.";
                $data = [
                    'order_id' => strval($order->id),
                    'pickup_pin' => strval($order->pickup_pin),
                    'status' => 'READY',
                ];

                \App\Services\FcmService::sendPush($fcmToken, $title, $body, $data);
            }
        }
    }
}
