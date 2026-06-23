<?php

namespace App\Notifications;

use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Notification;

class LowStockAlertNotification extends Notification
{
    use Queueable;

    protected $itemName;
    protected $itemId;
    protected $remainingStock;
    protected $orderFrequency24h;
    protected $sourceTable;

    /**
     * Create a new notification instance.
     *
     * @param string $itemName
     * @param int $itemId
     * @param int $remainingStock
     * @param int $orderFrequency24h
     * @param string $sourceTable
     */
    public function __construct($itemName, $itemId, $remainingStock, $orderFrequency24h, $sourceTable = 'food_items')
    {
        $this->itemName = $itemName;
        $this->itemId = $itemId;
        $this->remainingStock = $remainingStock;
        $this->orderFrequency24h = $orderFrequency24h;
        $this->sourceTable = $sourceTable;
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
        return ['orders-vendor-' . $this->itemId];
    }

    /**
     * Get the array representation of the notification for the database.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function toDatabase($notifiable)
    {
        $alertMessage = "INVENTORY ALERT: '{$this->itemName}' is running low! Demand is high with {$this->orderFrequency24h} orders processed in the last 24h. Only {$this->remainingStock} units left—please prepare a new batch!";

        if ($this->remainingStock <= 0) {
            $alertMessage = "OUT OF STOCK ALERT: '{$this->itemName}' is sold out! High-frequency order velocity was reached with {$this->orderFrequency24h} orders in 24h. Stock is exhausted (0 remaining)—update availability now.";
        }

        return [
            'item_id' => $this->itemId,
            'item_name' => $this->itemName,
            'remaining_stock' => $this->remainingStock,
            'order_frequency_24h' => $this->orderFrequency24h,
            'source_table' => $this->sourceTable,
            'message' => $alertMessage,
            'time' => date('Y-m-d H:i:s'),
        ];
    }
}
