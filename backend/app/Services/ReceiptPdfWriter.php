<?php

namespace App\Services;

class ReceiptPdfWriter
{
    private $output = '';

    private $objects = [];

    private $offsets = [];

    /**
     * Generate binary PDF string containing the printable receipt.
     */
    public function generate($order)
    {
        $this->output = "%PDF-1.4\n";
        $this->objects = [];
        $this->offsets = [];

        // 1. Catalog Object
        $this->addObject(1, '<< /Type /Catalog /Pages 2 0 R >>');

        // 3. Font Objects (Regular & Bold Helvetica)
        $this->addObject(3, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
        $this->addObject(4, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>');

        // Build Content Stream (Standard A4 canvas text commands)
        $content = "BT\n";

        // Document Header (Left aligned)
        $content .= "/F4 18 Tf\n"; // Bold 18pt
        $content .= "50 780 Td\n";
        $content .= '('.$this->escapePdfString('ACCRA TECHNICAL UNIVERSITY - CAFETERIA HUB').") Tj\n";
        $content .= "ET\n";

        $content .= "BT\n";
        $content .= "/F4 14 Tf\n"; // Bold 14pt
        $content .= "50 755 Td\n";
        $content .= '('.$this->escapePdfString('OFFICIAL TRANSACTION RECEIPT').") Tj\n";
        $content .= "ET\n";

        // Decorative separating line
        $content .= "BT\n/F4 10 Tf\n50 740 Td\n(".$this->escapePdfString('=========================================================================').") Tj\nET\n";

        // Order Details
        $orderId = $order->id;
        $orderDate = date('F d, Y H:i:s', $order->order_timestamp / 1000);

        $customerName = 'Student (ID: '.$order->customer_id.')';
        if ($order->customer) {
            $customerName = $order->customer->fullName;
        } elseif ($order->student) {
            $customerName = $order->student->fullName;
        }

        $vendorName = 'Vendor (ID: '.$order->vendor_id.')';
        if ($order->vendor) {
            $vendorName = $order->vendor->fullName;
        }

        $content .= "BT\n/F4 11 Tf\n50 715 Td\n(".$this->escapePdfString('Receipt Details:').") Tj\nET\n";
        $content .= "BT\n/F3 10 Tf\n60 695 Td\n(".$this->escapePdfString('• Order ID: #'.$orderId).") Tj\nET\n";
        $content .= "BT\n/F3 10 Tf\n60 680 Td\n(".$this->escapePdfString('• Transaction Timestamp: '.$orderDate).") Tj\nET\n";
        $content .= "BT\n/F3 10 Tf\n60 665 Td\n(".$this->escapePdfString('• Student Name: '.$customerName).") Tj\nET\n";
        $content .= "BT\n/F3 10 Tf\n60 650 Td\n(".$this->escapePdfString('• Cafeteria Vendor: '.$vendorName).") Tj\nET\n";

        $content .= "BT\n/F4 10 Tf\n50 635 Td\n(".$this->escapePdfString('-------------------------------------------------------------------------').") Tj\nET\n";

        // Itemized Table Header
        $content .= "BT\n/F4 11 Tf\n50 615 Td\n(".$this->escapePdfString('Itemized Purchases:').") Tj\nET\n";

        // Dish column header
        $content .= "BT\n/F4 10 Tf\n60 595 Td\n(".$this->escapePdfString('Dish Details').") Tj\nET\n";
        $content .= "BT\n/F4 10 Tf\n350 595 Td\n(".$this->escapePdfString('Qty').") Tj\nET\n";
        $content .= "BT\n/F4 10 Tf\n450 595 Td\n(".$this->escapePdfString('Unit Price').") Tj\nET\n";

        $content .= "BT\n/F4 10 Tf\n50 585 Td\n(".$this->escapePdfString('-------------------------------------------------------------------------').") Tj\nET\n";

        // Itemized purchase details
        $dishName = $order->food_name ?? 'Cafeteria Meal';
        $qty = $order->quantity ?? 1;
        $totalPrice = $order->total_price;
        $unitPrice = $qty > 0 ? ($totalPrice / $qty) : $totalPrice;

        $content .= "BT\n/F3 10 Tf\n60 565 Td\n(".$this->escapePdfString($dishName).") Tj\nET\n";
        $content .= "BT\n/F3 10 Tf\n350 565 Td\n(".$this->escapePdfString((string) $qty).") Tj\nET\n";
        $content .= "BT\n/F3 10 Tf\n450 565 Td\n(".$this->escapePdfString('GHc '.number_format($unitPrice, 2)).") Tj\nET\n";

        $content .= "BT\n/F4 10 Tf\n50 545 Td\n(".$this->escapePdfString('-------------------------------------------------------------------------').") Tj\nET\n";

        // Totals
        $content .= "BT\n/F4 11 Tf\n320 520 Td\n(".$this->escapePdfString('Subtotal:').") Tj\nET\n";
        $content .= "BT\n/F3 11 Tf\n450 520 Td\n(".$this->escapePdfString('GHc '.number_format($totalPrice, 2)).") Tj\nET\n";

        $content .= "BT\n/F4 11 Tf\n320 500 Td\n(".$this->escapePdfString('Service Tax / VAT (0%):').") Tj\nET\n";
        $content .= "BT\n/F3 11 Tf\n450 500 Td\n(".$this->escapePdfString('GHc 0.00').") Tj\nET\n";

        $content .= "BT\n/F4 12 Tf\n320 475 Td\n(".$this->escapePdfString('Total Amount Paid:').") Tj\nET\n";
        $content .= "BT\n/F4 12 Tf\n450 475 Td\n(".$this->escapePdfString('GHc '.number_format($totalPrice, 2)).") Tj\nET\n";

        // Order PIN verification
        $pin = $order->pickup_pin ?? 'N/A';
        $content .= "BT\n/F4 10 Tf\n50 430 Td\n(".$this->escapePdfString('Verification PIN: '.$pin).") Tj\nET\n";

        // Payment status badge
        $paymentStatus = strtoupper($order->payment_status ?? 'PAID');
        $content .= "BT\n/F4 10 Tf\n50 415 Td\n(".$this->escapePdfString('Payment Status: '.$paymentStatus).") Tj\nET\n";

        // Fulfillment status badge
        $fulfillmentStatus = strtoupper($order->status ?? 'COMPLETED');
        $content .= "BT\n/F4 10 Tf\n50 400 Td\n(".$this->escapePdfString('Fulfillment Status: '.$fulfillmentStatus).") Tj\nET\n";

        // Footer note
        $content .= "BT\n/F4 10 Tf\n50 350 Td\n(".$this->escapePdfString('=========================================================================').") Tj\nET\n";

        $content .= "BT\n/F3 9 Tf\n50 330 Td\n(".$this->escapePdfString('Thank you for dining at Accra Technical University Cafeteria Hub.').") Tj\nET\n";
        $content .= "BT\n/F3 9 Tf\n50 315 Td\n(".$this->escapePdfString('For support, please visit the cafeteria admin offices or contact the hub supervisor.').") Tj\nET\n";
        $content .= "BT\n/F3 8 Tf\n50 290 Td\n(".$this->escapePdfString('This is a system-generated printable receipt and does not require a physical signature.').") Tj\nET\n";

        // Output content stream length and object
        $contentLength = strlen($content);
        $this->addObject(5, "<< /Length {$contentLength} >>\nstream\n{$content}\nendstream");

        // 2. Pages structure Catalog
        $this->addObject(2, '<< /Type /Pages /Kids [ 6 0 R ] /Count 1 >>');

        // 6. Main Page definition (Standard A4 MediaBox)
        $this->addObject(6, '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 5 0 R /Resources << /Font << /F3 3 0 R /F4 4 0 R >> >> >>');

        // Write offsets cross-reference table and EOF tag
        $xrefOffset = strlen($this->output);
        $this->output .= "xref\n0 ".(count($this->objects) + 1)."\n";
        $this->output .= "0000000000 65535 f \n";

        foreach ($this->objects as $id => $obj) {
            $this->output .= sprintf("%010d 00000 n \n", $this->offsets[$id]);
        }

        $this->output .= "trailer\n<< /Size ".(count($this->objects) + 1)." /Root 1 0 R >>\n";
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
        $str = str_replace(['\\', '(', ')'], ['\\\\', '\\(', '\\)'], $str);

        return iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $str);
    }
}
