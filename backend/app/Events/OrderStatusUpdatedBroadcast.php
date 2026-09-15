<?php

namespace App\Events;

use App\Models\Order;
use Illuminate\Broadcasting\Channel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcastNow;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class OrderStatusUpdatedBroadcast implements ShouldBroadcastNow
{
    use Dispatchable, SerializesModels;

    public $order;

    public $oldStatus;

    public $newStatus;

    /**
     * Create a new event instance.
     *
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
     * Get the channels the event should broadcast on.
     *
     * @return Channel|array
     */
    public function broadcastOn()
    {
        $studentId = $this->order->customer_id ?? $this->order->student_id;

        return new Channel('orders-student-'.$studentId);
    }

    /**
     * The event's broadcast name.
     *
     * @return string
     */
    public function broadcastAs()
    {
        return 'order.status.updated';
    }

    /**
     * Get the data to broadcast.
     *
     * @return array
     */
    public function broadcastWith()
    {
        $statusMessage = "Your order #{$this->order->id} for '{$this->order->food_name}' has been updated to {$this->newStatus}.";

        switch (strtoupper($this->newStatus)) {
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
            'food_name' => $this->order->food_name,
            'old_status' => $this->oldStatus,
            'new_status' => $this->newStatus,
            'message' => $statusMessage,
            'pickup_pin' => $this->order->pickup_pin,
            'estimated_pickup_time' => $this->order->estimated_pickup_time,
            'timestamp' => time() * 1000,
        ];
    }
}
