package com.topdon.tc001.orchestrator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.topdon.tc001.R
import com.topdon.tc001.gsr.GSRManager
import com.topdon.tc001.recording.EnhancedRecordingActivity
import kotlinx.coroutines.*

class OrchestratorService : Service(), OrchestratorClient.OrchestratorListener {
    
    companion object {
        private const val TAG = "OrchestratorService"
        private const val NOTIFICATION_ID = 2000
        private const val CHANNEL_ID = "orchestrator_service_channel"
        private const val SERVICE_TYPE = "_bucika-gsr._tcp"
        private const val DISCOVERY_TIMEOUT = 30000L
        
        const val ACTION_CONNECT = "com.topdon.tc001.orchestrator.CONNECT"
        const val ACTION_DISCONNECT = "com.topdon.tc001.orchestrator.DISCONNECT"
        const val ACTION_MANUAL_CONNECT = "com.topdon.tc001.orchestrator.MANUAL_CONNECT"
        
        const val EXTRA_SERVER_URL = "server_url"
    }

    private lateinit var orchestratorClient: OrchestratorClient
    private lateinit var gsrManager: GSRManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = OrchestratorBinder()
    
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val discoveredServices = mutableSetOf<NsdServiceInfo>()
    
    private var isConnectedToOrchestrator = false
    private var currentServerUrl: String? = null
    private var serviceListener: ServiceListener? = null
    private var isSessionActive = false
    private var dataPointsCollected = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Orchestrator service created")
        
        createNotificationChannel()
        orchestratorClient = OrchestratorClient(this, this)
        gsrManager = GSRManager.getInstance(this)
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        
        setupGSRDataStreaming()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_CONNECT -> {
                startForeground(NOTIFICATION_ID, createNotification("Discovering orchestrators..."))
                startDiscovery()
            }
            ACTION_DISCONNECT -> {
                disconnectFromOrchestrator()
            }
            ACTION_MANUAL_CONNECT -> {
                val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
                if (serverUrl != null) {
                    startForeground(NOTIFICATION_ID, createNotification("Connecting to orchestrator..."))
                    connectToOrchestrator(serverUrl)
                }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        Log.i(TAG, "Orchestrator service destroyed")
        
        stopDiscovery()
        orchestratorClient.disconnect()
        serviceScope.cancel()
        
        super.onDestroy()
    }

    private fun startDiscovery() {
        if (discoveryListener != null) {
            Log.w(TAG, "Discovery already in progress")
            return
        }
        
        Log.i(TAG, "Starting orchestrator discovery")
        discoveredServices.clear()
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
                updateNotification("Discovery failed")
                serviceListener?.onDiscoveryError("Discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i(TAG, "Discovery started for $serviceType")
                updateNotification("Discovering orchestrators...")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(TAG, "Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                Log.i(TAG, "Service found: ${serviceInfo?.serviceName}")
                serviceInfo?.let { 
                    discoveredServices.add(it)
                    resolveService(it)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.i(TAG, "Service lost: ${serviceInfo?.serviceName}")
                serviceInfo?.let { discoveredServices.remove(it) }
            }
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener!!)
            
            serviceScope.launch {
                delay(DISCOVERY_TIMEOUT)
                if (discoveredServices.isEmpty() && !isConnectedToOrchestrator) {
                    stopDiscovery()
                    updateNotification("No orchestrators found")
                    serviceListener?.onDiscoveryError("No orchestrators found after timeout")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            updateNotification("Discovery error")
            serviceListener?.onDiscoveryError("Failed to start discovery: ${e.message}")
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        Log.i(TAG, "Resolving service: ${serviceInfo.serviceName}")
        
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { service ->
                    Log.i(TAG, "Service resolved: ${service.serviceName} at ${service.host}:${service.port}")
                    
                    val serverUrl = "ws://${service.host.hostAddress}:${service.port}"
                    updateNotification("Connecting to ${service.serviceName}...")
                    connectToOrchestrator(serverUrl)
                }
            }
        }

        try {
            nsdManager?.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve service", e)
        }
    }

    private fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
                discoveryListener = null
                Log.i(TAG, "Discovery stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop discovery", e)
            }
        }
    }

    private fun connectToOrchestrator(serverUrl: String) {
        currentServerUrl = serverUrl
        Log.i(TAG, "Connecting to orchestrator at $serverUrl")
        
        stopDiscovery()
        orchestratorClient.connect(serverUrl)
    }

    private fun disconnectFromOrchestrator() {
        Log.i(TAG, "Disconnecting from orchestrator")
        
        orchestratorClient.disconnect()
        isConnectedToOrchestrator = false
        currentServerUrl = null
        
        updateNotification("Disconnected")
        serviceListener?.onDisconnected()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onConnected() {
        Log.i(TAG, "Connected to orchestrator")
        isConnectedToOrchestrator = true
        updateNotification("Connected to orchestrator")
        serviceListener?.onConnected()
    }

    override fun onDisconnected() {
        Log.i(TAG, "Disconnected from orchestrator")
        isConnectedToOrchestrator = false
        updateNotification("Connection lost")
        serviceListener?.onDisconnected()
        
        serviceScope.launch {
            delay(5000)
            if (currentServerUrl != null) {
                connectToOrchestrator(currentServerUrl!!)
            } else {
                startDiscovery()
            }
        }
    }

    override fun onRegistered(role: String) {
        Log.i(TAG, "Registered with orchestrator as $role")
        updateNotification("Registered as $role")
        serviceListener?.onRegistered(role)
    }

    override fun onConnectionError(error: String) {
        Log.e(TAG, "Orchestrator connection error: $error")
        updateNotification("Connection error")
        serviceListener?.onConnectionError(error)
    }

    override fun onStartSession(sessionId: String, config: Map<String, Any>) {
        Log.i(TAG, "Session start requested: $sessionId")
        updateNotification("Recording session active")
        serviceListener?.onStartSession(sessionId, config)
        
        val intent = Intent(this, EnhancedRecordingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("sessionId", sessionId)
            putExtra("orchestratorMode", true)
        }
        startActivity(intent)
    }

    override fun onStopSession() {
        Log.i(TAG, "Session stop requested")
        updateNotification("Session ended")
        serviceListener?.onStopSession()
    }

    override fun onSyncMark(markerId: String, referenceTime: Long) {
        Log.i(TAG, "Sync mark: $markerId at $referenceTime")
        serviceListener?.onSyncMark(markerId, referenceTime)
    }

    override fun onError(errorCode: String, message: String, details: Map<String, Any>) {
        Log.e(TAG, "Orchestrator error: $errorCode - $message")
        serviceListener?.onError(errorCode, message, details)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orchestrator Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "PC orchestrator connection status"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        val intent = Intent(this, EnhancedRecordingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bucika GSR Orchestrator")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_recording_active)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun setupGSRDataStreaming() {
        gsrManager.setGSRDataListener(object : GSRManager.GSRDataListener {
            override fun onGSRDataReceived(timestamp: Long, gsrValue: Double, skinTemperature: Double) {

                if (isSessionActive && isConnectedToOrchestrator) {
                    orchestratorClient.sendGSRData(gsrValue, skinTemperature, timestamp)
                    dataPointsCollected++
                    
                    if (dataPointsCollected % 128 == 0) {
                        orchestratorClient.sendSessionStatus(true, 0, dataPointsCollected)
                    }
                }
            }

            override fun onConnectionStatusChanged(isConnected: Boolean, deviceName: String?) {
                Log.i(TAG, "GSR device connection: $isConnected ($deviceName)")
                serviceListener?.onGSRConnectionChanged(isConnected, deviceName)
            }
        })
    }

    override fun onStartSession(sessionId: String, config: Map<String, Any>) {
        Log.i(TAG, "Starting session: $sessionId")
        isSessionActive = true
        dataPointsCollected = 0
        
        updateNotification("Recording session active")
        serviceListener?.onSessionStarted(sessionId, config)
        
        if (gsrManager.isConnected()) {
            gsrManager.startRecording()
        }
    }

    override fun onStopSession() {
        Log.i(TAG, "Stopping session")
        isSessionActive = false
        
        updateNotification("Session ended")
        serviceListener?.onSessionStopped()
        
        gsrManager.stopRecording()
        
        uploadSessionFiles()
    }
    
    private fun uploadSessionFiles() {
        try {

            val gsrFiles = findGSRDataFiles()
            Log.i(TAG, "Found ${gsrFiles.size} GSR files to upload")
            
            gsrFiles.forEach { file ->
                if (file.exists() && file.length() > 0) {
                    Log.i(TAG, "Uploading GSR file: ${file.name} (${file.length()} bytes)")
                    orchestratorClient.uploadFile(
                        filePath = file.absolutePath,
                        fileType = "gsr_data",
                        callback = object : OrchestratorClient.FileUploadCallback {
                            override fun onProgress(bytesUploaded: Long, totalBytes: Long) {
                                val progress = (bytesUploaded * 100 / totalBytes).toInt()
                                Log.d(TAG, "Upload progress for ${file.name}: $progress%")
                                updateNotification("Uploading ${file.name}: $progress%")
                            }
                            
                            override fun onCompleted(uploadedFilePath: String) {
                                Log.i(TAG, "Upload completed: ${file.name}")
                                updateNotification("Upload completed")
                            }
                            
                            override fun onError(error: String) {
                                Log.e(TAG, "Upload failed for ${file.name}: $error")
                                updateNotification("Upload failed")
                            }
                        }
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading session files", e)
        }
    }
    
    private fun findGSRDataFiles(): List<java.io.File> {
        val files = mutableListOf<java.io.File>()
        
        try {

            val externalDir = android.os.Environment.getExternalStorageDirectory()
            val gsrDataDirs = listOf(
                java.io.File(externalDir, "BucikaGSR/gsr_data"),
                java.io.File(externalDir, "Documents/BucikaGSR"),
                java.io.File(applicationContext.getExternalFilesDir(null), "gsr_data")
            )
            
            gsrDataDirs.forEach { dir ->
                if (dir.exists()) {
                    dir.listFiles()?.let { dirFiles ->
                        dirFiles.filter { file ->
                            file.isFile && 
                            (file.name.endsWith(".csv") || file.name.endsWith(".json")) &&
                            file.name.contains("gsr", ignoreCase = true) &&
                            isRecentFile(file)
                        }.let { gsrFiles ->
                            files.addAll(gsrFiles)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding GSR data files", e)
        }
        
        return files
    }
    
    private fun isRecentFile(file: java.io.File): Boolean {
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        return file.lastModified() > oneHourAgo
    }

    inner class OrchestratorBinder : Binder() {
        fun getService(): OrchestratorService = this@OrchestratorService
    }

    interface ServiceListener {
        fun onConnected()
        fun onDisconnected()
        fun onRegistered(role: String)
        fun onConnectionError(error: String)
        fun onDiscoveryError(error: String)
        fun onStartSession(sessionId: String, config: Map<String, Any>)
        fun onStopSession()
        fun onSyncMark(markerId: String, referenceTime: Long)
        fun onError(errorCode: String, message: String, details: Map<String, Any>)
        
        fun onGSRConnectionChanged(isConnected: Boolean, deviceName: String?)
        fun onSessionStarted(sessionId: String, config: Map<String, Any>)
        fun onSessionStopped()
    }

    fun setServiceListener(listener: ServiceListener?) {
        this.serviceListener = listener
    }

    fun isConnected(): Boolean = isConnectedToOrchestrator

    fun getServerUrl(): String? = currentServerUrl

    fun getDiscoveredServices(): Set<NsdServiceInfo> = discoveredServices.toSet()
