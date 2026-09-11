<?php

namespace App\Http\Requests;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Contracts\Validation\Validator;
use Illuminate\Http\Exceptions\HttpResponseException;

class StoreOrderRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    /**
     * Prepare the data for validation.
     */
    protected function prepareForValidation()
    {
        if ($this->has('student_id') && !$this->has('customer_id')) {
            $this->merge(['customer_id' => $this->input('student_id')]);
        } elseif ($this->has('customer_id') && !$this->has('student_id')) {
            $this->merge(['student_id' => $this->input('customer_id')]);
        }
    }

    /**
     * Get the validation rules that apply to the request.
     */
    public function rules(): array
    {
        return [
            'customer_id' => 'required|integer|exists:users,id',
            'student_id' => 'required|integer|exists:users,id',
            'vendor_id' => 'required|integer|exists:users,id',
            'food_item_id' => 'nullable|integer',
            'menu_item_id' => 'required|integer|exists:menu_items,id',
            'food_name' => 'required|string|min:2',
            'quantity' => 'required|integer|min:1',
            'unit_price' => 'required|numeric|min:0.01',
            'total_price' => 'required|numeric|min:0.01',
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
                'message' => 'Input parameters invalid or missing.',
                'errors' => $validator->errors()
            ], 400)
        );
    }
}
