<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class UpdateFoodItemRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        $user = $this->user();

        return $user && (strtoupper($user->role) === 'VENDOR' || strtoupper($user->role) === 'ADMIN');
    }

    /**
     * Get the validation rules that apply to the request.
     */
    public function rules(): array
    {
        return [
            'name' => 'nullable|string|max:255',
            'price' => 'nullable|numeric|min:0',
            'category' => 'nullable|string',
            'description' => 'nullable|string|min:10|max:1000',
            'image_url' => 'nullable|string',
            'is_available' => 'nullable|boolean',
            'initial_stock' => 'nullable|integer|min:1',
            'low_stock_threshold' => 'nullable|integer|min:0',
        ];
    }

    /**
     * Handle a failed validation attempt.
     */
    protected function failedValidation(Validator $validator)
    {
        throw new HttpResponseException(
            response()->json([
                'success' => false,
                'message' => 'Input validations failed.',
                'errors' => $validator->errors(),
            ], 400)
        );
    }
}
