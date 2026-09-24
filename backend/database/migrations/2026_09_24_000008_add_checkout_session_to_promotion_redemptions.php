<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('promotion_redemptions') && ! Schema::hasColumn('promotion_redemptions', 'checkout_session_id')) {
            Schema::table('promotion_redemptions', function (Blueprint $table) {
                $table->foreignId('checkout_session_id')
                    ->nullable()
                    ->after('order_id')
                    ->constrained('checkout_sessions')
                    ->nullOnDelete();

                $table->index(['promotion_id', 'customer_id', 'checkout_session_id']);
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('promotion_redemptions') && Schema::hasColumn('promotion_redemptions', 'checkout_session_id')) {
            Schema::table('promotion_redemptions', function (Blueprint $table) {
                $table->dropForeign(['checkout_session_id']);
                $table->dropIndex(['promotion_id', 'customer_id', 'checkout_session_id']);
                $table->dropColumn('checkout_session_id');
            });
        }
    }
};
