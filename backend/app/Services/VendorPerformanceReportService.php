<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class VendorPerformanceReportService
{
    protected GeminiPerformanceReportService $geminiService;

    public function __construct(GeminiPerformanceReportService $geminiService)
    {
        $this->geminiService = $geminiService;
    }

    /**
     * Compile report data and generate performance report via the Gemini API.
     *
     * @param int $vendorId
     * @return array
     */
    public function generateVendorReport(int $vendorId): array
    {
        Log::info("VendorPerformanceReportService: Requesting performance report for vendor ID {$vendorId}");
        
        // Use the Gemini service to compile stats and query Gemini
        return $this->geminiService->generateReport($vendorId);
    }

    /**
     * Retrieve source metrics used for the report.
     *
     * @param int $vendorId
     * @return array
     */
    public function getReportSourceMetrics(int $vendorId): array
    {
        return $this->geminiService->gatherVendorData($vendorId);
    }
}
