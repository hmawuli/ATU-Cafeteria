<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Contracts\Validation\Validator;
use Illuminate\Http\Exceptions\HttpResponseException;

class CreateOrderRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    /**
     * Prepare inputs prior to validation.
     */
    protected function prepareForValidation(): void
    {
        if ($this->has('student_id') && !$this->has('customer_id')) {
            $this->merge(['customer_id' => $this->input('student_id')]);
        } elseif ($this->has('customer_id') && !$this->has('student_id')) {
            $this->merge(['student_id' => $this->input('customer_id')]);
        }
    }

    /**
     * Get the validation rules that apply to order creation.
     */
    public function rules(): array
    {
        return [
            'customer_id' => 'required|integer|exists:users,id',
            'vendor_id' => 'required|integer|exists:users,id',
            'menu_item_id' => 'required|integer|exists:menu_items,id',
            'food_name' => 'required|string|min:2|max:255',
            'quantity' => 'required|integer|min:1|max:50',
            'unit_price' => 'required|numeric|min:0.01',
            'total_price' => 'required|numeric|min:0.01',
            'payment_method' => 'nullable|string|in:WALLET,MOMO,CASH,POINTS',
            'notes' => 'nullable|string|max:500',
        ];
    }

    /**
     * Custom validation messages.
     */
    public function messages(): array
    {
        return [
            'customer_id.required' => 'Customer user identification is required.',
            'vendor_id.required' => 'Target vendor identification is required.',
            'menu_item_id.required' => 'Selected menu item ID is invalid or missing.',
            'quantity.min' => 'Order quantity must be at least 1 portion.',
            'quantity.max' => 'Maximum allowed single order portion limit is 50.',
            'total_price.min' => 'Order total price must be greater than zero.',
        ];
    }

    /**
     * Format a consistent JSON error structure when validation fails.
     */
    protected function failedValidation(Validator $validator)
    {
        throw new HttpResponseException(
            response()->json([
                'success' => false,
                'message' => 'Order request validation failed.',
                'errors' => $validator->errors(),
                'status_code' => 422
            ], 422)
        );
    }
}
