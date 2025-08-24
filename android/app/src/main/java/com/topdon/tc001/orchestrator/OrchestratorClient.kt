package com.topdon.tc001.orchestrator

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.*
import okio.ByteString
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

/**
 * WebSocket client for communicating with the PC orchestrator
 * Implements the BucikaGSR protocol for session control and data coordination
 */
class OrchestratorClient(
    private val context: Context,
    private val listener: OrchestratorListener? = null
) {
    companion object {
        private const val TAG = "OrchestratorClient"
        private const val PING_INTERVAL = 30000L // 30 seconds
        private const val RECONNECT_DELAY = 5000L // 5 seconds
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
    }

    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    private var serverUrl: String = ""
    private var isConnected = false
    private var reconnectAttempts = 0
    
    private val deviceId = "${Build.MANUFACTURER}-${Build.MODEL}-${Build.ID}".replace(" ", "-")
    private val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
    
    private val handler = Handler(Looper.getMainLooper())
    private val pingRunnable = object : Runnable {
        override fun run() {
            sendPing()
            handler.postDelayed(this, PING_INTERVAL)
        }
    }

    private var currentSessionId: String? = null
    private val pendingMessages = HashMap<String, PendingMessage>()

    /**
     * Connect to the PC orchestrator
     * @param serverUrl WebSocket URL (e.g., "ws://192.168.1.100:8080")
     */
    fun connect(serverUrl: String) {
        this.serverUrl = serverUrl
        
        if (isConnected) {
            Log.w(TAG, "Already connected to orchestrator")
            return
        }

        Log.i(TAG, "Connecting to orchestrator at $serverUrl")
        
        try {
            okHttpClient = OkHttpClient.Builder()
                .pingInterval(PING_INTERVAL, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .build()

            webSocket = okHttpClient?.newWebSocket(request, webSocketListener)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to orchestrator", e)
            listener?.onConnectionError(e.message ?: "Connection failed")
            scheduleReconnect()
        }
    }

    /**
     * Disconnect from orchestrator
     */
    fun disconnect() {
        Log.i(TAG, "Disconnecting from orchestrator")
        isConnected = false
        handler.removeCallbacks(pingRunnable)
        
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        
        okHttpClient?.dispatcher?.executorService?.shutdown()
        okHttpClient = null
        
        listener?.onDisconnected()
    }

    /**
     * Send HELLO message to register with orchestrator
     */
    private fun sendHello() {
        val capabilities = mutableListOf<String>()
        capabilities.add("RGB") // Camera recording
        capabilities.add("GSR_LEADER") // Can connect to Shimmer GSR device
        
        // Check for thermal camera capability
        if (hasThermalCamera()) {
            capabilities.add("THERMAL")
        }

        val helloPayload = mapOf(
            "deviceName" to deviceName,
            "capabilities" to capabilities,
            "batteryLevel" to getBatteryLevel(),
            "version" to "1.0.0"
        )

        val message = createMessage("HELLO", helloPayload)
        sendMessage(message)
    }

    /**
     * Send PING message for liveness detection
     */
    private fun sendPing() {
        if (!isConnected) return
        
        val message = createMessage("PING", emptyMap<String, Any>())
        sendMessage(message, expectAck = false) // Pings don't need ACK
    }

    /**
     * Send ACK message in response to received message
     */
    private fun sendAck(originalMessageId: String, status: String = "OK") {
        val ackPayload = mapOf(
            "ackId" to originalMessageId,
            "status" to status
        )
        
        val message = createMessage("ACK", ackPayload)
        sendMessage(message, expectAck = false) // ACKs don't need ACK
    }

    /**
     * Send ERROR message
     */
    fun sendError(errorCode: String, message: String, details: Map<String, Any> = emptyMap()) {
        val errorPayload = mapOf(
            "errorCode" to errorCode,
            "message" to message,
            "details" to details
        )
        
        val errorMessage = createMessage("ERROR", errorPayload)
        sendMessage(errorMessage, expectAck = false)
    }

    /**
     * Create standardized message envelope
     */
    private fun createMessage(
        type: String, 
        payload: Any, 
        messageId: String = UUID.randomUUID().toString()
    ): Map<String, Any> {
        return mapOf(
            "id" to messageId,
            "type" to type,
            "ts" to System.nanoTime(),
            "sessionId" to currentSessionId,
            "deviceId" to deviceId,
            "payload" to payload
        )
    }

    /**
     * Send message to orchestrator
     */
    private fun sendMessage(message: Map<String, Any>, expectAck: Boolean = true) {
        if (!isConnected) {
            Log.w(TAG, "Not connected, cannot send message: ${message["type"]}")
            return
        }

        try {
            val json = objectMapper.writeValueAsString(message)
            val sent = webSocket?.send(json) == true
            
            if (sent) {
                Log.d(TAG, "Sent message: ${message["type"]}")
                
                if (expectAck) {
                    val messageId = message["id"] as String
                    pendingMessages[messageId] = PendingMessage(message, System.currentTimeMillis())
                }
            } else {
                Log.e(TAG, "Failed to send message: ${message["type"]}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
        }
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "Connected to orchestrator")
            isConnected = true
            reconnectAttempts = 0
            
            // Send HELLO message to register
            sendHello()
            
            // Start ping timer
            handler.post(pingRunnable)
            
            listener?.onConnected()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val message: Map<String, Any> = objectMapper.readValue(text)
                handleMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: $text", e)
                sendError("PARSE_ERROR", "Failed to parse message", mapOf("rawMessage" to text))
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.w(TAG, "Received binary message, expected text")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Connection closing: $code $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "Connection closed: $code $reason")
            isConnected = false
            handler.removeCallbacks(pingRunnable)
            
            if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            } else {
                Log.e(TAG, "Max reconnect attempts reached")
                listener?.onConnectionError("Max reconnect attempts reached")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            isConnected = false
            listener?.onConnectionError(t.message ?: "Connection failed")
            scheduleReconnect()
        }
    }

    /**
     * Handle received message from orchestrator
     */
    private fun handleMessage(message: Map<String, Any>) {
        val messageId = message["id"] as? String
        val messageType = message["type"] as? String
        val payload = message["payload"] as? Map<String, Any> ?: emptyMap()
        
        Log.d(TAG, "Received message: $messageType")

        // Send ACK for most message types (except PING, PONG, ACK)
        if (messageType !in listOf("PING", "PONG", "ACK") && messageId != null) {
            sendAck(messageId)
        }

        when (messageType) {
            "REGISTER" -> handleRegister(payload)
            "PING" -> handlePing()
            "PONG" -> handlePong()
            "START" -> handleStart(payload)
            "STOP" -> handleStop(payload) 
            "SYNC_MARK" -> handleSyncMark(payload)
            "ACK" -> handleAck(payload)
            "ERROR" -> handleError(payload)
            else -> {
                Log.w(TAG, "Unknown message type: $messageType")
            }
        }
    }

    private fun handleRegister(payload: Map<String, Any>) {
        val registered = payload["registered"] as? Boolean ?: false
        val role = payload["role"] as? String
        
        if (registered) {
            Log.i(TAG, "Successfully registered with orchestrator as $role")
            listener?.onRegistered(role ?: "UNKNOWN")
        } else {
            Log.e(TAG, "Registration failed")
            listener?.onConnectionError("Registration failed")
        }
    }

    private fun handlePing() {
        // Respond to PING with PONG
        val message = createMessage("PONG", emptyMap<String, Any>())
        sendMessage(message, expectAck = false)
    }

    private fun handlePong() {
        // PONG received, connection is alive
        Log.d(TAG, "PONG received")
    }

    private fun handleStart(payload: Map<String, Any>) {
        val sessionConfig = payload["sessionConfig"] as? Map<String, Any>
        currentSessionId = UUID.randomUUID().toString() // Generate session ID
        
        Log.i(TAG, "START command received for session: $currentSessionId")
        listener?.onStartSession(currentSessionId!!, sessionConfig ?: emptyMap())
    }

    private fun handleStop(payload: Map<String, Any>) {
        Log.i(TAG, "STOP command received")
        listener?.onStopSession()
        currentSessionId = null
    }

    private fun handleSyncMark(payload: Map<String, Any>) {
        val markerId = payload["markerId"] as? String
        val referenceTime = payload["referenceTime"] as? Long
        
        Log.i(TAG, "SYNC_MARK received: $markerId at $referenceTime")
        listener?.onSyncMark(markerId ?: "unknown", referenceTime ?: 0L)
    }

    private fun handleAck(payload: Map<String, Any>) {
        val ackId = payload["ackId"] as? String
        if (ackId != null) {
            pendingMessages.remove(ackId)
            Log.d(TAG, "ACK received for message: $ackId")
        }
    }

    private fun handleError(payload: Map<String, Any>) {
        val errorCode = payload["errorCode"] as? String ?: "UNKNOWN_ERROR"
        val message = payload["message"] as? String ?: "Unknown error"
        val details = payload["details"] as? Map<String, Any> ?: emptyMap()
        
        Log.e(TAG, "Error received: $errorCode - $message")
        listener?.onError(errorCode, message, details)
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
        
        reconnectAttempts++
        val delay = RECONNECT_DELAY * reconnectAttempts // Exponential backoff
        
        Log.i(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${delay}ms")
        
        handler.postDelayed({
            connect(serverUrl)
        }, delay)
    }

    /**
     * Check if device has thermal camera capability
     */
    private fun hasThermalCamera(): Boolean {
        // This would be implemented based on the specific thermal camera detection logic
        // For now, assume it's available if the thermal camera module exists
        return try {
            Class.forName("com.topdon.tc001.thermal.ThermalCamera")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Get current battery level percentage
     */
    private fun getBatteryLevel(): Int {
        // This would be implemented to get actual battery level
        // For now, return a placeholder value
        return 85
    }

    /**
     * Data class for pending messages awaiting ACK
     */
    private data class PendingMessage(
        val message: Map<String, Any>,
        val timestamp: Long
    )

    /**
     * Interface for orchestrator client events
     */
    interface OrchestratorListener {
        fun onConnected()
        fun onDisconnected()
        fun onRegistered(role: String)
        fun onConnectionError(error: String)
        fun onStartSession(sessionId: String, config: Map<String, Any>)
        fun onStopSession()
        fun onSyncMark(markerId: String, referenceTime: Long)
        fun onError(errorCode: String, message: String, details: Map<String, Any>)
    }
}