<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        $indexes = [
            'users' => [['username'], ['role']],
            'orders' => [['customer_id', 'status'], ['vendor_id', 'status'], ['created_at'], ['order_status']],
            'food_items' => [['vendor_id'], ['is_available']],
            'menu_items' => [['vendor_id'], ['is_available']],
            'wallet_transactions' => [['user_id', 'created_at']],
            'audit_logs' => [['user_id', 'created_at'], ['action']],
            'feedback' => [['vendor_id', 'created_at'], ['customer_id', 'created_at']],
        ];
        foreach ($indexes as $table => $sets) {
            if (! Schema::hasTable($table)) {
                continue;
            }
            foreach ($sets as $columns) {
                $name = $table.'_'.implode('_', $columns).'_idx';
                try {
                    Schema::table($table, fn (Blueprint $t) => $t->index($columns, $name));
                } catch (Throwable) {
                }
            }
        }
    }

    public function down(): void {}
};
