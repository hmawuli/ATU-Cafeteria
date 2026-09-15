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
        // 1. Menu Categories (e.g., Breakfast, Lunch Specials, Local Dishes, Drinks)
        Schema::create('menu_categories', function (Blueprint $table) {
            $table->id();
            $table->string('name'); // e.g., "Traditional Ghanaian"
            $table->string('slug')->unique();
            $table->text('description')->nullable();
            $table->boolean('is_active')->default(true);
            $table->integer('sort_order')->default(0);
            $table->timestamps();
        });

        // 2. Comprehensive Menus definition (Tying category, vendors and availability schedules)
        Schema::create('vendor_menus', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('vendor_id'); // Foreign key to vendors or users table with 'VENDOR' role
            $table->unsignedBigInteger('category_id')->nullable();
            $table->string('title'); // e.g., "Mary Joint Lunch Promo"
            $table->text('description')->nullable();
            $table->string('banner_image')->nullable();
            $table->time('available_from')->default('07:00:00'); // Availability hours
            $table->time('available_until')->default('18:00:00');
            $table->string('seasonal_tag')->nullable(); // e.g., "Harmattan specials"
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
            $table->foreign('category_id')->references('id')->on('menu_categories')->onDelete('set null');
        });

        // 3. Extended Menu Items within the menu structure
        Schema::create('vendor_menu_items', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('menu_id');
            $table->string('name'); // e.g., "Waakye Deluxe"
            $table->text('description')->nullable();
            $table->decimal('price', 10, 2); // standard pricing
            $table->decimal('discount_price', 10, 2)->nullable(); // promotional pricing
            $table->string('image_url')->nullable();
            $table->integer('preparation_time_minutes')->default(15);

            // Dietary & Compliance Properties (ATU Health standards)
            $table->boolean('is_halal')->default(true);
            $table->boolean('is_vegetarian')->default(false);
            $table->boolean('is_vegan')->default(false);
            $table->string('allergens_notice')->nullable(); // e.g., "Contains peanuts, fish"

            // Stock & Delivery variables
            $table->boolean('is_available')->default(true);
            $table->integer('daily_stock_limit')->default(-1); // -1 for unlimited
            $table->integer('current_stock_count')->default(0);

            $table->timestamps();

            $table->foreign('menu_id')->references('id')->on('vendor_menus')->onDelete('cascade');
        });

        // 4. Menu Item Options & Addons Schema (e.g., double eggs, extra fish, packaging)
        Schema::create('menu_item_options', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('menu_item_id');
            $table->string('name'); // e.g., "Choose Protein", "Extra toppings"
            $table->string('type')->default('checkbox'); // checkbox, radio, select
            $table->integer('min_selections')->default(0);
            $table->integer('max_selections')->default(1);
            $table->boolean('is_required')->default(false);
            $table->timestamps();

            $table->foreign('menu_item_id')->references('id')->on('vendor_menu_items')->onDelete('cascade');
        });

        // 5. Menu Item Option Values (Actual sub-choices with adjusted price deltas)
        Schema::create('menu_item_option_values', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('option_id');
            $table->string('value_name'); // e.g., "Boiled Egg", "Double Beef", "Shito"
            $table->decimal('price_modifier', 10, 2)->default(0.00); // delta addition (e.g. +3.00 GH¢)
            $table->boolean('is_in_stock')->default(true);
            $table->timestamps();

            $table->foreign('option_id')->references('id')->on('menu_item_options')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('menu_item_option_values');
        Schema::dropIfExists('menu_item_options');
        Schema::dropIfExists('vendor_menu_items');
        Schema::dropIfExists('vendor_menus');
        Schema::dropIfExists('menu_categories');
    }
};
