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
        // 1. Table for detailed vendor-specific menu/item availability schedules
        Schema::create('vendor_menu_availabilities', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('vendor_id');
            $table->unsignedBigInteger('menu_item_id');
            $table->string('day_of_week'); // monday, tuesday, etc., or 'all'
            $table->time('start_time')->nullable(); // e.g., 08:00:00
            $table->time('end_time')->nullable();   // e.g., 17:00:00
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('menu_item_id')->references('id')->on('menu_items')->onDelete('cascade');
            $table->unique(['vendor_id', 'menu_item_id', 'day_of_week'], 'vendor_menu_day_unique');
        });

        // 2. Table for daily cached order summaries for fast retrieval
        Schema::create('vendor_order_summaries', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('vendor_id');
            $table->date('summary_date');
            $table->integer('total_orders')->default(0);
            $table->integer('completed_orders')->default(0);
            $table->integer('pending_orders')->default(0);
            $table->decimal('total_revenue', 10, 2)->default(0.00);
            $table->decimal('average_rating', 3, 2)->default(5.00);
            $table->timestamps();

            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
            $table->unique(['vendor_id', 'summary_date']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('vendor_order_summaries');
        Schema::dropIfExists('vendor_menu_availabilities');
    }
};
