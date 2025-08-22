package com.multisensor.recording.controllers

import com.multisensor.recording.persistence.ConnectionAction
import com.multisensor.recording.persistence.ShimmerConnectionHistory
import com.multisensor.recording.persistence.ShimmerDeviceState
import com.multisensor.recording.persistence.ShimmerDeviceStateRepository
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
class ShimmerErrorHandlerTest {

    private lateinit var mockRepository: ShimmerDeviceStateRepository
    private lateinit var shimmerErrorHandler: ShimmerErrorHandler

    private val testDeviceAddress = "00:11:22:33:44:55"
    private val testDeviceName = "Shimmer3-1234"
    private val testConnectionType = ShimmerBluetoothManagerAndroid.BT_TYPE.BT_CLASSIC

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        shimmerErrorHandler = ShimmerErrorHandler(mockRepository)

        coEvery { mockRepository.logConnectionAttempt(any(), any(), any(), any(), any()) } just Runs
        coEvery { mockRepository.getDeviceState(any()) } returns createTestDeviceState()
        coEvery { mockRepository.getConnectionHistory(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `classifyError should identify connection timeout`() {
        val errorMessage = "Connection timed out after 30 seconds"

        val errorType = shimmerErrorHandler.classifyError(null, errorMessage)

        assertEquals(ShimmerErrorHandler.ShimmerErrorType.CONNECTION_TIMEOUT, errorType)
    }

    @Test
    fun `classifyError should identify bluetooth disabled`() {
        val errorMessage = "Bluetooth is disabled on this device"

        val errorType = shimmerErrorHandler.classifyError(null, errorMessage)

        assertEquals(ShimmerErrorHandler.ShimmerErrorType.BLUETOOTH_DISABLED, errorType)
    }

    @Test
    fun `classifyError should identify permission denied`() {
        val errorMessage = "Bluetooth permission denied by user"

        val errorType = shimmerErrorHandler.classifyError(null, errorMessage)

        assertEquals(ShimmerErrorHandler.ShimmerErrorType.BLUETOOTH_PERMISSION_DENIED, errorType)
    }

    @Test
    fun `classifyError should identify device not found`() {
        val errorMessage = "Device not found in scan results"

        val errorType = shimmerErrorHandler.classifyError(null, errorMessage)

        assertEquals(ShimmerErrorHandler.ShimmerErrorType.DEVICE_NOT_FOUND, errorType)
    }

    @Test
    fun `classifyError should identify unknown error`() {
        val errorMessage = "Some unexpected error occurred"

        val errorType = shimmerErrorHandler.classifyError(null, errorMessage)

        assertEquals(ShimmerErrorHandler.ShimmerErrorType.UNKNOWN_ERROR, errorType)
    }

    @Test
    fun `classifyError should handle null messages`() {
        val errorType = shimmerErrorHandler.classifyError(null, null)

        assertEquals(ShimmerErrorHandler.ShimmerErrorType.UNKNOWN_ERROR, errorType)
    }

    @Test
    fun `getErrorHandlingStrategy should return retry strategy for connection timeout`() {
        val errorType = ShimmerErrorHandler.ShimmerErrorType.CONNECTION_TIMEOUT

        val strategy = shimmerErrorHandler.getErrorHandlingStrategy(errorType, 1)

        assertTrue(strategy.shouldRetry)
        assertEquals(3, strategy.maxRetries)
        assertFalse(strategy.userActionRequired)
        assertTrue(strategy.retryDelay > 0)
    }

    @Test
    fun `getErrorHandlingStrategy should return no retry for bluetooth disabled`() {
        val errorType = ShimmerErrorHandler.ShimmerErrorType.BLUETOOTH_DISABLED

        val strategy = shimmerErrorHandler.getErrorHandlingStrategy(errorType, 1)

        assertFalse(strategy.shouldRetry)
        assertEquals(0, strategy.maxRetries)
        assertTrue(strategy.userActionRequired)
        assertEquals(0L, strategy.retryDelay)
    }

    @Test
    fun `getErrorHandlingStrategy should increase delay with attempt number`() {
        val errorType = ShimmerErrorHandler.ShimmerErrorType.CONNECTION_TIMEOUT

        val strategy1 = shimmerErrorHandler.getErrorHandlingStrategy(errorType, 1)
        val strategy2 = shimmerErrorHandler.getErrorHandlingStrategy(errorType, 2)

        assertTrue(strategy2.retryDelay > strategy1.retryDelay)
    }

    @Test
    fun `getErrorHandlingStrategy should stop retrying after max attempts`() {
        val errorType = ShimmerErrorHandler.ShimmerErrorType.CONNECTION_TIMEOUT

        val strategy = shimmerErrorHandler.getErrorHandlingStrategy(errorType, 5)

        assertTrue(strategy.shouldRetry)
    }

    @Test
    fun `handleError should attempt retry for retryable errors`() = runTest {
        var retryAttempted = false
        val onRetry: suspend () -> Boolean = {
            retryAttempted = true
            true
        }
        var finalFailureCalled = false
        val onFinalFailure: (ShimmerErrorHandler.ErrorHandlingStrategy) -> Unit = {
            finalFailureCalled = true
        }

        val result = shimmerErrorHandler.handleError(
            deviceAddress = testDeviceAddress,
            deviceName = testDeviceName,
            exception = RuntimeException("Connection timed out"),
            errorMessage = null,
            attemptNumber = 1,
            connectionType = testConnectionType,
            onRetry = onRetry,
            onFinalFailure = onFinalFailure
        )

        assertTrue(result)
        assertTrue(retryAttempted)
        assertFalse(finalFailureCalled)
        coVerify {
            mockRepository.logConnectionAttempt(
                testDeviceAddress,
                false,
                any(),
                testDeviceName,
                testConnectionType
            )
        }
    }

    @Test
    fun `handleError should call final failure for non-retryable errors`() = runTest {
        var retryAttempted = false
        val onRetry: suspend () -> Boolean = {
            retryAttempted = true
            true
        }
        var finalFailureCalled = false
        val onFinalFailure: (ShimmerErrorHandler.ErrorHandlingStrategy) -> Unit = {
            finalFailureCalled = true
        }

        val result = shimmerErrorHandler.handleError(
            deviceAddress = testDeviceAddress,
            deviceName = testDeviceName,
            exception = RuntimeException("Bluetooth is disabled"),
            errorMessage = null,
            attemptNumber = 1,
            connectionType = testConnectionType,
            onRetry = onRetry,
            onFinalFailure = onFinalFailure
        )

        assertFalse(result)
        assertFalse(retryAttempted)
        assertTrue(finalFailureCalled)
    }

    @Test
    fun `handleError should call final failure after max retries`() = runTest {
        var retryAttempted = false
        val onRetry: suspend () -> Boolean = {
            retryAttempted = true
            false
        }
        var finalFailureCalled = false
        val onFinalFailure: (ShimmerErrorHandler.ErrorHandlingStrategy) -> Unit = {
            finalFailureCalled = true
        }

        val result = shimmerErrorHandler.handleError(
            deviceAddress = testDeviceAddress,
            deviceName = testDeviceName,
            exception = RuntimeException("Connection timed out"),
            errorMessage = null,
            attemptNumber = 4,
            connectionType = testConnectionType,
            onRetry = onRetry,
            onFinalFailure = onFinalFailure
        )

        assertFalse(result)
        assertFalse(retryAttempted)
        assertTrue(finalFailureCalled)
    }

    @Test
    fun `checkDeviceHealth should identify low battery`() = runTest {
        val deviceState = createTestDeviceState(batteryLevel = 15)
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns deviceState

        val recommendations = shimmerErrorHandler.checkDeviceHealth(testDeviceAddress)

        assertTrue(recommendations.any { it.contains("Battery level is low") })
    }

    @Test
    fun `checkDeviceHealth should identify weak signal`() = runTest {
        val deviceState = createTestDeviceState(signalStrength = -85)
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns deviceState

        val recommendations = shimmerErrorHandler.checkDeviceHealth(testDeviceAddress)

        assertTrue(recommendations.any { it.contains("Signal strength is weak") })
    }

    @Test
    fun `checkDeviceHealth should identify connection failures`() = runTest {
        val failedHistory = listOf(
            createFailedConnectionHistory(),
            createFailedConnectionHistory(),
            createFailedConnectionHistory(),
            createFailedConnectionHistory()
        )
        coEvery { mockRepository.getConnectionHistory(testDeviceAddress, 10) } returns failedHistory

        val recommendations = shimmerErrorHandler.checkDeviceHealth(testDeviceAddress)

        assertTrue(recommendations.any { it.contains("Multiple recent connection failures") })
    }

    @Test
    fun `checkDeviceHealth should identify old last connection`() = runTest {
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000)
        val deviceState = createTestDeviceState(lastConnectedTimestamp = oldTimestamp)
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns deviceState

        val recommendations = shimmerErrorHandler.checkDeviceHealth(testDeviceAddress)

        assertTrue(recommendations.any { it.contains("hasn't been connected recently") })
    }

    @Test
    fun `checkDeviceHealth should return empty for healthy device`() = runTest {
        val healthyDevice = createTestDeviceState(
            batteryLevel = 80,
            signalStrength = -60,
            lastConnectedTimestamp = System.currentTimeMillis() - (1 * 60 * 60 * 1000)
        )
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns healthyDevice
        coEvery { mockRepository.getConnectionHistory(testDeviceAddress, 10) } returns listOf(
            createSuccessfulConnectionHistory()
        )

        val recommendations = shimmerErrorHandler.checkDeviceHealth(testDeviceAddress)

        assertTrue(recommendations.isEmpty())
    }

    @Test
    fun `generateDiagnosticReport should include device information`() = runTest {
        val deviceState = createTestDeviceState()
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns deviceState

        val report = shimmerErrorHandler.generateDiagnosticReport(testDeviceAddress)

        assertTrue(report.contains("=== Shimmer Device Diagnostic Report ==="))
        assertTrue(report.contains(testDeviceName))
        assertTrue(report.contains(testDeviceAddress))
        assertTrue(report.contains("=== Device State ==="))
    }

    @Test
    fun `generateDiagnosticReport should include connection history`() = runTest {
        val history = listOf(createSuccessfulConnectionHistory(), createFailedConnectionHistory())
        coEvery { mockRepository.getConnectionHistory(testDeviceAddress, 20) } returns history

        val report = shimmerErrorHandler.generateDiagnosticReport(testDeviceAddress)

        assertTrue(report.contains("=== Recent Connection History ==="))
        assertTrue(report.contains("SUCCESS"))
        assertTrue(report.contains("FAILED"))
    }

    @Test
    fun `generateDiagnosticReport should include health recommendations`() = runTest {
        val deviceState = createTestDeviceState(batteryLevel = 10)
        coEvery { mockRepository.getDeviceState(testDeviceAddress) } returns deviceState

        val report = shimmerErrorHandler.generateDiagnosticReport(testDeviceAddress)

        assertTrue(report.contains("=== Health Recommendations ==="))
        assertTrue(report.contains("Battery level is low"))
    }

    @Test
    fun `resetErrorState should clear error state`() = runTest {
        shimmerErrorHandler.resetErrorState(testDeviceAddress)

        coVerify { mockRepository.getDeviceState(testDeviceAddress) }
        coVerify { mockRepository.saveDeviceState(any()) }
    }

    private fun createTestDeviceState(
        batteryLevel: Int = 50,
        signalStrength: Int = -70,
        lastConnectedTimestamp: Long = System.currentTimeMillis()
    ): ShimmerDeviceState {
        return ShimmerDeviceState(
            deviceAddress = testDeviceAddress,
            deviceName = testDeviceName,
            connectionType = testConnectionType,
            isConnected = false,
            lastConnectedTimestamp = lastConnectedTimestamp,
            batteryLevel = batteryLevel,
            signalStrength = signalStrength,
            autoReconnectEnabled = true
        )
    }

    private fun createSuccessfulConnectionHistory(): ShimmerConnectionHistory {
        return ShimmerConnectionHistory(
            deviceAddress = testDeviceAddress,
            deviceName = testDeviceName,
            connectionType = testConnectionType,
            action = ConnectionAction.CONNECT_SUCCESS,
            success = true,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createFailedConnectionHistory(): ShimmerConnectionHistory {
        return ShimmerConnectionHistory(
            deviceAddress = testDeviceAddress,
            deviceName = testDeviceName,
            connectionType = testConnectionType,
            action = ConnectionAction.CONNECT_FAILED,
            success = false,
            errorMessage = "Connection timeout",
            timestamp = System.currentTimeMillis()
        )
    }
}
