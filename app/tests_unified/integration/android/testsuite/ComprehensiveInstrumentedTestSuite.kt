package com.multisensor.recording.testsuite

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.ui.MainActivityIntegrationTest::class,
    com.multisensor.recording.ui.FileViewActivityUITest::class,

    com.multisensor.recording.recording.ThermalRecorderHardwareTest::class,
    com.multisensor.recording.recording.ComprehensiveCameraAccessTest::class,
    com.multisensor.recording.recording.BluetoothDiagnosticTest::class,
    com.multisensor.recording.recording.ShimmerRecorderDirectTest::class,

    com.multisensor.recording.integration.DataFlowIntegrationTest::class,
    com.multisensor.recording.integration.MultiSensorCoordinationTest::class,
    com.multisensor.recording.integration.ProtocolIntegrationTest::class,
    com.multisensor.recording.integration.FileIOIntegrationTest::class
)
class ComprehensiveInstrumentedTestSuite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.recording.ThermalCameraBulletproofIntegrationTest::class,
    com.multisensor.recording.integration.MultiSensorCoordinationTest::class,
    com.multisensor.recording.integration.DataFlowIntegrationTest::class
)
class HardwareStressTestSuite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.ui.MainActivityIntegrationTest::class,
    com.multisensor.recording.ui.FileViewActivityUITest::class
)
class UIPerformanceTestSuite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.recording.CameraRecorderManualTest::class,
    com.multisensor.recording.recording.ShimmerRecorderManualTest::class
)
class ManualTestSuite
