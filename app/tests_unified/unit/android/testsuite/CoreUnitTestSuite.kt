package com.multisensor.recording.testsuite

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.recording.session.SessionInfoTest::class,

    com.multisensor.recording.ui.viewmodel.MainUiStateTest::class,

    com.multisensor.recording.network.NetworkQualityMonitorTest::class,
)
class CoreUnitTestSuite
