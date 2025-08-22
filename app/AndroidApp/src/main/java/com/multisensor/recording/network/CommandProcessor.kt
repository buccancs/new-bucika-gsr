package com.multisensor.recording.network

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import com.multisensor.recording.calibration.CalibrationCaptureManager
import com.multisensor.recording.calibration.SyncClockManager
import com.multisensor.recording.recording.CameraRecorder
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.service.RecordingService
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@ServiceScoped
class CommandProcessor
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val cameraRecorder: CameraRecorder,
    private val thermalRecorder: ThermalRecorder,
    private val calibrationCaptureManager: CalibrationCaptureManager,
    private val syncClockManager: SyncClockManager,
    private val fileTransferHandler: FileTransferHandler,
    private val logger: Logger,
) {
    private val processingScope = CoroutineScope(Dispatchers.Default + Job())
    private var jsonSocketClient: JsonSocketClient? = null
    private var isRecording = false
    private var currentSessionId: String? = null
    private var stimulusTime: Long? = null

    fun setSocketClient(client: JsonSocketClient) {
        jsonSocketClient = client
        fileTransferHandler.initialize(client)
        logger.info("CommandProcessor connected to JsonSocketClient")
    }

    fun processCommand(message: JsonMessage) {
        processingScope.launch {
            try {
                when (message) {
                    is StartRecordCommand -> handleStartRecord(message)
                    is StopRecordCommand -> handleStopRecord(message)
                    is CaptureCalibrationCommand -> handleCaptureCalibration(message)
                    is SetStimulusTimeCommand -> handleSetStimulusTime(message)
                    is FlashSyncCommand -> handleFlashSync(message)
                    is BeepSyncCommand -> handleBeepSync(message)
                    is SyncTimeCommand -> handleSyncTime(message)
                    is SendFileCommand -> handleSendFile(message)
                    else -> {
                        logger.warning("Received unsupported command: ${message.type}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                logger.error("IO error processing command: ${message.type}", e)
                jsonSocketClient?.sendAck(message.type, false, "Network error: ${e.message}")
            } catch (e: SecurityException) {
                logger.error("Security exception processing command: ${message.type}", e)
                jsonSocketClient?.sendAck(message.type, false, "Permission error: ${e.message}")
            } catch (e: IllegalStateException) {
                logger.error("Invalid state processing command: ${message.type}", e)
                jsonSocketClient?.sendAck(message.type, false, "State error: ${e.message}")
            } catch (e: RuntimeException) {
                logger.error("Runtime error processing command: ${message.type}", e)
                jsonSocketClient?.sendAck(message.type, false, "Processing error: ${e.message}")
            }
        }
    }

    private suspend fun handleStartRecord(command: StartRecordCommand) {
        logger.info("Processing start_record command: ${command.session_id}")

        try {
            if (isRecording) {
                logger.warning("Already recording - ignoring start_record command")
                jsonSocketClient?.sendAck("start_record", false, "Already recording")
                return
            }

            val intent =
                Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                    putExtra("session_id", command.session_id)
                    putExtra("record_video", command.record_video)
                    putExtra("record_thermal", command.record_thermal)
                    putExtra("record_shimmer", command.record_shimmer)
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            isRecording = true
            currentSessionId = command.session_id

            jsonSocketClient?.sendAck("start_record", true)

            sendStatusUpdate()

            logger.info("Recording started successfully: ${command.session_id}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Security exception starting recording - check permissions", e)
            jsonSocketClient?.sendAck("start_record", false, "Permission error: ${e.message}")
        } catch (e: IllegalStateException) {
            logger.error("Invalid state starting recording", e)
            jsonSocketClient?.sendAck("start_record", false, "Service state error: ${e.message}")
        } catch (e: RuntimeException) {
            logger.error("Runtime error starting recording", e)
            jsonSocketClient?.sendAck("start_record", false, "Failed to start recording: ${e.message}")
        }
    }

    private suspend fun handleStopRecord(command: StopRecordCommand) {
        logger.info("Processing stop_record command")

        try {
            if (!isRecording) {
                logger.warning("Not recording - ignoring stop_record command")
                jsonSocketClient?.sendAck("stop_record", false, "Not currently recording")
                return
            }

            val intent =
                Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }

            context.startService(intent)

            isRecording = false
            currentSessionId = null

            jsonSocketClient?.sendAck("stop_record", true)

            sendStatusUpdate()

            logger.info("Recording stopped successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Security exception stopping recording - check permissions", e)
            jsonSocketClient?.sendAck("stop_record", false, "Permission error: ${e.message}")
        } catch (e: IllegalStateException) {
            logger.error("Invalid state stopping recording", e)
            jsonSocketClient?.sendAck("stop_record", false, "Service state error: ${e.message}")
        } catch (e: RuntimeException) {
            logger.error("Runtime error stopping recording", e)
            jsonSocketClient?.sendAck("stop_record", false, "Failed to stop recording: ${e.message}")
        }
    }

    private suspend fun handleCaptureCalibration(command: CaptureCalibrationCommand) {
        logger.info("[DEBUG_LOG] Processing enhanced capture_calibration command")
        logger.info("[DEBUG_LOG] Calibration ID: ${command.calibration_id}")
        logger.info(
            "[DEBUG_LOG] Capture RGB: ${command.capture_rgb}, Thermal: ${command.capture_thermal}, High-res: ${command.high_resolution}",
        )

        try {
            val result =
                calibrationCaptureManager.captureCalibrationImages(
                    calibrationId = command.calibration_id,
                    captureRgb = command.capture_rgb,
                    captureThermal = command.capture_thermal,
                    highResolution = command.high_resolution,
                )

            if (result.success) {
                val resultMessage =
                    buildString {
                        append("Calibration capture successful: ${result.calibrationId}")
                        result.rgbFilePath?.let { append(", RGB: $it") }
                        result.thermalFilePath?.let { append(", Thermal: $it") }
                        append(", Synced timestamp: ${result.syncedTimestamp}")
                    }

                jsonSocketClient?.sendAck("capture_calibration", true, resultMessage)
                logger.info("[DEBUG_LOG] $resultMessage")
            } else {
                val errorMessage = result.errorMessage ?: "Unknown calibration capture error"
                jsonSocketClient?.sendAck("capture_calibration", false, "Calibration capture failed: $errorMessage")
                logger.error("Calibration capture failed: $errorMessage")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Permission error capturing calibration: ${e.message}", e)
            jsonSocketClient?.sendAck("capture_calibration", false, "Permission error: ${e.message}")
        } catch (e: IllegalStateException) {
            logger.error("Invalid state capturing calibration: ${e.message}", e)
            jsonSocketClient?.sendAck("capture_calibration", false, "Invalid state: ${e.message}")
        } catch (e: RuntimeException) {
            logger.error("Runtime error capturing calibration: ${e.message}", e)
            jsonSocketClient?.sendAck("capture_calibration", false, "Runtime error: ${e.message}")
        }
    }

    private suspend fun handleSetStimulusTime(command: SetStimulusTimeCommand) {
        logger.info("Processing set_stimulus_time command: ${command.time}")

        try {
            stimulusTime = command.time
            val currentTime = System.currentTimeMillis()
            val timeOffset = command.time - currentTime

            logger.info("Stimulus time set: ${command.time} (offset: ${timeOffset}ms from current time)")

            if (timeOffset > 0) {
                scheduleStimulusActions(command.time, timeOffset)
                logger.info("Scheduled stimulus actions for future execution in ${timeOffset}ms")
            } else if (Math.abs(timeOffset) < 1000) {
                executeStimulusActions(command.time)
                logger.info("Executed immediate stimulus actions (time offset: ${timeOffset}ms)")
            } else {
                logger.info("Stimulus time is in the past (${Math.abs(timeOffset)}ms ago) - recorded for data alignment")
            }

            createSynchronizationMarker(command.time)

            val statusMessage = "Stimulus time processed (offset: ${timeOffset}ms)"
            jsonSocketClient?.sendAck("set_stimulus_time", true, statusMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Permission error setting stimulus time: ${e.message}", e)
            jsonSocketClient?.sendAck("set_stimulus_time", false, "Permission error: ${e.message}")
        } catch (e: IllegalStateException) {
            logger.error("Invalid state setting stimulus time: ${e.message}", e)
            jsonSocketClient?.sendAck("set_stimulus_time", false, "Invalid state: ${e.message}")
        } catch (e: RuntimeException) {
            logger.error("Runtime error setting stimulus time: ${e.message}", e)
            jsonSocketClient?.sendAck("set_stimulus_time", false, "Runtime error: ${e.message}")
        }
    }

    private fun scheduleStimulusActions(
        stimulusTime: Long,
        delayMs: Long,
    ) {
        processingScope.launch {
            try {
                kotlinx.coroutines.delay(delayMs)

                executeStimulusActions(stimulusTime)
            } catch (e: Exception) {
                logger.error("Error executing scheduled stimulus actions", e)
            }
        }
    }

    private suspend fun executeStimulusActions(stimulusTime: Long) {
        try {
            logger.info("Executing stimulus actions at time: $stimulusTime")

            logger.info("STIMULUS_EVENT: timestamp=$stimulusTime, device_time=${System.currentTimeMillis()}")

            sendStimulusNotification(stimulusTime)

            triggerDeviceStimulusActions(stimulusTime)

            if (isRecording) {
                updateRecordingMetadata(stimulusTime)
            }

            logger.info("Stimulus actions completed successfully")
        } catch (e: Exception) {
            logger.error("Error executing stimulus actions", e)
        }
    }

    private fun sendStimulusNotification(stimulusTime: Long) {
        try {
            val statusMessage =
                StatusMessage(
                    battery = getBatteryLevel(),
                    storage = getAvailableStorage(),
                    temperature = getDeviceTemperature(),
                    recording = isRecording,
                    connected = true,
                )

            jsonSocketClient?.sendMessage(statusMessage)
            logger.debug("Sent stimulus notification to PC")
        } catch (e: Exception) {
            logger.error("Failed to send stimulus notification", e)
        }
    }

    private suspend fun triggerDeviceStimulusActions(stimulusTime: Long) {
        try {
            triggerVisualStimulus()

            triggerAudioStimulus()

            triggerHapticFeedback()

            logger.info("STIMULUS_TRIGGERS: visual=true, audio=true, haptic=true, timestamp=$stimulusTime")
        } catch (e: Exception) {
            logger.error("Error triggering device stimulus actions", e)
        }
    }

    private fun triggerVisualStimulus() {
        try {
            val intent =
                Intent("com.multisensor.recording.VISUAL_STIMULUS").apply {
                    putExtra("stimulus_type", "screen_flash")
                    putExtra("duration_ms", 200L)
                    putExtra("timestamp", System.currentTimeMillis())
                }

            context.sendBroadcast(intent)

            logger.debug("Visual stimulus triggered - screen flash broadcast sent")
        } catch (e: Exception) {
            logger.error("Failed to trigger visual stimulus", e)
        }
    }

    private fun triggerAudioStimulus() {
        try {
            val toneGenerator =
                android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_NOTIFICATION,
                    80,
                )

            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                kotlinx.coroutines.delay(300)
                try {
                    toneGenerator.release()
                } catch (e: Exception) {
                    logger.debug("ToneGenerator already released", e)
                }
            }

            logger.debug("Audio stimulus triggered - tone generated")
        } catch (e: Exception) {
            logger.error("Failed to trigger audio stimulus", e)
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        100,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }

            logger.debug("Haptic feedback triggered")
        } catch (e: Exception) {
            logger.debug("Haptic feedback not available", e)
        }
    }

    private fun triggerAudioStimulusWithParameters(
        frequencyHz: Int,
        durationMs: Long,
        volume: Float,
    ) {
        try {
            val volumePercent = (volume * 100).toInt().coerceIn(0, 100)
            val toneGenerator =
                android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_NOTIFICATION,
                    volumePercent,
                )

            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, durationMs.toInt())

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                kotlinx.coroutines.delay(durationMs + 100)
                try {
                    toneGenerator.release()
                } catch (e: Exception) {
                    logger.debug("ToneGenerator already released", e)
                }
            }

            logger.debug("Audio stimulus triggered - tone generated (${frequencyHz}Hz, ${durationMs}ms, vol=$volume)")
        } catch (e: Exception) {
            logger.error("Failed to trigger audio stimulus", e)
        }
    }

    private fun createFlashSyncMarker(
        syncId: String,
        durationMs: Long,
    ) {
        try {
            val syncDir = File(context.getExternalFilesDir(null), "synchronisation")
            syncDir.mkdirs()

            val syncFile = File(syncDir, "flash_sync_${syncId}_${System.currentTimeMillis()}.txt")
            syncFile.writeText(
                "FLASH_SYNC_MARKER\n" +
                        "sync_id=$syncId\n" +
                        "duration_ms=$durationMs\n" +
                        "device_time=${System.currentTimeMillis()}\n" +
                        "session_id=$currentSessionId\n" +
                        "recording_active=$isRecording\n",
            )

            logger.debug("Created flash sync marker: ${syncFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to create flash sync marker", e)
        }
    }

    private fun createBeepSyncMarker(
        syncId: String,
        frequencyHz: Int,
        durationMs: Long,
        volume: Float,
    ) {
        try {
            val syncDir = File(context.getExternalFilesDir(null), "synchronisation")
            syncDir.mkdirs()

            val syncFile = File(syncDir, "beep_sync_${syncId}_${System.currentTimeMillis()}.txt")
            syncFile.writeText(
                "BEEP_SYNC_MARKER\n" +
                        "sync_id=$syncId\n" +
                        "frequency_hz=$frequencyHz\n" +
                        "duration_ms=$durationMs\n" +
                        "volume=$volume\n" +
                        "device_time=${System.currentTimeMillis()}\n" +
                        "session_id=$currentSessionId\n" +
                        "recording_active=$isRecording\n",
            )

            logger.debug("Created beep sync marker: ${syncFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to create beep sync marker", e)
        }
    }

    private fun updateRecordingMetadata(stimulusTime: Long) {
        try {
            val sessionId = currentSessionId ?: "unknown_session"
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(stimulusTime))

            val metadataEntry = mapOf(
                "type" to "stimulus_marker",
                "session_id" to sessionId,
                "timestamp_ms" to stimulusTime,
                "timestamp_iso" to timestamp,
                "device_timestamp" to System.currentTimeMillis(),
                "event_type" to "beep_stimulus"
            )

            sessionManager?.let { sm ->
                writeMetadataToFile(metadataEntry, sm.getSessionOutputDir())
            }

            logger.info("RECORDING_METADATA: stimulus_time=$stimulusTime, session_id=$sessionId, iso_timestamp=$timestamp")

            sessionManager?.addStimulusEvent(stimulusTime, "beep_stimulus")

        } catch (e: Exception) {
            logger.error("Failed to update recording metadata", e)
        }
    }

    private fun writeMetadataToFile(metadataEntry: Map<String, Any>, outputDir: File?) {
        try {
            outputDir?.let { dir ->
                val metadataFile = File(dir, "stimulus_metadata.json")
                val jsonEntry = buildString {
                    append("{\n")
                    metadataEntry.entries.forEachIndexed { index, (key, value) ->
                        append("  \"$key\": ")
                        when (value) {
                            is String -> append("\"$value\"")
                            is Number -> append(value)
                            else -> append("\"$value\"")
                        }
                        if (index < metadataEntry.size - 1) append(",")
                        append("\n")
                    }
                    append("}\n")
                }

                FileWriter(metadataFile, true).use { writer ->
                    writer.write(jsonEntry)
                }

                logger.info("Wrote stimulus metadata to: ${metadataFile.absolutePath}")
            }
        } catch (e: Exception) {
            logger.error("Failed to write metadata to file", e)
        }
    }

    private fun createSynchronizationMarker(stimulusTime: Long) {
        try {
            val syncDir = File(context.getExternalFilesDir(null), "synchronisation")
            syncDir.mkdirs()

            val syncFile = File(syncDir, "stimulus_sync_${System.currentTimeMillis()}.txt")
            syncFile.writeText(
                "STIMULUS_SYNC_MARKER\n" +
                        "stimulus_time=$stimulusTime\n" +
                        "device_time=${System.currentTimeMillis()}\n" +
                        "session_id=$currentSessionId\n" +
                        "recording_active=$isRecording\n",
            )

            logger.debug("Created synchronisation marker: ${syncFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to create synchronisation marker", e)
        }
    }

    private suspend fun handleFlashSync(command: FlashSyncCommand) {
        logger.info("[DEBUG_LOG] Processing flash_sync command")
        logger.info("[DEBUG_LOG] Duration: ${command.duration_ms}ms, Sync ID: ${command.sync_id}")

        try {
            triggerVisualStimulusWithDuration(command.duration_ms)

            command.sync_id?.let { syncId ->
                createFlashSyncMarker(syncId, command.duration_ms)
            }

            val resultMessage = "Flash sync triggered (${command.duration_ms}ms)"
            jsonSocketClient?.sendAck("flash_sync", true, resultMessage)

            logger.info("[DEBUG_LOG] Flash sync completed: $resultMessage")
        } catch (e: Exception) {
            logger.error("Failed to trigger flash sync", e)
            jsonSocketClient?.sendAck("flash_sync", false, "Flash sync failed: ${e.message}")
        }
    }

    private suspend fun handleBeepSync(command: BeepSyncCommand) {
        logger.info("[DEBUG_LOG] Processing beep_sync command")
        logger.info(
            "[DEBUG_LOG] Frequency: ${command.frequency_hz}Hz, Duration: ${command.duration_ms}ms, Volume: ${command.volume}, Sync ID: ${command.sync_id}",
        )

        try {
            triggerAudioStimulusWithParameters(
                frequencyHz = command.frequency_hz,
                durationMs = command.duration_ms,
                volume = command.volume,
            )

            command.sync_id?.let { syncId ->
                createBeepSyncMarker(syncId, command.frequency_hz, command.duration_ms, command.volume)
            }

            val resultMessage =
                "Beep sync triggered (${command.frequency_hz}Hz, ${command.duration_ms}ms, vol=${command.volume})"
            jsonSocketClient?.sendAck("beep_sync", true, resultMessage)

            logger.info("[DEBUG_LOG] Beep sync completed: $resultMessage")
        } catch (e: Exception) {
            logger.error("Failed to trigger beep sync", e)
            jsonSocketClient?.sendAck("beep_sync", false, "Beep sync failed: ${e.message}")
        }
    }

    private suspend fun handleSendFile(command: SendFileCommand) {
        logger.info("Processing send_file command for: ${command.filepath}")

        try {
            fileTransferHandler.handleSendFileCommand(command)

            logger.info("File transfer request delegated to FileTransferHandler")
        } catch (e: Exception) {
            logger.error("Failed to handle send_file command", e)
            jsonSocketClient?.sendAck("send_file", false, "Failed to process file transfer: ${e.message}")
        }
    }

    private suspend fun handleSyncTime(command: SyncTimeCommand) {
        logger.info("[DEBUG_LOG] Processing sync_time command")
        logger.info("[DEBUG_LOG] PC timestamp: ${command.pc_timestamp}, Sync ID: ${command.sync_id}")

        try {
            val success =
                syncClockManager.synchronizeWithPc(
                    pcTimestamp = command.pc_timestamp,
                    syncId = command.sync_id,
                )

            if (success) {
                val syncStatus = syncClockManager.getSyncStatus()
                val resultMessage =
                    buildString {
                        append("Clock sync successful: offset=${syncStatus.clockOffsetMs}ms")
                        command.sync_id?.let { syncId -> append(", sync_id=$syncId") }
                        append(", age=${syncStatus.syncAge}ms")
                    }

                jsonSocketClient?.sendAck("sync_time", true, resultMessage)
                logger.info("[DEBUG_LOG] $resultMessage")
            } else {
                val errorMessage = "Clock synchronisation failed - invalid PC timestamp or sync error"
                jsonSocketClient?.sendAck("sync_time", false, "Clock sync failed: $errorMessage")
                logger.error("Clock sync failed: $errorMessage")
            }
        } catch (e: Exception) {
            logger.error("Failed to sync time", e)
            jsonSocketClient?.sendAck("sync_time", false, "Clock sync failed: ${e.message}")
        }
    }

    private suspend fun captureRgbCalibrationImage(): String? =
        try {
            logger.info("Starting RGB calibration image capture...")

            val calibrationDir = File(context.getExternalFilesDir(null), "calibration")
            calibrationDir.mkdirs()

            val calibrationFile = File(calibrationDir, "rgb_calibration_${System.currentTimeMillis()}.jpg")

            val success = cameraRecorder.captureCalibrationImage(calibrationFile.absolutePath)

            if (success) {
                logger.info("RGB calibration image captured successfully: ${calibrationFile.absolutePath}")
                calibrationFile.absolutePath
            } else {
                logger.warning("RGB calibration image capture failed")
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to capture RGB calibration image", e)
            null
        }

    private suspend fun captureThermalCalibrationImage(): String? =
        try {
            logger.info("Starting thermal calibration image capture...")

            val calibrationDir = File(context.getExternalFilesDir(null), "calibration")
            calibrationDir.mkdirs()

            val calibrationFile = File(calibrationDir, "thermal_calibration_${System.currentTimeMillis()}.jpg")

            val success = thermalRecorder.captureCalibrationImage(calibrationFile.absolutePath)

            if (success) {
                logger.info("Thermal calibration image captured successfully: ${calibrationFile.absolutePath}")
                calibrationFile.absolutePath
            } else {
                logger.warning("Thermal calibration image capture failed")
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to capture thermal calibration image", e)
            null
        }

    private fun sendStatusUpdate() {
        try {
            val battery = getBatteryLevel()
            val storage = getAvailableStorage()
            val temperature = getDeviceTemperature()

            jsonSocketClient?.sendStatusUpdate(
                battery = battery,
                storage = storage,
                temperature = temperature,
                recording = isRecording,
            )
        } catch (e: Exception) {
            logger.error("Failed to send status update", e)
        }
    }

    private fun getBatteryLevel(): Int? =
        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            logger.debug("Failed to get battery level", e)
            null
        }

    private fun getAvailableStorage(): String? =
        try {
            val externalDir = context.getExternalFilesDir(null)
            if (externalDir != null) {
                val stat = StatFs(externalDir.path)
                val availableBytes = stat.availableBytes
                val availableGB = availableBytes / (1024 * 1024 * 1024)
                "${availableGB}GB_free"
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Failed to get storage info", e)
            null
        }

    private fun getDeviceTemperature(): Double? =
        try {
            null
        } catch (e: Exception) {
            logger.debug("Failed to get device temperature", e)
            null
        }

    fun isRecording(): Boolean = isRecording

    fun getCurrentSessionId(): String? = currentSessionId

    fun getStimulusTime(): Long? = stimulusTime

    private suspend fun triggerVisualStimulusWithDuration(durationMs: Long) {
        processingScope.launch {
            val success = cameraRecorder.triggerFlashSync(durationMs)
            if (!success) {
                logger.warning("Visual stimulus (flash) could not be triggered.")
            }
        }
    }

    private suspend fun triggerAudioStimulusWithParameters(
        frequencyHz: Int,
        durationMs: Int,
        volume: Float,
    ) {
        try {
            logger.info("[DEBUG_LOG] Triggering audio stimulus: ${frequencyHz}Hz, ${durationMs}ms, vol=$volume")

            val toneGenerator =
                ToneGenerator(
                    AudioManager.STREAM_MUSIC,
                    (volume * 100).toInt().coerceIn(0, 100),
                )

            val toneType =
                when (frequencyHz) {
                    in 800..1200 -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                    in 400..800 -> ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
                    in 200..400 -> ToneGenerator.TONE_CDMA_HIGH_L
                    else -> ToneGenerator.TONE_CDMA_MED_L
                }

            toneGenerator.startTone(toneType, durationMs)
            logger.debug("[DEBUG_LOG] Audio tone started")

            delay(durationMs.toLong())

            toneGenerator.stopTone()
            toneGenerator.release()
            logger.debug("[DEBUG_LOG] Audio tone completed and resources released")
        } catch (e: Exception) {
            logger.error("Failed to trigger audio stimulus", e)
            throw e
        }
    }

    private fun createFlashSyncMarker(
        syncId: String,
        durationMs: Int,
    ) {
        try {
            val syncDir = File(context.getExternalFilesDir(null), "sync_markers")
            syncDir.mkdirs()

            val syncFile = File(syncDir, "flash_sync_${syncId}_${System.currentTimeMillis()}.txt")
            val currentTime = System.currentTimeMillis()
            val syncedTime = syncClockManager.getSyncedTimestamp(currentTime)

            syncFile.writeText(
                "FLASH_SYNC_MARKER\n" +
                        "sync_id=$syncId\n" +
                        "duration_ms=$durationMs\n" +
                        "device_time=$currentTime\n" +
                        "synced_time=$syncedTime\n" +
                        "session_id=$currentSessionId\n" +
                        "recording_active=$isRecording\n",
            )

            logger.debug("[DEBUG_LOG] Created flash sync marker: ${syncFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to create flash sync marker", e)
        }
    }

    private fun createBeepSyncMarker(
        syncId: String,
        frequencyHz: Int,
        durationMs: Int,
        volume: Float,
    ) {
        try {
            val syncDir = File(context.getExternalFilesDir(null), "sync_markers")
            syncDir.mkdirs()

            val syncFile = File(syncDir, "beep_sync_${syncId}_${System.currentTimeMillis()}.txt")
            val currentTime = System.currentTimeMillis()
            val syncedTime = syncClockManager.getSyncedTimestamp(currentTime)

            syncFile.writeText(
                "BEEP_SYNC_MARKER\n" +
                        "sync_id=$syncId\n" +
                        "frequency_hz=$frequencyHz\n" +
                        "duration_ms=$durationMs\n" +
                        "volume=$volume\n" +
                        "device_time=$currentTime\n" +
                        "synced_time=$syncedTime\n" +
                        "session_id=$currentSessionId\n" +
                        "recording_active=$isRecording\n",
            )

            logger.debug("[DEBUG_LOG] Created beep sync marker: ${syncFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to create beep sync marker", e)
        }
    }
}
