package com.multisensor.recording.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.multisensor.recording.MainActivity
import com.multisensor.recording.R
import com.multisensor.recording.network.CommandProcessor
import com.multisensor.recording.network.JsonSocketClient
import com.multisensor.recording.network.NetworkConfiguration
import com.multisensor.recording.network.NetworkQualityMonitor
import com.multisensor.recording.recording.AdaptiveFrameRateController
import com.multisensor.recording.recording.CameraRecorder
import com.multisensor.recording.recording.ShimmerRecorder
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.streaming.PreviewStreamer
import com.multisensor.recording.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {
    @Inject
    lateinit var cameraRecorder: CameraRecorder

    @Inject
    lateinit var thermalRecorder: ThermalRecorder

    @Inject
    lateinit var shimmerRecorder: ShimmerRecorder

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var jsonSocketClient: JsonSocketClient

    @Inject
    lateinit var commandProcessor: CommandProcessor

    @Inject
    lateinit var previewStreamer: PreviewStreamer

    @Inject
    lateinit var networkConfiguration: NetworkConfiguration

    @Inject
    lateinit var networkQualityMonitor: NetworkQualityMonitor

    @Inject
    lateinit var adaptiveFrameRateController: AdaptiveFrameRateController

    @Inject
    lateinit var logger: Logger

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isRecording = false
    private var currentSessionId: String? = null

    companion object {
        const val ACTION_START_RECORDING = "com.multisensor.recording.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.multisensor.recording.STOP_RECORDING"
        const val ACTION_GET_STATUS = "com.multisensor.recording.GET_STATUS"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val CHANNEL_NAME = "Recording Service"
    }

    data class DeviceStatusInfo(
        val isRecording: Boolean,
        val currentSessionId: String?,
        val recordingStartTime: Long?,
        val cameraStatus: String,
        val thermalStatus: String,
        val shimmerStatus: String,
        val batteryLevel: Int?,
        val availableStorage: String?,
        val deviceTemperature: Double?,
        val networkConfig: String,
        val connectionStatus: String,
        val previewStreamingActive: Boolean,
        val timestamp: Long,
        val deviceModel: String,
        val androidVersion: String,
    ) {
        companion object {
            fun createErrorStatus(errorMessage: String): DeviceStatusInfo =
                DeviceStatusInfo(
                    isRecording = false,
                    currentSessionId = null,
                    recordingStartTime = null,
                    cameraStatus = "error",
                    thermalStatus = "error",
                    shimmerStatus = "error",
                    batteryLevel = null,
                    availableStorage = null,
                    deviceTemperature = null,
                    networkConfig = "error: $errorMessage",
                    connectionStatus = "error",
                    previewStreamingActive = false,
                    timestamp = System.currentTimeMillis(),
                    deviceModel = android.os.Build.MODEL,
                    androidVersion = android.os.Build.VERSION.RELEASE,
                )
        }
    }

    override fun onCreate() {
        super.onCreate()
        logger.info("RecordingService created")
        createNotificationChannel()

        initializeJsonCommunication()

        cameraRecorder.setPreviewStreamer(previewStreamer)

        initializeAdaptiveFrameRateControl()

        logger.info("RecordingService initialization complete")
    }

    private fun initializeAdaptiveFrameRateControl() {
        try {
            logger.info("[DEBUG_LOG] Initializing adaptive frame rate control system")

            val serverConfig = networkConfiguration.getServerConfiguration()

            networkQualityMonitor.startMonitoring(serverConfig.serverIp, serverConfig.legacyPort)

            adaptiveFrameRateController.addListener(
                object : AdaptiveFrameRateController.FrameRateChangeListener {
                    override fun onFrameRateChanged(
                        newFrameRate: Float,
                        reason: String,
                    ) {
                        logger.info("[DEBUG_LOG] Adaptive frame rate changed to ${newFrameRate}fps - $reason")
                        previewStreamer.updateFrameRate(newFrameRate)
                    }

                    override fun onAdaptationModeChanged(isAdaptive: Boolean) {
                        logger.info("[DEBUG_LOG] Adaptive mode changed: $isAdaptive")
                    }
                },
            )

            adaptiveFrameRateController.start()

            logger.info("[DEBUG_LOG] Adaptive frame rate control system initialized successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Permission error initializing adaptive frame rate control: ${e.message}", e)
        } catch (e: IllegalStateException) {
            logger.error("Invalid state during adaptive frame rate control initialization: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error during adaptive frame rate control initialization: ${e.message}", e)
        }
    }

    private fun initializeJsonCommunication() {
        try {
            commandProcessor.setSocketClient(jsonSocketClient)

            jsonSocketClient.setCommandCallback { message ->
                commandProcessor.processCommand(message)
            }

            val serverConfig = networkConfiguration.getServerConfiguration()
            jsonSocketClient.configure(serverConfig.serverIp, serverConfig.jsonPort)
            jsonSocketClient.connect()

            logger.info("JSON communication system initialized successfully: ${serverConfig.getJsonAddress()}")
            logger.info("Network configuration: ${networkConfiguration.getConfigurationSummary()}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Network permission error initializing JSON communication: ${e.message}", e)
        } catch (e: java.net.ConnectException) {
            logger.error("Connection error initializing JSON communication: ${e.message}", e)
        } catch (e: IllegalStateException) {
            logger.error("Invalid state during JSON communication initialization: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error during JSON communication initialization: ${e.message}", e)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                startRecording()
            }

            ACTION_STOP_RECORDING -> {
                stopRecording()
            }

            ACTION_GET_STATUS -> {
                broadcastCurrentStatus()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.info("RecordingService destroyed")

        if (isRecording) {
            serviceScope.launch {
                stopRecordingInternal()
            }
        }

        previewStreamer.stopStreaming()

        try {
            adaptiveFrameRateController.stop()
            networkQualityMonitor.stopMonitoring()
            logger.info("[DEBUG_LOG] Adaptive frame rate control system stopped")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            logger.error("Invalid state stopping adaptive frame rate control: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error stopping adaptive frame rate control: ${e.message}", e)
        }

        jsonSocketClient.disconnect()

        serviceScope.cancel()

        logger.info("RecordingService cleanup complete")
    }

    private fun broadcastCurrentStatus() {
        serviceScope.launch {
            try {
                logger.info("Broadcasting current status - Recording: $isRecording, Session: $currentSessionId")

                val statusInfo = gatherStatusInformation()

                broadcastStatusViaJsonSocket(statusInfo)

                sendLocalStatusBroadcast(statusInfo)

                logger.info("Status broadcast completed successfully")
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Permission error broadcasting status: ${e.message}", e)
            } catch (e: IllegalStateException) {
                logger.error("Invalid state during status broadcast: ${e.message}", e)
            } catch (e: RuntimeException) {
                logger.error("Runtime error during status broadcast: ${e.message}", e)
            }
        }
    }

    private suspend fun gatherStatusInformation(): DeviceStatusInfo =
        try {
            DeviceStatusInfo(
                isRecording = isRecording,
                currentSessionId = currentSessionId,
                recordingStartTime = if (isRecording) System.currentTimeMillis() else null,
                cameraStatus = getCameraStatus(),
                thermalStatus = getThermalStatus(),
                shimmerStatus = getShimmerStatus(),
                batteryLevel = getBatteryLevel(),
                availableStorage = getAvailableStorage(),
                deviceTemperature = getDeviceTemperature(),
                networkConfig = networkConfiguration.getConfigurationSummary(),
                connectionStatus = getConnectionStatus(),
                previewStreamingActive = previewStreamer.getStreamingStats().isStreaming,
                timestamp = System.currentTimeMillis(),
                deviceModel = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Permission error gathering status information: ${e.message}", e)
            DeviceStatusInfo.createErrorStatus("Permission error: ${e.message}")
        } catch (e: IllegalStateException) {
            logger.error("Invalid state gathering status information: ${e.message}", e)
            DeviceStatusInfo.createErrorStatus("Invalid state: ${e.message}")
        } catch (e: RuntimeException) {
            logger.error("Runtime error gathering status information: ${e.message}", e)
            DeviceStatusInfo.createErrorStatus("Runtime error: ${e.message}")
        }

    private fun broadcastStatusViaJsonSocket(statusInfo: DeviceStatusInfo) {
        try {
            jsonSocketClient.sendStatusUpdate(
                battery = statusInfo.batteryLevel,
                storage = statusInfo.availableStorage,
                temperature = statusInfo.deviceTemperature,
                recording = statusInfo.isRecording,
            )

            logger.debug("JSON status broadcast sent successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.net.ConnectException) {
            logger.error("Connection error broadcasting status via JSON socket: ${e.message}", e)
        } catch (e: IllegalStateException) {
            logger.error("Invalid state broadcasting status via JSON socket: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error broadcasting status via JSON socket: ${e.message}", e)
        }
    }

    private fun sendLocalStatusBroadcast(statusInfo: DeviceStatusInfo) {
        try {
            val intent =
                Intent("com.multisensor.recording.STATUS_UPDATE").apply {
                    putExtra("is_recording", statusInfo.isRecording)
                    putExtra("session_id", statusInfo.currentSessionId)
                    putExtra("battery_level", statusInfo.batteryLevel)
                    putExtra("available_storage", statusInfo.availableStorage)
                    putExtra("preview_streaming", statusInfo.previewStreamingActive)
                    putExtra("timestamp", statusInfo.timestamp)
                }

            sendBroadcast(intent)
            logger.debug("Local status broadcast sent")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Permission error sending local status broadcast: ${e.message}", e)
        } catch (e: IllegalStateException) {
            logger.error("Invalid state sending local status broadcast: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error sending local status broadcast: ${e.message}", e)
        }
    }

    private fun getCameraStatus(): String =
        try {
            if (isRecording) "recording" else "ready"
        } catch (e: SecurityException) {
            logger.error("Permission error getting camera status: ${e.message}", e)
            "error"
        } catch (e: IllegalStateException) {
            logger.error("Invalid state getting camera status: ${e.message}", e)
            "error"
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting camera status: ${e.message}", e)
            "error"
        }

    private fun getThermalStatus(): String =
        try {
            val status = thermalRecorder.getThermalCameraStatus()
            when {
                status.isRecording -> "recording"
                status.isAvailable && status.isPreviewActive -> "ready"
                status.isAvailable -> "connected"
                !status.isAvailable -> "unavailable"
                else -> "unknown"
            }
        } catch (e: SecurityException) {
            logger.error("Permission error getting thermal status: ${e.message}", e)
            "error"
        } catch (e: IllegalStateException) {
            logger.error("Invalid state getting thermal status: ${e.message}", e)
            "error"
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting thermal status: ${e.message}", e)
            "error"
        }

    private fun getShimmerStatus(): String =
        try {
            val status = shimmerRecorder.getShimmerStatus()
            when {
                !status.isAvailable -> "unavailable"
                !status.isConnected -> "disconnected"
                status.isRecording -> "recording"
                status.isConnected -> "ready"
                else -> "unknown"
            }
        } catch (e: SecurityException) {
            logger.error("Permission error getting shimmer status: ${e.message}", e)
            "error"
        } catch (e: IllegalStateException) {
            logger.error("Invalid state getting shimmer status: ${e.message}", e)
            "error"
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting shimmer status: ${e.message}", e)
            "error"
        }

    private fun getBatteryLevel(): Int? =
        try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: SecurityException) {
            logger.error("Permission error getting battery level: ${e.message}", e)
            null
        } catch (e: IllegalStateException) {
            logger.error("Invalid state getting battery level: ${e.message}", e)
            null
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting battery level: ${e.message}", e)
            null
        }

    private fun getAvailableStorage(): String? =
        try {
            val externalDir = getExternalFilesDir(null)
            if (externalDir != null) {
                val stat = android.os.StatFs(externalDir.path)
                val availableBytes = stat.availableBytes
                val availableGB = availableBytes / (1024 * 1024 * 1024)
                "${availableGB}GB"
            } else {
                null
            }
        } catch (e: SecurityException) {
            logger.error("Permission error getting available storage: ${e.message}", e)
            null
        } catch (e: java.io.IOException) {
            logger.error("IO error getting available storage: ${e.message}", e)
            null
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting available storage: ${e.message}", e)
            null
        }

    private fun getDeviceTemperature(): Double? =
        try {
            null
        } catch (e: SecurityException) {
            logger.error("Permission error getting device temperature: ${e.message}", e)
            null
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting device temperature: ${e.message}", e)
            null
        }

    private fun getConnectionStatus(): String =
        try {
            val jsonConnected = jsonSocketClient.isConnected()
            if (jsonConnected) "connected" else "disconnected"
        } catch (e: IllegalStateException) {
            logger.error("Invalid state getting connection status: ${e.message}", e)
            "error"
        } catch (e: RuntimeException) {
            logger.error("Runtime error getting connection status: ${e.message}", e)
            "error"
        }

    private fun startRecording() {
        if (isRecording) {
            logger.warning("Recording already in progress")
            return
        }

        serviceScope.launch {
            try {
                logger.info("Starting recording session...")

                currentSessionId = sessionManager.createNewSession()
                logger.info("Created session: $currentSessionId")

                startForeground(NOTIFICATION_ID, createRecordingNotification())

                val cameraSessionInfo = cameraRecorder.startSession(recordVideo = true, captureRaw = false)
                val thermalStarted = thermalRecorder.startRecording(currentSessionId!!)
                val shimmerStarted = shimmerRecorder.startRecording(currentSessionId!!)

                if (cameraSessionInfo != null) {
                    isRecording = true

                    previewStreamer.startStreaming()

                    logger.info("Recording started successfully")
                    updateNotification("Recording in progress - Session: $currentSessionId")
                } else {
                    logger.error("Failed to start camera recording")
                    stopRecordingInternal()
                }

                logger.info("Recording status - Camera: ${cameraSessionInfo != null}, Thermal: $thermalStarted, Shimmer: $shimmerStarted")
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                logger.error("Permission error starting recording: ${e.message}", e)
                stopRecordingInternal()
            } catch (e: IllegalStateException) {
                logger.error("Invalid state starting recording: ${e.message}", e)
                stopRecordingInternal()
            } catch (e: RuntimeException) {
                logger.error("Runtime error starting recording: ${e.message}", e)
                stopRecordingInternal()
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) {
            logger.warning("No recording in progress")
            return
        }

        serviceScope.launch {
            stopRecordingInternal()
        }
    }

    private suspend fun stopRecordingInternal() {
        try {
            logger.info("Stopping recording session...")

            cameraRecorder.stopSession()
            thermalRecorder.stopRecording()
            shimmerRecorder.stopRecording()

            previewStreamer.stopStreaming()

            currentSessionId?.let { sessionId ->
                sessionManager.finalizeCurrentSession()
                logger.info("Session finalized: $sessionId")
            }

            isRecording = false
            currentSessionId = null

            updateNotification("Recording stopped")

            kotlinx.coroutines.delay(2000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()

            logger.info("Recording stopped successfully")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            logger.error("Permission error stopping recording: ${e.message}", e)
        } catch (e: IllegalStateException) {
            logger.error("Invalid state stopping recording: ${e.message}", e)
        } catch (e: RuntimeException) {
            logger.error("Runtime error stopping recording: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Notifications for multi-sensor recording sessions"
                    setShowBadge(false)
                }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createRecordingNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Multi-Sensor Recording")
            .setContentText("Preparing to record...")
            .setSmallIcon(R.drawable.ic_multisensor_idle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setContentTitle("Multi-Sensor Recording")
                .setContentText(message)
                .setSmallIcon(if (isRecording) R.drawable.ic_multisensor_recording else R.drawable.ic_multisensor_idle)
                .setContentIntent(pendingIntent)
                .setOngoing(isRecording)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
