package com.multisensor.recording.recording

import android.content.Context
import com.multisensor.recording.recording.DeviceConfiguration.SensorChannel
import com.multisensor.recording.service.SessionManager
import com.multisensor.recording.util.Logger
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShimmerRecorderEnhancedTest {
    private lateinit var mockContext: Context
    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockLogger: Logger
    private lateinit var shimmerRecorder: ShimmerRecorder

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockSessionManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        shimmerRecorder = ShimmerRecorder(mockContext, mockSessionManager, mockLogger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `initialize should return true when successful`() =
        runTest {
            println("[DEBUG_LOG] Testing enhanced ShimmerRecorder initialization")

            val result = shimmerRecorder.initialize()

            assertTrue("ShimmerRecorder should initialize successfully", result)
            verify(exactly = 1) { mockLogger.info("Initializing ShimmerRecorder (stub implementation)...") }
        }

    @Test
    fun `scanAndPairDevices should return device list when successful`() =
        runTest {
            println("[DEBUG_LOG] Testing Shimmer device scanning functionality")

            shimmerRecorder.initialize()

            val devices = shimmerRecorder.scanAndPairDevices()

            assertNotNull("Device list should not be null", devices)
            assertTrue("Device list should be empty for mock environment", devices.isEmpty())
            verify { mockLogger.info("=== SHIMMER DEVICE DISCOVERY DIAGNOSTIC ===") }
        }

    @Test
    fun `connectDevices should handle empty device list gracefully`() =
        runTest {
            println("[DEBUG_LOG] Testing connection with empty device list")

            shimmerRecorder.initialize()
            val emptyDeviceList = emptyList<String>()

            val result = shimmerRecorder.connectDevices(emptyDeviceList)

            assertFalse("Connection should return false for empty device list", result)
        }

    @Test
    fun `setEnabledChannels should validate device existence`() =
        runTest {
            println("[DEBUG_LOG] Testing sensor channel configuration")

            val deviceId = "test_device"
            val channels = setOf(SensorChannel.GSR, SensorChannel.PPG)

            val result = shimmerRecorder.setEnabledChannels(deviceId, channels)

            assertFalse("Should return false for non-existent device", result)
            verify { mockLogger.error("Device not found: $deviceId") }
        }

    @Test
    fun `setSamplingRate should validate parameters`() =
        runTest {
            println("[DEBUG_LOG] Testing sampling rate configuration")

            val deviceId = "test_device"
            val samplingRate = 51.2

            val result = shimmerRecorder.setSamplingRate(deviceId, samplingRate)

            assertFalse("Should return false for non-existent device", result)
            verify { mockLogger.error("Device not found: $deviceId") }
        }

    @Test
    fun `setGSRRange should validate range parameters`() =
        runTest {
            println("[DEBUG_LOG] Testing GSR range configuration")

            val deviceId = "test_device"
            val validGsrRange = 2
            val invalidGsrRange = 10

            val result1 = shimmerRecorder.setGSRRange(deviceId, validGsrRange)

            val result2 = shimmerRecorder.setGSRRange(deviceId, invalidGsrRange)

            assertFalse("Should return false for non-existent device", result1)
            assertFalse("Should return false for invalid GSR range", result2)
            verify { mockLogger.error("Device not found: $deviceId") }
        }

    @Test
    fun `setAccelRange should validate range parameters`() =
        runTest {
            println("[DEBUG_LOG] Testing accelerometer range configuration")

            val deviceId = "test_device"
            val validAccelRange = 4
            val invalidAccelRange = 3

            val result1 = shimmerRecorder.setAccelRange(deviceId, validAccelRange)

            val result2 = shimmerRecorder.setAccelRange(deviceId, invalidAccelRange)

            assertFalse("Should return false for non-existent device", result1)
            assertFalse("Should return false for invalid accelerometer range", result2)
        }

    @Test
    fun `getDeviceInformation should return null for non-existent device`() =
        runTest {
            println("[DEBUG_LOG] Testing device information retrieval")

            val deviceId = "non_existent_device"

            val deviceInfo = shimmerRecorder.getDeviceInformation(deviceId)

            assertNull("Should return null for non-existent device", deviceInfo)
        }

    @Test
    fun `getDataQualityMetrics should return null for non-existent device`() =
        runTest {
            println("[DEBUG_LOG] Testing data quality metrics")

            val deviceId = "non_existent_device"

            val metrics = shimmerRecorder.getDataQualityMetrics(deviceId)

            assertNull("Should return null for non-existent device", metrics)
        }

    @Test
    fun `enableClockSync should handle non-existent device gracefully`() =
        runTest {
            println("[DEBUG_LOG] Testing clock synchronisation")

            val deviceId = "non_existent_device"

            val result = shimmerRecorder.enableClockSync(deviceId, true)

            assertFalse("Should return false for non-existent device", result)
            verify { mockLogger.error("Device or Shimmer instance not found: $deviceId") }
        }

    @Test
    fun `setEXGConfiguration should handle non-existent device gracefully`() =
        runTest {
            println("[DEBUG_LOG] Testing EXG configuration")

            val deviceId = "non_existent_device"

            val result = shimmerRecorder.setEXGConfiguration(deviceId, true, false)

            assertFalse("Should return false for non-existent device", result)
            verify { mockLogger.error("Device or Shimmer instance not found: $deviceId") }
        }

    @Test
    fun `startSDLogging should handle no connected devices`() =
        runTest {
            println("[DEBUG_LOG] Testing SD logging start with no devices")

            val result = shimmerRecorder.startSDLogging()

            assertFalse("Should return false when no devices connected", result)
            verify { mockLogger.info("No connected Shimmer devices found for SD logging") }
        }

    @Test
    fun `stopSDLogging should handle no connected devices`() =
        runTest {
            println("[DEBUG_LOG] Testing SD logging stop with no devices")

            val result = shimmerRecorder.stopSDLogging()

            assertFalse("Should return false when no devices connected", result)
            verify { mockLogger.info("No connected Shimmer devices found for stopping SD logging") }
        }

    @Test
    fun `disconnectAllDevices should succeed even with no devices`() =
        runTest {
            println("[DEBUG_LOG] Testing disconnect all devices")

            val result = shimmerRecorder.disconnectAllDevices()

            assertTrue("Should return true even with no devices to disconnect", result)
            verify { mockLogger.info("Disconnecting from 0 devices...") }
        }

    @Test
    fun `isAnyDeviceStreaming should return false when no devices connected`() {
        println("[DEBUG_LOG] Testing device streaming status check")

        val result = shimmerRecorder.isAnyDeviceStreaming()

        assertFalse("Should return false when no devices connected", result)
    }

    @Test
    fun `isAnyDeviceSDLogging should return false when no devices connected`() {
        println("[DEBUG_LOG] Testing device SD logging status check")

        val result = shimmerRecorder.isAnyDeviceSDLogging()

        assertFalse("Should return false when no devices connected", result)
    }

    @Test
    fun `getConnectedShimmerDevice should return null for non-existent device`() {
        println("[DEBUG_LOG] Testing connected device retrieval")

        val macAddress = "00:11:22:33:44:55"

        val device = shimmerRecorder.getConnectedShimmerDevice(macAddress)

        assertNull("Should return null for non-existent device", device)
    }

    @Test
    fun `getFirstConnectedShimmerDevice should return null when no devices connected`() {
        println("[DEBUG_LOG] Testing first connected device retrieval")

        val device = shimmerRecorder.getFirstConnectedShimmerDevice()

        assertNull("Should return null when no devices connected", device)
    }

    @Test
    fun `getShimmerBluetoothManager should return null initially`() {
        println("[DEBUG_LOG] Testing Shimmer Bluetooth manager retrieval")

        val manager = shimmerRecorder.getShimmerBluetoothManager()

        assertNull("Should return null when not initialized", manager)
    }

    @Test
    fun `DeviceInformation getDisplaySummary should format correctly`() {
        println("[DEBUG_LOG] Testing DeviceInformation display formatting")

        val deviceInfo = ShimmerRecorder.DeviceInformation(
            deviceId = "test_device",
            macAddress = "00:11:22:33:44:55",
            deviceName = "Shimmer3-GSR+",
            firmwareVersion = "1.0.0",
            hardwareVersion = "3.0",
            batteryLevel = 85,
            connectionState = ShimmerDevice.ConnectionState.CONNECTED,
            isStreaming = false,
            configuration = null,
            samplesRecorded = 1000L,
            lastSampleTime = System.currentTimeMillis(),
            bluetoothType = "Classic",
            signalStrength = 80,
            totalConnectedTime = 60000L
        )

        val summary = deviceInfo.getDisplaySummary()

        assertTrue("Summary should contain device name", summary.contains("Shimmer3-GSR+"))
        assertTrue("Summary should contain connection state", summary.contains("CONNECTED"))
        assertTrue("Summary should contain battery level", summary.contains("85%"))
        assertTrue("Summary should contain sample count", summary.contains("1000"))
    }

    @Test
    fun `DataQualityMetrics getDisplaySummary should format correctly`() {
        println("[DEBUG_LOG] Testing DataQualityMetrics display formatting")

        val metrics = ShimmerRecorder.DataQualityMetrics(
            deviceId = "test_device",
            samplesAnalyzed = 100,
            averageSamplingRate = 51.2,
            signalQuality = "Good",
            batteryLevel = 85,
            connectionStability = "Stable",
            dataLossPercentage = 0.5
        )

        val summary = metrics.getDisplaySummary()

        assertTrue("Summary should contain device ID", summary.contains("test_device"))
        assertTrue("Summary should contain sampling rate", summary.contains("51.2 Hz"))
        assertTrue("Summary should contain signal quality", summary.contains("Good"))
        assertTrue("Summary should contain battery level", summary.contains("85%"))
        assertTrue("Summary should contain connection stability", summary.contains("Stable"))
    }
}
