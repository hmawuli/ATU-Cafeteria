<?php

namespace App\Notifications;

use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Notifications\Notification;

class AuthenticationCodeNotification extends Notification
{
    use Queueable;

    public function __construct(private readonly string $purpose, private readonly string $code) {}

    public function via(object $notifiable): array { return ['mail']; }

    public function toMail(object $notifiable): MailMessage
    {
        $label = $this->purpose === 'ADMIN_2FA' ? 'Admin security verification' : 'Password reset';
        return (new MailMessage)
            ->subject("ATU Cafeteria - {$label}")
            ->greeting('ATU Cafeteria Security')
            ->line("Your {$label} code is:")
            ->line($this->code)
            ->line('This code expires in 10 minutes and can only be used once.')
            ->line('If you did not request this, please contact the system administrator.');
    }
}
