<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use App\Models\Vendor;
use Illuminate\Http\Request;

class AdminReportController extends Controller
{
    /**
     * Generate and serve a CSV file containing filtered vendor sales and student order history data.
     */
    public function exportVendorSalesAndOrdersCsv(Request $request)
    {
        $vendorId = $request->input('vendor_id') ?? $request->query('vendor_id');
        $startDate = $request->query('start_date');
        $endDate = $request->query('end_date');
        $status = $request->query('status');

        $query = Order::query()->with(['user', 'vendor', 'orderItems']);

        if ($vendorId) {
            $query->where('vendor_id', $vendorId);
        }

        if ($status) {
            $query->where('status', $status);
        }

        if ($startDate) {
            $query->whereDate('created_at', '>=', $startDate);
        }

        if ($endDate) {
            $query->whereDate('created_at', '<=', $endDate);
        }

        $orders = $query->orderBy('created_at', 'desc')->get();

        $fileName = 'ATU_Sales_And_Orders_Report_'.date('Y_m_d_His').'.csv';

        $headers = [
            'Content-Type' => 'text/csv; charset=UTF-8',
            'Content-Disposition' => "attachment; filename=\"$fileName\"",
            'Pragma' => 'no-cache',
            'Cache-Control' => 'must-revalidate, post-check=0, pre-check=0',
            'Expires' => '0',
        ];

        $callback = function () use ($orders) {
            $file = fopen('php://output', 'w');

            // UTF-8 BOM for Excel compatibility
            fprintf($file, chr(0xEF).chr(0xBB).chr(0xBF));

            // CSV Header Row
            fputcsv($file, [
                'Order ID',
                'Reference Code',
                'Student Name',
                'Student Email',
                'Vendor Name',
                'Total Amount (GHS)',
                'Payment Method',
                'Status',
                'Items Summary',
                'Created At',
            ]);

            foreach ($orders as $order) {
                $itemsSummary = [];
                if ($order->orderItems) {
                    foreach ($order->orderItems as $item) {
                        $itemsSummary[] = ($item->item_name ?? 'Item').' x'.($item->quantity ?? 1);
                    }
                }

                fputcsv($file, [
                    $order->id,
                    $order->reference_code ?? $order->order_code ?? ('ORD-'.$order->id),
                    $order->user->name ?? $order->customer_name ?? 'Student',
                    $order->user->email ?? 'N/A',
                    $order->vendor->name ?? $order->vendor_name ?? 'ATU Vendor',
                    number_format($order->total_amount ?? $order->total_price ?? 0.00, 2),
                    $order->payment_method ?? 'Mobile Money',
                    strtoupper($order->status ?? 'PLACED'),
                    implode(' | ', $itemsSummary),
                    $order->created_at ? $order->created_at->toDateTimeString() : date('Y-m-d H:i:s'),
                ]);
            }

            fclose($file);
        };

        return response()->stream($callback, 200, $headers);
    }

    /**
     * Export student specific order history CSV.
     */
    public function exportStudentOrdersCsv(Request $request, $studentId = null)
    {
        $id = $studentId ?? $request->query('student_id');
        $query = Order::query()->with(['vendor', 'orderItems']);

        if ($id) {
            $query->where('user_id', $id);
        }

        $orders = $query->orderBy('created_at', 'desc')->get();
        $fileName = 'Student_Orders_History_'.($id ?? 'All').'_'.date('Y_m_d').'.csv';

        $headers = [
            'Content-Type' => 'text/csv; charset=UTF-8',
            'Content-Disposition' => "attachment; filename=\"$fileName\"",
            'Pragma' => 'no-cache',
            'Cache-Control' => 'must-revalidate, post-check=0, pre-check=0',
            'Expires' => '0',
        ];

        $callback = function () use ($orders) {
            $file = fopen('php://output', 'w');
            fprintf($file, chr(0xEF).chr(0xBB).chr(0xBF));

            fputcsv($file, [
                'Order ID',
                'Vendor',
                'Total Amount (GHS)',
                'Payment Status',
                'Order Status',
                'Date',
            ]);

            foreach ($orders as $order) {
                fputcsv($file, [
                    $order->id,
                    $order->vendor->name ?? 'ATU Cafeteria Vendor',
                    number_format($order->total_amount ?? 0.00, 2),
                    $order->payment_status ?? 'PAID',
                    $order->status ?? 'PLACED',
                    $order->created_at ? $order->created_at->toDateTimeString() : date('Y-m-d H:i:s'),
                ]);
            }

            fclose($file);
        };

        return response()->stream($callback, 200, $headers);
    }
}
