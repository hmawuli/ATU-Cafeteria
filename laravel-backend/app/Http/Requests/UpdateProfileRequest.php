<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Contracts\Validation\Validator;
use Illuminate\Http\Exceptions\HttpResponseException;
use Illuminate\Validation\Rule;

class UpdateProfileRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to perform profile updates.
     */
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    /**
     * Get the validation rules for profile updates.
     */
    public function rules(): array
    {
        $userId = $this->user()?->id ?? $this->input('id');

        return [
            'fullName' => 'nullable|string|min:2|max:150',
            'username' => [
                'nullable',
                'string',
                'min:3',
                'max:50',
                Rule::unique('users', 'username')->ignore($userId)
            ],
            'info' => 'nullable|string|max:255',
            'phoneNumber' => 'nullable|string|max:20',
            'dietaryPreferences' => 'nullable|string|max:255',
            'pin' => 'nullable|string|min:4|max:8',
        ];
    }

    /**
     * Custom validation messages.
     */
    public function messages(): array
    {
        return [
            'username.unique' => 'This username is already registered to another account.',
            'fullName.min' => 'Full name must contain at least 2 characters.',
            'pin.min' => 'Account PIN must be at least 4 digits.',
        ];
    }

    /**
     * Handle failed validation with standard JSON format.
     */
    protected function failedValidation(Validator $validator)
    {
        throw new HttpResponseException(
            response()->json([
                'success' => false,
                'message' => 'Profile update validation failed.',
                'errors' => $validator->errors(),
                'status_code' => 422
            ], 422)
        );
    }
}
