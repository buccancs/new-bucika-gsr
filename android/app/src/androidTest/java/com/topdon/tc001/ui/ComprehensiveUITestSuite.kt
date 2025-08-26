package com.topdon.tc001.ui

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    MainActivityUITest::class,
    SplashActivityUITest::class,
    DeviceTypeActivityUITest::class,
    com.topdon.tc001.thermal.ui.IRThermalNightActivityUITest::class,
    com.topdon.tc001.gsr.ui.GSRSettingsActivityUITest::class,
    com.topdon.tc001.gsr.ui.GSRActivityUITest::class,
    com.topdon.tc001.recording.ui.EnhancedRecordingActivityUITest::class
)
class ComprehensiveUITestSuite {
    companion object {
        
    }
}

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.topdon.tc001.thermal.ui.IRThermalNightActivityUITest::class,
    com.topdon.tc001.gsr.ui.GSRActivityUITest::class,
    MainActivityUITest::class
)
class ManagerPatternUIIntegrationSuite {

}

@RunWith(Suite::class)
@Suite.SuiteClasses(
    MainActivityUITest::class,
    com.topdon.tc001.thermal.ui.IRThermalNightActivityUITest::class
)
class PerformanceAccessibilityUITestSuite {
