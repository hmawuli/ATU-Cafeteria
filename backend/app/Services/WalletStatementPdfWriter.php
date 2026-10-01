<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Support\Collection;

/**
 * Minimal dependency-free PDF generator for a student wallet statement.
 * Mirrors the approach of ReceiptPdfWriter (raw PDF objects, A4).
 */
class WalletStatementPdfWriter
{
    private string $output = '';

    /** @var array<int, string> */
    private array $objects = [];

    /** @var array<int, int> */
    private array $offsets = [];

    public function generate(User $user, Collection $transactions, \DateTimeInterface $from, \DateTimeInterface $to): string
    {
        $this->output = "%PDF-1.4\n";
        $this->objects = [];
        $this->offsets = [];

        $this->addObject(1, '<< /Type /Catalog /Pages 2 0 R >>');
        $this->addObject(2, '<< /Type /Pages /Kids [3 0 R] /Count 1 >>');
        $this->addObject(3, '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>');
        $this->addObject(4, $this->contentStream($user, $transactions, $from, $to));
        $this->addObject(5, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>');
        $this->addObject(6, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>');

        $xrefOffset = strlen($this->output);
        $this->output .= "xref\n0 7\n0000000000 65535 f \n";
        foreach ($this->offsets as $offset) {
            $this->output .= sprintf("%010d 00000 n \n", $offset);
        }
        $this->output .= "trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n{$xrefOffset}\n%%EOF";

        return $this->output;
    }

    private function contentStream(User $user, Collection $transactions, \DateTimeInterface $from, \DateTimeInterface $to): string
    {
        $content = "BT /F2 16 Tf 50 780 Td (ATU CAFETERIA - WALLET STATEMENT) Tj ET\n";
        $content .= 'BT /F1 10 Tf 50 762 Td ('.$this->escape("Account: {$user->username} ({$user->fullName}) · {$from->format('Y-m-d')} → {$to->format('Y-m-d')}").") Tj ET\n";
        $content .= 'BT /F1 9 Tf 50 748 Td (Balance: GH₵ '.number_format((float) $user->balance, 2).") Tj ET\n";
        $content .= 'BT /F1 10 Tf 50 730 Td ('.$this->escape('=====================================================================').") Tj ET\n";

        $y = 712;
        $total = 0.0;
        foreach ($transactions as $tx) {
            if ($y < 70) {
                break;
            }
            $amount = (float) $tx->amount;
            $total += $amount;
            $sign = $amount >= 0 ? '+' : '-';
            $line = $this->escape(
                $tx->created_at?->format('Y-m-d H:i') ?? ''
                .'  '.str_pad((string) $tx->type, 9, ' ')
                .'  '.((string) $tx->reference)
                .'  '.$sign.'GH₵ '.number_format(abs($amount), 2)
            );
            $content .= "BT /F1 9 Tf 50 {$y} Td ({$line}) Tj ET\n";
            $y -= 16;
        }

        $content .= 'BT /F1 9 Tf 50 '.($y - 8).' Td ('.$this->escape('---------------------------------------------------------------------').") Tj ET\n";
        $content .= 'BT /F2 10 Tf 50 '.($y - 24).' Td (Statement total: GH₵ '.number_format($total, 2).") Tj ET\n";

        return $content;
    }

    private function addObject(int $id, string $data): void
    {
        $this->offsets[$id] = strlen($this->output);
        $this->output .= "{$id} 0 obj\n{$data}\nendobj\n";
    }

    private function escape(string $str): string
    {
        return str_replace(['(', ')', '\\'], ['\\(', '\\)', '\\\\'], $str);
    }
}
