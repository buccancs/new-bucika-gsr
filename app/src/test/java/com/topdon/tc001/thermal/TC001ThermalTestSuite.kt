package com.topdon.tc001.thermal

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for TC001 thermal imaging functionality.
 * 
 * This test suite provides complete coverage of the TC001 thermal imaging integration
 * including unit tests, integration tests, UI tests, and performance tests.
 * 
 * Coverage Goals:
 * - Unit Tests: >90% line coverage
 * - Integration Tests: >85% feature coverage  
 * - UI Tests: >80% user journey coverage
 * - Performance Tests: 100% critical path coverage
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Unit Tests
    TC001CameraTest::class,
    TemperatureViewTest::class,
    OpencvToolsTest::class,
    ThermalCalibrationTest::class,
    USBConnectionTest::class,
    
    // Data Layer Tests
    com.topdon.tc001.thermal.data.ThermalDataWriterTest::class,
    com.topdon.tc001.thermal.data.ThermalExportTest::class,
    com.topdon.tc001.thermal.data.ThermalValidationTest::class,
    
    // UI Component Tests
    com.topdon.tc001.thermal.ui.TemperatureViewUITest::class,
    com.topdon.tc001.thermal.ui.ThermalSettingsUITest::class,
    
    // Integration Tests
    ThermalIntegrationTest::class,
    MultiModalSyncTest::class,
    
    // Performance Tests
    com.topdon.tc001.thermal.performance.FrameRatePerformanceTest::class,
    com.topdon.tc001.thermal.performance.MemoryUsageTest::class,
    com.topdon.tc001.thermal.performance.ThermalProcessingBenchmark::class
)
class TC001ThermalTestSuite {
    
    companion object {
        /**
         * Test coverage requirements for TC001 thermal imaging functionality
         */
        const val MINIMUM_LINE_COVERAGE = 90.0
        const val MINIMUM_BRANCH_COVERAGE = 85.0
        const val MINIMUM_FEATURE_COVERAGE = 85.0
        
        /**
         * Performance benchmarks for TC001 thermal imaging
         */
        const val MINIMUM_FRAME_RATE = 20.0 // FPS
        const val MAXIMUM_FRAME_PROCESSING_TIME = 40.0 // milliseconds
        const val MAXIMUM_MEMORY_GROWTH = 50.0 // MB during extended operation
        
        /**
         * Test data constants
         */
        const val TEST_THERMAL_WIDTH = 256
        const val TEST_THERMAL_HEIGHT = 192
        const val TEST_THERMAL_DATA_SIZE = TEST_THERMAL_WIDTH * TEST_THERMAL_HEIGHT * 2
        
        /**
         * Test device constants
         */
        const val TEST_TC001_VENDOR_ID = 0x1234
        const val TEST_TC001_PRODUCT_ID = 0x5678
        
        /**
         * Test temperature ranges
         */
        const val MIN_REASONABLE_TEMP = -40.0f // Celsius
        const val MAX_REASONABLE_TEMP = 200.0f // Celsius
        const val TYPICAL_ROOM_TEMP = 25.0f // Celsius
        
        /**
         * Test timing constants
         */
        const val FRAME_INTERVAL_MS = 40L // 25 FPS
        const val TEST_RECORDING_DURATION_MS = 5000L
        const val UI_RESPONSE_TIMEOUT_MS = 3000L
    }
}