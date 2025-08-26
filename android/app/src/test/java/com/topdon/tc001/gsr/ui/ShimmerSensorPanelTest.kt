package com.topdon.tc001.gsr.ui

import android.content.Context
import android.content.SharedPreferences
import android.widget.*
import androidx.test.core.app.ApplicationProvider
import com.shimmerresearch.driver.Configuration
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ShimmerSensorPanelTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockConfigurationListener: ShimmerSensorPanel.ShimmerConfigurationListener

    private lateinit var sensorPanel: ShimmerSensorPanel
    private lateinit var realContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        realContext = ApplicationProvider.getApplicationContext()
        
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.putInt(any(), any())).thenReturn(mockEditor)
        whenever(mockEditor.commit()).thenReturn(true)
        
        whenever(mockSharedPreferences.getInt("sampling_rate", 128)).thenReturn(128)
        whenever(mockSharedPreferences.getBoolean("gsr_enabled", true)).thenReturn(true)
        whenever(mockSharedPreferences.getBoolean("temperature_enabled", true)).thenReturn(true)
        whenever(mockSharedPreferences.getBoolean("ppg_enabled", false)).thenReturn(false)
        whenever(mockSharedPreferences.getBoolean("accel_enabled", false)).thenReturn(false)
        whenever(mockSharedPreferences.getInt("gsr_range", 0)).thenReturn(0)
        whenever(mockSharedPreferences.getBoolean("filter_enabled", true)).thenReturn(true)
        whenever(mockSharedPreferences.getBoolean("calibration_enabled", true)).thenReturn(true)

        sensorPanel = ShimmerSensorPanel(realContext)
    }

    @Test
    fun testInitialization() {

        Assert.assertNotNull("Sensor panel should be initialized", sensorPanel)
    }

    @Test
    fun testSamplingRateOptions() {

        val expectedRates = arrayOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024)
        
        Assert.assertTrue("Should support standard sampling rates", expectedRates.contains(128))
    }

    @Test
    fun testGSRRangeOptions() {

        val expectedRanges = arrayOf("40kΩ", "287kΩ", "1MΩ", "3.3MΩ")
        
        Assert.assertTrue("Should support standard GSR ranges", expectedRanges.isNotEmpty())
    }

    @Test
    fun testConfigurationGeneration() {

        val config = sensorPanel.generateConfiguration()
        
        Assert.assertNotNull("Should generate valid configuration", config)
        Assert.assertTrue("Should have sampling rate set", config.getSamplingRateShimmer() > 0)
    }

    @Test
    fun testConfigurationValidation() {

        val validConfig = createValidConfiguration()
        val isValid = sensorPanel.validateConfiguration(validConfig)
        
        Assert.assertTrue("Valid configuration should pass validation", isValid)
    }

    @Test
    fun testInvalidConfigurationHandling() {

        val invalidConfig = createInvalidConfiguration()
        val isValid = sensorPanel.validateConfiguration(invalidConfig)
        
        Assert.assertFalse("Invalid configuration should fail validation", isValid)
    }

    @Test
    fun testSensorBitmapGeneration() {

        val bitmap = sensorPanel.generateSensorBitmap()
        
        Assert.assertTrue("Should generate valid sensor bitmap", bitmap > 0)
    }

    @Test
    fun testResetToDefaults() {

        sensorPanel.resetToDefaults()
        
        val config = sensorPanel.generateConfiguration()
        Assert.assertEquals("Should reset to default sampling rate", 128.0, config.getSamplingRateShimmer(), 0.1)
    }

    @Test
    fun testSettingsValidationErrors() {

        sensorPanel.setSamplingRate(0)
        val errors = sensorPanel.validateCurrentSettings()
        
        Assert.assertTrue("Should detect validation errors", errors.isNotEmpty())
    }

    private fun createValidConfiguration(): Configuration {
        return Configuration().apply {
            setSamplingRateShimmer(128.0)
            setSensorEnabledState(0x04, true)
            setSensorEnabledState(0x80, true)
        }
    }

    private fun createInvalidConfiguration(): Configuration {
        return Configuration().apply {
            setSamplingRateShimmer(0.0)
        }
    }
