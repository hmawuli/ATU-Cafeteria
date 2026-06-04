<?php

namespace App\Notifications;

use App\Models\Order;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Notification;

class NewIncomingOrderNotification extends Notification
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
        $customerName = 'A student';
        if ($this->order->customer) {
            $customerName = $this->order->customer->fullName;
        }

        return [
            'order_id' => $this->order->id,
            'customer_id' => $this->order->customer_id,
            'food_name' => $this->order->food_name,
            'quantity' => $this->order->quantity,
            'total_price' => $this->order->total_price,
            'message' => "New Order Alert! Pre-order #{$this->order->id} for '{$this->order->food_name}' (QTY: {$this->order->quantity}) placed by {$customerName}. Total: GH₵" . number_format($this->order->total_price, 2) . ".",
            'time' => date('Y-m-d H:i:s'),
        ];
    }
}
