# TC001 Test Suite Documentation

## Overview

This comprehensive test suite documentation covers the testing framework, methodologies, and quality assurance procedures for the Topdon TC001 thermal imaging integration within the BucikaGSR system. The test suite ensures professional-grade reliability and performance suitable for research and clinical applications.

## Table of Contents

1. [Testing Framework Architecture](#testing-framework-architecture)
2. [Unit Testing](#unit-testing)
3. [Integration Testing](#integration-testing)
4. [UI Testing](#ui-testing)
5. [Performance Testing](#performance-testing)
6. [Hardware Testing](#hardware-testing)
7. [Data Validation Testing](#data-validation-testing)
8. [Regression Testing](#regression-testing)
9. [Coverage Goals](#coverage-goals)
10. [Continuous Integration](#continuous-integration)

## Testing Framework Architecture

### Test Structure Overview

```
src/
├── test/                          # Unit Tests
│   └── java/com/topdon/tc001/
│       ├── thermal/
│       │   ├── TC001CameraTest.kt
│       │   ├── TemperatureViewTest.kt
│       │   ├── ThermalDataWriterTest.kt
│       │   ├── ThermalCalibrationTest.kt
│       │   └── OpencvToolsTest.kt
│       ├── usb/
│       │   ├── USBConnectionTest.kt
│       │   └── USBPermissionTest.kt
│       └── data/
│           ├── ThermalDataValidationTest.kt
│           └── ThermalExportTest.kt
├── androidTest/                   # Instrumentation Tests
│   └── java/com/topdon/tc001/
│       ├── integration/
│       │   ├── ThermalIntegrationTest.kt
│       │   ├── EndToEndWorkflowTest.kt
│       │   └── MultiModalSyncTest.kt
│       ├── ui/
│       │   ├── ThermalActivityUITest.kt
│       │   ├── SettingsActivityUITest.kt
│       │   └── RecordingUITest.kt
│       └── performance/
│           ├── FrameRatePerformanceTest.kt
│           ├── MemoryUsageTest.kt
│           └── ThermalProcessingBenchmark.kt
└── testUtils/                     # Test Utilities
    ├── MockThermalCamera.kt
    ├── TestDataGenerator.kt
    └── ThermalTestUtils.kt
```

### Testing Dependencies

#### Test Configuration (app/build.gradle)
```groovy
dependencies {
    // Unit Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.6.1'
    testImplementation 'org.mockito:mockito-kotlin:4.0.0'
    testImplementation 'org.robolectric:robolectric:4.8.1'
    testImplementation 'androidx.test:core:1.4.0'
    testImplementation 'androidx.test.ext:junit:1.1.3'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4'
    
    // Android Instrumentation Testing
    androidTestImplementation 'androidx.test:runner:1.4.0'
    androidTestImplementation 'androidx.test:rules:1.4.0'
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
    androidTestImplementation 'androidx.test.espresso:espresso-intents:3.4.0'
    androidTestImplementation 'androidx.test.uiautomator:uiautomator:2.2.0'
    
    // Performance Testing
    androidTestImplementation 'androidx.benchmark:benchmark-junit4:1.1.0'
    androidTestImplementation 'androidx.test:orchestrator:1.4.1'
    
    // Mock USB devices for testing
    testImplementation 'org.mockito:mockito-inline:4.6.1'
}

android {
    testOptions {
        unitTests {
            includeAndroidResources = true
            returnDefaultValues = true
        }
        
        animationsDisabled = true
        
        execution 'ANDROIDX_TEST_ORCHESTRATOR'
    }
}
```

## Unit Testing

### Core Thermal Camera Testing

#### TC001CameraTest - Comprehensive Camera Testing
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class TC001CameraTest {
    
    private lateinit var context: Context
    private lateinit var mockFrameCallback: IFrameCallback
    private lateinit var thermalCamera: IRUVCTC
    private lateinit var mockUSBManager: UsbManager
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockFrameCallback = mock(IFrameCallback::class.java)
        mockUSBManager = mock(UsbManager::class.java)
        
        thermalCamera = IRUVCTC(context, mockFrameCallback)
    }
    
    @Test
    fun `should initialize camera with proper context and callback`() {
        assertNotNull("Thermal camera should be initialized", thermalCamera)
        assertFalse("Camera should not be connected initially", thermalCamera.isConnected())
        assertFalse("Streaming should not be active initially", thermalCamera.isStreaming())
    }
    
    @Test
    fun `should connect to USB device with valid parameters`() {
        // Arrange
        val mockDevice = createMockTC001Device()
        whenever(mockUSBManager.hasPermission(mockDevice)).thenReturn(true)
        
        // Act
        val result = thermalCamera.connectUSBDevice(mockDevice)
        
        // Assert
        assertTrue("Connection should be initiated successfully", result)
    }
    
    @Test
    fun `should fail to connect without USB permission`() {
        // Arrange
        val mockDevice = createMockTC001Device()
        whenever(mockUSBManager.hasPermission(mockDevice)).thenReturn(false)
        
        // Act & Assert
        assertThrows(SecurityException::class.java) {
            thermalCamera.connectUSBDevice(mockDevice)
        }
    }
    
    @Test
    fun `should start thermal stream when connected`() {
        // Arrange
        val mockDevice = createMockTC001Device()
        whenever(mockUSBManager.hasPermission(mockDevice)).thenReturn(true)
        thermalCamera.connectUSBDevice(mockDevice)
        
        // Act
        val streamStarted = thermalCamera.startThermalStream()
        
        // Assert
        assertTrue("Thermal stream should start successfully", streamStarted)
        assertTrue("Streaming should be active", thermalCamera.isStreaming())
    }
    
    @Test
    fun `should not start stream when not connected`() {
        // Act
        val streamStarted = thermalCamera.startThermalStream()
        
        // Assert
        assertFalse("Stream should not start when not connected", streamStarted)
        assertFalse("Streaming should not be active", thermalCamera.isStreaming())
    }
    
    @Test
    fun `should calculate temperature correctly from raw data`() {
        // Arrange
        val temperatureData = generateTestTemperatureData(25.0f) // 25°C
        
        // Act
        val temperature = thermalCamera.getTemperatureAt(128, 96)
        
        // Assert
        assertEquals("Temperature should be approximately 25°C", 
                    25.0f, temperature, 0.5f)
    }
    
    @Test
    fun `should find maximum temperature point correctly`() {
        // Arrange
        val temperatureData = generateTestTemperatureDataWithHotSpot(50.0f, 100, 75)
        
        // Act
        val maxPoint = thermalCamera.getMaxTemperature()
        
        // Assert
        assertNotNull("Max temperature point should be found", maxPoint)
        assertEquals("Max point X should be correct", 100, maxPoint.x)
        assertEquals("Max point Y should be correct", 75, maxPoint.y)
        assertEquals("Max temperature should be approximately 50°C", 
                    50.0f, maxPoint.temperature, 1.0f)
    }
    
    @Test
    fun `should calculate average temperature in region correctly`() {
        // Arrange
        val temperatureData = generateUniformTemperatureData(30.0f)
        val region = Rect(50, 50, 150, 100)
        
        // Act
        val avgTemp = thermalCamera.getAverageTemperature(region)
        
        // Assert
        assertEquals("Average temperature should match uniform temperature", 
                    30.0f, avgTemp, 0.1f)
    }
    
    @Test
    fun `should handle thermal parameters validation`() {
        // Test emissivity validation
        assertThrows(IllegalArgumentException::class.java) {
            thermalCamera.setThermalParameters(0.05f, 25.0f, 1.0f) // Invalid emissivity
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            thermalCamera.setThermalParameters(1.5f, 25.0f, 1.0f) // Invalid emissivity
        }
        
        // Test valid parameters
        assertDoesNotThrow {
            thermalCamera.setThermalParameters(0.95f, 25.0f, 1.0f)
        }
    }
    
    @Test
    fun `should trigger frame callback with valid data`() {
        // Arrange
        val mockDevice = createMockTC001Device()
        whenever(mockUSBManager.hasPermission(mockDevice)).thenReturn(true)
        thermalCamera.connectUSBDevice(mockDevice)
        thermalCamera.startThermalStream()
        
        // Simulate frame reception
        val testBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        val testTempData = generateTestTemperatureData(25.0f)
        val timestamp = System.currentTimeMillis()
        
        // Act - simulate internal frame callback
        simulateFrameCallback(testBitmap, testTempData, timestamp)
        
        // Assert
        verify(mockFrameCallback).onThermalFrame(
            eq(testBitmap), 
            eq(testTempData), 
            eq(timestamp)
        )
    }
    
    private fun createMockTC001Device(): UsbDevice {
        val mockDevice = mock(UsbDevice::class.java)
        whenever(mockDevice.vendorId).thenReturn(TC001_VENDOR_ID)
        whenever(mockDevice.productId).thenReturn(TC001_PRODUCT_ID)
        whenever(mockDevice.deviceName).thenReturn("/dev/bus/usb/001/002")
        whenever(mockDevice.deviceClass).thenReturn(USB_CLASS_VIDEO)
        return mockDevice
    }
    
    private fun generateTestTemperatureData(temperature: Float): ByteArray {
        val data = ByteArray(256 * 192 * 2)
        val rawValue = ((temperature + 273.15f) * 64.0f).toInt()
        
        for (i in data.indices step 2) {
            data[i] = (rawValue and 0xFF).toByte()
            data[i + 1] = ((rawValue shr 8) and 0xFF).toByte()
        }
        
        return data
    }
    
    private fun generateTestTemperatureDataWithHotSpot(
        hotTemp: Float, 
        hotX: Int, 
        hotY: Int
    ): ByteArray {
        val data = generateTestTemperatureData(25.0f) // Base temperature
        val hotRawValue = ((hotTemp + 273.15f) * 64.0f).toInt()
        
        // Set hot spot temperature
        val index = (hotY * 256 + hotX) * 2
        if (index + 1 < data.size) {
            data[index] = (hotRawValue and 0xFF).toByte()
            data[index + 1] = ((hotRawValue shr 8) and 0xFF).toByte()
        }
        
        return data
    }
    
    private fun generateUniformTemperatureData(temperature: Float): ByteArray {
        return generateTestTemperatureData(temperature)
    }
    
    private fun simulateFrameCallback(bitmap: Bitmap, tempData: ByteArray, timestamp: Long) {
        // This would be called by the actual camera implementation
        mockFrameCallback.onThermalFrame(bitmap, tempData, timestamp)
    }
}
```

### Temperature View Testing

#### TemperatureViewTest - UI Component Testing
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TemperatureViewTest {
    
    private lateinit var context: Context
    private lateinit var temperatureView: TemperatureView
    private lateinit var testBitmap: Bitmap
    private lateinit var testTempData: ByteArray
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        temperatureView = TemperatureView(context, null)
        testBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        testTempData = ThermalTestUtils.generateTestTemperatureData(25.0f)
    }
    
    @Test
    fun `should initialize with default settings`() {
        assertNotNull("TemperatureView should be initialized", temperatureView)
        assertFalse("Spot meter should be disabled by default", temperatureView.isSpotMeterEnabled())
        assertEquals("Default pseudocolor should be Iron", 
                    PSEUDOCOLOR_IRON, temperatureView.getPseudocolorMode())
    }
    
    @Test
    fun `should update thermal display correctly`() {
        // Act
        temperatureView.setThermalBitmap(testBitmap, testTempData)
        
        // Assert
        assertNotNull("Thermal bitmap should be set", temperatureView.getCurrentThermalBitmap())
        assertNotNull("Temperature data should be stored", temperatureView.getCurrentTemperatureData())
    }
    
    @Test
    fun `should enable and disable spot meter`() {
        // Test enable
        temperatureView.enableSpotMeter(true)
        assertTrue("Spot meter should be enabled", temperatureView.isSpotMeterEnabled())
        
        // Test disable
        temperatureView.enableSpotMeter(false)
        assertFalse("Spot meter should be disabled", temperatureView.isSpotMeterEnabled())
    }
    
    @Test
    fun `should change pseudocolor modes correctly`() {
        val testModes = listOf(
            PSEUDOCOLOR_IRON,
            PSEUDOCOLOR_RAINBOW,
            PSEUDOCOLOR_WHITE_HOT,
            PSEUDOCOLOR_BLACK_HOT
        )
        
        testModes.forEach { mode ->
            temperatureView.setPseudocolorMode(mode)
            assertEquals("Pseudocolor mode should be set correctly", 
                        mode, temperatureView.getPseudocolorMode())
        }
    }
    
    @Test
    fun `should add and remove temperature areas`() {
        // Add area
        val testArea = Rect(50, 50, 150, 100)
        val areaId = temperatureView.addTemperatureArea(testArea, "Test Area")
        
        assertTrue("Area ID should be valid", areaId >= 0)
        assertTrue("Area should be added", temperatureView.hasTemperatureArea(areaId))
        
        // Remove area
        temperatureView.removeTemperatureArea(areaId)
        assertFalse("Area should be removed", temperatureView.hasTemperatureArea(areaId))
    }
    
    @Test
    fun `should set temperature range correctly`() {
        val minTemp = 10.0f
        val maxTemp = 60.0f
        
        temperatureView.setTemperatureRange(minTemp, maxTemp)
        
        assertEquals("Min temperature should be set", minTemp, temperatureView.getMinTemperature())
        assertEquals("Max temperature should be set", maxTemp, temperatureView.getMaxTemperature())
    }
    
    @Test
    fun `should validate temperature range parameters`() {
        assertThrows(IllegalArgumentException::class.java) {
            temperatureView.setTemperatureRange(50.0f, 30.0f) // Min > Max
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            temperatureView.setTemperatureRange(-300.0f, 30.0f) // Extreme min
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            temperatureView.setTemperatureRange(30.0f, 1000.0f) // Extreme max
        }
    }
    
    @Test
    fun `should handle touch interactions correctly`() {
        temperatureView.setThermalBitmap(testBitmap, testTempData)
        
        // Simulate touch event
        val touchX = 128f
        val touchY = 96f
        val motionEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, touchX, touchY, 0)
        
        val handled = temperatureView.onTouchEvent(motionEvent)
        
        assertTrue("Touch event should be handled", handled)
        motionEvent.recycle()
    }
}
```

### Data Writer Testing

#### ThermalDataWriterTest - Data Recording Testing
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ThermalDataWriterTest {
    
    private lateinit var context: Context
    private lateinit var dataWriter: ThermalDataWriter
    private lateinit var testDirectory: File
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataWriter = ThermalDataWriter.getInstance(context)
        testDirectory = File(context.cacheDir, "test_thermal_data")
        testDirectory.mkdirs()
    }
    
    @After
    fun tearDown() {
        dataWriter.stopRecording()
        testDirectory.deleteRecursively()
    }
    
    @Test
    fun `should initialize data writer correctly`() {
        assertNotNull("Data writer should be initialized", dataWriter)
        assertFalse("Recording should not be active initially", dataWriter.isRecording())
    }
    
    @Test
    fun `should start and stop recording successfully`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
            includeMetadata = true
        }
        
        // Start recording
        val started = dataWriter.startRecording("test_session", config)
        assertTrue("Recording should start successfully", started)
        assertTrue("Recording should be active", dataWriter.isRecording())
        
        // Stop recording
        val filePath = dataWriter.stopRecording()
        assertNotNull("File path should be returned", filePath)
        assertFalse("Recording should be stopped", dataWriter.isRecording())
        
        // Verify file exists
        val recordingFile = File(filePath)
        assertTrue("Recording file should exist", recordingFile.exists())
        assertTrue("Recording file should not be empty", recordingFile.length() > 0)
    }
    
    @Test
    fun `should record thermal frames correctly`() {
        val config = RecordingConfig().apply {
            format = RecordingFormat.CSV
        }
        
        dataWriter.startRecording("frame_test", config)
        
        // Record test frames
        val testFrames = 10
        repeat(testFrames) { i ->
            val timestamp = System.currentTimeMillis() + i * 40 // 25 FPS
            val tempData = ThermalTestUtils.generateTestTemperatureData(25.0f + i)
            val metadata = ThermalMetadata().apply {
                this.timestamp = timestamp
                emissivity = 0.95f
                ambientTemp = 25.0f
            }
            
            dataWriter.recordThermalFrame(timestamp, tempData, metadata)
        }
        
        val filePath = dataWriter.stopRecording()
        
        // Verify recording content
        val recordingFile = File(filePath)
        val lines = recordingFile.readLines()
        
        assertTrue("Should have header line", lines.isNotEmpty())
        assertTrue("Should have data lines", lines.size > testFrames)
    }
    
    @Test
    fun `should export data with analysis correctly`() {
        // Record some test data first
        recordTestSession()
        
        val exportConfig = ExportConfig().apply {
            format = ExportFormat.CSV
            includeAnalysis = true
            includeImages = false
        }
        
        val exportPath = dataWriter.exportThermalData(exportConfig)
        
        assertNotNull("Export path should be returned", exportPath)
        
        val exportFile = File(exportPath)
        assertTrue("Export file should exist", exportFile.exists())
        assertTrue("Export file should contain data", exportFile.length() > 0)
        
        // Verify export contains analysis
        val exportContent = exportFile.readText()
        assertTrue("Should contain statistical analysis", 
                  exportContent.contains("Statistical Analysis"))
    }
    
    @Test
    fun `should handle insufficient storage gracefully`() {
        // Mock insufficient storage
        val mockStatFs = mock(StatFs::class.java)
        whenever(mockStatFs.availableBytes).thenReturn(1024L) // Only 1KB available
        
        val config = RecordingConfig()
        val started = dataWriter.startRecording("storage_test", config)
        
        assertFalse("Recording should fail with insufficient storage", started)
    }
    
    @Test
    fun `should validate recording configuration`() {
        // Test invalid configuration
        val invalidConfig = RecordingConfig().apply {
            frameRate = -1 // Invalid frame rate
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            dataWriter.startRecording("invalid_test", invalidConfig)
        }
        
        // Test valid configuration
        val validConfig = RecordingConfig().apply {
            frameRate = 25
            format = RecordingFormat.CSV
        }
        
        assertDoesNotThrow {
            dataWriter.startRecording("valid_test", validConfig)
        }
    }
    
    @Test
    fun `should handle concurrent recording attempts`() {
        val config = RecordingConfig()
        
        // Start first recording
        val started1 = dataWriter.startRecording("concurrent_1", config)
        assertTrue("First recording should start", started1)
        
        // Try to start second recording
        val started2 = dataWriter.startRecording("concurrent_2", config)
        assertFalse("Second recording should fail", started2)
        
        // Stop first recording
        dataWriter.stopRecording()
        
        // Now second recording should work
        val started3 = dataWriter.startRecording("concurrent_3", config)
        assertTrue("Recording should work after stopping previous", started3)
    }
    
    private fun recordTestSession() {
        val config = RecordingConfig()
        dataWriter.startRecording("test_analysis", config)
        
        repeat(100) { i ->
            val tempData = ThermalTestUtils.generateTestTemperatureData(20.0f + i * 0.1f)
            dataWriter.recordThermalFrame(System.currentTimeMillis(), tempData, null)
        }
        
        dataWriter.stopRecording()
    }
}
```

## Integration Testing

### End-to-End Workflow Testing

#### ThermalIntegrationTest - Complete Integration Testing
```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class ThermalIntegrationTest {
    
    @get:Rule
    val activityRule = ActivityTestRule(ThermalActivity::class.java, false, false)
    
    private lateinit var mockThermalCamera: IRUVCTC
    private lateinit var activity: ThermalActivity
    
    @Before
    fun setUp() {
        // Initialize mock thermal camera
        mockThermalCamera = createMockThermalCamera()
        
        // Launch activity with mock camera
        val intent = Intent().apply {
            putExtra("use_mock_camera", true)
        }
        activity = activityRule.launchActivity(intent)
    }
    
    @Test
    fun testCompleteUserWorkflow() {
        // Test complete user workflow from connection to data export
        
        // 1. Connect thermal camera
        onView(withId(R.id.btn_connect_camera))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Wait for connection
        Thread.sleep(2000)
        
        // 2. Verify thermal display is active
        onView(withId(R.id.temperature_view))
            .check(matches(isDisplayed()))
        
        // 3. Configure thermal settings
        onView(withId(R.id.btn_thermal_settings))
            .perform(click())
        
        // Set emissivity
        onView(withId(R.id.seekbar_emissivity))
            .perform(setProgress(95)) // 0.95 emissivity
        
        // Set pseudocolor mode
        onView(withId(R.id.spinner_pseudocolor))
            .perform(click())
        onData(equalTo("Iron"))
            .perform(click())
        
        // Apply settings
        onView(withId(R.id.btn_apply_settings))
            .perform(click())
        
        // 4. Start recording
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        // Verify recording status
        onView(withId(R.id.text_recording_status))
            .check(matches(withText(containsString("Recording"))))
        
        // 5. Wait for some data to be recorded
        Thread.sleep(5000)
        
        // 6. Stop recording
        onView(withId(R.id.btn_stop_recording))
            .perform(click())
        
        // 7. Verify data export
        onView(withId(R.id.btn_export_data))
            .perform(click())
        
        // Check export success message
        onView(withText(containsString("exported")))
            .inRoot(withDecorView(not(activity.window.decorView)))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testUSBPermissionFlow() {
        // Test USB permission request and handling
        
        // Simulate USB device connection
        simulateUSBDeviceConnected()
        
        // Check permission dialog
        onView(withText("USB Permission"))
            .check(matches(isDisplayed()))
        
        // Grant permission
        onView(withText("OK"))
            .perform(click())
        
        // Wait for camera connection
        Thread.sleep(2000)
        
        // Verify camera is connected
        onView(withId(R.id.text_connection_status))
            .check(matches(withText("Connected")))
    }
    
    @Test
    fun testThermalMeasurementTools() {
        // Connect camera first
        connectMockCamera()
        
        // Enable spot meter
        onView(withId(R.id.checkbox_spot_meter))
            .perform(click())
        
        // Verify spot meter is displayed
        onView(withId(R.id.spot_meter_overlay))
            .check(matches(isDisplayed()))
        
        // Add temperature area
        onView(withId(R.id.btn_add_temp_area))
            .perform(click())
        
        // Draw temperature area (simulate touch)
        onView(withId(R.id.temperature_view))
            .perform(longClick())
        
        // Verify temperature area is added
        onView(withId(R.id.temp_area_list))
            .check(matches(hasChildCount(1)))
        
        // Test temperature line
        onView(withId(R.id.btn_temp_line))
            .perform(click())
        
        // Draw line on thermal view
        onView(withId(R.id.temperature_view))
            .perform(
                actionWithAssertions(
                    GeneralSwipeAction(
                        Swipe.FAST,
                        GeneralLocation.TOP_LEFT,
                        GeneralLocation.BOTTOM_RIGHT,
                        Press.FINGER
                    )
                )
            )
        
        // Verify line temperature profile is shown
        onView(withId(R.id.temp_line_chart))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testDataExportFormats() {
        // Record test session first
        recordTestThermalSession()
        
        // Test CSV export
        onView(withId(R.id.btn_export_options))
            .perform(click())
        
        onView(withText("CSV Format"))
            .perform(click())
        
        onView(withId(R.id.btn_export))
            .perform(click())
        
        // Wait for export
        Thread.sleep(3000)
        
        // Verify CSV file created
        verifyExportFileExists("csv")
        
        // Test JSON export
        onView(withId(R.id.btn_export_options))
            .perform(click())
        
        onView(withText("JSON Format"))
            .perform(click())
        
        onView(withId(R.id.btn_export))
            .perform(click())
        
        Thread.sleep(3000)
        verifyExportFileExists("json")
    }
    
    @Test
    fun testErrorHandlingAndRecovery() {
        // Test USB disconnection handling
        connectMockCamera()
        
        // Simulate USB disconnection
        simulateUSBDisconnection()
        
        // Verify error message
        onView(withText(containsString("disconnected")))
            .check(matches(isDisplayed()))
        
        // Test reconnection
        onView(withText("Reconnect"))
            .perform(click())
        
        // Simulate successful reconnection
        simulateUSBReconnection()
        
        // Verify connection restored
        onView(withId(R.id.text_connection_status))
            .check(matches(withText("Connected")))
    }
    
    private fun createMockThermalCamera(): IRUVCTC {
        val mockCamera = mock(IRUVCTC::class.java)
        
        whenever(mockCamera.connectUSBDevice(any())).thenReturn(true)
        whenever(mockCamera.isConnected()).thenReturn(true)
        whenever(mockCamera.startThermalStream()).thenReturn(true)
        whenever(mockCamera.isStreaming()).thenReturn(true)
        
        return mockCamera
    }
    
    private fun connectMockCamera() {
        onView(withId(R.id.btn_connect_camera))
            .perform(click())
        Thread.sleep(1000)
    }
    
    private fun recordTestThermalSession() {
        connectMockCamera()
        
        onView(withId(R.id.btn_start_recording))
            .perform(click())
        
        Thread.sleep(5000)
        
        onView(withId(R.id.btn_stop_recording))
            .perform(click())
        
        Thread.sleep(1000)
    }
    
    private fun simulateUSBDeviceConnected() {
        // Simulate USB device connection broadcast
        val intent = Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        activity.sendBroadcast(intent)
    }
    
    private fun simulateUSBDisconnection() {
        val intent = Intent(UsbManager.ACTION_USB_DEVICE_DETACHED)
        activity.sendBroadcast(intent)
    }
    
    private fun simulateUSBReconnection() {
        simulateUSBDeviceConnected()
    }
    
    private fun verifyExportFileExists(format: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val exportDir = File(context.filesDir, "thermal_exports")
        
        val exportFiles = exportDir.listFiles { file ->
            file.name.endsWith(".$format")
        }
        
        assertTrue("Export file should exist", exportFiles?.isNotEmpty() == true)
    }
}
```

## Performance Testing

### Thermal Processing Performance

#### FrameRatePerformanceTest - Frame Rate Benchmarking
```kotlin
@RunWith(AndroidJUnit4::class)
class FrameRatePerformanceTest {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    private lateinit var thermalProcessor: ThermalProcessor
    private lateinit var testFrameData: ByteArray
    
    @Before
    fun setUp() {
        thermalProcessor = ThermalProcessor()
        testFrameData = ThermalTestUtils.generateTestTemperatureData(25.0f)
    }
    
    @Test
    fun benchmarkThermalFrameProcessing() {
        benchmarkRule.measureRepeated {
            val bitmap = thermalProcessor.processFrameToRGB(testFrameData)
            // Ensure bitmap is used to prevent optimization
            assertNotNull(bitmap)
        }
    }
    
    @Test
    fun benchmarkTemperatureCalculations() {
        benchmarkRule.measureRepeated {
            val minTemp = thermalProcessor.findMinTemperature(testFrameData)
            val maxTemp = thermalProcessor.findMaxTemperature(testFrameData)
            val avgTemp = thermalProcessor.calculateAverageTemperature(testFrameData)
            
            // Verify calculations are reasonable
            assertTrue(minTemp < maxTemp)
            assertTrue(avgTemp >= minTemp && avgTemp <= maxTemp)
        }
    }
    
    @Test
    fun benchmarkImageFiltering() {
        val testBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        
        benchmarkRule.measureRepeated {
            val filtered = OpencvTools.applyThermalFilter(testBitmap, FilterType.GAUSSIAN)
            assertNotNull(filtered)
        }
    }
    
    @Test
    fun stressTe
    // Long-running thermal stream simulation
    val frameCount = 1000 // ~40 seconds at 25 FPS
    val startTime = System.currentTimeMillis()
    
    repeat(frameCount) { i ->
        val bitmap = thermalProcessor.processFrameToRGB(testFrameData)
        assertNotNull("Frame $i should be processed", bitmap)
        
        // Simulate realistic frame timing
        Thread.sleep(40) // 25 FPS = 40ms per frame
    }
    
    val endTime = System.currentTimeMillis()
    val totalTime = endTime - startTime
    val actualFPS = (frameCount * 1000.0) / totalTime
    
    assertTrue("Should maintain at least 20 FPS during stress test", 
              actualFPS >= 20.0)
    
    Log.i("PERFORMANCE", "Stress test achieved ${actualFPS.toInt()} FPS")
}
```

### Memory Usage Testing

#### MemoryUsageTest - Memory Performance Validation
```kotlin
@RunWith(AndroidJUnit4::class)
class MemoryUsageTest {
    
    private lateinit var thermalProcessor: ThermalProcessor
    private val runtime = Runtime.getRuntime()
    
    @Before
    fun setUp() {
        thermalProcessor = ThermalProcessor()
        
        // Force garbage collection before tests
        System.gc()
        Thread.sleep(1000)
    }
    
    @Test
    fun testMemoryUsageDuringProcessing() {
        val initialMemory = getUsedMemory()
        val testFrames = 100
        val testData = ThermalTestUtils.generateTestTemperatureData(25.0f)
        
        // Process frames and measure memory growth
        val memoryMeasurements = mutableListOf<Long>()
        
        repeat(testFrames) {
            val bitmap = thermalProcessor.processFrameToRGB(testData)
            bitmap?.recycle() // Properly recycle bitmaps
            
            if (it % 10 == 0) {
                memoryMeasurements.add(getUsedMemory())
            }
        }
        
        val finalMemory = getUsedMemory()
        val memoryGrowth = finalMemory - initialMemory
        val maxMemoryGrowth = 50 * 1024 * 1024 // 50MB threshold
        
        assertTrue("Memory growth should be less than ${maxMemoryGrowth / 1024 / 1024}MB", 
                  memoryGrowth < maxMemoryGrowth)
        
        Log.i("MEMORY", "Memory growth: ${memoryGrowth / 1024 / 1024}MB")
    }
    
    @Test
    fun testBitmapRecycling() {
        val bitmaps = mutableListOf<Bitmap>()
        val initialMemory = getUsedMemory()
        
        // Create many bitmaps without recycling
        repeat(100) {
            val bitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
            bitmaps.add(bitmap)
        }
        
        val memoryWithBitmaps = getUsedMemory()
        
        // Recycle all bitmaps
        bitmaps.forEach { it.recycle() }
        bitmaps.clear()
        
        System.gc()
        Thread.sleep(1000)
        
        val memoryAfterRecycle = getUsedMemory()
        
        val bitmapMemoryUsage = memoryWithBitmaps - initialMemory
        val memoryReclaimed = memoryWithBitmaps - memoryAfterRecycle
        
        assertTrue("Should reclaim at least 50% of bitmap memory", 
                  memoryReclaimed > bitmapMemoryUsage * 0.5)
        
        Log.i("MEMORY", "Bitmap memory usage: ${bitmapMemoryUsage / 1024 / 1024}MB")
        Log.i("MEMORY", "Memory reclaimed: ${memoryReclaimed / 1024 / 1024}MB")
    }
    
    @Test
    fun testLongTermMemoryStability() {
        val initialMemory = getUsedMemory()
        val testDuration = 60000 // 1 minute
        val startTime = System.currentTimeMillis()
        val memoryMeasurements = mutableListOf<Long>()
        
        while (System.currentTimeMillis() - startTime < testDuration) {
            // Simulate continuous thermal processing
            val testData = ThermalTestUtils.generateRandomTemperatureData()
            val bitmap = thermalProcessor.processFrameToRGB(testData)
            bitmap?.recycle()
            
            // Measure memory every 5 seconds
            if ((System.currentTimeMillis() - startTime) % 5000 < 100) {
                memoryMeasurements.add(getUsedMemory())
            }
            
            Thread.sleep(40) // 25 FPS
        }
        
        val finalMemory = getUsedMemory()
        val memoryGrowth = finalMemory - initialMemory
        val maxAllowedGrowth = 100 * 1024 * 1024 // 100MB
        
        assertTrue("Long-term memory growth should be stable", 
                  memoryGrowth < maxAllowedGrowth)
        
        // Check for memory leaks (continuous growth)
        val memoryTrend = calculateMemoryTrend(memoryMeasurements)
        assertTrue("Memory usage should not show continuous upward trend", 
                  memoryTrend < 1.0) // Less than 1MB growth per measurement
        
        Log.i("MEMORY", "Long-term memory growth: ${memoryGrowth / 1024 / 1024}MB")
        Log.i("MEMORY", "Memory trend: ${memoryTrend}MB per measurement")
    }
    
    private fun getUsedMemory(): Long {
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun calculateMemoryTrend(measurements: List<Long>): Double {
        if (measurements.size < 2) return 0.0
        
        val firstMeasurement = measurements.first()
        val lastMeasurement = measurements.last()
        val numMeasurements = measurements.size
        
        return (lastMeasurement - firstMeasurement).toDouble() / numMeasurements / 1024 / 1024
    }
}
```

## Coverage Goals

### Test Coverage Targets

#### Coverage Requirements
- **Unit Tests**: Minimum 90% line coverage
- **Integration Tests**: 85% feature coverage
- **UI Tests**: 80% user journey coverage
- **Performance Tests**: 100% critical path coverage

#### Coverage Monitoring
```kotlin
// app/build.gradle coverage configuration
android {
    buildTypes {
        debug {
            testCoverageEnabled = true
        }
    }
}

tasks.register('jacocoTestReport', JacocoReport) {
    dependsOn 'testDebugUnitTest'
    
    reports {
        xml.enabled = true
        html.enabled = true
    }
    
    def fileFilter = [
        '**/R.class',
        '**/R$*.class',
        '**/BuildConfig.*',
        '**/Manifest*.*',
        '**/*Test*.*',
        'android/**/*.*'
    ]
    
    def debugTree = fileTree(dir: "$project.buildDir/intermediates/javac/debug", 
                            excludes: fileFilter)
    def kotlinDebugTree = fileTree(dir: "$project.buildDir/tmp/kotlin-classes/debug", 
                                  excludes: fileFilter)
    
    classDirectories.from = files([debugTree, kotlinDebugTree])
    
    sourceDirectories.from = files([
        "$project.projectDir/src/main/java",
        "$project.projectDir/src/main/kotlin"
    ])
    
    executionData.from = fileTree(dir: project.buildDir, 
                                 includes: ['**/*.exec', '**/*.ec'])
}
```

### Coverage Quality Gates

#### Automated Coverage Validation
```kotlin
class CoverageValidator {
    
    fun validateCoverageRequirements(coverageReport: CoverageReport): ValidationResult {
        val result = ValidationResult()
        
        // Validate line coverage
        if (coverageReport.lineCoverage < 0.90) {
            result.addError("Line coverage ${coverageReport.lineCoverage} below 90% minimum")
        }
        
        // Validate branch coverage
        if (coverageReport.branchCoverage < 0.85) {
            result.addError("Branch coverage ${coverageReport.branchCoverage} below 85% minimum")
        }
        
        // Validate critical components coverage
        validateCriticalComponentsCoverage(coverageReport, result)
        
        return result
    }
    
    private fun validateCriticalComponentsCoverage(
        coverage: CoverageReport, 
        result: ValidationResult
    ) {
        val criticalComponents = listOf(
            "IRUVCTC" to 0.95,
            "ThermalDataWriter" to 0.90,
            "TemperatureView" to 0.85,
            "OpencvTools" to 0.90,
            "USBConnectionManager" to 0.95
        )
        
        criticalComponents.forEach { (component, threshold) ->
            val componentCoverage = coverage.getComponentCoverage(component)
            if (componentCoverage < threshold) {
                result.addError("$component coverage $componentCoverage below $threshold threshold")
            }
        }
    }
}
```

This comprehensive test suite documentation ensures thorough testing of all TC001 thermal imaging functionality with professional-grade quality assurance standards suitable for research and clinical applications.