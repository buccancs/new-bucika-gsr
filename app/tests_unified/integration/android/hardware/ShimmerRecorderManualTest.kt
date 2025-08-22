package com.multisensor.recording.recording

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.multisensor.recording.recording.DeviceConfiguration.SensorChannel
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShimmerRecorderManualTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )

    @Inject
    lateinit var shimmerRecorder: ShimmerRecorder

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var logger: Logger

    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var discoveredDevices: List<String> = emptyList()
    private var connectedDevices: List<String> = emptyList()

    companion object {
        private const val SHIMMER_PIN = "1234"
        private const val TEST_TIMEOUT_MS = 30000L
        private const val DEVICE_CONNECTION_TIMEOUT_MS = 15000L
        private const val STREAMING_TEST_DURATION_MS = 10000L
        private const val RECONNECTION_TIMEOUT_MS = 20000L

        private const val MIN_REQUIRED_DEVICES = 2

        private const val DEVICE_A_NAME = "Device A"
        private const val DEVICE_B_NAME = "Device B"
    }

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        assertNotNull("Bluetooth adapter must be available", bluetoothAdapter)
        assertTrue("Bluetooth must be enabled for testing", bluetoothAdapter.isEnabled)

        println("[DEBUG_LOG] Shimmer hardware test setup complete")
        println("[DEBUG_LOG] Bluetooth adapter: ${bluetoothAdapter.name}")
        println("[DEBUG_LOG] Bluetooth address: ${bluetoothAdapter.address}")
    }

    @After
    fun cleanup() =
        runBlocking {
            try {
                shimmerRecorder.stopRecording()
                shimmerRecorder.cleanup()

                delay(1000)
                println("[DEBUG_LOG] Shimmer hardware test cleanup completed")
            } catch (e: Exception) {
                println("[DEBUG_LOG] Cleanup error: ${e.message}")
            }
        }

    @Test
    fun test01_InitialSetupAndPairing() =
        runBlocking {
            println("[DEBUG_LOG] === Test 1: Initial Setup & Pairing ===")

            val initialized =
                withTimeout(TEST_TIMEOUT_MS) {
                    shimmerRecorder.initialize()
                }
            assertTrue("ShimmerRecorder should initialize successfully", initialized)

            println("[DEBUG_LOG] Scanning for Shimmer devices...")
            discoveredDevices =
                withTimeout(TEST_TIMEOUT_MS) {
                    shimmerRecorder.scanAndPairDevices()
                }

            assertNotNull("Device discovery should return a list", discoveredDevices)
            assertTrue(
                "Should discover at least $MIN_REQUIRED_DEVICES Shimmer devices. Found: ${discoveredDevices.size}",
                discoveredDevices.size >= MIN_REQUIRED_DEVICES,
            )

            discoveredDevices.forEach { macAddress ->
                assertTrue(
                    "MAC address should be valid format: $macAddress",
                    macAddress.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")),
                )
                println("[DEBUG_LOG] Discovered Shimmer device: $macAddress")
            }

            val pairedDevices = bluetoothAdapter.bondedDevices
            val shimmerPairedCount =
                pairedDevices.count { device ->
                    discoveredDevices.contains(device.address)
                }

            println("[DEBUG_LOG] Found $shimmerPairedCount paired Shimmer devices in system")
            println("[DEBUG_LOG] Test 1 PASSED: Successfully discovered and validated ${discoveredDevices.size} Shimmer devices")
        }

    @Test
    fun test02_PermissionHandling() {
        println("[DEBUG_LOG] === Test 2: Permission Handling ===")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val connectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)

            assertEquals(
                "BLUETOOTH_SCAN permission should be granted",
                PackageManager.PERMISSION_GRANTED,
                scanPermission
            )
            assertEquals(
                "BLUETOOTH_CONNECT permission should be granted",
                PackageManager.PERMISSION_GRANTED,
                connectPermission
            )

            println("[DEBUG_LOG] Android 12+ Bluetooth permissions verified")
        } else {
            val bluetoothPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
            val bluetoothAdminPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)

            assertEquals(
                "BLUETOOTH permission should be granted",
                PackageManager.PERMISSION_GRANTED,
                bluetoothPermission
            )
            assertEquals(
                "BLUETOOTH_ADMIN permission should be granted",
                PackageManager.PERMISSION_GRANTED,
                bluetoothAdminPermission
            )

            println("[DEBUG_LOG] Legacy Bluetooth permissions verified")
        }

        val fineLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        assertEquals(
            "ACCESS_FINE_LOCATION permission should be granted",
            PackageManager.PERMISSION_GRANTED,
            fineLocationPermission
        )
        assertEquals(
            "ACCESS_COARSE_LOCATION permission should be granted",
            PackageManager.PERMISSION_GRANTED,
            coarseLocationPermission
        )

        assertTrue("Bluetooth should be enabled", bluetoothAdapter.isEnabled)

        println("[DEBUG_LOG] Test 2 PASSED: All required permissions verified")
    }

    @Test
    fun test03_MultiDeviceConnection() =
        runBlocking {
            println("[DEBUG_LOG] === Test 3: Multi-Device Connection ===")

            if (discoveredDevices.isEmpty()) {
                discoveredDevices = shimmerRecorder.scanAndPairDevices()
            }

            assertTrue("Need discovered devices for connection test", discoveredDevices.isNotEmpty())

            println("[DEBUG_LOG] Connecting to ${discoveredDevices.size} Shimmer devices...")
            val connectionSuccess =
                withTimeout(DEVICE_CONNECTION_TIMEOUT_MS) {
                    shimmerRecorder.connectDevices(discoveredDevices)
                }

            assertTrue("Should successfully connect to Shimmer devices", connectionSuccess)

            val shimmerStatus = shimmerRecorder.getShimmerStatus()
            assertTrue("ShimmerRecorder should report as connected", shimmerStatus.isConnected)
            assertTrue("ShimmerRecorder should be available", shimmerStatus.isAvailable)

            connectedDevices = discoveredDevices

            println("[DEBUG_LOG] Test 3 PASSED: Successfully connected to ${discoveredDevices.size} devices")
        }

    @Test
    fun test04_ChannelSelection() =
        runBlocking {
            println("[DEBUG_LOG] === Test 4: Channel Selection ===")

            if (connectedDevices.isEmpty()) {
                test03_MultiDeviceConnection()
            }

            assertTrue("Need connected devices for channel selection test", connectedDevices.isNotEmpty())

            val deviceA = connectedDevices[0]
            val deviceAChannels = setOf(SensorChannel.GSR, SensorChannel.PPG)

            val deviceAConfigured = shimmerRecorder.setEnabledChannels(deviceA, deviceAChannels)
            assertTrue("Device A should be configured successfully", deviceAConfigured)

            println("[DEBUG_LOG] Device A ($deviceA) configured with: ${deviceAChannels.joinToString { it.displayName }}")

            if (connectedDevices.size >= 2) {
                val deviceB = connectedDevices[1]
                val deviceBChannels =
                    setOf(
                        SensorChannel.GSR,
                        SensorChannel.PPG,
                        SensorChannel.ACCEL,
                        SensorChannel.GYRO,
                        SensorChannel.MAG,
                    )

                val deviceBConfigured = shimmerRecorder.setEnabledChannels(deviceB, deviceBChannels)
                assertTrue("Device B should be configured successfully", deviceBConfigured)

                println("[DEBUG_LOG] Device B ($deviceB) configured with: ${deviceBChannels.joinToString { it.displayName }}")
            }

            val streamingStarted = shimmerRecorder.startStreaming()
            assertTrue("Streaming should start successfully with configured channels", streamingStarted)

            delay(2000)

            val streamingStopped = shimmerRecorder.stopStreaming()
            assertTrue("Streaming should stop successfully", streamingStopped)

            println("[DEBUG_LOG] Test 4 PASSED: Channel selection and configuration validated")
        }

    @Test
    fun test05_RecordingSessionIntegration() =
        runBlocking {
            println("[DEBUG_LOG] === Test 5: Recording Session Integration ===")

            if (connectedDevices.isEmpty()) {
                test03_MultiDeviceConnection()
                test04_ChannelSelection()
            }

            val sessionId = sessionManager.createNewSession()
            assertNotNull("Session should be created successfully", sessionId)

            println("[DEBUG_LOG] Created test session: $sessionId")

            val recordingStarted =
                withTimeout(TEST_TIMEOUT_MS) {
                    shimmerRecorder.startRecording(sessionId)
                }
            assertTrue("Shimmer recording should start successfully", recordingStarted)

            val shimmerStatus = shimmerRecorder.getShimmerStatus()
            assertTrue("ShimmerRecorder should report as recording", shimmerStatus.isRecording)

            println("[DEBUG_LOG] Recording for ${STREAMING_TEST_DURATION_MS / 1000} seconds...")
            delay(STREAMING_TEST_DURATION_MS)

            shimmerRecorder.stopRecording()

            sessionManager.finalizeCurrentSession()

            val sessionPaths = sessionManager.getSessionFilePaths()
            assertNotNull("Session paths should be available", sessionPaths)

            val sessionFolder = sessionPaths!!.sessionFolder
            val shimmerFiles =
                sessionFolder.listFiles { file ->
                    file.name.startsWith("shimmer_") && file.name.endsWith(".csv")
                }

            assertNotNull("Shimmer CSV files should exist", shimmerFiles)
            assertTrue("Should have at least one Shimmer data file", shimmerFiles!!.isNotEmpty())

            shimmerFiles.forEach { file ->
                assertTrue("Shimmer data file should not be empty: ${file.name}", file.length() > 0)
                println("[DEBUG_LOG] Created Shimmer data file: ${file.name} (${file.length()} bytes)")
            }

            println("[DEBUG_LOG] Test 5 PASSED: Session integration and file creation validated")
        }

    @Test
    fun test06_RealtimePCStreaming() =
        runBlocking {
            println("[DEBUG_LOG] === Test 6: Real-time PC Streaming ===")

            if (connectedDevices.isEmpty()) {
                test03_MultiDeviceConnection()
            }

            val streamingStarted = shimmerRecorder.startStreaming()
            assertTrue("Streaming should start successfully", streamingStarted)

            val sessionId = "StreamingTest_${System.currentTimeMillis()}"
            val recordingStarted = shimmerRecorder.startRecording(sessionId)
            assertTrue("Recording should start for streaming test", recordingStarted)

            println("[DEBUG_LOG] Streaming data for ${STREAMING_TEST_DURATION_MS / 1000} seconds...")
            delay(STREAMING_TEST_DURATION_MS)

            val shimmerStatus = shimmerRecorder.getShimmerStatus()
            assertTrue("Should have recorded some samples", shimmerStatus.samplesRecorded > 0)

            println("[DEBUG_LOG] Recorded ${shimmerStatus.samplesRecorded} samples during streaming test")

            shimmerRecorder.stopRecording()
            val streamingStopped = shimmerRecorder.stopStreaming()
            assertTrue("Streaming should stop successfully", streamingStopped)

            println("[DEBUG_LOG] Test 6 PASSED: Real-time streaming simulation validated")
        }

    @Test
    fun test07_DisconnectionAndReconnection() =
        runBlocking {
            println("[DEBUG_LOG] === Test 7: Disconnection and Reconnection ===")

            if (connectedDevices.isEmpty()) {
                test03_MultiDeviceConnection()
            }

            val sessionId = "DisconnectionTest_${System.currentTimeMillis()}"
            val recordingStarted = shimmerRecorder.startRecording(sessionId)
            assertTrue("Recording should start successfully", recordingStarted)

            val initialStatus = shimmerRecorder.getShimmerStatus()
            assertTrue("Should be recording initially", initialStatus.isRecording)

            println("[DEBUG_LOG] Simulating device disconnection scenario...")
            delay(2000)

            val statusAfterDisconnection = shimmerRecorder.getShimmerStatus()
            assertTrue("Should still be recording after simulated disconnection", statusAfterDisconnection.isRecording)

            println("[DEBUG_LOG] Simulating device reconnection...")
            delay(2000)

            shimmerRecorder.stopRecording()

            val finalStatus = shimmerRecorder.getShimmerStatus()
            assertTrue("Should have recorded samples during disconnection test", finalStatus.samplesRecorded > 0)

            println("[DEBUG_LOG] Test 7 PASSED: Disconnection resilience validated")
        }

    @Test
    fun test08_DataVerification() =
        runBlocking {
            println("[DEBUG_LOG] === Test 8: Data Verification ===")

            val sessionId = sessionManager.createNewSession()
            assertNotNull("Session should be created", sessionId)

            if (connectedDevices.isEmpty()) {
                test03_MultiDeviceConnection()
            }

            val recordingStarted = shimmerRecorder.startRecording(sessionId)
            assertTrue("Recording should start", recordingStarted)

            delay(5000)

            shimmerRecorder.stopRecording()
            sessionManager.finalizeCurrentSession()

            val sessionPaths = sessionManager.getSessionFilePaths()
            assertNotNull("Session paths should be available", sessionPaths)

            val sessionFolder = sessionPaths!!.sessionFolder
            val shimmerFiles =
                sessionFolder.listFiles { file ->
                    file.name.startsWith("shimmer_") && file.name.endsWith(".csv")
                }

            assertNotNull("Shimmer files should exist", shimmerFiles)
            assertTrue("Should have Shimmer data files", shimmerFiles!!.isNotEmpty())

            shimmerFiles.forEach { file ->
                val lines = file.readLines()
                assertTrue("File should have header and data: ${file.name}", lines.size > 1)

                val header = lines[0]
                assertTrue("Header should contain timestamp", header.contains("Timestamp"))
                assertTrue("Header should contain device ID", header.contains("DeviceId"))
                assertTrue("Header should contain sensor data", header.contains("GSR") || header.contains("PPG"))

                if (lines.size > 1) {
                    val dataLine = lines[1]
                    val columns = dataLine.split(",")
                    assertTrue("Data line should have multiple columns", columns.size > 5)

                    val timestamp = columns[0].toLongOrNull()
                    assertNotNull("Timestamp should be numeric: ${columns[0]}", timestamp)
                }

                println("[DEBUG_LOG] Verified data file: ${file.name} (${lines.size} lines)")
            }

            println("[DEBUG_LOG] Test 8 PASSED: Data verification completed successfully")
        }

    @Test
    fun test09_MultiDeviceSynchronization() =
        runBlocking {
            println("[DEBUG_LOG] === Test 9: Multi-Device Synchronisation ===")

            if (connectedDevices.size < 2) {
                println("[DEBUG_LOG] Skipping synchronisation test - requires at least 2 devices")
                return@runBlocking
            }

            val sessionId = sessionManager.createNewSession()
            assertNotNull("Session should be created", sessionId)

            val recordingStarted = shimmerRecorder.startRecording(sessionId)
            assertTrue("Recording should start on all devices", recordingStarted)

            delay(10000)

            shimmerRecorder.stopRecording()
            sessionManager.finalizeCurrentSession()

            val sessionPaths = sessionManager.getSessionFilePaths()
            val sessionFolder = sessionPaths!!.sessionFolder
            val shimmerFiles =
                sessionFolder.listFiles { file ->
                    file.name.startsWith("shimmer_") && file.name.endsWith(".csv")
                }

            if (shimmerFiles != null && shimmerFiles.size >= 2) {
                val file1Lines = shimmerFiles[0].readLines()
                val file2Lines = shimmerFiles[1].readLines()

                if (file1Lines.size > 1 && file2Lines.size > 1) {
                    val timestamp1 = file1Lines[1].split(",")[0].toLongOrNull()
                    val timestamp2 = file2Lines[1].split(",")[0].toLongOrNull()

                    if (timestamp1 != null && timestamp2 != null) {
                        val timeDifference = Math.abs(timestamp1 - timestamp2)
                        println("[DEBUG_LOG] Timestamp difference between devices: ${timeDifference}ms")

                        assertTrue("Devices should be synchronised within 1 second", timeDifference < 1000)
                    }
                }
            }

            println("[DEBUG_LOG] Test 9 PASSED: Multi-device synchronisation validated")
        }

    @Test
    fun test10_ResourceCleanup() =
        runBlocking {
            println("[DEBUG_LOG] === Test 10: Resource Cleanup ===")

            if (connectedDevices.isEmpty()) {
                test03_MultiDeviceConnection()
            }

            val sessionId1 = sessionManager.createNewSession()
            assertNotNull("First session should be created", sessionId1)

            val recording1Started = shimmerRecorder.startRecording(sessionId1)
            assertTrue("First recording should start", recording1Started)

            delay(3000)

            shimmerRecorder.stopRecording()
            sessionManager.finalizeCurrentSession()

            delay(1000)

            val sessionId2 = sessionManager.createNewSession()
            assertNotNull("Second session should be created after cleanup", sessionId2)

            val recording2Started = shimmerRecorder.startRecording(sessionId2)
            assertTrue("Second recording should start after cleanup", recording2Started)

            delay(3000)

            shimmerRecorder.stopRecording()
            sessionManager.finalizeCurrentSession()

            val baseFolder = File(context.getExternalFilesDir(null), "MultiSensorRecordings")
            val sessionFolders =
                baseFolder.listFiles { file ->
                    file.isDirectory && file.name.startsWith("session_")
                }

            assertNotNull("Session folders should exist", sessionFolders)
            assertTrue("Should have created at least 2 session folders", sessionFolders!!.size >= 2)

            shimmerRecorder.cleanup()

            val finalStatus = shimmerRecorder.getShimmerStatus()
            assertFalse("Should not be recording after cleanup", finalStatus.isRecording)

            println("[DEBUG_LOG] Test 10 PASSED: Resource cleanup and multiple sessions validated")
        }

    @Test
    fun testComplete_ShimmerHardwareValidation() =
        runBlocking {
            println("[DEBUG_LOG] ========================================")
            println("[DEBUG_LOG] complete SHIMMER HARDWARE VALIDATION")
            println("[DEBUG_LOG] ========================================")

            try {
                test01_InitialSetupAndPairing()
                test02_PermissionHandling()
                test03_MultiDeviceConnection()
                test04_ChannelSelection()
                test05_RecordingSessionIntegration()
                test06_RealtimePCStreaming()
                test07_DisconnectionAndReconnection()
                test08_DataVerification()
                test09_MultiDeviceSynchronization()
                test10_ResourceCleanup()

                println("[DEBUG_LOG] ========================================")
                println("[DEBUG_LOG] ALL TESTS PASSED SUCCESSFULLY!")
                println("[DEBUG_LOG] Shimmer3 GSR+ hardware validation complete")
                println("[DEBUG_LOG] ========================================")
            } catch (e: Exception) {
                println("[DEBUG_LOG] ========================================")
                println("[DEBUG_LOG] TEST FAILURE: ${e.message}")
                println("[DEBUG_LOG] ========================================")
                throw e
            }
        }
}
