<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('orders')) {
            Schema::table('orders', function (Blueprint $table) {
                if (! Schema::hasColumn('orders', 'order_number')) $table->string('order_number', 40)->nullable();
                if (! Schema::hasColumn('orders', 'order_type')) $table->string('order_type', 30)->default('TAKEAWAY');
                if (! Schema::hasColumn('orders', 'payment_method')) $table->string('payment_method', 30)->default('WALLET');
                if (! Schema::hasColumn('orders', 'payment_status')) $table->string('payment_status', 30)->default('PAID')->index();
                if (! Schema::hasColumn('orders', 'subtotal')) $table->decimal('subtotal', 12, 2)->default(0);
                if (! Schema::hasColumn('orders', 'discount_amount')) $table->decimal('discount_amount', 12, 2)->default(0);
                if (! Schema::hasColumn('orders', 'tax_amount')) $table->decimal('tax_amount', 12, 2)->default(0);
                if (! Schema::hasColumn('orders', 'service_fee')) $table->decimal('service_fee', 12, 2)->default(0);
                if (! Schema::hasColumn('orders', 'delivery_fee')) $table->decimal('delivery_fee', 12, 2)->default(0);
                if (! Schema::hasColumn('orders', 'grand_total')) $table->decimal('grand_total', 12, 2)->default(0);
                if (! Schema::hasColumn('orders', 'currency')) $table->string('currency', 3)->default('GHS');
                if (! Schema::hasColumn('orders', 'customer_note')) $table->text('customer_note')->nullable();
                if (! Schema::hasColumn('orders', 'cancellation_reason')) $table->string('cancellation_reason', 255)->nullable();
                if (! Schema::hasColumn('orders', 'placed_at')) $table->timestamp('placed_at')->nullable();
                if (! Schema::hasColumn('orders', 'confirmed_at')) $table->timestamp('confirmed_at')->nullable();
                if (! Schema::hasColumn('orders', 'cancelled_at')) $table->timestamp('cancelled_at')->nullable();
            });

            DB::table('orders')->whereNull('order_number')->orderBy('id')->each(function ($order) {
                DB::table('orders')->where('id', $order->id)->update([
                    'order_number' => 'CAF-'.now()->format('ymd').'-'.str_pad((string) $order->id, 8, '0', STR_PAD_LEFT),
                ]);
            });

            try {
                Schema::table('orders', fn (Blueprint $table) => $table->unique('order_number', 'orders_order_number_unique'));
            } catch (\Throwable) {
                // Existing deployments may already contain this constraint.
            }
        }

        if (Schema::hasTable('order_items')) {
            Schema::table('order_items', function (Blueprint $table) {
                if (! Schema::hasColumn('order_items', 'name_snapshot')) $table->string('name_snapshot')->nullable();
                if (! Schema::hasColumn('order_items', 'sku_snapshot')) $table->string('sku_snapshot', 100)->nullable();
                if (! Schema::hasColumn('order_items', 'discount_amount')) $table->decimal('discount_amount', 12, 2)->default(0);
                if (! Schema::hasColumn('order_items', 'tax_amount')) $table->decimal('tax_amount', 12, 2)->default(0);
                if (! Schema::hasColumn('order_items', 'line_total')) $table->decimal('line_total', 12, 2)->default(0);
            });
            DB::table('order_items')->whereNull('name_snapshot')->update(['name_snapshot' => DB::raw('name')]);
            DB::table('order_items')->where('line_total', 0)->update(['line_total' => DB::raw('total_price')]);
        }

        if (! Schema::hasTable('payments')) {
            Schema::create('payments', function (Blueprint $table) {
                $table->id();
                $table->foreignId('order_id')->nullable()->constrained('orders')->nullOnDelete();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->string('reference', 120)->unique();
                $table->string('gateway', 40)->default('paystack');
                $table->string('gateway_transaction_id', 120)->nullable()->index();
                $table->decimal('amount', 12, 2);
                $table->string('currency', 3)->default('GHS');
                $table->string('purpose', 40);
                $table->string('method', 40)->nullable();
                $table->string('status', 30)->default('INITIATED')->index();
                $table->json('gateway_response')->nullable();
                $table->timestamp('initiated_at')->nullable();
                $table->timestamp('paid_at')->nullable();
                $table->timestamp('failed_at')->nullable();
                $table->timestamp('refunded_at')->nullable();
                $table->timestamps();
            });
        }

        if (Schema::hasTable('wallet_transactions')) {
            Schema::table('wallet_transactions', function (Blueprint $table) {
                if (! Schema::hasColumn('wallet_transactions', 'order_id')) $table->foreignId('order_id')->nullable()->constrained('orders')->nullOnDelete();
                if (! Schema::hasColumn('wallet_transactions', 'payment_id')) $table->foreignId('payment_id')->nullable()->constrained('payments')->nullOnDelete();
                if (! Schema::hasColumn('wallet_transactions', 'source')) $table->string('source', 40)->default('SYSTEM');
                if (! Schema::hasColumn('wallet_transactions', 'performed_by')) $table->foreignId('performed_by')->nullable()->constrained('users')->nullOnDelete();
                if (! Schema::hasColumn('wallet_transactions', 'balance_before')) $table->decimal('balance_before', 12, 2)->nullable();
                if (! Schema::hasColumn('wallet_transactions', 'balance_after')) $table->decimal('balance_after', 12, 2)->nullable();
            });
        }

        if (! Schema::hasTable('order_status_histories')) {
            Schema::create('order_status_histories', function (Blueprint $table) {
                $table->id();
                $table->foreignId('order_id')->constrained('orders')->cascadeOnDelete();
                $table->string('from_status', 40)->nullable();
                $table->string('to_status', 40);
                $table->foreignId('changed_by')->nullable()->constrained('users')->nullOnDelete();
                $table->string('reason', 255)->nullable();
                $table->timestamp('changed_at');
                $table->timestamps();
                $table->index(['order_id', 'changed_at']);
            });
        }

        if (! Schema::hasTable('inventory_movements')) {
            Schema::create('inventory_movements', function (Blueprint $table) {
                $table->id();
                $table->foreignId('vendor_id')->nullable()->constrained('users')->nullOnDelete();
                $table->foreignId('food_item_id')->nullable()->constrained('food_items')->nullOnDelete();
                $table->foreignId('menu_item_id')->nullable()->constrained('menu_items')->nullOnDelete();
                $table->foreignId('order_id')->nullable()->constrained('orders')->nullOnDelete();
                $table->string('type', 30);
                $table->integer('quantity');
                $table->integer('balance_after')->nullable();
                $table->string('reference', 120)->nullable()->index();
                $table->string('reason', 255)->nullable();
                $table->foreignId('performed_by')->nullable()->constrained('users')->nullOnDelete();
                $table->timestamps();
                $table->index(['menu_item_id', 'created_at']);
                $table->index(['food_item_id', 'created_at']);
            });
        }

        if (! Schema::hasTable('inventory_reservations')) {
            Schema::create('inventory_reservations', function (Blueprint $table) {
                $table->id();
                $table->foreignId('order_id')->constrained('orders')->cascadeOnDelete();
                $table->foreignId('menu_item_id')->nullable()->constrained('menu_items')->nullOnDelete();
                $table->foreignId('food_item_id')->nullable()->constrained('food_items')->nullOnDelete();
                $table->unsignedInteger('quantity');
                $table->string('status', 20)->default('RESERVED');
                $table->timestamp('reserved_until');
                $table->timestamp('released_at')->nullable();
                $table->timestamps();
                $table->index(['status', 'reserved_until']);
            });
        }

        if (! Schema::hasTable('customer_devices')) {
            Schema::create('customer_devices', function (Blueprint $table) {
                $table->id();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->string('device_id', 191);
                $table->string('platform', 30);
                $table->text('push_token')->nullable();
                $table->string('app_version', 40)->nullable();
                $table->timestamp('last_seen_at')->nullable();
                $table->timestamp('revoked_at')->nullable();
                $table->timestamps();
                $table->unique(['customer_id', 'device_id']);
                $table->index(['customer_id', 'revoked_at']);
            });
        }

        if (! Schema::hasTable('customer_addresses')) {
            Schema::create('customer_addresses', function (Blueprint $table) {
                $table->id();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->string('label', 80);
                $table->string('contact_name', 160)->nullable();
                $table->string('phone', 30)->nullable();
                $table->string('address_line1', 255);
                $table->string('address_line2', 255)->nullable();
                $table->string('city', 120)->nullable();
                $table->string('landmark', 255)->nullable();
                $table->boolean('is_default')->default(false);
                $table->timestamps();
                $table->index(['customer_id', 'is_default']);
            });
        }

        if (! Schema::hasTable('refunds')) {
            Schema::create('refunds', function (Blueprint $table) {
                $table->id();
                $table->foreignId('order_id')->nullable()->constrained('orders')->nullOnDelete();
                $table->foreignId('payment_id')->nullable()->constrained('payments')->nullOnDelete();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('requested_by')->nullable()->constrained('users')->nullOnDelete();
                $table->decimal('amount', 12, 2);
                $table->string('reason', 255);
                $table->string('status', 30)->default('PENDING');
                $table->string('gateway_reference', 120)->nullable()->index();
                $table->timestamp('processed_at')->nullable();
                $table->timestamps();
            });
        }

        if (! Schema::hasTable('support_tickets')) {
            Schema::create('support_tickets', function (Blueprint $table) {
                $table->id();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('order_id')->nullable()->constrained('orders')->nullOnDelete();
                $table->string('category', 40);
                $table->string('subject', 180);
                $table->text('description');
                $table->string('priority', 20)->default('NORMAL');
                $table->string('status', 30)->default('OPEN');
                $table->foreignId('assigned_to')->nullable()->constrained('users')->nullOnDelete();
                $table->timestamp('resolved_at')->nullable();
                $table->timestamps();
                $table->index(['customer_id', 'status']);
            });
        }

        if (! Schema::hasTable('promotions')) {
            Schema::create('promotions', function (Blueprint $table) {
                $table->id();
                $table->string('code', 50)->unique();
                $table->string('name', 160);
                $table->string('type', 20);
                $table->decimal('value', 12, 2);
                $table->decimal('minimum_order_amount', 12, 2)->default(0);
                $table->decimal('maximum_discount_amount', 12, 2)->nullable();
                $table->unsignedInteger('usage_limit')->nullable();
                $table->unsignedInteger('per_customer_limit')->nullable();
                $table->timestamp('starts_at')->nullable();
                $table->timestamp('ends_at')->nullable();
                $table->boolean('is_active')->default(true)->index();
                $table->timestamps();
                $table->index(['is_active', 'starts_at', 'ends_at']);
            });
        }

        if (! Schema::hasTable('promotion_redemptions')) {
            Schema::create('promotion_redemptions', function (Blueprint $table) {
                $table->id();
                $table->foreignId('promotion_id')->constrained('promotions')->cascadeOnDelete();
                $table->foreignId('customer_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('order_id')->constrained('orders')->cascadeOnDelete();
                $table->decimal('discount_amount', 12, 2);
                $table->timestamps();
                $table->unique(['promotion_id', 'customer_id', 'order_id']);
            });
        }

        if (! Schema::hasTable('vendor_settlements')) {
            Schema::create('vendor_settlements', function (Blueprint $table) {
                $table->id();
                $table->foreignId('vendor_id')->constrained('users')->cascadeOnDelete();
                $table->string('period_start', 10);
                $table->string('period_end', 10);
                $table->decimal('gross_sales', 12, 2)->default(0);
                $table->decimal('refunds', 12, 2)->default(0);
                $table->decimal('fees', 12, 2)->default(0);
                $table->decimal('net_amount', 12, 2)->default(0);
                $table->string('status', 30)->default('PENDING');
                $table->string('payout_reference', 120)->nullable()->index();
                $table->timestamp('settled_at')->nullable();
                $table->timestamps();
                $table->unique(['vendor_id', 'period_start', 'period_end']);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('vendor_settlements');
        Schema::dropIfExists('promotion_redemptions');
        Schema::dropIfExists('promotions');
        Schema::dropIfExists('support_tickets');
        Schema::dropIfExists('refunds');
        Schema::dropIfExists('customer_addresses');
        Schema::dropIfExists('customer_devices');
        Schema::dropIfExists('inventory_reservations');
        Schema::dropIfExists('inventory_movements');
        Schema::dropIfExists('order_status_histories');
        Schema::table('wallet_transactions', function (Blueprint $table) {
            foreach (['balance_before', 'balance_after', 'source', 'performed_by', 'payment_id', 'order_id'] as $column) {
                if (Schema::hasColumn('wallet_transactions', $column)) $table->dropColumn($column);
            }
        });
        Schema::dropIfExists('payments');
        Schema::table('order_items', function (Blueprint $table) {
            foreach (['name_snapshot', 'sku_snapshot', 'discount_amount', 'tax_amount', 'line_total'] as $column) {
                if (Schema::hasColumn('order_items', $column)) $table->dropColumn($column);
            }
        });
        Schema::table('orders', function (Blueprint $table) {
            foreach (['order_number','order_type','payment_method','payment_status','subtotal','discount_amount','tax_amount','service_fee','delivery_fee','grand_total','currency','customer_note','cancellation_reason','placed_at','confirmed_at','cancelled_at'] as $column) {
                if (Schema::hasColumn('orders', $column)) $table->dropColumn($column);
            }
        });
    }
};
