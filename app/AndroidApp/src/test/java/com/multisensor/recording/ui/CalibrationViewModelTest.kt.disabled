package com.multisensor.recording.ui

import com.multisensor.recording.TestConstants
import com.multisensor.recording.testutils.BaseUnitTest
import com.multisensor.recording.testutils.ViewModelTestUtils
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/**
 * Comprehensive unit tests for CalibrationViewModel.
 * Covers calibration workflows for camera, thermal, and Shimmer devices.
 * 
 * Test Categories:
 * - Initialization and state management
 * - Camera calibration workflow
 * - Thermal calibration workflow
 * - Shimmer calibration workflow
 * - System validation
 * - Error handling
 * - Progress tracking
 */
@ExperimentalCoroutinesApi
class CalibrationViewModelTest : BaseUnitTest() {

    @get:Rule
    val mockKRule = io.mockk.junit4.MockKRule(this)

    private lateinit var viewModel: CalibrationViewModel
    private lateinit var testScope: TestScope

    @Before
    override fun setUp() {
        super.setUp()
        testScope = ViewModelTestUtils.createTestScope(testDispatcher)
        
        viewModel = CalibrationViewModel()
    }

    @After
    override fun tearDown() {
        super.tearDown()
        testScope.cancel()
    }

    @Test
    fun `test_initial_calibration_state`() = testScope.runTest {
        // Given: ViewModel is initialized
        
        // When: Getting initial state
        val initialState = viewModel.uiState.value
        
        // Then: All calibration states should be false initially
        assertFalse("Camera should not be calibrated initially", initialState.isCameraCalibrated)
        assertFalse("Camera should not be calibrating initially", initialState.isCameraCalibrating)
        assertEquals("Camera calibration progress should be 0", 0, initialState.cameraCalibrationProgress)
        assertEquals("Camera calibration error should be 0.0", 0.0, initialState.cameraCalibrationError, 0.01)
        
        assertFalse("Thermal should not be calibrated initially", initialState.isThermalCalibrated)
        assertFalse("Thermal should not be calibrating initially", initialState.isThermalCalibrating)
        assertEquals("Thermal calibration progress should be 0", 0, initialState.thermalCalibrationProgress)
        
        assertFalse("Shimmer should not be calibrated initially", initialState.isShimmerCalibrated)
        assertFalse("Shimmer should not be calibrating initially", initialState.isShimmerCalibrating)
        assertEquals("Shimmer calibration progress should be 0", 0, initialState.shimmerCalibrationProgress)
        
        assertFalse("Should not be validating initially", initialState.isValidating)
        assertFalse("System should not be valid initially", initialState.isSystemValid)
        assertTrue("Validation errors should be empty initially", initialState.validationErrors.isEmpty())
        
        assertTrue("Should be able to start calibration initially", initialState.canStartCalibration)
        assertFalse("Nothing should be calibrating initially", initialState.isAnyCalibrating)
    }

    @Test
    fun `test_camera_calibration_start`() = testScope.runTest {
        // Given: Initial state
        val initialState = viewModel.uiState.value
        assertTrue("Can start calibration", initialState.canStartCalibration)
        
        // When: Starting camera calibration
        viewModel.startCameraCalibration()
        
        // Then: State should reflect calibration in progress
        val newState = viewModel.uiState.value
        assertTrue("Camera should be calibrating", newState.isCameraCalibrating)
        assertTrue("Should indicate any calibrating", newState.isAnyCalibrating)
        assertFalse("Cannot start new calibration while one is running", newState.canStartCalibration)
    }

    @Test
    fun `test_camera_calibration_progress_updates`() = testScope.runTest {
        // Given: Camera calibration is started
        viewModel.startCameraCalibration()
        
        // When: Updating progress
        viewModel.updateCameraCalibrationProgress(50)
        
        // Then: Progress should be updated
        val state = viewModel.uiState.value
        assertEquals("Progress should be updated", 50, state.cameraCalibrationProgress)
        assertTrue("Should still be calibrating", state.isCameraCalibrating)
    }

    @Test
    fun `test_camera_calibration_completion_success`() = testScope.runTest {
        // Given: Camera calibration is in progress
        viewModel.startCameraCalibration()
        viewModel.updateCameraCalibrationProgress(100)
        
        // When: Completing calibration successfully
        val calibrationError = 0.5
        viewModel.completeCameraCalibration(calibrationError, isSuccess = true)
        
        // Then: State should reflect successful completion
        val state = viewModel.uiState.value
        assertTrue("Camera should be calibrated", state.isCameraCalibrated)
        assertFalse("Camera should not be calibrating", state.isCameraCalibrating)
        assertEquals("Calibration error should be set", calibrationError, state.cameraCalibrationError, 0.01)
        assertTrue("Should be able to start new calibration", state.canStartCalibration)
        assertFalse("Nothing should be calibrating", state.isAnyCalibrating)
        assertNotEquals("Calibration date should be set", "", state.cameraCalibrationDate)
    }

    @Test
    fun `test_camera_calibration_completion_failure`() = testScope.runTest {
        // Given: Camera calibration is in progress
        viewModel.startCameraCalibration()
        
        // When: Completing calibration with failure
        viewModel.completeCameraCalibration(0.0, isSuccess = false)
        
        // Then: State should reflect failure
        val state = viewModel.uiState.value
        assertFalse("Camera should not be calibrated on failure", state.isCameraCalibrated)
        assertFalse("Camera should not be calibrating", state.isCameraCalibrating)
        assertTrue("Should be able to retry calibration", state.canStartCalibration)
    }

    @Test
    fun `test_thermal_calibration_workflow`() = testScope.runTest {
        // Given: Initial state
        
        // When: Starting thermal calibration
        viewModel.startThermalCalibration()
        
        // Then: Thermal calibration should be active
        var state = viewModel.uiState.value
        assertTrue("Thermal should be calibrating", state.isThermalCalibrating)
        assertTrue("Should indicate any calibrating", state.isAnyCalibrating)
        
        // When: Updating thermal progress
        viewModel.updateThermalCalibrationProgress(75)
        
        // Then: Progress should be updated
        state = viewModel.uiState.value
        assertEquals("Thermal progress should be updated", 75, state.thermalCalibrationProgress)
        
        // When: Completing thermal calibration
        val tempRange = "0-100°C"
        val emissivity = "0.95"
        val colorPalette = "Iron"
        viewModel.completeThermalCalibration(tempRange, emissivity, colorPalette, isSuccess = true)
        
        // Then: Thermal should be calibrated
        state = viewModel.uiState.value
        assertTrue("Thermal should be calibrated", state.isThermalCalibrated)
        assertFalse("Thermal should not be calibrating", state.isThermalCalibrating)
        assertEquals("Temp range should be set", tempRange, state.thermalTempRange)
        assertEquals("Emissivity should be set", emissivity, state.thermalEmissivity)
        assertEquals("Color palette should be set", colorPalette, state.thermalColorPalette)
        assertNotEquals("Thermal calibration date should be set", "", state.thermalCalibrationDate)
    }

    @Test
    fun `test_shimmer_calibration_workflow`() = testScope.runTest {
        // Given: Initial state
        
        // When: Starting Shimmer calibration
        viewModel.startShimmerCalibration()
        
        // Then: Shimmer calibration should be active
        var state = viewModel.uiState.value
        assertTrue("Shimmer should be calibrating", state.isShimmerCalibrating)
        assertTrue("Should indicate any calibrating", state.isAnyCalibrating)
        
        // When: Updating Shimmer progress
        viewModel.updateShimmerCalibrationProgress(60)
        
        // Then: Progress should be updated
        state = viewModel.uiState.value
        assertEquals("Shimmer progress should be updated", 60, state.shimmerCalibrationProgress)
        
        // When: Completing Shimmer calibration
        val macAddress = TestConstants.TEST_SHIMMER_MAC
        val sensorConfig = "GSR+,AccelWR"
        val samplingRate = "51.2Hz"
        viewModel.completeShimmerCalibration(macAddress, sensorConfig, samplingRate, isSuccess = true)
        
        // Then: Shimmer should be calibrated
        state = viewModel.uiState.value
        assertTrue("Shimmer should be calibrated", state.isShimmerCalibrated)
        assertFalse("Shimmer should not be calibrating", state.isShimmerCalibrating)
        assertEquals("MAC address should be set", macAddress, state.shimmerMacAddress)
        assertEquals("Sensor config should be set", sensorConfig, state.shimmerSensorConfig)
        assertEquals("Sampling rate should be set", samplingRate, state.shimmerSamplingRate)
        assertNotEquals("Shimmer calibration date should be set", "", state.shimmerCalibrationDate)
    }

    @Test
    fun `test_system_validation_success`() = testScope.runTest {
        // Given: All devices are calibrated
        viewModel.startCameraCalibration()
        viewModel.completeCameraCalibration(0.5, true)
        viewModel.startThermalCalibration()
        viewModel.completeThermalCalibration("0-100°C", "0.95", "Iron", true)
        viewModel.startShimmerCalibration()
        viewModel.completeShimmerCalibration(TestConstants.TEST_SHIMMER_MAC, "GSR+", "51.2Hz", true)
        
        // When: Starting system validation
        viewModel.startSystemValidation()
        
        // Then: Validation should be in progress
        var state = viewModel.uiState.value
        assertTrue("Should be validating", state.isValidating)
        assertTrue("Should indicate any calibrating", state.isAnyCalibrating)
        
        // When: Completing validation successfully
        viewModel.completeSystemValidation(isSuccess = true, errors = emptyList())
        
        // Then: System should be valid
        state = viewModel.uiState.value
        assertFalse("Should not be validating", state.isValidating)
        assertTrue("System should be valid", state.isSystemValid)
        assertTrue("Validation errors should be empty", state.validationErrors.isEmpty())
        assertFalse("Nothing should be calibrating", state.isAnyCalibrating)
    }

    @Test
    fun `test_system_validation_failure`() = testScope.runTest {
        // Given: Some devices are not calibrated
        
        // When: Starting system validation
        viewModel.startSystemValidation()
        
        // When: Completing validation with errors
        val errors = listOf("Camera not calibrated", "Thermal device not found")
        viewModel.completeSystemValidation(isSuccess = false, errors = errors)
        
        // Then: System should not be valid
        val state = viewModel.uiState.value
        assertFalse("Should not be validating", state.isValidating)
        assertFalse("System should not be valid", state.isSystemValid)
        assertEquals("Validation errors should match", errors, state.validationErrors)
    }

    @Test
    fun `test_reset_calibration`() = testScope.runTest {
        // Given: Some calibrations are completed
        viewModel.startCameraCalibration()
        viewModel.completeCameraCalibration(0.5, true)
        viewModel.completeSystemValidation(true, emptyList())
        
        // When: Resetting calibration
        viewModel.resetCalibration()
        
        // Then: All calibration states should be reset
        val state = viewModel.uiState.value
        assertFalse("Camera should not be calibrated", state.isCameraCalibrated)
        assertFalse("Thermal should not be calibrated", state.isThermalCalibrated)
        assertFalse("Shimmer should not be calibrated", state.isShimmerCalibrated)
        assertFalse("System should not be valid", state.isSystemValid)
        assertTrue("Validation errors should be empty", state.validationErrors.isEmpty())
        assertTrue("Should be able to start calibration", state.canStartCalibration)
    }

    @Test
    fun `test_concurrent_calibration_prevention`() = testScope.runTest {
        // Given: Camera calibration is in progress
        viewModel.startCameraCalibration()
        val stateAfterStart = viewModel.uiState.value
        assertTrue("Camera should be calibrating", stateAfterStart.isCameraCalibrating)
        assertFalse("Cannot start new calibration", stateAfterStart.canStartCalibration)
        
        // When: Trying to start thermal calibration
        viewModel.startThermalCalibration()
        
        // Then: Thermal calibration should not start
        val finalState = viewModel.uiState.value
        assertTrue("Camera should still be calibrating", finalState.isCameraCalibrating)
        assertFalse("Thermal should not be calibrating", finalState.isThermalCalibrating)
    }

    @Test
    fun `test_calibration_error_handling`() = testScope.runTest {
        // Given: Calibration is in progress
        viewModel.startCameraCalibration()
        
        // When: An error occurs during calibration
        viewModel.handleCalibrationError("Camera connection lost")
        
        // Then: Calibration should be stopped and error handled
        val state = viewModel.uiState.value
        assertFalse("Camera should not be calibrating after error", state.isCameraCalibrating)
        assertTrue("Should be able to retry", state.canStartCalibration)
    }

    @Test
    fun `test_isAnyCalibrating_computed_property`() = testScope.runTest {
        // Given: Initial state
        var state = viewModel.uiState.value
        assertFalse("Nothing should be calibrating initially", state.isAnyCalibrating)
        
        // When: Starting camera calibration
        viewModel.startCameraCalibration()
        state = viewModel.uiState.value
        assertTrue("Should indicate calibrating when camera calibrating", state.isAnyCalibrating)
        
        // When: Starting validation after camera completion
        viewModel.completeCameraCalibration(0.5, true)
        viewModel.startSystemValidation()
        state = viewModel.uiState.value
        assertTrue("Should indicate calibrating when validating", state.isAnyCalibrating)
        
        // When: Completing validation
        viewModel.completeSystemValidation(true, emptyList())
        state = viewModel.uiState.value
        assertFalse("Should not indicate calibrating when done", state.isAnyCalibrating)
    }

    @Test
    fun `test_calibration_date_formatting`() = testScope.runTest {
        // Given: Starting and completing calibration
        viewModel.startCameraCalibration()
        viewModel.completeCameraCalibration(0.5, true)
        
        // When: Getting calibration date
        val state = viewModel.uiState.value
        
        // Then: Date should be in proper format
        assertNotEquals("Date should not be empty", "", state.cameraCalibrationDate)
        assertTrue("Date should contain valid format", 
            state.cameraCalibrationDate.isNotEmpty() && state.cameraCalibrationDate.length > 8)
    }
}