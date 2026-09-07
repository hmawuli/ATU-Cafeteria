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
        Schema::create('users', function (Blueprint $table) {
            $table->id();
            $table->string('username')->unique();
            $table->string('password'); // Laravel adaptive password hash (bcrypt/argon configuration).
            $table->string('role');     // STUDENT, VENDOR, ADMIN
            $table->string('fullName');
            $table->string('student_staff_id')->nullable()->unique(); // ATU Student or Staff unique identifier registration ID
            $table->text('profile_info')->nullable(); // Consolidated profile metadata e.g. department, telephone, program of study
            $table->text('info')->nullable();
            $table->decimal('balance', 10, 2)->default(0.00);
            $table->boolean('is_open')->default(true);
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('users');
    }
};
