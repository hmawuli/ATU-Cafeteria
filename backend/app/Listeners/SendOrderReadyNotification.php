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

        // Retrieve the customer who placed the order and notify them
        $customerId = $order->customer_id ?? $order->user_id; // Standardized customer ID with legacy fallback
        if ($customerId) {
            $customer = User::find($customerId);
            if ($customer) {
                $customer->notify(new OrderReadyNotification($order));

                $title = 'Order Ready for Pickup! 🍔';
                $body = "Your order #{$order->id} ('{$order->food_name}') is ready.";
                $data = [
                    'order_id' => strval($order->id),
                    'status' => 'READY',
                ];

                CustomerDevice::where('customer_id', $customer->id)
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
