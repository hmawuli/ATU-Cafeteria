<?php

namespace App\Http\Resources;

use Illuminate\Http\Resources\Json\JsonResource;

class OrderResource extends JsonResource
{
    /**
     * Transform the resource into an array.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function toArray($request)
    {
        return [
            'id' => $this->id,
            'customer_id' => $this->customer_id,
            'student_id' => $this->student_id,
            'user_id' => $this->user_id,
            'vendor_id' => $this->vendor_id,
            'food_item_id' => $this->food_item_id,
            'menu_item_id' => $this->menu_item_id,
            'food_name' => $this->food_name,
            'quantity' => $this->quantity,
            'unit_price' => (float) $this->unit_price,
            'total_price' => (float) $this->total_price,
            'order_timestamp' => $this->order_timestamp,
            'status' => $this->status,
            'order_status' => $this->order_status,
            'pickup_pin' => $this->pickup_pin,
            'estimated_pickup_time' => $this->estimated_pickup_time,
            'points_redeemed' => (int) $this->points_redeemed,
            'discount_applied' => (float) $this->discount_applied,
            'created_at' => $this->created_at ? $this->created_at->toIso8601String() : null,
            'updated_at' => $this->updated_at ? $this->updated_at->toIso8601String() : null,
            
            // Standardizing relationships for front-end consumption
            'customer' => $this->relationLoaded('customer') && $this->customer ? [
                'id' => $this->customer->id,
                'username' => $this->customer->username,
                'fullName' => $this->customer->fullName,
                'role' => $this->customer->role,
                'email' => $this->customer->email,
            ] : null,

            'student' => $this->relationLoaded('student') && $this->student ? [
                'id' => $this->student->id,
                'username' => $this->student->username,
                'fullName' => $this->student->fullName,
                'role' => $this->student->role,
                'email' => $this->student->email,
            ] : null,

            'vendor' => $this->relationLoaded('vendor') && $this->vendor ? [
                'id' => $this->vendor->id,
                'username' => $this->vendor->username,
                'fullName' => $this->vendor->fullName,
                'role' => $this->vendor->role,
                'info' => $this->vendor->info,
            ] : null,

            'food_item' => $this->relationLoaded('foodItem') && $this->foodItem ? [
                'id' => $this->foodItem->id,
                'name' => $this->foodItem->name,
                'price' => (float) $this->foodItem->price,
                'category' => $this->foodItem->category,
                'image_url' => $this->foodItem->image_url,
            ] : null,

            'menu_item' => $this->relationLoaded('menuItem') && $this->menuItem ? [
                'id' => $this->menuItem->id,
                'name' => $this->menuItem->name,
                'price' => (float) $this->menuItem->price,
                'category' => $this->menuItem->category,
            ] : null,

            'feedback' => $this->relationLoaded('feedback') && $this->feedback ? [
                'id' => $this->feedback->id,
                'comment' => $this->feedback->comment,
                'rating' => $this->feedback->rating,
                'vendor_reply' => $this->feedback->vendor_reply,
            ] : null,
        ];
    }
}
