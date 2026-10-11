<?php

namespace App\Support;

use Symfony\Component\HttpFoundation\Response;

/**
 * Minimal, dependency-free CSV builder used by the export endpoints so every
 * controller produces identical, properly quoted CSV.
 */
final class CsvExporter
{
    /**
     * Return an Excel-friendly CSV download response.
     *
     * @param  array<int, string>  $headers
     * @param  array<int, array<int|string, mixed>>  $rows
     */
    public static function download(string $filename, array $headers, array $rows, string $title): Response
    {
        return response(self::build($headers, $rows, $title), 200, [
            'Content-Type' => 'text/csv; charset=UTF-8',
            'Content-Disposition' => 'attachment; filename="'.self::safeFilename($filename).'"',
            'Pragma' => 'no-cache',
            'Cache-Control' => 'must-revalidate, post-check=0, pre-check=0',
            'Expires' => '0',
        ]);
    }

    /**
     * Build a CSV string from a header row and data rows.
     *
     * @param  array<int, string>  $headers
     * @param  array<int, array<int|string, mixed>>  $rows
     */
    public static function build(array $headers, array $rows, string $title): string
    {
        $handle = fopen('php://temp', 'w+');

        // UTF-8 BOM keeps Ghanaian names and currency text readable in Excel.
        fwrite($handle, chr(0xEF).chr(0xBB).chr(0xBF));
        fputcsv($handle, [$title]);
        fputcsv($handle, []);
        fputcsv($handle, $headers);

        foreach ($rows as $row) {
            $values = [];
            foreach ($headers as $header) {
                $values[] = $row[$header] ?? '';
            }
            fputcsv($handle, $values);
        }

        rewind($handle);
        $csv = stream_get_contents($handle);
        fclose($handle);

        return $csv;
    }

    private static function safeFilename(string $filename): string
    {
        $sanitized = preg_replace('/[^A-Za-z0-9._-]/', '-', $filename) ?? 'export.csv';

        return str_ends_with(strtolower($sanitized), '.csv') ? $sanitized : $sanitized.'.csv';
    }
}
