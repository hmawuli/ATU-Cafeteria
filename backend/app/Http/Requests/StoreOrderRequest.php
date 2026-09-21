<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class StoreOrderRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    /**
     * Never trust customer/student IDs supplied by the client. For a student
     * order, the authenticated Sanctum user is the sole source of identity.
     * Vendor/admin integrations may still use their explicitly supplied IDs.
     */
    protected function prepareForValidation()
    {
        $user = $this->user();
        if ($user && strtoupper((string) $user->role) === 'STUDENT') {
            $this->merge([
                'customer_id' => $user->id,
                'student_id' => $user->id,
            ]);
            return;
        }

        if ($this->has('student_id') && ! $this->has('customer_id')) {
            $this->merge(['customer_id' => $this->input('student_id')]);
        } elseif ($this->has('customer_id') && ! $this->has('student_id')) {
            $this->merge(['student_id' => $this->input('customer_id')]);
        }
    }

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

    protected function failedValidation(Validator $validator)
    {
        throw new HttpResponseException(
            response()->json([
                'success' => false,
                'message' => 'Input parameters invalid or missing.',
                'errors' => $validator->errors(),
            ], 400)
        );
    }
}
