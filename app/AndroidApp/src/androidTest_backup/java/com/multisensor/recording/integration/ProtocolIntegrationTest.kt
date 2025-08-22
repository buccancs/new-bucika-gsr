package com.multisensor.recording.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.multisensor.recording.config.CommonConstants
import com.multisensor.recording.protocol.SchemaManager
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ProtocolIntegrationTest {
    private lateinit var schemaManager: SchemaManager
    private lateinit var mockConnectionManager: MockConnectionManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        schemaManager = SchemaManager.getInstance(context)
        mockConnectionManager = MockConnectionManager()
    }

    @Test
    fun testSchemaManagerLoading() {
        assertTrue("Schema should be loaded", schemaManager.isSchemaLoaded)

        val validTypes = schemaManager.validMessageTypes
        assertTrue("Should have valid message types", validTypes.isNotEmpty())

        val expectedTypes =
            setOf(
                "start_record",
                "stop_record",
                "preview_frame",
                "file_chunk",
                "device_status",
                "ack",
                "calibration_start",
                "calibration_result",
            )

        for (expectedType in expectedTypes) {
            assertTrue("Should contain $expectedType", validTypes.contains(expectedType))
        }
    }

    @Test
    fun testCommonConstantsAccess() {
        val host = CommonConstants.Network.HOST
        val port = CommonConstants.Network.PORT

        assertNotNull("Host should not be null", host)
        assertTrue("Host should not be empty", host.isNotEmpty())
        assertTrue("Port should be positive", port > 0)
        assertTrue("Port should be valid", port <= 65535)

        assertTrue("Protocol version should be positive", CommonConstants.PROTOCOL_VERSION > 0)
        assertNotNull("App version should not be null", CommonConstants.APP_VERSION)
        assertTrue("Frame rate should be positive", CommonConstants.Devices.FRAME_RATE > 0)
    }

    @Test
    fun testMessageValidation() {
        val validStartMessage =
            JSONObject().apply {
                put("type", "start_record")
                put("timestamp", System.currentTimeMillis())
                put("session_id", "test_session_123")
            }

        assertTrue(
            "Valid start message should pass validation",
            schemaManager.validateMessage(validStartMessage),
        )

        val invalidMessage =
            JSONObject().apply {
                put("type", "start_record")
                put("timestamp", System.currentTimeMillis())
            }

        assertFalse(
            "Invalid message should fail validation",
            schemaManager.validateMessage(invalidMessage),
        )
    }

    @Test
    fun testMessageCreation() {
        val startMessage = schemaManager.createMessage("start_record")
        assertEquals("start_record", startMessage.getString("type"))
        assertTrue("Should have timestamp", startMessage.has("timestamp"))
        assertTrue("Should have session_id field", startMessage.has("session_id"))

        val previewMessage = schemaManager.createMessage("preview_frame")
        assertEquals("preview_frame", previewMessage.getString("type"))
        assertTrue("Should have frame_id", previewMessage.has("frame_id"))
        assertTrue("Should have image_data", previewMessage.has("image_data"))
        assertTrue("Should have width", previewMessage.has("width"))
        assertTrue("Should have height", previewMessage.has("height"))
    }

    @Test
    fun testMockSocketConnection() {
        val latch = CountDownLatch(1)
        var receivedMessage: JSONObject? = null

        mockConnectionManager.onMessageReceived = { message ->
            receivedMessage = message
            latch.countDown()
        }

        val testMessage =
            schemaManager.createMessage("device_status").apply {
                put("device_id", "test_device")
                put("status", "idle")
                put("battery_level", 85)
                put("storage_available", 1024)
            }

        mockConnectionManager.sendMessage(testMessage)

        assertTrue(
            "Should receive message within timeout",
            latch.await(5, TimeUnit.SECONDS),
        )

        assertNotNull("Should have received a message", receivedMessage)
        assertEquals("device_status", receivedMessage?.getString("type"))
        assertEquals("test_device", receivedMessage?.getString("device_id"))
    }

    @Test
    fun testRecordingStateTransitions() {
        val stateManager = MockRecordingStateManager()

        assertEquals(RecordingState.IDLE, stateManager.currentState)

        val startMessage =
            schemaManager.createMessage("start_record").apply {
                put("session_id", "test_session")
            }

        stateManager.handleMessage(startMessage)
        assertEquals(RecordingState.RECORDING, stateManager.currentState)

        val stopMessage =
            schemaManager.createMessage("stop_record").apply {
                put("session_id", "test_session")
            }

        stateManager.handleMessage(stopMessage)
        assertEquals(RecordingState.IDLE, stateManager.currentState)
    }

    @Test
    fun testMessageGenerationDuringRecording() {
        val messageCollector = mutableListOf<JSONObject>()
        val stateManager = MockRecordingStateManager()

        stateManager.onMessageGenerated = { message ->
            messageCollector.add(message)
        }

        val startMessage =
            schemaManager.createMessage("start_record").apply {
                put("session_id", "test_session")
            }
        stateManager.handleMessage(startMessage)

        Thread.sleep(1000)

        val stopMessage =
            schemaManager.createMessage("stop_record").apply {
                put("session_id", "test_session")
            }
        stateManager.handleMessage(stopMessage)

        assertTrue("Should have generated messages", messageCollector.isNotEmpty())

        val messageTypes = messageCollector.map { it.getString("type") }.toSet()
        assertTrue("Should generate preview frames", messageTypes.contains("preview_frame"))
        assertTrue("Should generate device status", messageTypes.contains("device_status"))
    }

    @Test
    fun testCalibrationFlow() {
        val stateManager = MockRecordingStateManager()
        val messageCollector = mutableListOf<JSONObject>()

        stateManager.onMessageGenerated = { message ->
            messageCollector.add(message)
        }

        val calibrationStart =
            schemaManager.createMessage("calibration_start").apply {
                put("pattern_type", "chessboard")
                put(
                    "pattern_size",
                    JSONObject().apply {
                        put("rows", 7)
                        put("cols", 6)
                    },
                )
            }

        stateManager.handleMessage(calibrationStart)

        Thread.sleep(2000)

        val calibrationResults =
            messageCollector.filter {
                it.getString("type") == "calibration_result"
            }

        assertTrue("Should generate calibration result", calibrationResults.isNotEmpty())

        val result = calibrationResults.first()
        assertTrue("Should have success field", result.has("success"))
    }

    @Test
    fun testErrorHandling() {
        val stateManager = MockRecordingStateManager()

        val stopMessage =
            schemaManager.createMessage("stop_record").apply {
                put("session_id", "nonexistent_session")
            }

        stateManager.handleMessage(stopMessage)
        assertEquals(RecordingState.IDLE, stateManager.currentState)

        val invalidMessage =
            JSONObject().apply {
                put("type", "unknown_type")
                put("timestamp", System.currentTimeMillis())
            }

        stateManager.handleMessage(invalidMessage)
        assertEquals(RecordingState.IDLE, stateManager.currentState)
    }

    @Test
    fun testConfigSchemaConsistency() {
        val patternRows = CommonConstants.Calibration.PATTERN_ROWS
        val patternCols = CommonConstants.Calibration.PATTERN_COLS

        val calibrationMessage =
            schemaManager.createMessage("calibration_start").apply {
                put("pattern_type", CommonConstants.Calibration.PATTERN_TYPE)
                put(
                    "pattern_size",
                    JSONObject().apply {
                        put("rows", patternRows)
                        put("cols", patternCols)
                    },
                )
            }

        assertTrue(
            "Config-based message should validate",
            schemaManager.validateMessage(calibrationMessage),
        )
    }
}

class MockConnectionManager {
    var onMessageReceived: ((JSONObject) -> Unit)? = null
    private val messageQueue = mutableListOf<JSONObject>()

    fun sendMessage(message: JSONObject) {
        thread {
            Thread.sleep(10)
            onMessageReceived?.invoke(message)
        }
    }

    fun getReceivedMessages(): List<JSONObject> = messageQueue.toList()
}

enum class RecordingState {
    IDLE,
    RECORDING,
    CALIBRATING,
    ERROR,
}

class MockRecordingStateManager {
    var currentState = RecordingState.IDLE
    var onMessageGenerated: ((JSONObject) -> Unit)? = null
    private var currentSessionId: String? = null

    fun handleMessage(message: JSONObject) {
        val messageType = message.optString("type")

        when (messageType) {
            "start_record" -> {
                if (currentState == RecordingState.IDLE) {
                    currentSessionId = message.optString("session_id")
                    currentState = RecordingState.RECORDING
                    startGeneratingMessages()
                }
            }

            "stop_record" -> {
                if (currentState == RecordingState.RECORDING) {
                    currentState = RecordingState.IDLE
                    currentSessionId = null
                }
            }

            "calibration_start" -> {
                currentState = RecordingState.CALIBRATING
                startCalibrationProcess()
            }
        }
    }

    private fun startGeneratingMessages() {
        thread {
            var frameId = 0
            while (currentState == RecordingState.RECORDING) {
                val previewFrame =
                    JSONObject().apply {
                        put("type", "preview_frame")
                        put("timestamp", System.currentTimeMillis())
                        put("frame_id", frameId++)
                        put("image_data", "fake_image_data_$frameId")
                        put("width", 640)
                        put("height", 480)
                    }
                onMessageGenerated?.invoke(previewFrame)

                val deviceStatus =
                    JSONObject().apply {
                        put("type", "device_status")
                        put("timestamp", System.currentTimeMillis())
                        put("device_id", "test_device")
                        put("status", "recording")
                        put("battery_level", 85)
                        put("storage_available", 1024)
                    }
                onMessageGenerated?.invoke(deviceStatus)

                Thread.sleep(100)
            }
        }
    }

    private fun startCalibrationProcess() {
        thread {
            Thread.sleep(1500)

            val calibrationResult =
                JSONObject().apply {
                    put("type", "calibration_result")
                    put("timestamp", System.currentTimeMillis())
                    put("success", true)
                    put("rms_error", 0.8)
                }
            onMessageGenerated?.invoke(calibrationResult)

            currentState = RecordingState.IDLE
        }
    }
}
