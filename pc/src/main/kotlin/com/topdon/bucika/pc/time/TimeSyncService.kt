package com.topdon.bucika.pc.time

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import java.net.*
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

/**
 * SNTP-like time synchronization service for precise clock alignment
 * 
 * Provides nanosecond-precision reference clock for Android clients
 * Target: median offset ≤5ms, p95 ≤15ms over 30 min
 */
class TimeSyncService(private val port: Int = 9123) {
    
    private var socket: DatagramSocket? = null
    private var isRunning = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Client offset tracking
    private val clientOffsets = ConcurrentHashMap<String, MutableList<TimeSyncMeasurement>>()
    
    suspend fun start() {
        try {
            socket = DatagramSocket(port)
            isRunning = true
            
            logger.info { "Time sync service starting on port $port" }
            
            serviceScope.launch {
                handleTimeRequests()
            }
            
            serviceScope.launch {
                periodicCleanup()
            }
            
            logger.info { "Time sync service started successfully" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to start time sync service" }
            throw e
        }
    }
    
    suspend fun stop() {
        isRunning = false
        socket?.close()
        serviceScope.cancel()
        serviceScope.coroutineContext[Job]?.join()
        logger.info { "Time sync service stopped" }
    }
    
    private suspend fun handleTimeRequests() {
        val buffer = ByteArray(64)
        val packet = DatagramPacket(buffer, buffer.size)
        
        while (isRunning) {
            try {
                socket?.receive(packet)
                
                // Record receive time immediately
                val receiveTime = System.nanoTime()
                val receiveUTC = Instant.now().toEpochMilli() * 1_000_000L
                
                processTimeSyncRequest(packet, receiveTime, receiveUTC)
                
            } catch (e: SocketTimeoutException) {
                // Normal timeout, continue
            } catch (e: Exception) {
                if (isRunning) {
                    logger.error(e) { "Error handling time sync request" }
                }
            }
        }
    }
    
    private suspend fun processTimeSyncRequest(
        packet: DatagramPacket,
        receiveTime: Long,
        receiveUTC: Long
    ) = withContext(Dispatchers.IO) {
        
        try {
            val requestData = ByteBuffer.wrap(packet.data, 0, packet.length)
            
            // Parse client request (simplified SNTP-like protocol)
            val clientId = String(requestData.array(), 0, 32).trim('\u0000')
            val clientTransmitTime = requestData.getLong(32)
            
            // Prepare response
            val responseBuffer = ByteBuffer.allocate(64)
            val transmitTime = System.nanoTime()
            val transmitUTC = Instant.now().toEpochMilli() * 1_000_000L
            
            // Response format:
            // 0-32: client ID (null-terminated)
            // 32-40: original client transmit time
            // 40-48: server receive time
            // 48-56: server transmit time  
            // 56-64: server UTC time
            
            responseBuffer.put(clientId.toByteArray().sliceArray(0..31))
            responseBuffer.putLong(clientTransmitTime)
            responseBuffer.putLong(receiveTime)
            responseBuffer.putLong(transmitTime)
            responseBuffer.putLong(transmitUTC)
            
            // Send response
            val responsePacket = DatagramPacket(
                responseBuffer.array(),
                responseBuffer.array().size,
                packet.address,
                packet.port
            )
            
            socket?.send(responsePacket)
            
            // Calculate and store offset estimation
            val clockOffset = estimateClockOffset(
                clientTransmitTime,
                receiveTime,
                transmitTime
            )
            
            recordTimeMeasurement(clientId, clockOffset, receiveUTC)
            
        } catch (e: Exception) {
            logger.error(e) { "Error processing time sync request from ${packet.address}" }
        }
    }
    
    private fun estimateClockOffset(
        clientTransmit: Long,
        serverReceive: Long,
        serverTransmit: Long
    ): Double {
        // Simplified offset calculation (in nanoseconds)
        // More sophisticated algorithms could be used for better accuracy
        val networkDelay = (serverTransmit - serverReceive) / 2.0
        return (serverReceive - clientTransmit - networkDelay) / 1_000_000.0 // Convert to milliseconds
    }
    
    private fun recordTimeMeasurement(clientId: String, offsetMs: Double, timestamp: Long) {
        val measurement = TimeSyncMeasurement(
            timestamp = timestamp,
            offsetMs = offsetMs,
            uncertainty = calculateUncertainty(offsetMs)
        )
        
        clientOffsets.computeIfAbsent(clientId) { mutableListOf() }.add(measurement)
        
        // Keep only last 100 measurements per client
        val measurements = clientOffsets[clientId]!!
        if (measurements.size > 100) {
            measurements.removeAt(0)
        }
        
        // Log statistics periodically
        if (measurements.size % 10 == 0) {
            logClientStatistics(clientId, measurements)
        }
    }
    
    private fun calculateUncertainty(offsetMs: Double): Double {
        // Simple uncertainty estimation based on offset magnitude
        return minOf(abs(offsetMs) * 0.1, 2.0) // Max 2ms uncertainty
    }
    
    private fun logClientStatistics(clientId: String, measurements: List<TimeSyncMeasurement>) {
        val offsets = measurements.map { it.offsetMs }
        val median = offsets.sorted()[offsets.size / 2]
        val p95Index = (offsets.size * 0.95).toInt()
        val p95 = offsets.sorted()[p95Index]
        
        logger.debug { 
            "Time sync stats for $clientId: " +
            "median=${String.format("%.2f", median)}ms, " +
            "p95=${String.format("%.2f", p95)}ms, " +
            "samples=${offsets.size}"
        }
        
        // Warn if targets not met
        if (abs(median) > 5.0 || abs(p95) > 15.0) {
            logger.warn {
                "Time sync targets not met for $clientId: " +
                "median=${String.format("%.2f", median)}ms (target ≤5ms), " +
                "p95=${String.format("%.2f", p95)}ms (target ≤15ms)"
            }
        }
    }
    
    private suspend fun periodicCleanup() {
        while (isRunning) {
            delay(300_000) // 5 minutes
            
            // Remove stale measurements (older than 30 minutes)
            val cutoffTime = Instant.now().toEpochMilli() * 1_000_000L - (30 * 60 * 1_000_000_000L)
            
            clientOffsets.values.forEach { measurements ->
                measurements.removeIf { it.timestamp < cutoffTime }
            }
            
            // Remove empty client entries
            clientOffsets.entries.removeIf { it.value.isEmpty() }
        }
    }
    
    fun getClientOffset(clientId: String): TimeSyncStats? {
        val measurements = clientOffsets[clientId] ?: return null
        
        if (measurements.isEmpty()) return null
        
        val offsets = measurements.map { it.offsetMs }
        val median = offsets.sorted()[offsets.size / 2]
        val p95Index = (offsets.size * 0.95).toInt().coerceAtMost(offsets.size - 1)
        val p95 = offsets.sorted()[p95Index]
        val latest = measurements.last()
        
        return TimeSyncStats(
            clientId = clientId,
            latestOffsetMs = latest.offsetMs,
            medianOffsetMs = median,
            p95OffsetMs = p95,
            uncertainty = latest.uncertainty,
            sampleCount = measurements.size,
            lastUpdated = latest.timestamp
        )
    }
    
    fun getAllClientStats(): List<TimeSyncStats> {
        return clientOffsets.keys.mapNotNull { getClientOffset(it) }
    }
}

data class TimeSyncMeasurement(
    val timestamp: Long, // nanoseconds UTC
    val offsetMs: Double,
    val uncertainty: Double
)

data class TimeSyncStats(
    val clientId: String,
    val latestOffsetMs: Double,
    val medianOffsetMs: Double,
    val p95OffsetMs: Double,
    val uncertainty: Double,
    val sampleCount: Int,
    val lastUpdated: Long
)