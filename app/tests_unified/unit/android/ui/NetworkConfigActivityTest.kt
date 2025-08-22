package com.multisensor.recording.ui

import android.widget.Button
import android.widget.EditText
import com.multisensor.recording.R
import com.multisensor.recording.network.NetworkConfiguration
import com.multisensor.recording.network.ServerConfiguration
import com.multisensor.recording.util.Logger
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(
    application = HiltTestApplication::class,
    manifest = Config.NONE,
    sdk = [28],
)
class NetworkConfigActivityTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var networkConfiguration: NetworkConfiguration

    @Inject
    lateinit var logger: Logger

    private lateinit var activity: NetworkConfigActivity
    private lateinit var mockNetworkConfiguration: NetworkConfiguration
    private lateinit var mockLogger: Logger

    @Before
    fun setup() {
        hiltRule.inject()

        mockNetworkConfiguration = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)

        every { mockNetworkConfiguration.getServerConfiguration() } returns
                ServerConfiguration(
                    serverIp = "192.168.1.100",
                    legacyPort = 8080,
                    jsonPort = 9000,
                )
        every { mockNetworkConfiguration.getConfigurationSummary() } returns "NetworkConfig[IP=192.168.1.100, Legacy=8080, JSON=9000]"

        activity = Robolectric.buildActivity(NetworkConfigActivity::class.java).create().get()

        val networkConfigField = NetworkConfigActivity::class.java.getDeclaredField("networkConfiguration")
        networkConfigField.isAccessible = true
        networkConfigField.set(activity, mockNetworkConfiguration)

        val loggerField = NetworkConfigActivity::class.java.getDeclaredField("logger")
        loggerField.isAccessible = true
        loggerField.set(activity, mockLogger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `onCreate should initialize views and load configuration`() {
        println("[DEBUG_LOG] Testing NetworkConfigActivity onCreate initialization")

        val serverIpEditText = activity.findViewById<EditText>(R.id.edit_server_ip)
        val legacyPortEditText = activity.findViewById<EditText>(R.id.edit_legacy_port)
        val jsonPortEditText = activity.findViewById<EditText>(R.id.edit_json_port)
        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)
        val resetButton = activity.findViewById<Button>(R.id.btn_reset_config)

        assertTrue("Server IP EditText should be initialized", serverIpEditText != null)
        assertTrue("Legacy Port EditText should be initialized", legacyPortEditText != null)
        assertTrue("JSON Port EditText should be initialized", jsonPortEditText != null)
        assertTrue("Save Button should be initialized", saveButton != null)
        assertTrue("Reset Button should be initialized", resetButton != null)

        verify { mockNetworkConfiguration.getServerConfiguration() }
        verify { mockLogger.info("NetworkConfigActivity created") }

        println("[DEBUG_LOG] Activity initialization test passed")
    }

    @Test
    fun `loadCurrentConfiguration should populate fields with current values`() {
        println("[DEBUG_LOG] Testing loadCurrentConfiguration functionality")

        val serverIpEditText = activity.findViewById<EditText>(R.id.edit_server_ip)
        val legacyPortEditText = activity.findViewById<EditText>(R.id.edit_legacy_port)
        val jsonPortEditText = activity.findViewById<EditText>(R.id.edit_json_port)

        assertEquals("192.168.1.100", serverIpEditText.text.toString())
        assertEquals("8080", legacyPortEditText.text.toString())
        assertEquals("9000", jsonPortEditText.text.toString())

        verify { mockLogger.debug(any()) }

        println("[DEBUG_LOG] Configuration loading test passed")
    }

    @Test
    fun `saveConfiguration should validate and save valid configuration`() {
        println("[DEBUG_LOG] Testing saveConfiguration with valid inputs")

        val serverIpEditText = activity.findViewById<EditText>(R.id.edit_server_ip)
        val legacyPortEditText = activity.findViewById<EditText>(R.id.edit_legacy_port)
        val jsonPortEditText = activity.findViewById<EditText>(R.id.edit_json_port)
        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)

        serverIpEditText.setText("192.168.1.200")
        legacyPortEditText.setText("8081")
        jsonPortEditText.setText("9001")

        saveButton.performClick()

        verify { mockNetworkConfiguration.setServerIp("192.168.1.200") }
        verify { mockNetworkConfiguration.setLegacyPort(8081) }
        verify { mockNetworkConfiguration.setJsonPort(9001) }
        verify { mockLogger.info(any()) }

        assertEquals("Configuration saved successfully", ShadowToast.getTextOfLatestToast())

        println("[DEBUG_LOG] Valid configuration saving test passed")
    }

    @Test
    fun `saveConfiguration should reject empty server IP`() {
        println("[DEBUG_LOG] Testing saveConfiguration with empty server IP")

        val serverIpEditText = activity.findViewById<EditText>(R.id.edit_server_ip)
        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)

        serverIpEditText.setText("")

        saveButton.performClick()

        assertEquals("Server IP cannot be empty", ShadowToast.getTextOfLatestToast())

        verify(exactly = 0) { mockNetworkConfiguration.setServerIp(any()) }

        println("[DEBUG_LOG] Empty server IP validation test passed")
    }

    @Test
    fun `saveConfiguration should reject invalid IP address`() {
        println("[DEBUG_LOG] Testing saveConfiguration with invalid IP address")

        val serverIpEditText = activity.findViewById<EditText>(R.id.edit_server_ip)
        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)

        serverIpEditText.setText("invalid.ip.address")

        saveButton.performClick()

        assertEquals("Invalid IP address format", ShadowToast.getTextOfLatestToast())

        verify(exactly = 0) { mockNetworkConfiguration.setServerIp(any()) }

        println("[DEBUG_LOG] Invalid IP address validation test passed")
    }

    @Test
    fun `saveConfiguration should reject invalid port numbers`() {
        println("[DEBUG_LOG] Testing saveConfiguration with invalid port numbers")

        val legacyPortEditText = activity.findViewById<EditText>(R.id.edit_legacy_port)
        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)

        legacyPortEditText.setText("99999")

        saveButton.performClick()

        assertEquals("Legacy port must be between 1024 and 65535", ShadowToast.getTextOfLatestToast())

        println("[DEBUG_LOG] Invalid port number validation test passed")
    }

    @Test
    fun `saveConfiguration should reject same port numbers`() {
        println("[DEBUG_LOG] Testing saveConfiguration with duplicate port numbers")

        val legacyPortEditText = activity.findViewById<EditText>(R.id.edit_legacy_port)
        val jsonPortEditText = activity.findViewById<EditText>(R.id.edit_json_port)
        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)

        legacyPortEditText.setText("8080")
        jsonPortEditText.setText("8080")

        saveButton.performClick()

        assertEquals("Legacy and JSON ports must be different", ShadowToast.getTextOfLatestToast())

        println("[DEBUG_LOG] Duplicate port number validation test passed")
    }

    @Test
    fun `resetToDefaults should reset configuration and reload fields`() {
        println("[DEBUG_LOG] Testing resetToDefaults functionality")

        val resetButton = activity.findViewById<Button>(R.id.btn_reset_config)

        resetButton.performClick()

        verify { mockNetworkConfiguration.resetToDefaults() }
        verify { mockNetworkConfiguration.getServerConfiguration() }
        verify { mockLogger.info("Network configuration reset to defaults") }

        assertEquals("Configuration reset to defaults", ShadowToast.getTextOfLatestToast())

        println("[DEBUG_LOG] Reset to defaults test passed")
    }

    @Test
    fun `saveConfiguration should handle exceptions gracefully`() {
        println("[DEBUG_LOG] Testing saveConfiguration exception handling")

        val saveButton = activity.findViewById<Button>(R.id.btn_save_config)

        every { mockNetworkConfiguration.setServerIp(any()) } throws RuntimeException("Test exception")

        saveButton.performClick()

        verify { mockLogger.error("Failed to save network configuration", any()) }
        assertTrue(
            "Toast should contain error message",
            ShadowToast.getTextOfLatestToast().contains("Failed to save configuration")
        )

        println("[DEBUG_LOG] Exception handling test passed")
    }

    @Test
    fun `resetToDefaults should handle exceptions gracefully`() {
        println("[DEBUG_LOG] Testing resetToDefaults exception handling")

        val resetButton = activity.findViewById<Button>(R.id.btn_reset_config)

        every { mockNetworkConfiguration.resetToDefaults() } throws RuntimeException("Test exception")

        resetButton.performClick()

        verify { mockLogger.error("Failed to reset network configuration", any()) }
        assertTrue(
            "Toast should contain error message",
            ShadowToast.getTextOfLatestToast().contains("Failed to reset configuration")
        )

        println("[DEBUG_LOG] Reset exception handling test passed")
    }
}
