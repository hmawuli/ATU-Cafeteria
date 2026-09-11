<?php

namespace App\Notifications;

use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Notification;
use Illuminate\Notifications\Messages\MailMessage;

class WeeklyPerformanceReportNotification extends Notification
{
    use Queueable;

    protected array $reportData;
    protected string $vendorName;
    protected ?string $pdfPath;

    /**
     * Create a new notification instance.
     *
     * @param string $vendorName
     * @param array $reportData
     * @param string|null $pdfPath
     */
    public function __construct(string $vendorName, array $reportData, ?string $pdfPath = null)
    {
        $this->vendorName = $vendorName;
        $this->reportData = $reportData;
        $this->pdfPath = $pdfPath;
    }

    /**
     * Get the notification's delivery channels.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function via($notifiable)
    {
        return ['mail', 'database'];
    }

    /**
     * Get the mail representation of the notification.
     */
    public function toMail($notifiable)
    {
        $metrics = $this->reportData['source_metrics'] ?? $this->reportData;
        $geminiSummary = $this->reportData['gemini_response'] ?? $this->reportData['report_content'] ?? 'No AI generated insights available for this week.';

        $totalCompleted = $metrics['order_metrics']['completed_orders_all_time'] ?? $metrics['completed_orders'] ?? 0;
        $totalRevenue = $metrics['order_metrics']['total_completed_revenue'] ?? $metrics['total_revenue'] ?? 0.00;
        $completionRate = $metrics['order_metrics']['completion_rate'] ?? 100.0;
        
        $mail = (new MailMessage)
            ->subject("Weekly Performance Summary Report - {$this->vendorName}")
            ->greeting("Hello {$this->vendorName},")
            ->line("Here is your consolidated Weekly Performance Summary Report for ATU Cafeteria Hub.")
            ->line("Your food booth performance was evaluated across key operations, customer ratings, and logistics metrics:")
            ->line("• Total Completed Orders: {$totalCompleted}")
            ->line("• Weekly Sales Revenue: GH₵ " . number_format($totalRevenue, 2))
            ->line("• Order Completion Success Rate: {$completionRate}%");

        // Top dishes
        $topDishes = $metrics['order_metrics']['top_dishes'] ?? [];
        if (!empty($topDishes)) {
            $mail->line("Top Performing Dishes/Meals this week:");
            foreach (array_slice($topDishes, 0, 3) as $dish) {
                $name = $dish['food_name'] ?? 'Dish';
                $cnt = $dish['order_count'] ?? $dish['total_qty'] ?? 0;
                $mail->line("  - {$name} ({$cnt} portions ordered)");
            }
        }

        $mail->line("AI-Powered Performance Insights & Recommendations:")
             ->line($geminiSummary)
              ->line('Keep up the great work in serving the Accra Technical University community! For support or inventory requests, please coordinate with cafeteria administrators.');

        if ($this->pdfPath && file_exists($this->pdfPath)) {
            $mail->attach($this->pdfPath, [
                'as' => 'Weekly-Performance-Report.pdf',
                'mime' => 'application/pdf',
            ]);
        }

        return $mail;
    }

    /**
     * Get the array representation of the notification for the database.
     *
     * @param  mixed  $notifiable
     * @return array
     */
    public function toDatabase($notifiable)
    {
        $metrics = $this->reportData['source_metrics'] ?? $this->reportData;
        $totalCompleted = $metrics['order_metrics']['completed_orders_all_time'] ?? $metrics['completed_orders'] ?? 0;
        $totalRevenue = $metrics['order_metrics']['total_completed_revenue'] ?? $metrics['total_revenue'] ?? 0.00;

        return [
            'vendor_name' => $this->vendorName,
            'total_completed_orders' => $totalCompleted,
            'weekly_revenue' => $totalRevenue,
            'generated_at' => date('Y-m-d H:i:s'),
            'message' => "Your weekly performance summary is ready! Total Revenue: GH₵ " . number_format($totalRevenue, 2) . " with {$totalCompleted} completed orders."
        ];
    }
}
