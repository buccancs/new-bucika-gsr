package com.multisensor.recording.calibration

import android.content.Context
import android.graphics.Bitmap
import com.multisensor.recording.recording.CameraRecorder
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.sync.SyncClockManager
import com.multisensor.recording.recording.thermal.ThermalCameraSettings
import com.multisensor.recording.util.Logger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import java.io.File

@Config(sdk = [28])
@ExperimentalCoroutinesApi
class CalibrationCaptureManagerComprehensiveTest {

    private lateinit var mockContext: Context
    private lateinit var mockCameraRecorder: CameraRecorder
    private lateinit var mockThermalRecorder: ThermalRecorder
    private lateinit var mockSyncClockManager: SyncClockManager
    private lateinit var mockThermalSettings: ThermalCameraSettings
    private lateinit var mockLogger: Logger
    private lateinit var calibrationManager: CalibrationCaptureManager

    @BeforeEach
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockCameraRecorder = mockk(relaxed = true)
        mockThermalRecorder = mockk(relaxed = true)
        mockSyncClockManager = mockk(relaxed = true)
        mockThermalSettings = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        calibrationManager = CalibrationCaptureManager(
            mockContext,
            mockCameraRecorder,
            mockThermalRecorder,
            mockSyncClockManager,
            mockThermalSettings,
            mockLogger
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `calibration manager should be created successfully`() = runTest {
        assertNotNull("CalibrationCaptureManager should be created", calibrationManager)
    }

    @Test
    fun `getCalibrationSessions should return empty list initially`() = runTest {
        val sessions = calibrationManager.getCalibrationSessions()
        assertTrue("Initial sessions list should be empty", sessions.isEmpty())
    }

    @Test
    fun `getCalibrationStatistics should return valid statistics`() = runTest {
        val stats = calibrationManager.getCalibrationStatistics()
        assertNotNull("Statistics should not be null", stats)
        assertEquals("Initial session count should be 0", 0, stats.totalSessions)
    }

}
