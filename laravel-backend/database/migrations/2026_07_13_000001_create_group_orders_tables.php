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
        Schema::create('group_orders', function (Blueprint $table) {
            $table->id();
            $table->string('code')->unique();
            $table->unsignedBigInteger('creator_id');
            $table->unsignedBigInteger('vendor_id');
            $table->string('status')->default('OPEN'); // OPEN, LOCKED, COMPLETED, CANCELLED
            $table->string('payment_mode')->default('HOST_PAYS'); // HOST_PAYS, INDIVIDUAL
            $table->timestamp('expires_at')->nullable();
            $table->timestamps();

            $table->foreign('creator_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
        });

        Schema::create('group_order_items', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('group_order_id');
            $table->unsignedBigInteger('user_id');
            $table->unsignedBigInteger('menu_item_id');
            $table->integer('quantity');
            $table->string('custom_notes')->nullable();
            $table->timestamps();

            $table->foreign('group_order_id')->references('id')->on('group_orders')->onDelete('cascade');
            $table->foreign('user_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('menu_item_id')->references('id')->on('menu_items')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('group_order_items');
        Schema::dropIfExists('group_orders');
    }
};
