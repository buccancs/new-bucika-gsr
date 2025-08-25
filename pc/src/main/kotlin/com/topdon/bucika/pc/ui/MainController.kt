package com.topdon.bucika.pc.ui

import com.topdon.bucika.pc.discovery.DiscoveryService
import com.topdon.bucika.pc.protocol.EmptyPayload
import com.topdon.bucika.pc.protocol.MessageType
import com.topdon.bucika.pc.protocol.StartPayload
import com.topdon.bucika.pc.protocol.SyncMarkPayload
import com.topdon.bucika.pc.session.SessionManager
import com.topdon.bucika.pc.session.SessionState
import com.topdon.bucika.pc.websocket.WebSocketServer
import io.github.oshai.kotlinlogging.KotlinLogging
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import java.net.URL
import java.util.*

private val logger = KotlinLogging.logger {}

class MainController : Initializable {
    
    @FXML private lateinit var devicesTable: TableView<DeviceTableRow>
    @FXML private lateinit var deviceIdColumn: TableColumn<DeviceTableRow, String>
    @FXML private lateinit var deviceNameColumn: TableColumn<DeviceTableRow, String>
    @FXML private lateinit var batteryColumn: TableColumn<DeviceTableRow, String>
    @FXML private lateinit var statusColumn: TableColumn<DeviceTableRow, String>
    @FXML private lateinit var capabilitiesColumn: TableColumn<DeviceTableRow, String>
    
    @FXML private lateinit var sessionLabel: Label
    @FXML private lateinit var sessionStateLabel: Label
    @FXML private lateinit var elapsedTimeLabel: Label
    
    @FXML private lateinit var newSessionBtn: Button
    @FXML private lateinit var armSessionBtn: Button
    @FXML private lateinit var startRecordingBtn: Button
    @FXML private lateinit var syncMarkBtn: Button
    @FXML private lateinit var stopRecordingBtn: Button
    
    @FXML private lateinit var statusTextArea: TextArea
    @FXML private lateinit var refreshDevicesBtn: Button
    
    private lateinit var sessionManager: SessionManager
    private lateinit var webSocketServer: WebSocketServer
    private lateinit var discoveryService: DiscoveryService
    
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.JavaFx)
    private val devicesList = FXCollections.observableArrayList<DeviceTableRow>()
    
    private var currentSessionStartTime: Long = 0
    private var timerJob: Job? = null
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        setupTable()
        setupButtons()
        updateUIState()
    }
    
    fun initialize(sessionManager: SessionManager, webSocketServer: WebSocketServer, discoveryService: DiscoveryService) {
        this.sessionManager = sessionManager
        this.webSocketServer = webSocketServer
        this.discoveryService = discoveryService
        
        // Start monitoring services
        startMonitoring()
    }
    
    private fun setupTable() {
        deviceIdColumn.cellValueFactory = PropertyValueFactory("deviceId")
        deviceNameColumn.cellValueFactory = PropertyValueFactory("deviceName")
        batteryColumn.cellValueFactory = PropertyValueFactory("battery")
        statusColumn.cellValueFactory = PropertyValueFactory("status")
        capabilitiesColumn.cellValueFactory = PropertyValueFactory("capabilities")
        
        devicesTable.items = devicesList
    }
    
    private fun setupButtons() {
        newSessionBtn.setOnAction { createNewSession() }
        armSessionBtn.setOnAction { armCurrentSession() }
        startRecordingBtn.setOnAction { startRecording() }
        syncMarkBtn.setOnAction { insertSyncMark() }
        stopRecordingBtn.setOnAction { stopRecording() }
        refreshDevicesBtn.setOnAction { refreshDevices() }
        
        // Initially disable session control buttons
        armSessionBtn.isDisable = true
        startRecordingBtn.isDisable = true
        syncMarkBtn.isDisable = true
        stopRecordingBtn.isDisable = true
    }
    
    private fun startMonitoring() {
        uiScope.launch {
            // Monitor session state changes
            sessionManager.currentSession.collect { session ->
                Platform.runLater {
                    if (session != null) {
                        sessionLabel.text = "Session: ${session.name}"
                        sessionStateLabel.text = "State: ${session.state}"
                        
                        when (session.state) {
                            SessionState.NEW -> {
                                armSessionBtn.isDisable = false
                                startRecordingBtn.isDisable = true
                                syncMarkBtn.isDisable = true
                                stopRecordingBtn.isDisable = true
                            }
                            SessionState.ARMED -> {
                                armSessionBtn.isDisable = true
                                startRecordingBtn.isDisable = false
                                syncMarkBtn.isDisable = true
                                stopRecordingBtn.isDisable = true
                            }
                            SessionState.RECORDING -> {
                                armSessionBtn.isDisable = true
                                startRecordingBtn.isDisable = true
                                syncMarkBtn.isDisable = false
                                stopRecordingBtn.isDisable = false
                                startTimer()
                            }
                            else -> {
                                armSessionBtn.isDisable = true
                                startRecordingBtn.isDisable = true
                                syncMarkBtn.isDisable = true
                                stopRecordingBtn.isDisable = true
                                stopTimer()
                            }
                        }
                    } else {
                        sessionLabel.text = "No active session"
                        sessionStateLabel.text = "State: Idle"
                        newSessionBtn.isDisable = false
                        armSessionBtn.isDisable = true
                        startRecordingBtn.isDisable = true
                        syncMarkBtn.isDisable = true
                        stopRecordingBtn.isDisable = true
                    }
                }
            }
        }
        
        uiScope.launch {
            // Monitor discovered devices
            discoveryService.devices.collect { devices ->
                Platform.runLater {
                    updateDeviceTable()
                }
            }
        }
        
        // Periodic device table update
        uiScope.launch {
            while (true) {
                delay(5000) // Update every 5 seconds
                Platform.runLater { updateDeviceTable() }
            }
        }
    }
    
    private fun updateDeviceTable() {
        val connectedDevices = webSocketServer.getConnectedDevices()
        val discoveredDevices = discoveryService.getAllDevices()
        
        devicesList.clear()
        
        // Add connected devices
        connectedDevices.forEach { device ->
            devicesList.add(
                DeviceTableRow(
                    deviceId = device.deviceId,
                    deviceName = device.deviceName,
                    battery = "${device.batteryLevel}%",
                    status = "Connected",
                    capabilities = device.capabilities.joinToString(", ")
                )
            )
        }
        
        // Add discovered but not connected devices
        discoveredDevices.forEach { device ->
            if (connectedDevices.none { it.deviceId == device.deviceId }) {
                devicesList.add(
                    DeviceTableRow(
                        deviceId = device.deviceId,
                        deviceName = device.deviceName,
                        battery = if (device.batteryLevel >= 0) "${device.batteryLevel}%" else "Unknown",
                        status = "Discovered",
                        capabilities = device.capabilities.joinToString(", ")
                    )
                )
            }
        }
    }
    
    private fun createNewSession() {
        uiScope.launch {
            try {
                val session = sessionManager.createSession()
                addStatusMessage("Created new session: ${session.name}")
                newSessionBtn.isDisable = true
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to create session" }
                addStatusMessage("Error creating session: ${e.message}")
            }
        }
    }
    
    private fun armCurrentSession() {
        uiScope.launch {
            val currentSession = sessionManager.currentSession.value
            if (currentSession != null) {
                val success = sessionManager.armSession(currentSession.id)
                if (success) {
                    addStatusMessage("Armed session: ${currentSession.name}")
                } else {
                    addStatusMessage("Failed to arm session - check preflight requirements")
                }
            }
        }
    }
    
    private fun startRecording() {
        uiScope.launch {
            val currentSession = sessionManager.currentSession.value
            if (currentSession != null) {
                val success = sessionManager.startSession(currentSession.id)
                if (success) {
                    currentSessionStartTime = System.currentTimeMillis()
                    
                    // Broadcast START command to all devices
                    val startPayload = StartPayload(
                        sessionConfig = com.topdon.bucika.pc.protocol.SessionConfig(
                            videoConfig = com.topdon.bucika.pc.protocol.VideoConfig(
                                resolution = "1080p",
                                fps = 30,
                                bitrate = 8000000
                            ),
                            gsrConfig = com.topdon.bucika.pc.protocol.GSRConfig(
                                sampleRate = 128,
                                channels = listOf("GSR", "TEMP")
                            )
                        )
                    )
                    
                    webSocketServer.broadcastToAllDevices(
                        MessageType.START, 
                        startPayload, 
                        currentSession.id
                    )
                    
                    addStatusMessage("Started recording session: ${currentSession.name}")
                } else {
                    addStatusMessage("Failed to start recording session")
                }
            }
        }
    }
    
    private fun insertSyncMark() {
        uiScope.launch {
            val currentSession = sessionManager.currentSession.value
            if (currentSession != null) {
                val markerId = "sync-${System.currentTimeMillis()}"
                val referenceTime = System.nanoTime()
                
                sessionManager.recordSyncMark(currentSession.id, markerId)
                
                val syncMarkPayload = SyncMarkPayload(
                    markerId = markerId,
                    referenceTime = referenceTime
                )
                
                webSocketServer.broadcastToAllDevices(
                    MessageType.SYNC_MARK,
                    syncMarkPayload,
                    currentSession.id
                )
                
                addStatusMessage("Inserted sync mark: $markerId")
            }
        }
    }
    
    private fun stopRecording() {
        uiScope.launch {
            val currentSession = sessionManager.currentSession.value
            if (currentSession != null) {
                val success = sessionManager.stopSession(currentSession.id)
                if (success) {
                    // Broadcast STOP command to all devices
                    webSocketServer.broadcastToAllDevices(
                        MessageType.STOP,
                        EmptyPayload,
                        currentSession.id
                    )
                    
                    addStatusMessage("Stopped recording session: ${currentSession.name}")
                } else {
                    addStatusMessage("Failed to stop recording session")
                }
            }
        }
    }
    
    private fun refreshDevices() {
        discoveryService.refreshDiscovery()
        addStatusMessage("Refreshing device discovery...")
    }
    
    private fun startTimer() {
        timerJob = uiScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - currentSessionStartTime
                val minutes = elapsed / 60000
                val seconds = (elapsed % 60000) / 1000
                
                Platform.runLater {
                    elapsedTimeLabel.text = String.format("Elapsed: %02d:%02d", minutes, seconds)
                }
                
                delay(1000)
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        Platform.runLater {
            elapsedTimeLabel.text = "Elapsed: --:--"
        }
    }
    
    private fun updateUIState() {
        // Initial state
        sessionLabel.text = "No active session"
        sessionStateLabel.text = "State: Idle"
        elapsedTimeLabel.text = "Elapsed: --:--"
    }
    
    private fun addStatusMessage(message: String) {
        Platform.runLater {
            val timestamp = java.time.LocalTime.now().toString().substring(0, 8)
            statusTextArea.appendText("[$timestamp] $message\n")
            statusTextArea.scrollTop = Double.MAX_VALUE
        }
        logger.info { message }
    }
}

data class DeviceTableRow(
    val deviceId: String,
    val deviceName: String,
    val battery: String,
    val status: String,
    val capabilities: String
)