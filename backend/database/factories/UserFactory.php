<?php

namespace Database\Factories;

use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Facades\Hash;

/**
 * @extends Factory<User>
 */
class UserFactory extends Factory
{
    protected $model = User::class;

    public function definition(): array
    {
        return [
            'username' => fake()->unique()->userName(),
            'password' => Hash::make('1234'),
            'role' => 'STUDENT',
            'fullName' => fake()->name(),
            'student_staff_id' => fake()->unique()->bothify('ATU-####-???'),
            'profile_info' => [
                'email' => fake()->unique()->safeEmail(),
            ],
            'info' => null,
            'balance' => 250.00,
            'is_open' => true,
            'loyalty_points' => 0,
            'total_spent' => 0.00,
            'account_status' => 'ACTIVE',
            'admin_level' => null,
            'last_login_at' => null,
            'two_factor_enabled' => false,
            'email_verified_at' => null,
        ];
    }
}
