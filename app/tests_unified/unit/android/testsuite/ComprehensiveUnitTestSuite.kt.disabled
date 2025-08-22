package com.multisensor.recording.testsuite

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.recording.session.SessionInfoTest::class,
    com.multisensor.recording.recording.ThermalRecorderUnitTest::class,
    com.multisensor.recording.recording.ShimmerRecorderEnhancedTest::class,
    com.multisensor.recording.recording.ConnectionManagerTestSimple::class,
    com.multisensor.recording.recording.ShimmerRecorderConfigurationTest::class,
    com.multisensor.recording.recording.AdaptiveFrameRateControllerTest::class,

    com.multisensor.recording.ui.viewmodel.MainViewModelTest::class,
    com.multisensor.recording.ui.MainUiStateTest::class,
    com.multisensor.recording.ui.FileViewUiStateTest::class,
    com.multisensor.recording.ui.FileViewActivityTest::class,
    com.multisensor.recording.ui.ShimmerConfigUiStateTest::class,
    com.multisensor.recording.ui.SettingsUiStateTest::class,
    com.multisensor.recording.ui.FileManagementLogicTest::class,
    com.multisensor.recording.ui.NetworkConfigActivityTest::class,

    com.multisensor.recording.network.FileTransferHandlerTest::class,
    com.multisensor.recording.network.NetworkQualityMonitorTest::class,

    com.multisensor.recording.service.SessionManagerTest::class,
    com.multisensor.recording.service.SessionManagerBusinessLogicTest::class,

    com.multisensor.recording.util.LoggerTest::class,
    com.multisensor.recording.util.AppLoggerEnhancedTest::class,
    com.multisensor.recording.util.UserFeedbackManagerTest::class,
    com.multisensor.recording.util.AllAndroidPermissionsTest::class,
    com.multisensor.recording.util.AllAndroidPermissionsBusinessLogicTest::class,
    com.multisensor.recording.util.SimpleArchitectureTest::class,
    com.multisensor.recording.util.LoggerBusinessLogicTest::class,

    com.multisensor.recording.controllers.UsbControllerUnitTest::class,
    com.multisensor.recording.managers.UsbDeviceManagerUnitTest::class,

    com.multisensor.recording.streaming.PreviewStreamerTest::class,
    com.multisensor.recording.calibration.CalibrationCaptureManagerTest::class,
    com.multisensor.recording.calibration.SyncClockManagerTest::class,

    com.multisensor.recording.ui.components.ActionButtonPairTest::class,
    com.multisensor.recording.ui.components.StatusIndicatorViewTest::class,
    com.multisensor.recording.ui.components.CardSectionLayoutTest::class,
    com.multisensor.recording.ui.components.SectionHeaderViewTest::class,
    com.multisensor.recording.ui.components.LabelTextViewTest::class,

    com.multisensor.recording.ui.viewmodel.MainUiStateTest::class
)
class ComprehensiveUnitTestSuite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.recording.ShimmerRecorderEnhancedTest::class,
    com.multisensor.recording.network.NetworkQualityMonitorTest::class,
    com.multisensor.recording.service.SessionManagerTest::class,
    com.multisensor.recording.calibration.CalibrationCaptureManagerTest::class
)
class StressTestSuite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.network.FileTransferHandlerTest::class,
    com.multisensor.recording.streaming.PreviewStreamerTest::class,
    com.multisensor.recording.recording.AdaptiveFrameRateControllerTest::class,
    com.multisensor.recording.calibration.SyncClockManagerTest::class
)
class PerformanceTestSuite