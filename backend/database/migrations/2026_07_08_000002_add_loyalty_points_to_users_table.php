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
        Schema::table('users', function (Blueprint $table) {
            $table->integer('loyalty_points')->default(0)->after('balance');
            $table->decimal('total_spent', 12, 2)->default(0.00)->after('loyalty_points');
        });

        Schema::table('orders', function (Blueprint $table) {
            $table->integer('points_redeemed')->default(0)->after('total_price');
            $table->decimal('discount_applied', 8, 2)->default(0.00)->after('points_redeemed');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['loyalty_points', 'total_spent']);
        });

        Schema::table('orders', function (Blueprint $table) {
            $table->dropColumn(['points_redeemed', 'discount_applied']);
        });
    }
};
