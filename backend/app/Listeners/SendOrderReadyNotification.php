<?php

namespace App\Listeners;

use App\Events\OrderStatusReady;
use App\Models\User;
use App\Models\CustomerDevice;
use App\Notifications\OrderReadyNotification;
use App\Services\FcmService;

class SendOrderReadyNotification
{
    /**
     * Handle the event.
     *
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

                $title = 'Order Ready for Pickup! 🍔';
                $body = "Your order #{$order->id} ('{$order->food_name}') is ready.";
                $data = [
                    'order_id' => strval($order->id),
                    'status' => 'READY',
                ];

                CustomerDevice::where('customer_id', $student->id)
                    ->active()
                    ->whereNotNull('push_token')
                    ->get()
                    ->each(function (CustomerDevice $device) use ($title, $body, $data) {
                        FcmService::sendPush($device->push_token, $title, $body, $data);
                    });
            }
        }
    }
}
