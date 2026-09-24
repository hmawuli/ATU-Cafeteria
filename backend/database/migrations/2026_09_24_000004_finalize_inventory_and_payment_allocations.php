<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('order_items') && ! Schema::hasColumn('order_items', 'menu_item_id')) {
            Schema::table('order_items', function (Blueprint $table) {
                $table->foreignId('menu_item_id')->nullable()->after('food_item_id')
                    ->constrained('menu_items')->nullOnDelete();
                $table->index(['menu_item_id', 'created_at']);
            });
        }

        if (Schema::hasTable('food_items') && ! Schema::hasColumn('food_items', 'current_stock')) {
            Schema::table('food_items', function (Blueprint $table) {
                $table->integer('current_stock')->nullable()->after('is_available');
            });

            DB::table('food_items')
                ->whereNull('current_stock')
                ->update([
                    'current_stock' => DB::raw('COALESCE(initial_stock, 0)'),
                ]);
        }

        if (Schema::hasTable('demand_forecasts') && ! Schema::hasColumn('demand_forecasts', 'menu_item_id')) {
            Schema::table('demand_forecasts', function (Blueprint $table) {
                $table->foreignId('menu_item_id')->nullable()->after('food_item_id')
                    ->constrained('menu_items')->nullOnDelete();
                $table->index(['vendor_id', 'menu_item_id', 'forecast_date']);
            });
        }

        if (! Schema::hasTable('payment_allocations')) {
            Schema::create('payment_allocations', function (Blueprint $table) {
                $table->id();
                $table->foreignId('payment_id')->constrained('payments')->cascadeOnDelete();
                $table->foreignId('order_id')->constrained('orders')->cascadeOnDelete();
                $table->decimal('amount', 12, 2);
                $table->decimal('refunded_amount', 12, 2)->default(0);
                $table->timestamps();

                $table->unique(['payment_id', 'order_id']);
                $table->index(['order_id', 'payment_id']);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('payment_allocations');

        if (Schema::hasTable('demand_forecasts') && Schema::hasColumn('demand_forecasts', 'menu_item_id')) {
            Schema::table('demand_forecasts', function (Blueprint $table) {
                $table->dropForeign(['menu_item_id']);
                $table->dropIndex(['vendor_id', 'menu_item_id', 'forecast_date']);
                $table->dropColumn('menu_item_id');
            });
        }

        if (Schema::hasTable('food_items') && Schema::hasColumn('food_items', 'current_stock')) {
            Schema::table('food_items', function (Blueprint $table) {
                $table->dropColumn('current_stock');
            });
        }

        if (Schema::hasTable('order_items') && Schema::hasColumn('order_items', 'menu_item_id')) {
            Schema::table('order_items', function (Blueprint $table) {
                $table->dropForeign(['menu_item_id']);
                $table->dropIndex(['menu_item_id', 'created_at']);
                $table->dropColumn('menu_item_id');
            });
        }
    }
};
