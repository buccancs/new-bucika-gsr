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
        
        // TODO: Initialize upload tracking and storage
        sendAck(conn, envelope.id, "OK")
    }
    
    private suspend fun handleUploadChunk(envelope: MessageEnvelope) {
        val payload = envelope.payload as UploadChunkPayload
        logger.debug { 
            "Received chunk ${payload.chunkIndex} of ${payload.fileName} from ${envelope.deviceId}" 
        }
        
        // TODO: Process and verify chunk data
    }
    
    private suspend fun handleUploadEnd(envelope: MessageEnvelope) {
        val payload = envelope.payload as UploadEndPayload
        logger.info { 
            "Completed upload of ${payload.fileName} (${payload.totalChunks} chunks) from ${envelope.deviceId}" 
        }
        
        // TODO: Finalize upload and verify integrity
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