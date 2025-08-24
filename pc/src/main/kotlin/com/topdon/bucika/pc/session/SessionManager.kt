package com.topdon.bucika.pc.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.topdon.bucika.pc.protocol.GSRSample
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val logger = KotlinLogging.logger {}

/**
 * Manages session lifecycle and metadata for Bucika GSR recording sessions
 */
class SessionManager {
    
    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
    }
    
    private val sessions = ConcurrentHashMap<String, Session>()
    private val _currentSession = MutableStateFlow<Session?>(null)
    
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()
    
    fun createSession(): Session {
        val sessionId = UUID.randomUUID().toString()
        val timestamp = LocalDateTime.now()
        val sessionName = "session_${timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}"
        
        val session = Session(
            id = sessionId,
            name = sessionName,
            createdAt = timestamp,
            state = SessionState.NEW,
            devices = mutableMapOf(),
            metadata = SessionMetadata(
                version = "1.0.0",
                devices = mutableListOf(),
                events = mutableListOf(),
                files = mutableListOf(),
                startTime = null,
                endTime = null,
                offsets = mutableMapOf()
            )
        )
        
        // Create session directory
        val sessionDir = File("sessions", sessionName)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        
        session.directory = sessionDir
        sessions[sessionId] = session
        
        logger.info { "Created session: $sessionName (ID: $sessionId)" }
        return session
    }
    
    fun armSession(sessionId: String): Boolean {
        val session = sessions[sessionId] ?: return false
        
        if (session.state != SessionState.NEW) {
            logger.warn { "Cannot arm session ${session.name}: current state is ${session.state}" }
            return false
        }
        
        // Perform pre-flight checks
        val preflightResult = performPreflightChecks(session)
        if (!preflightResult.passed) {
            logger.error { "Preflight checks failed for session ${session.name}: ${preflightResult.errors}" }
            return false
        }
        
        session.state = SessionState.ARMED
        _currentSession.value = session
        saveSessionMetadata(session)
        
        logger.info { "Armed session: ${session.name}" }
        return true
    }
    
    fun startSession(sessionId: String): Boolean {
        val session = sessions[sessionId] ?: return false
        
        if (session.state != SessionState.ARMED) {
            logger.warn { "Cannot start session ${session.name}: current state is ${session.state}" }
            return false
        }
        
        session.state = SessionState.RECORDING
        session.metadata.startTime = LocalDateTime.now()
        session.metadata.events.add(SessionEvent("SESSION_STARTED", LocalDateTime.now()))
        
        saveSessionMetadata(session)
        
        logger.info { "Started recording session: ${session.name}" }
        return true
    }
    
    fun stopSession(sessionId: String): Boolean {
        val session = sessions[sessionId] ?: return false
        
        if (session.state != SessionState.RECORDING) {
            logger.warn { "Cannot stop session ${session.name}: current state is ${session.state}" }
            return false
        }
        
        session.state = SessionState.FINALISING
        session.metadata.endTime = LocalDateTime.now()
        session.metadata.events.add(SessionEvent("SESSION_STOPPED", LocalDateTime.now()))
        
        saveSessionMetadata(session)
        
        logger.info { "Stopped recording session: ${session.name}" }
        
        // TODO: Trigger data ingestion process
        finalizeSession(sessionId)
        
        return true
    }
    
    private fun finalizeSession(sessionId: String) {
        val session = sessions[sessionId] ?: return
        
        try {
            // Final metadata save
            saveSessionMetadata(session)
            
            session.state = SessionState.DONE
            logger.info { "Finalized session: ${session.name}" }
            
        } catch (e: Exception) {
            session.state = SessionState.FAILED
            logger.error(e) { "Failed to finalize session: ${session.name}" }
        }
        
        saveSessionMetadata(session)
    }
    
    fun addDevice(sessionId: String, deviceId: String, deviceInfo: DeviceInfo) {
        val session = sessions[sessionId] ?: return
        session.devices[deviceId] = deviceInfo
        session.metadata.devices.add(deviceInfo)
        saveSessionMetadata(session)
        
        logger.info { "Added device $deviceId to session ${session.name}" }
    }
    
    fun recordSyncMark(sessionId: String, markerId: String) {
        val session = sessions[sessionId] ?: return
        val event = SessionEvent("SYNC_MARK", LocalDateTime.now(), mapOf("markerId" to markerId))
        session.metadata.events.add(event)
        saveSessionMetadata(session)
        
        logger.info { "Recorded sync mark $markerId for session ${session.name}" }
    }
    
    /**
     * Store incoming GSR samples from a device
     */
    fun storeGSRSamples(sessionId: String, deviceId: String, samples: List<GSRSample>) {
        val session = sessions[sessionId] ?: return
        
        if (session.state != SessionState.RECORDING) {
            logger.warn { "Ignoring GSR samples - session ${session.name} is not recording" }
            return
        }
        
        try {
            // Create GSR data file if it doesn't exist
            val gsrFile = File(session.directory, "${deviceId}_gsr_data.csv")
            val isNewFile = !gsrFile.exists()
            
            gsrFile.appendText(
                if (isNewFile) {
                    "timestamp_mono_ns,timestamp_utc_ns,offset_ms,sequence,gsr_raw_uS,gsr_filtered_uS,temperature_C,flag_spike,flag_saturated,flag_dropout\n"
                } else {
                    ""
                }
            )
            
            // Append sample data
            samples.forEach { sample ->
                gsrFile.appendText(
                    "${sample.t_mono_ns},${sample.t_utc_ns},${sample.offset_ms},${sample.seq}," +
                    "${sample.gsr_raw_uS},${sample.gsr_filt_uS},${sample.temp_C}," +
                    "${sample.flag_spike},${sample.flag_sat},${sample.flag_dropout}\n"
                )
            }
            
            // Update session metadata
            val event = SessionEvent(
                "GSR_DATA",
                LocalDateTime.now(),
                mapOf(
                    "deviceId" to deviceId,
                    "sampleCount" to samples.size,
                    "file" to gsrFile.name
                )
            )
            session.metadata.events.add(event)
            
            logger.debug { "Stored ${samples.size} GSR samples from $deviceId to ${gsrFile.name}" }
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to store GSR samples from $deviceId" }
        }
    }
    
    private fun performPreflightChecks(session: Session): PreflightResult {
        val errors = mutableListOf<String>()
        
        // Check disk space (minimum 10GB)
        val sessionDir = session.directory!!
        val freeSpace = sessionDir.freeSpace
        if (freeSpace < 10L * 1024 * 1024 * 1024) {
            errors.add("Insufficient disk space: ${freeSpace / (1024 * 1024 * 1024)}GB available, 10GB required")
        }
        
        // Check registered devices
        if (session.devices.isEmpty()) {
            errors.add("No devices registered for session")
        }
        
        // Check GSR leader
        val hasGSRLeader = session.devices.values.any { it.capabilities.contains("GSR_LEADER") }
        if (!hasGSRLeader) {
            errors.add("No GSR leader device registered")
        }
        
        return PreflightResult(errors.isEmpty(), errors)
    }
    
    private fun saveSessionMetadata(session: Session) {
        try {
            val metaFile = File(session.directory, "meta.json")
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaFile, session.metadata)
        } catch (e: Exception) {
            logger.error(e) { "Failed to save metadata for session ${session.name}" }
        }
    }
    
    fun getSession(sessionId: String): Session? = sessions[sessionId]
    
    fun getAllSessions(): Collection<Session> = sessions.values
}

enum class SessionState {
    NEW, ARMED, RECORDING, FINALISING, DONE, FAILED
}

data class Session(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime,
    var state: SessionState,
    val devices: MutableMap<String, DeviceInfo>,
    val metadata: SessionMetadata,
    var directory: File? = null
)

data class SessionMetadata(
    val version: String,
    val devices: MutableList<DeviceInfo>,
    val events: MutableList<SessionEvent>,
    val files: MutableList<SessionFile>,
    var startTime: LocalDateTime?,
    var endTime: LocalDateTime?,
    val offsets: MutableMap<String, List<TimeOffset>>
)

data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val capabilities: List<String>,
    val batteryLevel: Int,
    val version: String,
    val role: String? = null
)

data class SessionEvent(
    val type: String,
    val timestamp: LocalDateTime,
    val data: Map<String, Any> = emptyMap()
)

data class SessionFile(
    val fileName: String,
    val fileSize: Long,
    val checksum: String,
    val deviceId: String,
    val uploadedAt: LocalDateTime
)

data class TimeOffset(
    val timestamp: LocalDateTime,
    val offsetMs: Double,
    val uncertainty: Double
)

data class PreflightResult(
    val passed: Boolean,
    val errors: List<String>
)