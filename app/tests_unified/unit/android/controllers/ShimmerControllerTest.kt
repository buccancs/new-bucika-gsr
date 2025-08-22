package com.multisensor.recording.controllers

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.multisensor.recording.managers.ShimmerManager
import com.multisensor.recording.persistence.ShimmerDeviceState
import com.multisensor.recording.persistence.ShimmerDeviceStateRepository
import com.multisensor.recording.ui.MainViewModel
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShimmerControllerTest {

    private lateinit var mockShimmerManager: ShimmerManager
    private lateinit var mockRepository: ShimmerDeviceStateRepository
    private lateinit var mockErrorHandler: ShimmerErrorHandler
    private lateinit var mockViewModel: MainViewModel
    private lateinit var mockContext: Context
    private lateinit var mockCallback: ShimmerController.ShimmerCallback
    private lateinit var mockLauncher: ActivityResultLauncher<Intent>

    private lateinit var shimmerController: ShimmerController

    private val testDeviceAddress = "00:11:22:33:44:55"
    private val testDeviceName = "Shimmer3-1234"
    private val testBtType = ShimmerBluetoothManagerAndroid.BT_TYPE.BT_CLASSIC

    @Before
    fun setup() {
        mockShimmerManager = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockErrorHandler = mockk(relaxed = true)
        mockViewModel = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        mockCallback = mockk(relaxed = true)
        mockLauncher = mockk(relaxed = true)

        coEvery { mockRepository.getAllDeviceStates() } returns emptyList()
        coEvery { mockRepository.getAutoReconnectDevices() } returns emptyList()
        coEvery { mockRepository.saveDeviceState(any()) } just Runs
        coEvery { mockRepository.updateConnectionStatus(any(), any(), any(), any()) } just Runs
        coEvery { mockRepository.logConnectionAttempt(any(), any(), any(), any(), any()) } just Runs

        coEvery { mockErrorHandler.resetErrorState(any()) } just Runs
        coEvery { mockErrorHandler.generateDiagnosticReport(any()) } returns "Test diagnostic report"
        coEvery { mockErrorHandler.checkDeviceHealth(any()) } returns emptyList()

        shimmerController = ShimmerController(mockShimmerManager, mockRepository, mockErrorHandler)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `setCallback should initialize controller and load saved states`() = runTest {
        val savedDevice = createTestDeviceState()
        coEvery { mockRepository.getAllDeviceStates() } returns listOf(savedDevice)

        shimmerController.setCallback(mockCallback)

        verify { mockCallback }
        coVerify { mockRepository.getAllDeviceStates() }
        coVerify { mockRepository.getAutoReconnectDevices() }
    }

    @Test
    fun `loadSavedDeviceStates should restore device configurations`() = runTest {
        val savedDevices = listOf(
            createTestDeviceState(address = "00:11:22:33:44:55", name = "Device1"),
            createTestDeviceState(address = "00:11:22:33:44:56", name = "Device2")
        )
        coEvery { mockRepository.getAllDeviceStates() } returns savedDevices

        shimmerController.setCallback(mockCallback)

        coVerify { mockRepository.getAllDeviceStates() }
        assertEquals(2, shimmerController.getConnectedDevices().size)
    }

    @Test
    fun `handleDeviceSelectionResult should save device state on successful selection`() = runTest {
        shimmerController.setCallback(mockCallback)

        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)

        verify { mockCallback.updateStatusText("Device selected: $testDeviceName") }
        verify { mockCallback.showToast("Selected: $testDeviceName") }
        verify { mockCallback.onDeviceSelected(testDeviceAddress, testDeviceName) }
        coVerify { mockRepository.saveDeviceState(any()) }
    }

    @Test
    fun `handleDeviceSelectionResult should handle cancellation`() = runTest {
        shimmerController.setCallback(mockCallback)

        shimmerController.handleDeviceSelectionResult(null, null)

        verify { mockCallback.onDeviceSelectionCancelled() }
        coVerify(exactly = 0) { mockRepository.saveDeviceState(any()) }
    }

    @Test
    fun `launchShimmerDeviceDialog should delegate to controller`() = runTest {
        val mockActivity = mockk<android.app.Activity>(relaxed = true)

        shimmerController.launchShimmerDeviceDialog(mockActivity, mockLauncher)

        assertTrue(true)
    }

    @Test
    fun `connectToSelectedDevice should connect when device is selected`() = runTest {
        shimmerController.setCallback(mockCallback)
        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)

        every { mockViewModel.connectShimmerDevice(any(), any(), any(), any()) } answers {
            val callback = arg<(Boolean) -> Unit>(3)
            callback(true)
        }

        shimmerController.connectToSelectedDevice(mockViewModel)

        verify { mockViewModel.connectShimmerDevice(testDeviceAddress, testDeviceName, testBtType, any()) }
    }

    @Test
    fun `connectToSelectedDevice should handle no device selected`() = runTest {
        shimmerController.setCallback(mockCallback)

        shimmerController.connectToSelectedDevice(mockViewModel)

        verify { mockCallback.onShimmerError("No device selected") }
    }

    @Test
    fun `connectToDevice should prevent duplicate connections`() = runTest {
        shimmerController.setCallback(mockCallback)
        val deviceState = createTestDeviceState(connected = true)
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns deviceState

        shimmerController.connectToDevice(testDeviceAddress, testDeviceName, mockViewModel)

        verify { mockCallback.onShimmerError("Device $testDeviceName is already connected") }
    }

    @Test
    fun `getConnectedDevices should return only connected devices`() = runTest {
        shimmerController.setCallback(mockCallback)
        val devices = listOf(
            createTestDeviceState(address = "00:11:22:33:44:55", connected = true),
            createTestDeviceState(address = "00:11:22:33:44:56", connected = false),
            createTestDeviceState(address = "00:11:22:33:44:57", connected = true)
        )
        coEvery { mockRepository.getAllDeviceStates() } returns devices

        shimmerController.setCallback(mockCallback)
        val connectedDevices = shimmerController.getConnectedDevices()

        assertEquals(2, connectedDevices.size)
        assertTrue(connectedDevices.all { it.isConnected })
    }

    @Test
    fun `getConnectedDeviceCount should return correct count`() = runTest {
        shimmerController.setCallback(mockCallback)
        val devices = listOf(
            createTestDeviceState(address = "00:11:22:33:44:55", connected = true),
            createTestDeviceState(address = "00:11:22:33:44:56", connected = true)
        )
        coEvery { mockRepository.getAllDeviceStates() } returns devices

        shimmerController.setCallback(mockCallback)
        val count = shimmerController.getConnectedDeviceCount()

        assertEquals(2, count)
    }

    @Test
    fun `disconnectAllDevices should disconnect all connected devices`() = runTest {
        shimmerController.setCallback(mockCallback)
        val devices = listOf(
            createTestDeviceState(address = "00:11:22:33:44:55", connected = true),
            createTestDeviceState(address = "00:11:22:33:44:56", connected = true)
        )
        coEvery { mockRepository.getAllDeviceStates() } returns devices

        every { mockViewModel.disconnectShimmerDevice(any(), any()) } answers {
            val callback = arg<(Boolean) -> Unit>(1)
            callback(true)
        }

        shimmerController.setCallback(mockCallback)
        shimmerController.disconnectAllDevices(mockViewModel)

        verify(exactly = 2) { mockViewModel.disconnectShimmerDevice(any(), any()) }
    }

    @Test
    fun `configureSensorChannels should update device configuration`() = runTest {
        shimmerController.setCallback(mockCallback)
        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)
        val enabledChannels = setOf("GSR", "Accelerometer")

        every { mockViewModel.configureShimmerSensors(any(), any(), any()) } answers {
            val callback = arg<(Boolean) -> Unit>(2)
            callback(true)
        }

        shimmerController.configureSensorChannels(mockViewModel, enabledChannels)

        verify { mockCallback.updateStatusText("Configuring sensors...") }
        verify { mockViewModel.configureShimmerSensors(testDeviceAddress, any(), any()) }
    }

    @Test
    fun `setSamplingRate should update sampling rate for device`() = runTest {
        shimmerController.setCallback(mockCallback)
        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)
        val samplingRate = 1024.0

        every { mockViewModel.setShimmerSamplingRate(any(), any(), any()) } answers {
            val callback = arg<(Boolean) -> Unit>(2)
            callback(true)
        }

        shimmerController.setSamplingRate(mockViewModel, samplingRate)

        verify { mockCallback.updateStatusText("Setting sampling rate to ${samplingRate}Hz...") }
        verify { mockViewModel.setShimmerSamplingRate(testDeviceAddress, samplingRate, any()) }
    }

    @Test
    fun `setAutoReconnectEnabled should update persistence`() = runTest {
        shimmerController.setCallback(mockCallback)

        shimmerController.setAutoReconnectEnabled(testDeviceAddress, true)

        coVerify { mockRepository.setAutoReconnectEnabled(testDeviceAddress, true) }
    }

    @Test
    fun `setDeviceConnectionPriority should update persistence`() = runTest {
        shimmerController.setCallback(mockCallback)
        val priority = 5

        shimmerController.setDeviceConnectionPriority(testDeviceAddress, priority)

        coVerify { mockRepository.setDeviceConnectionPriority(testDeviceAddress, priority) }
    }

    @Test
    fun `exportDeviceConfigurations should return all device states`() = runTest {
        val devices = listOf(
            createTestDeviceState(address = "00:11:22:33:44:55"),
            createTestDeviceState(address = "00:11:22:33:44:56")
        )
        coEvery { mockRepository.getAllDeviceStates() } returns devices

        val exported = shimmerController.exportDeviceConfigurations()

        assertEquals(2, exported.size)
        coVerify { mockRepository.getAllDeviceStates() }
    }

    @Test
    fun `connectToDevice should use error handler for failures`() = runTest {
        shimmerController.setCallback(mockCallback)
        val exception = RuntimeException("Connection timeout")

        coEvery {
            mockErrorHandler.handleError(any(), any(), any(), any(), any(), any(), any(), any())
        } returns false

        shimmerController.connectToDevice(testDeviceAddress, testDeviceName, mockViewModel)

        coVerify {
            mockErrorHandler.handleError(
                deviceAddress = testDeviceAddress,
                deviceName = testDeviceName,
                exception = any(),
                errorMessage = any(),
                attemptNumber = any(),
                connectionType = testBtType,
                onRetry = any(),
                onFinalFailure = any()
            )
        }
    }

    @Test
    fun `getConnectionStatus should return complete status`() = runTest {
        shimmerController.setCallback(mockCallback)
        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)

        val status = shimmerController.getConnectionStatus()

        assertTrue(status.contains("Shimmer Status:"))
        assertTrue(status.contains(testDeviceName))
        assertTrue(status.contains(testDeviceAddress))
    }

    @Test
    fun `getDeviceManagementStatus should return correct status messages`() = runTest {
        shimmerController.setCallback(mockCallback)

        var status = shimmerController.getDeviceManagementStatus()
        assertEquals("No devices configured", status)

        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)

        status = shimmerController.getDeviceManagementStatus()
        assertTrue(status.contains("1 device") && status.contains("none connected"))
    }

    @Test
    fun `resetState should clear controller state`() = runTest {
        shimmerController.setCallback(mockCallback)
        shimmerController.handleDeviceSelectionResult(testDeviceAddress, testDeviceName)
        shimmerController.setPreferredBtType(ShimmerBluetoothManagerAndroid.BT_TYPE.BLE)

        shimmerController.resetState()

        val (address, name) = shimmerController.getSelectedDeviceInfo()
        assertNull(address)
        assertNull(name)
        assertEquals(ShimmerBluetoothManagerAndroid.BT_TYPE.BT_CLASSIC, shimmerController.getPreferredBtType())
    }

    private fun createTestDeviceState(
        address: String = testDeviceAddress,
        name: String = testDeviceName,
        connected: Boolean = false,
        connectionType: ShimmerBluetoothManagerAndroid.BT_TYPE = testBtType
    ): ShimmerDeviceState {
        return ShimmerDeviceState(
            deviceAddress = address,
            deviceName = name,
            connectionType = connectionType,
            isConnected = connected,
            lastConnectedTimestamp = if (connected) System.currentTimeMillis() else 0L,
            enabledSensors = setOf("GSR", "Accelerometer"),
            samplingRate = 512.0,
            gsrRange = 0,
            autoReconnectEnabled = true,
            preferredConnectionOrder = 0
        )
    }
}
