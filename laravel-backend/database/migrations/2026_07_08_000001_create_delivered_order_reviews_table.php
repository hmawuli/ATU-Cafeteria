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
        Schema::create('delivered_order_reviews', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('order_id');
            $table->unsignedBigInteger('student_id');
            $table->unsignedBigInteger('vendor_id');
            $table->unsignedBigInteger('food_item_id')->nullable();
            
            // Vendor review details
            $table->integer('vendor_rating')->nullable(); // 1 to 5 stars
            $table->text('vendor_comment')->nullable();
            
            // Food item review details
            $table->integer('food_rating')->nullable(); // 1 to 5 stars
            $table->text('food_comment')->nullable();
            
            $table->timestamps();

            // Setup foreign key constraints
            $table->foreign('order_id')->references('id')->on('orders')->onDelete('cascade');
            $table->foreign('student_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('delivered_order_reviews');
    }
};
