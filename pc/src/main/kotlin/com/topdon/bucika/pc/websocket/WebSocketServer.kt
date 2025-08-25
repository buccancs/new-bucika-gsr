package com.topdon.bucika.pc.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.topdon.bucika.pc.protocol.*
import com.topdon.bucika.pc.session.DeviceInfo
import com.topdon.bucika.pc.session.SessionManager
import com.topdon.bucika.pc.time.TimeSyncService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.File
import java.net.InetSocketAddress
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.set

private val logger = KotlinLogging.logger {}

/**
 * WebSocket server for handling communication with Android clients
 */
class WebSocketServer(
    port: Int,
    private val sessionManager: SessionManager,
    private val timeSyncService: TimeSyncService
) : WebSocketServer(InetSocketAddress(port)) {
    
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }
    
    private val connectedDevices = ConcurrentHashMap<WebSocket, ConnectedDevice>()
    private val deviceConnections = ConcurrentHashMap<String, WebSocket>()
    private val activeUploads = ConcurrentHashMap<String, FileUploadTracker>()
    
    // Ping/pong tracking
    private val lastPingTime = ConcurrentHashMap<String, Long>()
    private val pingScheduler = Executors.newScheduledThreadPool(1)
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        logger.info { "New WebSocket connection from ${conn.remoteSocketAddress}" }
    }
    
    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val device = connectedDevices.remove(conn)
        if (device != null) {
            deviceConnections.remove(device.deviceId)
            lastPingTime.remove(device.deviceId)
            logger.info { "Device ${device.deviceId} disconnected: $reason" }
        }
    }
    
    override fun onMessage(conn: WebSocket, message: String) {
        serviceScope.launch {
            try {
                val envelope = objectMapper.readValue(message, MessageEnvelope::class.java)
                handleMessage(conn, envelope)
            } catch (e: Exception) {
                logger.error(e) { "Error processing message from ${conn.remoteSocketAddress}: $message" }
                sendError(conn, "INVALID_MESSAGE", "Failed to parse message: ${e.message}")
            }
        }
    }
    
    override fun onError(conn: WebSocket?, ex: Exception) {
        logger.error(ex) { "WebSocket error from ${conn?.remoteSocketAddress}" }
    }
    
    override fun onStart() {
        logger.info { "WebSocket server started on ${address}" }
        startPingScheduler()
    }
    
    private suspend fun handleMessage(conn: WebSocket, envelope: MessageEnvelope) {
        logger.debug { "Received ${envelope.type} from ${envelope.deviceId}" }
        
        when (envelope.type) {
            MessageType.HELLO -> handleHello(conn, envelope)
            MessageType.PONG -> handlePong(envelope)
            MessageType.ACK -> handleAck(envelope)
            MessageType.ERROR -> handleError(envelope)
            MessageType.GSR_SAMPLE -> handleGSRSample(envelope)
            MessageType.UPLOAD_BEGIN -> handleUploadBegin(conn, envelope)
            MessageType.UPLOAD_CHUNK -> handleUploadChunk(envelope)
            MessageType.UPLOAD_END -> handleUploadEnd(envelope)
            else -> {
                logger.warn { "Unexpected message type: ${envelope.type} from ${envelope.deviceId}" }
                sendError(conn, "UNEXPECTED_MESSAGE", "Unexpected message type: ${envelope.type}")
            }
        }
    }
    
    private suspend fun handleHello(conn: WebSocket, envelope: MessageEnvelope) {
        val payload = envelope.payload as HelloPayload
        
        val device = ConnectedDevice(
            deviceId = envelope.deviceId,
            deviceName = payload.deviceName,
            capabilities = payload.capabilities,
            batteryLevel = payload.batteryLevel,
            version = payload.version,
            connection = conn,
            lastPing = System.currentTimeMillis()
        )
        
        connectedDevices[conn] = device
        deviceConnections[envelope.deviceId] = conn
        lastPingTime[envelope.deviceId] = System.currentTimeMillis()
        
        // Register with session manager if there's an active session
        val currentSession = sessionManager.currentSession.value
        if (currentSession != null) {
            val deviceInfo = DeviceInfo(
                deviceId = envelope.deviceId,
                deviceName = payload.deviceName,
                capabilities = payload.capabilities,
                batteryLevel = payload.batteryLevel,
                version = payload.version,
                role = determineDeviceRole(payload.capabilities)
            )
            sessionManager.addDevice(currentSession.id, envelope.deviceId, deviceInfo)
        }
        
        // Send registration response
        val registerPayload = RegisterPayload(
            registered = true,
            role = determineDeviceRole(payload.capabilities),
            syncConfig = com.topdon.bucika.pc.protocol.SyncConfig(
                syncInterval = 30000L, // 30 seconds
                offsetThreshold = 5_000_000L // 5ms in nanoseconds
            )
        )
        
        sendMessage(conn, MessageType.REGISTER, envelope.deviceId, registerPayload)
        
        logger.info { 
            "Registered device: ${payload.deviceName} (${envelope.deviceId}) " +
            "with capabilities: ${payload.capabilities.joinToString()}"
        }
    }
    
    private fun determineDeviceRole(capabilities: List<String>): String {
        return when {
            capabilities.contains("GSR_LEADER") -> "GSR_LEADER"
            capabilities.contains("RGB") && capabilities.contains("THERMAL") -> "DUAL_CAMERA"
            capabilities.contains("RGB") -> "RGB_CAMERA"
            capabilities.contains("THERMAL") -> "THERMAL_CAMERA"
            else -> "BASIC"
        }
    }
    
    private fun handlePong(envelope: MessageEnvelope) {
        lastPingTime[envelope.deviceId] = System.currentTimeMillis()
        logger.debug { "Received pong from ${envelope.deviceId}" }
    }
    
    private fun handleAck(envelope: MessageEnvelope) {
        val payload = envelope.payload as AckPayload
        logger.debug { "Received ACK for ${payload.ackId} from ${envelope.deviceId}" }
        // TODO: Handle ACK tracking for reliability
    }
    
    private fun handleError(envelope: MessageEnvelope) {
        val payload = envelope.payload as ErrorPayload
        logger.warn { 
            "Received error from ${envelope.deviceId}: ${payload.errorCode} - ${payload.message}" 
        }
        // TODO: Handle device errors appropriately
    }
    
    private suspend fun handleGSRSample(envelope: MessageEnvelope) {
        val payload = envelope.payload as GSRSamplePayload
        logger.debug { 
            "Received ${payload.samples.size} GSR samples from ${envelope.deviceId}" 
        }
        
        // Store GSR samples if we have an active session
        val currentSession = sessionManager.currentSession.value
        if (currentSession != null) {
            sessionManager.storeGSRSamples(currentSession.id, envelope.deviceId, payload.samples)
            
            // Update UI with latest GSR data if needed
            payload.samples.lastOrNull()?.let { lastSample ->
                logger.trace { 
                    "Latest GSR from ${envelope.deviceId}: ${lastSample.gsr_filt_uS}µS at ${lastSample.temp_C}°C" 
                }
            }
        } else {
            logger.warn { "Received GSR samples but no active session - discarding data" }
        }
    }
    
    private suspend fun handleUploadBegin(conn: WebSocket, envelope: MessageEnvelope) {
        val payload = envelope.payload as UploadBeginPayload
        logger.info { 
            "Starting upload of ${payload.fileName} (${payload.fileSize} bytes) from ${envelope.deviceId}" 
        }
        
        val currentSession = sessionManager.currentSession.value
        if (currentSession == null) {
            logger.warn { "Upload request but no active session" }
            sendError(conn, "NO_ACTIVE_SESSION", "No active session for file upload")
            return
        }
        
        try {
            // Create upload directory if it doesn't exist
            val uploadDir = File(currentSession.directory, "uploads")
            uploadDir.mkdirs()
            
            // Initialize upload tracking
            val uploadFile = File(uploadDir, payload.fileName)
            val uploadInfo = FileUploadTracker(
                fileName = payload.fileName,
                totalSize = payload.fileSize,
                expectedChecksum = payload.checksum,
                chunkSize = payload.chunkSize,
                targetFile = uploadFile,
                deviceId = envelope.deviceId,
                sessionId = currentSession.id
            )
            
            activeUploads[payload.fileName] = uploadInfo
            
            // Send acknowledgment that we're ready to receive chunks
            val ackPayload = mapOf(
                "fileName" to payload.fileName,
                "status" to "READY",
                "chunkSize" to payload.chunkSize
            )
            val response = MessageEnvelope(
                id = UUID.randomUUID().toString(),
                type = MessageType.ACK,
                ts = System.nanoTime(),
                sessionId = currentSession.id,
                deviceId = "orchestrator",
                payload = objectMapper.convertValue(ackPayload, AckPayload::class.java)
            )
            
            conn.send(objectMapper.writeValueAsString(response))
            logger.info { "Ready to receive upload chunks for ${payload.fileName}" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize upload for ${payload.fileName}" }
            sendError(conn, "UPLOAD_INIT_FAILED", "Failed to initialize upload: ${e.message}")
        }
    }
    
    private suspend fun handleUploadChunk(envelope: MessageEnvelope) {
        val payload = envelope.payload as UploadChunkPayload
        logger.debug { 
            "Received chunk ${payload.chunkIndex} of ${payload.fileName} from ${envelope.deviceId}" 
        }
        
        val uploadInfo = activeUploads[payload.fileName]
        if (uploadInfo == null) {
            logger.warn { "Received chunk for unknown upload: ${payload.fileName}" }
            return
        }
        
        try {
            // Decode and write chunk data
            val chunkData = java.util.Base64.getDecoder().decode(payload.data)
            
            // Verify chunk checksum
            val expectedChecksum = calculateChunkChecksum(chunkData)
            if (expectedChecksum != payload.checksum) {
                logger.error { "Chunk checksum mismatch for ${payload.fileName} chunk ${payload.chunkIndex}" }
                return
            }
            
            // Write chunk to file at correct position
            uploadInfo.targetFile.let { file ->
                file.parentFile?.mkdirs()
                java.io.RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(payload.chunkIndex * uploadInfo.chunkSize.toLong())
                    raf.write(chunkData)
                }
            }
            
            uploadInfo.receivedChunks.add(payload.chunkIndex)
            uploadInfo.bytesReceived += chunkData.size
            
            logger.trace { 
                "Wrote chunk ${payload.chunkIndex} (${chunkData.size} bytes) for ${payload.fileName}" 
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to process chunk ${payload.chunkIndex} for ${payload.fileName}" }
        }
    }
    
    private suspend fun handleUploadEnd(envelope: MessageEnvelope) {
        val payload = envelope.payload as UploadEndPayload
        logger.info { 
            "Completed upload of ${payload.fileName} (${payload.totalChunks} chunks) from ${envelope.deviceId}" 
        }
        
        val uploadInfo = activeUploads[payload.fileName]
        if (uploadInfo == null) {
            logger.warn { "Received upload end for unknown upload: ${payload.fileName}" }
            return
        }
        
        try {
            // Verify all chunks received
            val expectedChunks = (0 until payload.totalChunks).toSet()
            val missingChunks = expectedChunks - uploadInfo.receivedChunks
            
            if (missingChunks.isNotEmpty()) {
                logger.error { "Missing chunks for ${payload.fileName}: $missingChunks" }
                sendUploadError(envelope.deviceId, payload.fileName, "Missing chunks: $missingChunks")
                return
            }
            
            // Verify final checksum
            val actualChecksum = calculateFileChecksum(uploadInfo.targetFile)
            if (actualChecksum != payload.finalChecksum) {
                logger.error { "Final checksum mismatch for ${payload.fileName}" }
                sendUploadError(envelope.deviceId, payload.fileName, "Checksum verification failed")
                return
            }
            
            // Success - file upload completed
            logger.info { "Successfully uploaded ${payload.fileName} (${uploadInfo.bytesReceived} bytes)" }
            
            // Update session metadata
            sessionManager.recordFileUpload(uploadInfo.sessionId ?: "", payload.fileName, uploadInfo.targetFile.absolutePath)
            
            // Clean up tracking
            activeUploads.remove(payload.fileName)
            
            // Send success acknowledgment
            val conn = deviceConnections[envelope.deviceId]
            if (conn != null) {
                val ackPayload = mapOf(
                    "fileName" to payload.fileName,
                    "status" to "COMPLETED"
                )
                sendMessage(conn, MessageType.ACK, "orchestrator", 
                    objectMapper.convertValue(ackPayload, AckPayload::class.java))
            }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to finalize upload for ${payload.fileName}" }
            sendUploadError(envelope.deviceId, payload.fileName, "Upload finalization failed: ${e.message}")
        }
    }
    
    private suspend fun sendUploadError(deviceId: String, fileName: String, error: String) {
        val conn = deviceConnections[deviceId] ?: return
        val errorPayload = mapOf(
            "fileName" to fileName,
            "status" to "ERROR",
            "error" to error
        )
        sendMessage(conn, MessageType.ACK, "orchestrator",
            objectMapper.convertValue(errorPayload, AckPayload::class.java))
    }
    
    private fun calculateFileChecksum(file: File): String {
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
            logger.error(e) { "Error calculating file checksum" }
            "unknown"
        }
    }
    
    private fun calculateChunkChecksum(data: ByteArray): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            md.update(data)
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error calculating chunk checksum" }
            "unknown"
        }
    }
    
    fun broadcastToAllDevices(messageType: MessageType, payload: MessagePayload, sessionId: String? = null) {
        deviceConnections.forEach { (deviceId, conn) ->
            serviceScope.launch {
                sendMessage(conn, messageType, deviceId, payload, sessionId)
            }
        }
    }
    
    fun sendToDevice(deviceId: String, messageType: MessageType, payload: MessagePayload, sessionId: String? = null): Boolean {
        val conn = deviceConnections[deviceId] ?: return false
        
        serviceScope.launch {
            sendMessage(conn, messageType, deviceId, payload, sessionId)
        }
        
        return true
    }
    
    private fun sendMessage(
        conn: WebSocket, 
        messageType: MessageType, 
        deviceId: String, 
        payload: MessagePayload,
        sessionId: String? = null
    ) {
        try {
            val envelope = MessageEnvelope(
                id = UUID.randomUUID().toString(),
                type = messageType,
                ts = Instant.now().toEpochMilli() * 1_000_000L, // nanoseconds
                sessionId = sessionId,
                deviceId = "orchestrator",
                payload = payload
            )
            
            val message = objectMapper.writeValueAsString(envelope)
            conn.send(message)
            
        } catch (e: Exception) {
            logger.error(e) { "Error sending message to $deviceId" }
        }
    }
    
    private fun sendError(conn: WebSocket, errorCode: String, message: String, deviceId: String = "unknown") {
        val errorPayload = ErrorPayload(errorCode, message)
        sendMessage(conn, MessageType.ERROR, deviceId, errorPayload)
    }
    
    private fun sendAck(conn: WebSocket, ackId: String, status: String, deviceId: String = "orchestrator") {
        val ackPayload = AckPayload(ackId, status)
        sendMessage(conn, MessageType.ACK, deviceId, ackPayload)
    }
    
    private fun startPingScheduler() {
        pingScheduler.scheduleAtFixedRate({
            try {
                val currentTime = System.currentTimeMillis()
                
                deviceConnections.forEach { (deviceId, conn) ->
                    val lastPing = lastPingTime[deviceId] ?: 0
                    
                    if (currentTime - lastPing > 10_000) { // 10 seconds timeout
                        logger.warn { "Device $deviceId appears offline (no pong for ${currentTime - lastPing}ms)" }
                        // TODO: Mark device as offline
                    } else {
                        // Send ping
                        serviceScope.launch {
                            sendMessage(conn, MessageType.PING, deviceId, EmptyPayload)
                        }
                    }
                }
                
            } catch (e: Exception) {
                logger.error(e) { "Error in ping scheduler" }
            }
        }, 5, 5, TimeUnit.SECONDS) // Ping every 5 seconds
    }
    
    fun getConnectedDevices(): List<ConnectedDevice> = connectedDevices.values.toList()
    
    fun getDeviceCount(): Int = connectedDevices.size
    
    fun isDeviceConnected(deviceId: String): Boolean = deviceConnections.containsKey(deviceId)
    
    override fun stop() {
        try {
            pingScheduler.shutdown()
            serviceScope.cancel()
            stop(1000)
            logger.info { "WebSocket server stopped" }
        } catch (e: Exception) {
            logger.error(e) { "Error stopping WebSocket server" }
        }
    }
}

data class ConnectedDevice(
    val deviceId: String,
    val deviceName: String,
    val capabilities: List<String>,
    val batteryLevel: Int,
    val version: String,
    val connection: WebSocket,
    var lastPing: Long
)

data class FileUploadTracker(
    val fileName: String,
    val totalSize: Long,
    val expectedChecksum: String,
    val chunkSize: Int,
    val targetFile: File,
    val deviceId: String,
    val sessionId: String? = null,
    val receivedChunks: MutableSet<Int> = mutableSetOf(),
    var bytesReceived: Long = 0L
)