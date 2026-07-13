<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Services\VendorPerformanceReportService;
use App\Services\SimplePdfWriter;
use App\Notifications\WeeklyPerformanceReportNotification;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class WeeklyReportCronController extends Controller
{
    protected VendorPerformanceReportService $reportService;

    public function __construct(VendorPerformanceReportService $reportService)
    {
        $this->reportService = $reportService;
    }

    /**
     * Run the automated scheduled task to compile and email previous week's performance reports with attached PDFs to registered vendors.
     */
    public function emailWeeklyReports(Request $request)
    {
        try {
            $vendors = User::where('role', 'VENDOR')->orWhere('role', 'vendor')->get();
            $sentReports = [];

            foreach ($vendors as $vendor) {
                // Generate the previous week's performance report data
                $reportData = $this->reportService->generateVendorReport($vendor->id);

                if (!empty($reportData)) {
                    // Create reports directory if it doesn't exist
                    $reportsDir = storage_path('app/reports');
                    if (!file_exists($reportsDir)) {
                        mkdir($reportsDir, 0755, true);
                    }

                    // Generate PDF Report via SimplePdfWriter
                    $pdfPath = $reportsDir . '/weekly-performance-vendor-' . $vendor->id . '.pdf';
                    $pdfWriter = new SimplePdfWriter();
                    $pdfContent = $pdfWriter->generate(
                        $vendor->fullName ?: $vendor->username,
                        $reportData['source_metrics'] ?? $reportData,
                        $reportData['report_markdown'] ?? $reportData['report_content'] ?? ''
                    );
                    file_put_contents($pdfPath, $pdfContent);

                    // Send notification via Mail & Database channels with attached PDF
                    $notification = new WeeklyPerformanceReportNotification(
                        $vendor->fullName ?: $vendor->username,
                        $reportData,
                        $pdfPath
                    );
                    $vendor->notify($notification);

                    $sentReports[] = [
                        'vendor_id' => $vendor->id,
                        'vendor_name' => $vendor->fullName ?: $vendor->username,
                        'email' => $vendor->email,
                        'pdf_saved_to' => $pdfPath,
                        'status' => 'SENT'
                    ];
                } else {
                    $sentReports[] = [
                        'vendor_id' => $vendor->id,
                        'vendor_name' => $vendor->fullName ?: $vendor->username,
                        'email' => $vendor->email,
                        'status' => 'SKIPPED_NO_DATA'
                    ];
                }
            }

            return response()->json([
                'success' => true,
                'message' => 'Weekly performance report summary emails with PDF attachments sent successfully to registered vendors.',
                'timestamp' => date('Y-m-d H:i:s'),
                'vendors_processed' => count($vendors),
                'reports_sent' => $sentReports
            ], 200);

        } catch (\Exception $e) {
            Log::error("WeeklyReportCronController: Cron execution failed: " . $e->getMessage());
            return response()->json([
                'success' => false,
                'message' => 'Failed to execute weekly performance report cron job.',
                'error' => $e->getMessage()
            ], 500);
        }
    }

    /**
     * Download the latest weekly PDF performance report.
     */
    public function downloadWeeklyReportPdf(Request $request, $vendorId = null)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only vendors or administrators can download performance report PDFs.'
            ], 403);
        }

        if ($role === 'ADMIN') {
            if (!$vendorId) {
                return response()->json([
                    'success' => false,
                    'message' => 'Vendor ID is required for administrative downloads.'
                ], 400);
            }
            $targetVendorId = (int)$vendorId;
        } else {
            $targetVendorId = $user->id;
        }

        $vendor = User::find($targetVendorId);
        if (!$vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor not found.'
            ], 404);
        }

        $pdfPath = storage_path('app/reports/weekly-performance-vendor-' . $targetVendorId . '.pdf');

        // If PDF doesn't exist, generate it dynamically
        if (!file_exists($pdfPath)) {
            $reportData = $this->reportService->generateVendorReport($targetVendorId);
            if (empty($reportData)) {
                return response()->json([
                    'success' => false,
                    'message' => 'Could not compile metrics for this vendor profile.'
                ], 400);
            }

            $reportsDir = storage_path('app/reports');
            if (!file_exists($reportsDir)) {
                mkdir($reportsDir, 0755, true);
            }

            $pdfWriter = new SimplePdfWriter();
            $pdfContent = $pdfWriter->generate(
                $vendor->fullName ?: $vendor->username,
                $reportData['source_metrics'] ?? $reportData,
                $reportData['report_markdown'] ?? $reportData['report_content'] ?? ''
            );

            file_put_contents($pdfPath, $pdfContent);
        }

        $cleanName = str_replace(' ', '-', $vendor->fullName ?: $vendor->username);
        return response()->download($pdfPath, "Weekly-Performance-Report-{$cleanName}.pdf", [
            'Content-Type' => 'application/pdf',
        ]);
    }
}
