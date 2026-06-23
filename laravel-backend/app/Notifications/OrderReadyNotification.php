<?php

namespace App\Notifications;

use App\Models\Order;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Notification;

class OrderReadyNotification extends Notification
{
    use Queueable;

    /**
     * @var \App\Models\Order
     */
    protected $order;

    /**
     * Create a new notification instance.
     *
     * @param  \App\Models\Order  $order
     */
    public function __construct(Order $order)
    {
        $this->order = $order;
    }

    /**
     * Get the notification's delivery channels.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function via($notifiable)
    {
        return ['database'];
    }

    /**
     * Get the array representation of the notification for the database.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function toDatabase($notifiable)
    {
        return [
            'order_id' => $this->order->id,
            'vendor_id' => $this->order->vendor_id,
            'total_price' => $this->order->total_price,
            'message' => "Your order #{$this->order->id} for '{$this->order->food_name}' is READY! Please use pickup PIN: {$this->order->pickup_pin} at the counter.",
            'time' => date('Y-m-d H:i:s'),
        ];
    }
}
