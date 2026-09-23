<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->index(['customer_id', 'created_at'], 'orders_customer_created_idx');
            $table->index(['student_id', 'created_at'], 'orders_student_created_idx');
            $table->index(['vendor_id', 'status', 'created_at'], 'orders_vendor_status_created_idx');
            $table->index(['status', 'created_at'], 'orders_status_created_idx');
            $table->index('food_item_id', 'orders_food_item_idx');
            $table->index('menu_item_id', 'orders_menu_item_idx');
        });

        Schema::table('menu_items', function (Blueprint $table) {
            $table->index(['vendor_id', 'is_available'], 'menu_items_vendor_available_idx');
            $table->index(['category', 'is_available'], 'menu_items_category_available_idx');
        });

        Schema::table('wallet_transactions', function (Blueprint $table) {
            $table->index(['user_id', 'created_at'], 'wallet_transactions_user_created_idx');
            $table->index(['type', 'status', 'created_at'], 'wallet_transactions_type_status_created_idx');
        });

        Schema::table('audit_logs', function (Blueprint $table) {
            $table->index(['user_id', 'created_at'], 'audit_logs_user_created_idx');
            $table->index(['action', 'created_at'], 'audit_logs_action_created_idx');
        });
    }

    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dropIndex('orders_customer_created_idx');
            $table->dropIndex('orders_student_created_idx');
            $table->dropIndex('orders_vendor_status_created_idx');
            $table->dropIndex('orders_status_created_idx');
            $table->dropIndex('orders_food_item_idx');
            $table->dropIndex('orders_menu_item_idx');
        });

        Schema::table('menu_items', function (Blueprint $table) {
            $table->dropIndex('menu_items_vendor_available_idx');
            $table->dropIndex('menu_items_category_available_idx');
        });

        Schema::table('wallet_transactions', function (Blueprint $table) {
            $table->dropIndex('wallet_transactions_user_created_idx');
            $table->dropIndex('wallet_transactions_type_status_created_idx');
        });

        Schema::table('audit_logs', function (Blueprint $table) {
            $table->dropIndex('audit_logs_user_created_idx');
            $table->dropIndex('audit_logs_action_created_idx');
        });
    }
};
