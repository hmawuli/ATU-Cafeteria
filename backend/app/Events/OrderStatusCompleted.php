<?php

namespace App\Events;

use App\Models\Order;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class OrderStatusCompleted
{
    use Dispatchable, SerializesModels;

    /**
     * @var \App\Models\Order
     */
    public $order;

    /**
     * Create a new event instance.
     *
     * @param  \App\Models\Order  $order
     */
    public function __construct(Order $order)
    {
        $this->order = $order;
    }
}
