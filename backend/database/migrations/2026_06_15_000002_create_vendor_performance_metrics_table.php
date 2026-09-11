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
        Schema::create('vendor_performance_metrics', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('vendor_id');
            $table->integer('total_orders')->default(0);
            $table->integer('total_completed_orders')->default(0);
            $table->decimal('total_sales', 10, 2)->default(0.00);
            $table->decimal('avg_completion_time_minutes', 5, 2)->default(0.00);
            $table->decimal('order_fulfillment_rate', 5, 2)->default(100.00);
            $table->decimal('rating_food_quality', 3, 2)->default(4.50);
            $table->decimal('rating_cleanliness', 3, 2)->default(4.20);
            $table->decimal('rating_service_speed', 3, 2)->default(4.30);
            $table->decimal('rating_price_value', 3, 2)->default(4.60);
            $table->decimal('rating_overall', 3, 2)->default(4.40);
            $table->json('popular_menu_items')->nullable(); // Store popular items as JSON payload
            $table->timestamp('calculated_at')->nullable();
            $table->timestamps();

            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('vendor_performance_metrics');
    }
};
