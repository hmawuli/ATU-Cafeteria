<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\ChatMessage;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class ChatController extends Controller
{
    /**
     * Get messages between the authenticated user and another user.
     */
    public function getConversation(Request $request, $otherUserId)
    {
        $userId = $request->user()->id;

        // Mark messages from the other user to authenticated user as read
        ChatMessage::where('sender_id', $otherUserId)
            ->where('receiver_id', $userId)
            ->where('is_read', false)
            ->update(['is_read' => true]);

        $messages = ChatMessage::where(function ($query) use ($userId, $otherUserId) {
            $query->where('sender_id', $userId)->where('receiver_id', $otherUserId);
        })->orWhere(function ($query) use ($userId, $otherUserId) {
            $query->where('sender_id', $otherUserId)->where('receiver_id', $userId);
        })
            ->orderBy('created_at', 'asc')
            ->get();

        return response()->json([
            'success' => true,
            'messages' => $messages,
        ], 200);
    }

    /**
     * Send a message to another user.
     */
    public function sendMessage(Request $request)
    {
        $senderId = $request->user()->id;

        $validator = Validator::make($request->all(), [
            'receiver_id' => 'required|integer|exists:users,id',
            'message' => 'required|string|min:1|max:1000',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid parameters.',
                'errors' => $validator->errors(),
            ], 400);
        }

        $receiverId = $request->input('receiver_id');

        if ($senderId === (int) $receiverId) {
            return response()->json([
                'success' => false,
                'message' => 'You cannot send a message to yourself.',
            ], 400);
        }

        $chatMessage = ChatMessage::create([
            'sender_id' => $senderId,
            'receiver_id' => $receiverId,
            'message' => $request->input('message'),
            'is_read' => false,
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Message delivered.',
            'chat_message' => $chatMessage,
        ], 201);
    }

    /**
     * Get unique users the authenticated user has chatted with.
     */
    public function getRecentChats(Request $request)
    {
        $userId = $request->user()->id;

        // Find users that have either sent messages to, or received messages from, the authenticated user
        $partnerIds = ChatMessage::where('sender_id', $userId)
            ->select('receiver_id as partner_id')
            ->union(
                ChatMessage::where('receiver_id', $userId)
                    ->select('sender_id as partner_id')
            )
            ->get()
            ->pluck('partner_id')
            ->unique()
            ->values();

        $partners = User::whereIn('id', $partnerIds)
            ->select(['id', 'username', 'fullName', 'role', 'info'])
            ->get()
            ->map(function ($partner) use ($userId) {
                // Fetch last message for each partner
                $lastMsg = ChatMessage::where(function ($q) use ($userId, $partner) {
                    $q->where('sender_id', $userId)->where('receiver_id', $partner->id);
                })->orWhere(function ($q) use ($userId, $partner) {
                    $q->where('sender_id', $partner->id)->where('receiver_id', $userId);
                })
                    ->orderBy('created_at', 'desc')
                    ->first();

                $unreadCount = ChatMessage::where('sender_id', $partner->id)
                    ->where('receiver_id', $userId)
                    ->where('is_read', false)
                    ->count();

                $partner->last_message = $lastMsg ? $lastMsg->message : '';
                $partner->last_message_time = $lastMsg ? $lastMsg->created_at->toIso8601String() : null;
                $partner->unread_count = $unreadCount;

                return $partner;
            })
            // Sort by last message time, descending
            ->sortByDesc('last_message_time')
            ->values();

        return response()->json([
            'success' => true,
            'chats' => $partners,
        ], 200);
    }
}
