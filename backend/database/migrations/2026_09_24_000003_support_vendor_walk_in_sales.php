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
                if (Schema::hasColumn('orders', 'customer_id')) {
                    $table->unsignedBigInteger('customer_id')->nullable()->change();
                }
                if (! Schema::hasColumn('orders', 'customer_name')) {
                    $table->string('customer_name', 160)->nullable();
                }
                if (! Schema::hasColumn('orders', 'customer_phone')) {
                    $table->string('customer_phone', 40)->nullable();
                }
                if (! Schema::hasColumn('orders', 'sales_channel')) {
                    $table->string('sales_channel', 20)->default('APP')->index();
                }
            });
        }

        if (Schema::hasTable('payments') && Schema::hasColumn('payments', 'customer_id')) {
            Schema::table('payments', function (Blueprint $table) {
                $table->unsignedBigInteger('customer_id')->nullable()->change();
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('payments') && Schema::hasColumn('payments', 'customer_id')) {
            Schema::table('payments', function (Blueprint $table) {
                $table->unsignedBigInteger('customer_id')->nullable(false)->change();
            });
        }

        if (Schema::hasTable('orders')) {
            Schema::table('orders', function (Blueprint $table) {
                foreach (['customer_name', 'customer_phone', 'sales_channel'] as $column) {
                    if (Schema::hasColumn('orders', $column)) {
                        $table->dropColumn($column);
                    }
                }
                $table->unsignedBigInteger('customer_id')->nullable(false)->change();
            });
        }
    }
};
