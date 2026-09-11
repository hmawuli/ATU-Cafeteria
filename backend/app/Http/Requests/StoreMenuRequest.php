<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Contracts\Validation\Validator;
use Illuminate\Http\Exceptions\HttpResponseException;

class StoreMenuRequest extends FormRequest
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
            'vendor_id' => 'required|integer|exists:users,id',
            'food_item_id' => 'required|integer|exists:food_items,id',
            'price' => 'required|numeric|min:0',
            'description' => 'nullable|string|max:1000',
            'is_available' => 'nullable|boolean',
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
                'message' => 'Validation error while registering menu item.',
                'errors' => $validator->errors()
            ], 400)
        );
    }
}
