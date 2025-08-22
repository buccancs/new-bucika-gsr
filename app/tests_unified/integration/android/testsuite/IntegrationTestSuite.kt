package com.multisensor.recording.testsuite

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.multisensor.recording.ui.MainActivityIntegrationTest::class,
)
class IntegrationTestSuite
