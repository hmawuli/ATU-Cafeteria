<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations to add high-performance composite indexes
     * for vendor dashboard metrics, student order lookups, and fast filtering.
     */
    public function up(): void
    {
        // 1. Performance Indexes on ORDERS Table
        Schema::table('orders', function (Blueprint $table) {
            // Composite index for Vendor Dashboard active orders filter (vendor_id + status)
            $table->index(['vendor_id', 'status'], 'idx_orders_vendor_status');

            // Composite index for Vendor sales performance timeline (vendor_id + order_timestamp)
            $table->index(['vendor_id', 'order_timestamp'], 'idx_orders_vendor_timestamp');

            // Composite index for Student active & historical order lookups (customer_id + status)
            $table->index(['customer_id', 'status'], 'idx_orders_customer_status');

            // Composite index for Student order history timeline sorting (customer_id + order_timestamp)
            $table->index(['customer_id', 'order_timestamp'], 'idx_orders_customer_timestamp');

            // Index for Quick Pickup Verification by PIN
            $table->index(['pickup_pin', 'status'], 'idx_orders_pickup_pin');
        });

        // 2. Performance Indexes on MENU_ITEMS Table
        Schema::table('menu_items', function (Blueprint $table) {
            // Composite index for fast vendor menu availability lookups
            $table->index(['vendor_id', 'is_available'], 'idx_menu_items_vendor_available');

            // Index for category filtering across cafeteria stands
            $table->index(['category', 'is_available'], 'idx_menu_items_category_available');
        });

        // 3. Performance Indexes on WALLET_TRANSACTIONS Table
        Schema::table('wallet_transactions', function (Blueprint $table) {
            // Index for fast student wallet history retrieval
            $table->index(['user_id', 'created_at'], 'idx_wallet_transactions_user_created');
        });

        // 4. Performance Indexes on CHAT_MESSAGES Table
        if (Schema::hasTable('chat_messages')) {
            Schema::table('chat_messages', function (Blueprint $table) {
                // Index for chat history lookups by sender and by receiver
                // (the chat_messages table has sender_id / receiver_id, not order_id)
                $table->index(['sender_id', 'created_at'], 'idx_chat_messages_sender_created');
                $table->index(['receiver_id', 'created_at'], 'idx_chat_messages_receiver_created');
            });
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dropIndex('idx_orders_vendor_status');
            $table->dropIndex('idx_orders_vendor_timestamp');
            $table->dropIndex('idx_orders_customer_status');
            $table->dropIndex('idx_orders_customer_timestamp');
            $table->dropIndex('idx_orders_pickup_pin');
        });

        Schema::table('menu_items', function (Blueprint $table) {
            $table->dropIndex('idx_menu_items_vendor_available');
            $table->dropIndex('idx_menu_items_category_available');
        });

        Schema::table('wallet_transactions', function (Blueprint $table) {
            $table->dropIndex('idx_wallet_transactions_user_created');
        });

        if (Schema::hasTable('chat_messages')) {
            Schema::table('chat_messages', function (Blueprint $table) {
                $table->dropIndex('idx_chat_messages_sender_created');
                $table->dropIndex('idx_chat_messages_receiver_created');
            });
        }
    }
};
