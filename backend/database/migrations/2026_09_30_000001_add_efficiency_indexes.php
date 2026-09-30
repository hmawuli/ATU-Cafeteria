<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Efficiency indexes for the remaining hot query paths (order line items,
     * inventory ledgers, payment lookups) so dashboards and vendor operations
     * stay fast as the campus grows.
     */
    public function up(): void
    {
        $plan = [
            'order_items' => [
                [['order_id'], 'order_items_order_id_idx'],
                [['menu_item_id'], 'order_items_menu_item_id_idx'],
            ],
            'inventory_movements' => [
                [['vendor_id', 'created_at'], 'inventory_movements_vendor_created_idx'],
            ],
            'payments' => [
                [['customer_id'], 'payments_customer_id_idx'],
            ],
        ];

        foreach ($plan as $table => $definitions) {
            if (! Schema::hasTable($table)) {
                continue;
            }

            foreach ($definitions as [$columns, $name]) {
                try {
                    if (Schema::hasIndex($table, $name)) {
                        continue;
                    }
                    Schema::table($table, function (Blueprint $t) use ($columns, $name) {
                        $t->index($columns, $name);
                    });
                } catch (Throwable) {
                    // Index may already exist under a different generated name.
                }
            }
        }
    }

    public function down(): void
    {
        foreach ([
            'order_items' => ['order_items_order_id_idx', 'order_items_menu_item_id_idx'],
            'inventory_movements' => ['inventory_movements_vendor_created_idx'],
            'payments' => ['payments_customer_id_idx'],
        ] as $table => $names) {
            if (! Schema::hasTable($table)) {
                continue;
            }
            foreach ($names as $name) {
                try {
                    Schema::table($table, fn (Blueprint $t) => $t->dropIndex($name));
                } catch (Throwable) {
                }
            }
        }
    }
};
