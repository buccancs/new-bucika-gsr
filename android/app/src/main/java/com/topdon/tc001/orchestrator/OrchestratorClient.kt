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
    private var gsrSequenceNumber = 0L
    
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
            "UPLOAD_BEGIN_ACK" -> handleUploadBeginAck(payload)
            "FILE_UPLOAD_READY" -> handleFileUploadReady(payload)
            "FILE_UPLOAD_ACK" -> handleFileUploadAck(payload)
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

    private fun handleFileUploadReady(payload: Map<String, Any>) {
        val filePath = payload["filePath"] as? String ?: return
        val uploadUrl = payload["uploadUrl"] as? String ?: return
        val chunkSize = payload["chunkSize"] as? Int ?: 8192
        
        Log.i(TAG, "File upload ready for: $filePath")
        // TODO: Implement chunked file upload
        fileUploadCallbacks[filePath]?.onCompleted(filePath)
    }

    private fun handleUploadBeginAck(payload: Map<String, Any>) {
        val fileName = payload["fileName"] as? String ?: return
        val status = payload["status"] as? String ?: "UNKNOWN"
        
        if (status == "READY") {
            Log.i(TAG, "Upload ready for: $fileName - starting chunks")
            val uploadInfo = pendingUploads[fileName]
            if (uploadInfo != null) {
                // Start chunked upload in background thread
                Thread {
                    uploadFileChunks(uploadInfo)
                }.start()
            }
        } else {
            Log.e(TAG, "Upload begin failed for: $fileName - $status")
            fileUploadCallbacks[fileName]?.onError("Upload initialization failed: $status")
            pendingUploads.remove(fileName)
        }
    }

    private fun handleFileUploadAck(payload: Map<String, Any>) {
        val filePath = payload["filePath"] as? String ?: return
        val fileName = payload["fileName"] as? String ?: filePath
        val status = payload["status"] as? String ?: "UNKNOWN"
        
        if (status == "COMPLETED") {
            Log.i(TAG, "File upload completed: $fileName")
            fileUploadCallbacks[fileName]?.onCompleted(fileName)
            fileUploadCallbacks.remove(fileName)
            pendingUploads.remove(fileName)
        } else if (status == "ERROR") {
            val error = payload["error"] as? String ?: "Upload failed"
            Log.e(TAG, "File upload failed: $fileName - $error")
            fileUploadCallbacks[fileName]?.onError(error)
            fileUploadCallbacks.remove(fileName)
            pendingUploads.remove(fileName)
        }
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
     * Send GSR data to orchestrator in real-time during active session
     */
    fun sendGSRData(gsrValue: Double, skinTemperature: Double, timestamp: Long = System.nanoTime()) {
        if (currentSessionId == null) {
            Log.w(TAG, "Cannot send GSR data - no active session")
            return
        }
        
        // Create GSR sample matching PC protocol format
        val gsrSample = mapOf(
            "t_mono_ns" to timestamp,
            "t_utc_ns" to System.currentTimeMillis() * 1000000L, // Convert to nanoseconds
            "offset_ms" to 0.0, // Clock offset, would be calculated from time sync
            "seq" to gsrSequenceNumber++,
            "gsr_raw_uS" to gsrValue,
            "gsr_filt_uS" to gsrValue, // For now, same as raw - filtering could be added
            "temp_C" to skinTemperature,
            "flag_spike" to false, // Quality flags - could be enhanced
            "flag_sat" to false,
            "flag_dropout" to false
        )
        
        val payload = mapOf(
            "samples" to listOf(gsrSample)
        )
        
        val message = createMessage("GSR_SAMPLE", payload)
        sendMessage(message, expectAck = false) // High-frequency data, no ACK needed
    }

    /**
     * Upload a file to the orchestrator using chunked transfer
     */
    fun uploadFile(filePath: String, fileType: String, callback: FileUploadCallback? = null) {
        if (currentSessionId == null) {
            callback?.onError("No active session")
            return
        }
        
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                callback?.onError("File does not exist: $filePath")
                return
            }
            
            val fileSize = file.length()
            val chunkSize = 8192 // 8KB chunks
            val checksum = calculateFileChecksum(file)
            
            // Send upload begin request
            val beginPayload = mapOf(
                "fileName" to file.name,
                "fileSize" to fileSize,
                "checksum" to checksum,
                "chunkSize" to chunkSize,
                "fileType" to fileType
            )
            
            val beginMessage = createMessage("UPLOAD_BEGIN", beginPayload)
            sendMessage(beginMessage, expectAck = true)
            
            // Store callback and file info for chunked upload
            fileUploadCallbacks[file.name] = callback
            pendingUploads[file.name] = FileUploadInfo(file, chunkSize, callback)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting file upload", e)
            callback?.onError("Failed to start upload: ${e.message}")
        }
    }
    
    /**
     * Perform chunked file upload
     */
    private fun uploadFileChunks(uploadInfo: FileUploadInfo) {
        try {
            val file = uploadInfo.file
            val chunkSize = uploadInfo.chunkSize
            val totalChunks = ((file.length() + chunkSize - 1) / chunkSize).toInt()
            
            Log.i(TAG, "Starting chunked upload of ${file.name} (${totalChunks} chunks)")
            
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(chunkSize)
                var chunkIndex = 0
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val chunkData = if (bytesRead < chunkSize) {
                        buffer.copyOfRange(0, bytesRead)
                    } else {
                        buffer
                    }
                    
                    val chunkChecksum = calculateChunkChecksum(chunkData)
                    val encodedData = android.util.Base64.encodeToString(chunkData, android.util.Base64.DEFAULT)
                    
                    val chunkPayload = mapOf(
                        "fileName" to file.name,
                        "chunkIndex" to chunkIndex,
                        "data" to encodedData,
                        "checksum" to chunkChecksum
                    )
                    
                    val chunkMessage = createMessage("UPLOAD_CHUNK", chunkPayload)
                    sendMessage(chunkMessage, expectAck = false)
                    
                    // Update progress
                    val bytesUploaded = (chunkIndex + 1) * chunkSize.toLong()
                    uploadInfo.callback?.onProgress(
                        minOf(bytesUploaded, file.length()), 
                        file.length()
                    )
                    
                    chunkIndex++
                    
                    // Small delay to avoid overwhelming the connection
                    Thread.sleep(10)
                }
                
                // Send upload end message
                val endPayload = mapOf(
                    "fileName" to file.name,
                    "totalChunks" to totalChunks,
                    "finalChecksum" to uploadInfo.fileChecksum
                )
                
                val endMessage = createMessage("UPLOAD_END", endPayload)
                sendMessage(endMessage, expectAck = true)
                
                Log.i(TAG, "Completed upload of ${file.name}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during chunked upload", e)
            uploadInfo.callback?.onError("Upload failed: ${e.message}")
            pendingUploads.remove(uploadInfo.file.name)
        }
    }
    
    /**
     * Calculate MD5 checksum for file integrity verification
     */
    private fun calculateFileChecksum(file: java.io.File): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating file checksum", e)
            "unknown"
        }
    }
    
    /**
     * Calculate MD5 checksum for chunk integrity verification
     */
    private fun calculateChunkChecksum(data: ByteArray): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            md.update(data)
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating chunk checksum", e)
            "unknown"
        }
    }
    
    /**
     * Send session status update to orchestrator
     */
    fun sendSessionStatus(recordingActive: Boolean, filesRecorded: Int, dataPointsCollected: Int) {
        if (currentSessionId == null) return
        
        val payload = mapOf(
            "sessionId" to currentSessionId!!,
            "recordingActive" to recordingActive,
            "filesRecorded" to filesRecorded,
            "dataPointsCollected" to dataPointsCollected,
            "batteryLevel" to getBatteryLevel(),
            "timestamp" to System.currentTimeMillis()
        )
        
        val message = createMessage("SESSION_STATUS", payload)
        sendMessage(message, expectAck = false)
    }

    private val fileUploadCallbacks = mutableMapOf<String, FileUploadCallback>()
    private val pendingUploads = mutableMapOf<String, FileUploadInfo>()
    
    /**
     * Data class for tracking file upload state
     */
    private data class FileUploadInfo(
        val file: java.io.File,
        val chunkSize: Int,
        val callback: FileUploadCallback?,
        val fileChecksum: String = ""
    ) {
        constructor(file: java.io.File, chunkSize: Int, callback: FileUploadCallback?) : 
            this(file, chunkSize, callback, calculateFileChecksum(file))
            
        companion object {
            private fun calculateFileChecksum(file: java.io.File): String {
                return try {
                    val md = java.security.MessageDigest.getInstance("MD5")
                    file.inputStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            md.update(buffer, 0, bytesRead)
                        }
                    }
                    md.digest().joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    "unknown"
                }
            }
        }
    }

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
    
    /**
     * Interface for file upload progress tracking
     */
    interface FileUploadCallback {
        fun onProgress(bytesUploaded: Long, totalBytes: Long)
        fun onCompleted(uploadedFilePath: String)
        fun onError(error: String)
    }
