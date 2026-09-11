<?php

namespace App\Notifications;

use App\Models\Order;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Notification;

class OrderStatusChangedNotification extends Notification
{
    use Queueable;

    /**
     * @var \App\Models\Order
     */
    protected $order;

    /**
     * @var string
     */
    protected $oldStatus;

    /**
     * @var string
     */
    protected $newStatus;

    /**
     * Create a new notification instance.
     *
     * @param  \App\Models\Order  $order
     * @param  string  $oldStatus
     * @param  string  $newStatus
     */
    public function __construct(Order $order, $oldStatus, $newStatus)
    {
        $this->order = $order;
        $this->oldStatus = $oldStatus;
        $this->newStatus = $newStatus;
    }

    /**
     * Get the notification's delivery channels.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function via($notifiable)
    {
        return ['database', 'broadcast'];
    }

    /**
     * Get the broadcastable representation of the notification.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function toBroadcast($notifiable)
    {
        return [
            'id' => $this->id,
            'type' => get_class($this),
            'notifiable_id' => $notifiable->id,
            'data' => $this->toDatabase($notifiable),
        ];
    }

    /**
     * Get the channels the event should broadcast on.
     *
     * @return array
     */
    public function broadcastOn()
    {
        $studentId = $this->order->customer_id ?? $this->order->student_id;
        return ['orders-student-' . $studentId];
    }

    /**
     * Get the array representation of the notification for the database.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function toDatabase($notifiable)
    {
        $statusMessage = "Your order #{$this->order->id} for '{$this->order->food_name}' has been updated to {$this->newStatus}.";
        
        switch (strtoupper($this->newStatus)) {
            case 'ORDER_PLACED':
                $statusMessage = "Your order #{$this->order->id} ('{$this->order->food_name}') has been successfully placed at the ATU Cafeteria!";
                break;
            case 'PREPARING':
                $statusMessage = "Chef is preparing your order #{$this->order->id} ('{$this->order->food_name}')! It will be ready soon.";
                break;
            case 'READY':
                $statusMessage = "Good news! Your order #{$this->order->id} ('{$this->order->food_name}') is READY for pickup. Secure Hand-off PIN: {$this->order->pickup_pin}.";
                break;
            case 'OUT_FOR_DELIVERY':
                $statusMessage = "Your order #{$this->order->id} ('{$this->order->food_name}') is OUT FOR DELIVERY! The rider is on their way.";
                break;
            case 'DELIVERED':
                $statusMessage = "Hurray! Your order #{$this->order->id} ('{$this->order->food_name}') has been delivered. Enjoy your delicious meal!";
                break;
            case 'COMPLETED':
                $statusMessage = "Hurray! Your order #{$this->order->id} has been picked up & marked as completed. Enjoy your delicious meal!";
                break;
            case 'DECLINED':
                $statusMessage = "We're sorry, your order #{$this->order->id} ('{$this->order->food_name}') was declined by the cafeteria vendor.";
                break;
            case 'CANCELLED':
                $statusMessage = "Order #{$this->order->id} ('{$this->order->food_name}') has been cancelled/voided.";
                break;
        }

        return [
            'order_id' => $this->order->id,
            'vendor_id' => $this->order->vendor_id,
            'total_price' => $this->order->total_price,
            'old_status' => $this->oldStatus,
            'new_status' => $this->newStatus,
            'message' => $statusMessage,
            'time' => date('Y-m-d H:i:s'),
        ];
    }
}
