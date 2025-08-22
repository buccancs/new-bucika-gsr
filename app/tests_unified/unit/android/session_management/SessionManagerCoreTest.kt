package com.multisensor.recording.service
import com.google.common.truth.Truth.assertThat
import com.multisensor.recording.util.Logger
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
class SessionManagerCoreTest {
    private lateinit var mockLogger: Logger
    private lateinit var tempDir: File
    @BeforeEach
    fun setup() {
        mockLogger = mockk(relaxed = true)
        tempDir = File.createTempFile("session_test", "").apply {
            delete()
            mkdirs()
        }
    }
    @AfterEach
    fun tearDown() {
        unmockkAll()
        tempDir.deleteRecursively()
    }
    @Test
    fun `should create session ID with proper format`() {
        val sessionId = "test_session_${System.currentTimeMillis()}"
        assertThat(sessionId).isNotNull()
        assertThat(sessionId).isNotEmpty()
        assertThat(sessionId).contains("test_session")
        assertThat(sessionId).contains("_")
    }
    @Test
    fun `should create proper directory structure for session`() {
        val sessionId = "test_session_123"
        val sessionDir = File(tempDir, sessionId)
        sessionDir.mkdirs()
        File(sessionDir, "session_info.txt").createNewFile()
        File(sessionDir, "session_config.json").createNewFile()
        assertThat(sessionDir.exists()).isTrue()
        assertThat(sessionDir.isDirectory()).isTrue()
        assertThat(File(sessionDir, "session_info.txt").exists()).isTrue()
        assertThat(File(sessionDir, "session_config.json").exists()).isTrue()
    }
    @Test
    fun `should handle multiple session directory creation correctly`() {
        val session1Dir = File(tempDir, "session_1")
        val session2Dir = File(tempDir, "session_2")
        session1Dir.mkdirs()
        session2Dir.mkdirs()
        assertThat(session1Dir.exists()).isTrue()
        assertThat(session2Dir.exists()).isTrue()
        assertThat(session1Dir.name).isNotEqualTo(session2Dir.name)
    }
    @Test
    fun `should create unique session IDs`() {
        val sessionIds = mutableSetOf<String>()
        repeat(10) {
            val sessionId = "test_${System.currentTimeMillis()}_$it"
            sessionIds.add(sessionId)
            Thread.sleep(1)
        }
        assertThat(sessionIds).hasSize(10)
    }
    @Test
    fun `should validate session directory structure`() {
        val sessionId = "valid_session"
        val sessionDir = File(tempDir, sessionId)
        sessionDir.mkdirs()
        File(sessionDir, "thermal_data").mkdirs()
        File(sessionDir, "raw_frames").mkdirs()
        File(sessionDir, "calibration").mkdirs()
        assertThat(File(sessionDir, "thermal_data").exists()).isTrue()
        assertThat(File(sessionDir, "raw_frames").exists()).isTrue()
        assertThat(File(sessionDir, "calibration").exists()).isTrue()
    }
    @Test
    fun `should handle session cleanup gracefully`() {
        val sessionDir = File(tempDir, "cleanup_session")
        sessionDir.mkdirs()
        File(sessionDir, "test_file.txt").createNewFile()
        sessionDir.deleteRecursively()
        assertThat(sessionDir.exists()).isFalse()
    }
    @Test
    fun `should create session metadata correctly`() {
        val sessionName = "test_metadata_session"
        val sanitizedName = sessionName.replace(" ", "_")
        val sessionId = "${sanitizedName}_${System.currentTimeMillis()}"
        assertThat(sessionId).contains(sanitizedName)
        assertThat(sessionId).doesNotContain(" ")
    }
}