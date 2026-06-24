package com.example.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * A highly robust real-time synchronization manager utilizing WebSockets and Pusher/Laravel Echo protocol elements.
 * Manages dual channels:
 * 1. Live WebSocket client connecting to remote Laravel socket servers (Pusher/Soketi/Laravel-WebSockets).
 * 2. Instant intra-app loopback WebSocket pipeline to stream order-created events directly across student-vendor roles.
 * 3. Graceful automated simulation fallback that delivers realistic campus orders periodically to keep KPIs alive.
 * 4. Dual channel management: Handles both VENDOR (orders-vendor-$id) and STUDENT (orders-student-$id) feeds.
 */
object LaravelEchoWebSocketManager {
    private const val TAG = "LaravelEchoWSManager"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isConnecting = false
    private var reconnectJob: Job? = null
    
    // Store current registration metrics for secure reconnections
    private var activeUserId: Int? = null
    private var activeUserRole: String? = null

    // Shared flow emitting direct real-time order creation event signals
    private val _realTimeOrderFlow = MutableSharedFlow<OrderBroadcastEvent>(extraBufferCapacity = 64)
    val realTimeOrderFlow: SharedFlow<OrderBroadcastEvent> = _realTimeOrderFlow.asSharedFlow()

    // Shared flow emitting direct real-time student notification event signals
    private val _realTimeStudentNotificationFlow = MutableSharedFlow<StudentNotificationEvent>(extraBufferCapacity = 64)
    val realTimeStudentNotificationFlow: SharedFlow<StudentNotificationEvent> = _realTimeStudentNotificationFlow.asSharedFlow()

    // Shared flow emitting direct real-time low stock inventory alert signals for administrators
    private val _realTimeInventoryAlertFlow = MutableSharedFlow<InventoryAlertEvent>(extraBufferCapacity = 64)
    val realTimeInventoryAlertFlow: SharedFlow<InventoryAlertEvent> = _realTimeInventoryAlertFlow.asSharedFlow()

    data class InventoryAlertEvent(
        val itemId: Int,
        val itemName: String,
        val remainingStock: Int,
        val message: String,
        val timestamp: Long
    )

    data class OrderBroadcastEvent(
        val orderId: Int,
        val vendorId: Int,
        val foodName: String,
        val qty: Int,
        val totalPrice: Double,
        val timestamp: Long,
        val message: String
    )

    data class StudentNotificationEvent(
        val notificationId: String,
        val orderId: Int,
        val vendorId: Int,
        val oldStatus: String,
        val newStatus: String,
        val message: String,
        val timestamp: Long
    )

    /**
     * Initializes connection to the Laravel WebSocket or Pusher channel.
     * Rewrites http/https BaseUrl dynamically into ws/wss stream.
     */
    fun startListening(userId: Int, role: String) {
        val normalizedRole = role.uppercase()
        activeUserId = userId
        activeUserRole = normalizedRole

        if (!LaravelClientManager.isLaravelEnabled) {
            Log.d(TAG, "Laravel sync is disabled. Starting mock loopback WebSocket channel instead for $normalizedRole.")
            if (normalizedRole == "VENDOR") {
                startSimulationLoop(userId)
            }
            return
        }

        if (webSocket != null || isConnecting) return
        isConnecting = true

        val baseHttpUrl = LaravelClientManager.baseUrl
        // Derive standard WebSockets Pusher/Soketi or custom Echo endpoint
        val wsUrl = when {
            baseHttpUrl.startsWith("https://") -> {
                baseHttpUrl.replace("https://", "wss://") + "app/app-key?protocol=7&client=js&version=4.3.0&flash=false"
            }
            baseHttpUrl.startsWith("http://") -> {
                baseHttpUrl.replace("http://", "ws://") + "app/app-key?protocol=7&client=js&version=4.3.0&flash=false"
            }
            else -> "ws://10.0.2.2:6001/app/app-key?protocol=7&client=js&version=4.3.0&flash=false"
        }

        Log.i(TAG, "Attempting connection to Laravel Echo WebSocket at: $wsUrl")
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting = false
                Log.i(TAG, "Laravel Echo WebSocket subscription pipe connected successfully.")
                // Send standard Pusher/Echo subscribe message
                subscribeToRoleChannel(webSocket, userId, normalizedRole)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket Frame Received: $text")
                handleIncomingFrame(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing (Code: $code): $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closed (Code: $code)")
                scheduleReconnect(userId, normalizedRole)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure, proceeding with auto-reconnection fallback: ${t.message}")
                scheduleReconnect(userId, normalizedRole)
            }
        })
    }

    private fun subscribeToRoleChannel(ws: WebSocket, userId: Int, role: String) {
        try {
            val channelName = when (role) {
                "VENDOR" -> "orders-vendor-$userId"
                "ADMIN" -> "orders-admin"
                else -> "orders-student-$userId"
            }
            val subMsg = JSONObject().apply {
                put("event", "pusher:subscribe")
                put("data", JSONObject().apply {
                    put("channel", channelName)
                })
            }
            ws.send(subMsg.toString())
            Log.d(TAG, "Subscribed successfully to channel '$channelName'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending subscription frame", e)
        }
    }

    private fun handleIncomingFrame(jsontext: String) {
        try {
            val json = JSONObject(jsontext)
            val eventName = json.optString("event")
            
            // Handle order notifications for Vendor Screen
            if (eventName == "OrderPlaced" || eventName == "App\\Events\\OrderPlaced") {
                val dataObj = json.optJSONObject("data") ?: JSONObject(json.optString("data", "{}"))
                val orderId = dataObj.optInt("order_id", dataObj.optInt("id", 0))
                val vendorId = dataObj.optInt("vendor_id", 0)
                val foodName = dataObj.optString("food_name", "Pre-order item")
                val quantity = dataObj.optInt("quantity", 1)
                val total = dataObj.optDouble("total_price", 0.0)
                
                scope.launch {
                    _realTimeOrderFlow.emit(
                        OrderBroadcastEvent(
                            orderId = orderId,
                            vendorId = vendorId,
                            foodName = foodName,
                            qty = quantity,
                            totalPrice = total,
                            timestamp = System.currentTimeMillis(),
                            message = "New Student order placed!"
                        )
                    )
                }
            } 
            // Handle status notifications for Student Screen (Pusher/Echo Broadcaster) and Vendor Incoming Orders
            else if (eventName.contains("BroadcastNotificationCreated") || eventName.contains("OrderStatusChanged")) {
                val dataObj = json.optJSONObject("data") ?: JSONObject(json.optString("data", "{}"))
                val typeStr = dataObj.optString("type", "")
                val nestedData = dataObj.optJSONObject("data") ?: dataObj
                
                if (typeStr.contains("NewIncomingOrderNotification")) {
                    val orderId = nestedData.optInt("order_id", 0)
                    val vendorId = activeUserId ?: nestedData.optInt("vendor_id", 0)
                    val foodName = nestedData.optString("food_name", "Pre-order item")
                    val quantity = nestedData.optInt("quantity", 1)
                    val total = nestedData.optDouble("total_price", 0.0)
                    scope.launch {
                        _realTimeOrderFlow.emit(
                            OrderBroadcastEvent(
                                orderId = orderId,
                                vendorId = vendorId,
                                foodName = foodName,
                                qty = quantity,
                                totalPrice = total,
                                timestamp = System.currentTimeMillis(),
                                message = "Real-time Broadcast Pre-order received!"
                            )
                        )
                    }
                } else if (typeStr.contains("LowStockAlertNotification")) {
                    val itemId = nestedData.optInt("item_id", 0)
                    val itemName = nestedData.optString("item_name", "Ingredient")
                    val remainingStock = nestedData.optInt("remaining_stock", 0)
                    val msg = nestedData.optString("message", "Critically low ingredient stock detected!")
                    scope.launch {
                        _realTimeInventoryAlertFlow.emit(
                            InventoryAlertEvent(
                                itemId = itemId,
                                itemName = itemName,
                                remainingStock = remainingStock,
                                message = msg,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    val notificationId = dataObj.optString("id", java.util.UUID.randomUUID().toString())
                    val orderId = nestedData.optInt("order_id", 0)
                    val vendorId = nestedData.optInt("vendor_id", 0)
                    val oldStatus = nestedData.optString("old_status", "PENDING")
                    val newStatus = nestedData.optString("new_status", "PREPARING")
                    val msg = nestedData.optString("message", "Your order status has changed.")
                    
                    scope.launch {
                        _realTimeStudentNotificationFlow.emit(
                            StudentNotificationEvent(
                                notificationId = notificationId,
                                orderId = orderId,
                                vendorId = vendorId,
                                oldStatus = oldStatus,
                                newStatus = newStatus,
                                message = msg,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding event package", e)
        }
    }

    private fun scheduleReconnect(userId: Int, role: String) {
        webSocket = null
        isConnecting = false
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            Log.i(TAG, "Initiating socket reconnection attempt...")
            startListening(userId, role)
        }
    }

    /**
     * Streams an instant manual order signal locally to update the dashboard of on-screen users immediately.
     */
    fun broadcastOrderPlacedLocally(orderId: Int, vendorId: Int, foodName: String, qty: Int, totalPrice: Double) {
        scope.launch {
            Log.i(TAG, "Broadcasting local order placement to real-time sync listeners. ID: $orderId")
            _realTimeOrderFlow.emit(
                OrderBroadcastEvent(
                    orderId = orderId,
                    vendorId = vendorId,
                    foodName = foodName,
                    qty = qty,
                    totalPrice = totalPrice,
                    timestamp = System.currentTimeMillis(),
                    message = "Live local pre-order submitted!"
                )
            )
        }
    }

    /**
     * Streams an instant manual student notification locally to update the status banner of on-screen students immediately.
     */
    fun broadcastStudentNotificationLocally(
        notificationId: String,
        orderId: Int,
        vendorId: Int,
        oldStatus: String,
        newStatus: String,
        message: String
    ) {
        scope.launch {
            Log.i(TAG, "Broadcasting local student status change notification to listeners. ID: $orderId")
            _realTimeStudentNotificationFlow.emit(
                StudentNotificationEvent(
                    notificationId = notificationId,
                    orderId = orderId,
                    vendorId = vendorId,
                    oldStatus = oldStatus,
                    newStatus = newStatus,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Mock simulation generator engine: Delivers realistic background pre-orders from campus students.
     */
    private var simulationJob: Job? = null
    private fun startSimulationLoop(vendorId: Int) {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            val foodMenus = listOf(
                "Gari Fortor with Assorted Fish",
                "Spiced Jollof Rice with Grilled Chicken",
                "Classic Waakye Bowl with Egg and Wele",
                "Fufu with Goat Soup",
                "Red Red with Fried Plantains",
                "Fried Yam with Turkey Tails"
            )
            while (true) {
                // Simulate order arrival every 25-45 seconds to maintain high-grade metrics demonstration
                delay(30000 + (Math.random() * 15000).toLong())
                val itemId = (1000..9999).random()
                val food = foodMenus.random()
                val qty = (1..3).random()
                val unitPrice = 12.0 + (Math.random() * 8.0)
                val tot = qty * unitPrice
                
                Log.d(TAG, "Simulation websocket packet dispatched: $food")
                _realTimeOrderFlow.emit(
                    OrderBroadcastEvent(
                        orderId = itemId,
                        vendorId = vendorId,
                        foodName = food,
                        qty = qty,
                        totalPrice = tot,
                        timestamp = System.currentTimeMillis(),
                        message = "Dynamic background student pre-order incoming!"
                    )
                )
            }
        }
    }

    fun stopListening() {
        simulationJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Dashboard exit complete")
        webSocket = null
        isConnecting = false
        Log.i(TAG, "WebSocket listeners cleanly deallocated.")
    }
}
