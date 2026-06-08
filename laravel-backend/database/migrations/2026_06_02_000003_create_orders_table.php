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
        Schema::create('orders', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('customer_id');
            $table->unsignedBigInteger('student_id')->nullable(); // Explicit student_id representation
            $table->unsignedBigInteger('user_id')->nullable(); // Standardised user_id for enterprise orders
            $table->unsignedBigInteger('vendor_id');
            $table->unsignedBigInteger('food_item_id')->nullable(); // Nullable to support advanced multi-item packages
            $table->unsignedBigInteger('menu_item_id')->nullable(); // Link to standard MenuItems
            $table->string('food_name')->nullable();
            $table->integer('quantity')->nullable();
            $table->decimal('unit_price', 10, 2)->nullable();
            $table->decimal('total_price', 10, 2);
            $table->bigInteger('order_timestamp');
            $table->string('status')->default('PENDING'); // PENDING, PREPARING, READY, COMPLETED, DECLINED, CANCELLED
            $table->string('pickup_pin'); // Secure 4 digit code to protect custodial hand-offs
            $table->string('estimated_pickup_time')->default('Calculating...');
            $table->timestamps();

            $table->foreign('customer_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('student_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('user_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('menu_item_id')->references('id')->on('menu_items')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('orders');
    }
};
