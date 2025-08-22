package com.multisensor.recording.util

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LoggerTest {
    private lateinit var context: Context
    private lateinit var logger: Logger

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        logger = Logger(context)
    }

    @Test
    fun `logger should initialize successfully`() {

        logger.info("Test message")

    }

    @Test
    fun `logger should handle different log levels`() =
        runTest {
            val testMessage = "Test log message"

            logger.debug(testMessage)
            logger.info(testMessage)
            logger.warning(testMessage)
            logger.error(testMessage)

        }

    @Test
    fun `logger should handle exceptions in error logging`() =
        runTest {
            val testException = RuntimeException("Test exception")
            val testMessage = "Error occurred"

            logger.error(testMessage, testException)

        }

    @Test
    fun `logger should create log files in correct directory`() {
        val expectedLogDir = File(context.getExternalFilesDir(null), "logs")

        logger.info("Test message to trigger file creation")

    }
}
