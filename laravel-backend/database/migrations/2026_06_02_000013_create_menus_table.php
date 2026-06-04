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
        Schema::create('menus', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('vendor_id');
            $table->unsignedBigInteger('food_item_id');
            $table->decimal('price', 10, 2);
            $table->text('description')->nullable();
            $table->boolean('is_available')->default(true);
            $table->timestamps();

            $table->foreign('vendor_id')->references('id')->on('vendors')->onDelete('cascade');
            $table->foreign('food_item_id')->references('id')->on('food_items')->onDelete('cascade');
            
            // Unique index to prevent duplicate associations of same food item to same vendor menu
            $table->unique(['vendor_id', 'food_item_id']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('menus');
    }
};
