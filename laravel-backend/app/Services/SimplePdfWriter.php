<?php

namespace App\Services;

class SimplePdfWriter
{
    private $output = '';
    private $objects = [];
    private $offsets = [];

    /**
     * Generate binary PDF string containing the vendor performance report.
     */
    public function generate($vendorName, $reportData, $markdownReportText)
    {
        $this->output = "%PDF-1.4\n";
        $this->objects = [];
        $this->offsets = [];

        // 1. Catalog Object
        $this->addObject(1, "<< /Type /Catalog /Pages 2 0 R >>");

        // 3. Font Objects (Regular & Bold Helvetica)
        $this->addObject(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        $this->addObject(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");

        // Build Content Stream (Standard A4 canvas text commands)
        $content = "BT\n";
        $content = "BT\n";
        
        // Document Header
        $content .= "/F4 18 Tf\n"; // Bold 18pt
        $content .= "50 780 Td\n";
        $content .= "(" . $this->escapePdfString("ACCRA TECHNICAL UNIVERSITY - CAFETERIA HUB") . ") Tj\n";
        $content .= "ET\n";

        $content .= "BT\n";
        $content .= "/F4 14 Tf\n"; // Bold 14pt
        $content .= "50 755 Td\n";
        $content .= "(" . $this->escapePdfString("WEEKLY PERFORMANCE REPORT") . ") Tj\n";
        $content .= "ET\n";

        // Vendor details
        $content .= "BT\n";
        $content .= "/F3 10 Tf\n"; // Regular 10pt
        $content .= "50 730 Td\n";
        $content .= "(" . $this->escapePdfString("Vendor Name: " . $vendorName) . ") Tj\n";
        $content .= "ET\n";

        $content .= "BT\n";
        $content .= "/F3 10 Tf\n";
        $content .= "50 715 Td\n";
        $content .= "(" . $this->escapePdfString("Report Week: " . date('W, Y') . " | Date Generated: " . date('Y-m-d H:i:s')) . ") Tj\n";
        $content .= "ET\n";

        // Section 1: Business Metrics
        $content .= "BT\n";
        $content .= "/F4 12 Tf\n";
        $content .= "50 680 Td\n";
        $content .= "(" . $this->escapePdfString("1. SALES & FULFILLMENT METRICS") . ") Tj\n";
        $content .= "ET\n";

        $metrics = $reportData['metrics'] ?? [];
        $totalOrders = $metrics['total_orders'] ?? 0;
        $completedOrders = $metrics['completed_orders'] ?? 0;
        $declinedOrders = $metrics['declined_orders'] ?? 0;
        $completionRate = $metrics['completion_rate_percent'] ?? 100.0;
        $totalRevenue = $metrics['total_revenue_ghs'] ?? 0.0;

        $content .= "BT\n";
        $content .= "/F3 10 Tf\n";
        $content .= "60 660 Td\n";
        $content .= "(" . $this->escapePdfString("• Total Orders Logged: " . $totalOrders) . ") Tj\n";
        $content .= "ET\n";

        $content .= "BT\n";
        $content .= "/F3 10 Tf\n";
        $content .= "60 645 Td\n";
        $content .= "(" . $this->escapePdfString("• Successfully Completed: " . $completedOrders . " | Declined/Cancelled: " . $declinedOrders) . ") Tj\n";
        $content .= "ET\n";

        $content .= "BT\n";
        $content .= "/F3 10 Tf\n";
        $content .= "60 630 Td\n";
        $content .= "(" . $this->escapePdfString("• Order Completion Success Rate: " . $completionRate . "%") . ") Tj\n";
        $content .= "ET\n";

        $content .= "BT\n";
        $content .= "/F3 10 Tf\n";
        $content .= "60 615 Td\n";
        $content .= "(" . $this->escapePdfString("• Weekly Total Revenue: GHc " . number_format($totalRevenue, 2)) . ") Tj\n";
        $content .= "ET\n";

        // Section 2: Top Selling Dishes
        $content .= "BT\n";
        $content .= "/F4 12 Tf\n";
        $content .= "50 580 Td\n";
        $content .= "(" . $this->escapePdfString("2. TOP-SELLING MENU ITEMS") . ") Tj\n";
        $content .= "ET\n";

        $y = 560;
        $topDishes = $metrics['top_dishes'] ?? [];
        if (empty($topDishes)) {
            $content .= "BT\n/F3 10 Tf\n60 {$y} Td\n(No menu item sales recorded this week.) Tj\nET\n";
            $y -= 15;
        } else {
            foreach (array_slice($topDishes, 0, 4) as $idx => $dish) {
                $foodName = $dish['food_name'] ?? 'Dish Item';
                $qty = $dish['total_qty'] ?? 0;
                $orders = $dish['order_count'] ?? 0;
                
                $content .= "BT\n";
                $content .= "/F3 10 Tf\n";
                $content .= "60 {$y} Td\n";
                $content .= "(" . $this->escapePdfString("• " . ($idx + 1) . ". {$foodName} - Sold {$qty} units across {$orders} orders") . ") Tj\n";
                $content .= "ET\n";
                $y -= 15;
            }
        }

        // Section 3: Average Ratings
        $y -= 10;
        $content .= "BT\n";
        $content .= "/F4 12 Tf\n";
        $content .= "50 {$y} Td\n";
        $content .= "(" . $this->escapePdfString("3. CUSTOMER SATISFACTION FEEDBACK (Rating out of 5.0)") . ") Tj\n";
        $content .= "ET\n";
        $y -= 20;

        $ratings = $reportData['ratings'] ?? [];
        $ratingSpecs = [
            'Food Flavor & Taste Quality' => $ratings['food_quality_avg'] ?? 0.0,
            'Kitchen Sanitation & Cleanliness' => $ratings['cleanliness_avg'] ?? 0.0,
            'Preparation & Hand-off Velocity' => $ratings['service_speed_avg'] ?? 0.0,
            'Economic Fairness & Portion Value' => $ratings['price_value_avg'] ?? 0.0,
        ];

        foreach ($ratingSpecs as $label => $score) {
            $content .= "BT\n";
            $content .= "/F3 10 Tf\n";
            $content .= "60 {$y} Td\n";
            $content .= "(" . $this->escapePdfString("• {$label}: {$score} / 5.0") . ") Tj\n";
            $content .= "ET\n";
            $y -= 15;
        }

        // Section 4: AI Insights
        $y -= 15;
        $content .= "BT\n";
        $content .= "/F4 12 Tf\n";
        $content .= "50 {$y} Td\n";
        $content .= "(" . $this->escapePdfString("4. BUSINESS RECOMMENDATIONS & EXECUTIVE OUTLOOK") . ") Tj\n";
        $content .= "ET\n";
        $y -= 20;

        // Extract paragraphs from markdown report
        $paragraphs = explode("\n", $markdownReportText);
        foreach ($paragraphs as $para) {
            $para = trim($para);
            if (empty($para)) continue;

            // Strip simple markdown tags
            $para = ltrim($para, '#-* ');
            $para = str_replace('**', '', $para);

            // Simple word-wrapping logic for clean PDF boundaries
            $words = explode(' ', $para);
            $currentLine = '';
            foreach ($words as $word) {
                if (strlen($currentLine . ' ' . $word) > 85) {
                    $content .= "BT\n/F3 9 Tf\n60 {$y} Td\n(" . $this->escapePdfString(trim($currentLine)) . ") Tj\nET\n";
                    $y -= 13;
                    $currentLine = $word;
                    if ($y < 40) {
                        break 2; // Keep in safe A4 single-page bounds
                    }
                } else {
                    $currentLine = empty($currentLine) ? $word : $currentLine . ' ' . $word;
                }
            }
            if (!empty($currentLine)) {
                $content .= "BT\n/F3 9 Tf\n60 {$y} Td\n(" . $this->escapePdfString(trim($currentLine)) . ") Tj\nET\n";
                $y -= 15;
            }

            if ($y < 40) {
                break;
            }
        }

        // Output content stream length and object
        $contentLength = strlen($content);
        $this->addObject(5, "<< /Length {$contentLength} >>\nstream\n{$content}\nendstream");

        // 2. Pages structure Catalog
        $this->addObject(2, "<< /Type /Pages /Kids [ 6 0 R ] /Count 1 >>");

        // 6. Main Page definition (Standard A4 MediaBox)
        $this->addObject(6, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 5 0 R /Resources << /Font << /F3 3 0 R /F4 4 0 R >> >> >>");

        // Write offsets cross-reference table and EOF tag
        $xrefOffset = strlen($this->output);
        $this->output .= "xref\n0 " . (count($this->objects) + 1) . "\n";
        $this->output .= "0000000000 65535 f \n";

        foreach ($this->objects as $id => $obj) {
            $this->output .= sprintf("%010d 00000 n \n", $this->offsets[$id]);
        }

        $this->output .= "trailer\n<< /Size " . (count($this->objects) + 1) . " /Root 1 0 R >>\n";
        $this->output .= "startxref\n{$xrefOffset}\n%%EOF";

        return $this->output;
    }

    private function addObject($id, $data)
    {
        $this->offsets[$id] = strlen($this->output);
        $this->objects[$id] = $data;
        $this->output .= "{$id} 0 obj\n{$data}\nendobj\n";
    }

    private function escapePdfString($str)
    {
        // Simple escape for PDF parentheses
        $str = str_replace(['\\', '(', ')'], ['\\\\', '\\(', '\\)'], $str);
        // Clean non-printable/Unicode chars to ensure standard Helvetica compatibility
        return iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $str);
    }
}
