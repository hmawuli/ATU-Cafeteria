<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('orders')) {
            Schema::table('orders', function (Blueprint $table) {
                if (! Schema::hasColumn('orders', 'payment_id')) {
                    $table->foreignId('payment_id')->nullable()->constrained('payments')->nullOnDelete();
                }
            });
        }

        if (Schema::hasTable('order_items') && Schema::hasColumn('order_items', 'food_item_id')) {
            Schema::table('order_items', function (Blueprint $table) {
                $table->unsignedBigInteger('food_item_id')->nullable()->change();
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('order_items') && Schema::hasColumn('order_items', 'food_item_id')) {
            Schema::table('order_items', function (Blueprint $table) {
                $table->unsignedBigInteger('food_item_id')->nullable(false)->change();
            });
        }

        if (Schema::hasTable('orders') && Schema::hasColumn('orders', 'payment_id')) {
            Schema::table('orders', function (Blueprint $table) {
                $table->dropConstrainedForeignId('payment_id');
            });
        }
    }
};
