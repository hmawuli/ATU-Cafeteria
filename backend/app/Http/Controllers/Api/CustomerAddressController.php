<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\CustomerAddress;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class CustomerAddressController extends Controller
{
    public function index(Request $request)
    {
        return response()->json([
            'success' => true,
            'addresses' => CustomerAddress::where('customer_id', $request->user()->id)
                ->orderByDesc('is_default')->latest()->get(),
        ]);
    }

    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'label' => 'required|string|max:80',
            'contact_name' => 'nullable|string|max:160',
            'phone' => 'nullable|string|max:30',
            'address_line1' => 'required|string|max:255',
            'address_line2' => 'nullable|string|max:255',
            'city' => 'nullable|string|max:120',
            'landmark' => 'nullable|string|max:255',
            'is_default' => 'boolean',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid address.', 'errors' => $validator->errors()], 422);
        }

        $address = DB::transaction(function () use ($request) {
            if ($request->boolean('is_default')) {
                CustomerAddress::where('customer_id', $request->user()->id)->update(['is_default' => false]);
            }

            return CustomerAddress::create([
                'customer_id' => $request->user()->id,
                'label' => trim($request->input('label')),
                'contact_name' => $request->input('contact_name'),
                'phone' => $request->input('phone'),
                'address_line1' => trim($request->input('address_line1')),
                'address_line2' => $request->input('address_line2'),
                'city' => $request->input('city'),
                'landmark' => $request->input('landmark'),
                'is_default' => $request->boolean('is_default'),
            ]);
        });

        return response()->json(['success' => true, 'address' => $address], 201);
    }

    public function update(Request $request, CustomerAddress $address)
    {
        abort_unless((int) $address->customer_id === (int) $request->user()->id, 403);

        $validator = Validator::make($request->all(), [
            'label' => 'sometimes|required|string|max:80',
            'contact_name' => 'nullable|string|max:160',
            'phone' => 'nullable|string|max:30',
            'address_line1' => 'sometimes|required|string|max:255',
            'address_line2' => 'nullable|string|max:255',
            'city' => 'nullable|string|max:120',
            'landmark' => 'nullable|string|max:255',
            'is_default' => 'boolean',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid address.', 'errors' => $validator->errors()], 422);
        }

        DB::transaction(function () use ($request, $address) {
            if ($request->boolean('is_default')) {
                CustomerAddress::where('customer_id', $request->user()->id)
                    ->whereKeyNot($address->id)->update(['is_default' => false]);
            }
            $address->fill($request->only([
                'label','contact_name','phone','address_line1','address_line2',
                'city','landmark','is_default',
            ]));
            $address->save();
        });

        return response()->json(['success' => true, 'address' => $address->fresh()]);
    }

    public function destroy(Request $request, CustomerAddress $address)
    {
        abort_unless((int) $address->customer_id === (int) $request->user()->id, 403);
        $wasDefault = $address->is_default;
        $address->delete();

        if ($wasDefault) {
            $replacement = CustomerAddress::where('customer_id', $request->user()->id)->latest()->first();
            if ($replacement) $replacement->update(['is_default' => true]);
        }

        return response()->json(['success' => true, 'message' => 'Address removed.']);
    }

    public function makeDefault(Request $request, CustomerAddress $address)
    {
        abort_unless((int) $address->customer_id === (int) $request->user()->id, 403);

        DB::transaction(function () use ($request, $address) {
            CustomerAddress::where('customer_id', $request->user()->id)->update(['is_default' => false]);
            $address->update(['is_default' => true]);
        });

        return response()->json(['success' => true, 'address' => $address->fresh()]);
    }
}
