<?php

namespace App\Notifications;

use App\Models\Order;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

class OrderReadyNotification extends Notification
{
    use Queueable;

    /**
     * @var Order
     */
    protected $order;

    /**
     * Create a new notification instance.
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
        return ['database', 'mail'];
    }

    /**
     * Get the mail representation of the notification.
     *
     * @param  mixed  $notifiable
     * @return MailMessage
     */
    public function toMail($notifiable)
    {
        return (new MailMessage)
            ->subject("🍔 ATU Cafeteria: Your Order #{$this->order->id} is READY!")
            ->greeting('Hello, '.($notifiable->fullName ?: $notifiable->username).'!')
            ->line('Great news! Your pre-ordered food is now ready for pickup at the counter.')
            ->line('Order Details:')
            ->line("• **Item:** {$this->order->food_name}")
            ->line("• **Quantity:** {$this->order->quantity}")
            ->line('• **Total Paid:** GH₵'.number_format($this->order->total_price, 2))
            ->line("🔑 **Your Security Pickup PIN:** {$this->order->pickup_pin}")
            ->line('Please show this PIN at the counter to retrieve your hot meal.')
            ->action('View My Orders', url('/'))
            ->line('Thank you for using Accra Technical University (ATU) Cafeteria Hub!');
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
