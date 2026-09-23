<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->index(['student_id', 'created_at'], 'orders_student_created_idx');
            $table->index(['vendor_id', 'status', 'created_at'], 'orders_vendor_status_created_idx');
            $table->index(['customer_id', 'status', 'created_at'], 'orders_customer_status_created_idx');
            $table->index(['user_id', 'status', 'created_at'], 'orders_user_status_created_idx');
        });

        Schema::table('wallet_transactions', function (Blueprint $table) {
            $table->index(['user_id', 'created_at'], 'wallet_user_created_idx');
            $table->index(['user_id', 'status', 'created_at'], 'wallet_user_status_created_idx');
            $table->index(['type', 'status', 'created_at'], 'wallet_type_status_created_idx');
        });

        Schema::table('audit_logs', function (Blueprint $table) {
            $table->index(['user_id', 'created_at'], 'audit_user_created_idx');
            $table->index(['action', 'created_at'], 'audit_action_created_idx');
        });
    }

    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dropIndex('orders_student_created_idx');
            $table->dropIndex('orders_vendor_status_created_idx');
            $table->dropIndex('orders_customer_status_created_idx');
            $table->dropIndex('orders_user_status_created_idx');
        });

        Schema::table('wallet_transactions', function (Blueprint $table) {
            $table->dropIndex('wallet_user_created_idx');
            $table->dropIndex('wallet_user_status_created_idx');
            $table->dropIndex('wallet_type_status_created_idx');
        });

        Schema::table('audit_logs', function (Blueprint $table) {
            $table->dropIndex('audit_user_created_idx');
            $table->dropIndex('audit_action_created_idx');
        });
    }
};
