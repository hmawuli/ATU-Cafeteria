<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('checkout_sessions')) {
            Schema::create('checkout_sessions', function (Blueprint $table) {
                $table->id();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->string('reference', 120)->unique();
                $table->decimal('total_amount', 12, 2)->default(0);
                $table->string('currency', 3)->default('GHS');
                $table->string('payment_method', 30)->default('WALLET');
                $table->string('status', 30)->default('PAID');
                $table->timestamps();
                $table->index(['customer_id', 'created_at']);
            });
        }

        if (Schema::hasTable('orders') && ! Schema::hasColumn('orders', 'checkout_session_id')) {
            Schema::table('orders', function (Blueprint $table) {
                $table->foreignId('checkout_session_id')->nullable()->after('payment_id')
                    ->constrained('checkout_sessions')->nullOnDelete();
                $table->index('checkout_session_id');
            });
        }

        if (Schema::hasTable('payments') && ! Schema::hasColumn('payments', 'checkout_session_id')) {
            Schema::table('payments', function (Blueprint $table) {
                $table->foreignId('checkout_session_id')->nullable()->after('order_id')
                    ->constrained('checkout_sessions')->nullOnDelete();
                $table->index('checkout_session_id');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('orders') && Schema::hasColumn('orders', 'checkout_session_id')) {
            Schema::table('orders', function (Blueprint $table) {
                $table->dropForeign(['checkout_session_id']);
                $table->dropColumn('checkout_session_id');
            });
        }

        if (Schema::hasTable('payments') && Schema::hasColumn('payments', 'checkout_session_id')) {
            Schema::table('payments', function (Blueprint $table) {
                $table->dropForeign(['checkout_session_id']);
                $table->dropColumn('checkout_session_id');
            });
        }

        Schema::dropIfExists('checkout_sessions');
    }
};
