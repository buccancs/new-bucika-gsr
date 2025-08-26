package com.topdon.tc001.thermal

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(

    TC001CameraTest::class,
    TemperatureViewTest::class,
    OpencvToolsTest::class,
    ThermalCalibrationTest::class,
    USBConnectionTest::class,
    
    com.topdon.tc001.thermal.data.ThermalDataWriterTest::class,
    com.topdon.tc001.thermal.data.ThermalExportTest::class,
    com.topdon.tc001.thermal.data.ThermalValidationTest::class,
    
    com.topdon.tc001.thermal.ui.TemperatureViewUITest::class,
    com.topdon.tc001.thermal.ui.ThermalSettingsUITest::class,
    
    ThermalIntegrationTest::class,
    MultiModalSyncTest::class,
    
    com.topdon.tc001.thermal.performance.FrameRatePerformanceTest::class,
    com.topdon.tc001.thermal.performance.MemoryUsageTest::class,
    com.topdon.tc001.thermal.performance.ThermalProcessingBenchmark::class
)
class TC001ThermalTestSuite {
    
    companion object {
        
        const val MINIMUM_LINE_COVERAGE = 90.0
        const val MINIMUM_BRANCH_COVERAGE = 85.0
        const val MINIMUM_FEATURE_COVERAGE = 85.0
        
        const val MINIMUM_FRAME_RATE = 20.0
        const val MAXIMUM_FRAME_PROCESSING_TIME = 40.0
        const val MAXIMUM_MEMORY_GROWTH = 50.0
        
        const val TEST_THERMAL_WIDTH = 256
        const val TEST_THERMAL_HEIGHT = 192
        const val TEST_THERMAL_DATA_SIZE = TEST_THERMAL_WIDTH * TEST_THERMAL_HEIGHT * 2
        
        const val TEST_TC001_VENDOR_ID = 0x1234
        const val TEST_TC001_PRODUCT_ID = 0x5678
        
        const val MIN_REASONABLE_TEMP = -40.0f
        const val MAX_REASONABLE_TEMP = 200.0f
        const val TYPICAL_ROOM_TEMP = 25.0f
        
        const val FRAME_INTERVAL_MS = 40L
        const val TEST_RECORDING_DURATION_MS = 5000L
        const val UI_RESPONSE_TIMEOUT_MS = 3000L
    }
