<?php

namespace App\Services;

/**
 * Provides vendor performance reports from first-party database metrics.
 */
class VendorPerformanceReportService
{
    public function __construct(
        protected PerformanceAnalyticsService $analytics
    ) {}

    public function generateVendorReport(int $vendorId): array
    {
        return $this->analytics->getVendorReport($vendorId);
    }

    public function getReportSourceMetrics(int $vendorId): array
    {
        return $this->analytics->getVendorReport($vendorId);
    }
}
