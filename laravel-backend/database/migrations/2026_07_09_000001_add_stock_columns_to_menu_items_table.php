<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        if (Schema::hasTable('menu_items')) {
            Schema::table('menu_items', function (Blueprint $table) {
                if (!Schema::hasColumn('menu_items', 'initial_stock')) {
                    $table->integer('initial_stock')->default(50)->after('is_available');
                }
                if (!Schema::hasColumn('menu_items', 'current_stock')) {
                    $table->integer('current_stock')->default(50)->after('initial_stock');
                }
                if (!Schema::hasColumn('menu_items', 'low_stock_threshold')) {
                    $table->integer('low_stock_threshold')->default(10)->after('current_stock');
                }
            });
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        if (Schema::hasTable('menu_items')) {
            Schema::table('menu_items', function (Blueprint $table) {
                $columns = [];
                if (Schema::hasColumn('menu_items', 'initial_stock')) {
                    $columns[] = 'initial_stock';
                }
                if (Schema::hasColumn('menu_items', 'current_stock')) {
                    $columns[] = 'current_stock';
                }
                if (Schema::hasColumn('menu_items', 'low_stock_threshold')) {
                    $columns[] = 'low_stock_threshold';
                }
                if (count($columns) > 0) {
                    $table->dropColumn($columns);
                }
            });
        }
    }
};
