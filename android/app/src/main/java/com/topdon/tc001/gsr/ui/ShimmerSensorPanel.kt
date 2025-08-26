package com.topdon.tc001.gsr.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.cardview.widget.CardView
import com.shimmerresearch.driver.Configuration
import com.elvishew.xlog.XLog
import com.topdon.tc001.R

class ShimmerSensorPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ShimmerSensorPanel"
        private const val PREFS_NAME = "shimmer_sensor_settings"
        
        private const val KEY_SAMPLING_RATE = "sampling_rate"
        private const val KEY_GSR_ENABLED = "gsr_enabled"
        private const val KEY_TEMPERATURE_ENABLED = "temperature_enabled"
        private const val KEY_PPG_ENABLED = "ppg_enabled"
        private const val KEY_ACCEL_ENABLED = "accel_enabled"
        private const val KEY_GSR_RANGE = "gsr_range"
        private const val KEY_FILTER_ENABLED = "filter_enabled"
        private const val KEY_CALIBRATION_ENABLED = "calibration_enabled"
    }

    private lateinit var spinnerSamplingRate: Spinner
    private lateinit var switchGSR: Switch
    private lateinit var switchTemperature: Switch
    private lateinit var switchPPG: Switch
    private lateinit var switchAccelerometer: Switch
    private lateinit var spinnerGSRRange: Spinner
    private lateinit var switchFilter: Switch
    private lateinit var switchCalibration: Switch
    private lateinit var btnApplySettings: Button
    private lateinit var btnResetDefaults: Button
    private lateinit var tvCurrentConfig: TextView
    
    private lateinit var sharedPrefs: SharedPreferences
    
    private var configurationListener: ShimmerConfigurationListener? = null
    
    interface ShimmerConfigurationListener {
        fun onConfigurationChanged(config: ShimmerConfiguration)
        fun onConfigurationApplied(config: ShimmerConfiguration)
        fun onConfigurationReset()
    }
    
    data class ShimmerConfiguration(
        val samplingRate: Double,
        val gsrEnabled: Boolean,
        val temperatureEnabled: Boolean,
        val ppgEnabled: Boolean,
        val accelerometerEnabled: Boolean,
        val gsrRange: Int,
        val filterEnabled: Boolean,
        val calibrationEnabled: Boolean
    )

    init {
        orientation = VERTICAL
        setupView()
        setupPreferences()
        loadSavedSettings()
        updateCurrentConfigDisplay()
    }

    private fun setupView() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.view_shimmer_sensor_panel, this, true)
        
        spinnerSamplingRate = view.findViewById(R.id.spinner_sampling_rate)
        switchGSR = view.findViewById(R.id.switch_gsr)
        switchTemperature = view.findViewById(R.id.switch_temperature)
        switchPPG = view.findViewById(R.id.switch_ppg)
        switchAccelerometer = view.findViewById(R.id.switch_accelerometer)
        spinnerGSRRange = view.findViewById(R.id.spinner_gsr_range)
        switchFilter = view.findViewById(R.id.switch_filter)
        switchCalibration = view.findViewById(R.id.switch_calibration)
        btnApplySettings = view.findViewById(R.id.btn_apply_settings)
        btnResetDefaults = view.findViewById(R.id.btn_reset_defaults)
        tvCurrentConfig = view.findViewById(R.id.tv_current_config)
        
        setupSamplingRateSpinner()
        setupGSRRangeSpinner()
        setupListeners()
    }

    private fun setupPreferences() {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun setupSamplingRateSpinner() {
        val samplingRates = arrayOf(
            "1 Hz", "10.24 Hz", "51.2 Hz", "102.4 Hz", 
            "128 Hz", "204.8 Hz", "256 Hz", "512 Hz", "1024 Hz"
        )
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, samplingRates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSamplingRate.adapter = adapter
        spinnerSamplingRate.setSelection(4)
    }

    private fun setupGSRRangeSpinner() {
        val gsrRanges = arrayOf("40kΩ (High Sensitivity)", "287kΩ (Medium)", "1MΩ (Low Sensitivity)")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, gsrRanges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGSRRange.adapter = adapter
        spinnerGSRRange.setSelection(1)
    }

    private fun setupListeners() {

        val configChangeListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                notifyConfigurationChanged()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerSamplingRate.onItemSelectedListener = configChangeListener
        spinnerGSRRange.onItemSelectedListener = configChangeListener
        
        val switchChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            notifyConfigurationChanged()
        }
        
        switchGSR.setOnCheckedChangeListener(switchChangeListener)
        switchTemperature.setOnCheckedChangeListener(switchChangeListener)
        switchPPG.setOnCheckedChangeListener(switchChangeListener)
        switchAccelerometer.setOnCheckedChangeListener(switchChangeListener)
        switchFilter.setOnCheckedChangeListener(switchChangeListener)
        switchCalibration.setOnCheckedChangeListener(switchChangeListener)
        
        btnApplySettings.setOnClickListener {
            applyConfiguration()
        }
        
        btnResetDefaults.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun getSamplingRateValue(position: Int): Double {
        return when (position) {
            0 -> Configuration.Shimmer3.SAMPLING_RATE_1HZ
            1 -> Configuration.Shimmer3.SAMPLING_RATE_10HZ
            2 -> Configuration.Shimmer3.SAMPLING_RATE_51HZ
            3 -> Configuration.Shimmer3.SAMPLING_RATE_102HZ
            4 -> Configuration.Shimmer3.SAMPLING_RATE_128HZ
            5 -> Configuration.Shimmer3.SAMPLING_RATE_204HZ
            6 -> Configuration.Shimmer3.SAMPLING_RATE_256HZ
            7 -> Configuration.Shimmer3.SAMPLING_RATE_512HZ
            8 -> Configuration.Shimmer3.SAMPLING_RATE_1024HZ
            else -> Configuration.Shimmer3.SAMPLING_RATE_128HZ
        }
    }

    private fun getSamplingRatePosition(rate: Double): Int {
        return when (rate) {
            Configuration.Shimmer3.SAMPLING_RATE_1HZ -> 0
            Configuration.Shimmer3.SAMPLING_RATE_10HZ -> 1
            Configuration.Shimmer3.SAMPLING_RATE_51HZ -> 2
            Configuration.Shimmer3.SAMPLING_RATE_102HZ -> 3
            Configuration.Shimmer3.SAMPLING_RATE_128HZ -> 4
            Configuration.Shimmer3.SAMPLING_RATE_204HZ -> 5
            Configuration.Shimmer3.SAMPLING_RATE_256HZ -> 6
            Configuration.Shimmer3.SAMPLING_RATE_512HZ -> 7
            Configuration.Shimmer3.SAMPLING_RATE_1024HZ -> 8
            else -> 4
        }
    }

    private fun getCurrentConfiguration(): ShimmerConfiguration {
        return ShimmerConfiguration(
            samplingRate = getSamplingRateValue(spinnerSamplingRate.selectedItemPosition),
            gsrEnabled = switchGSR.isChecked,
            temperatureEnabled = switchTemperature.isChecked,
            ppgEnabled = switchPPG.isChecked,
            accelerometerEnabled = switchAccelerometer.isChecked,
            gsrRange = spinnerGSRRange.selectedItemPosition,
            filterEnabled = switchFilter.isChecked,
            calibrationEnabled = switchCalibration.isChecked
        )
    }

    private fun applyConfiguration() {
        val config = getCurrentConfiguration()
        saveSettings(config)
        
        XLog.i(TAG, "Applying Shimmer configuration: $config")
        configurationListener?.onConfigurationApplied(config)
        
        updateCurrentConfigDisplay()
        Toast.makeText(context, "Shimmer configuration applied", Toast.LENGTH_SHORT).show()
    }

    private fun notifyConfigurationChanged() {
        val config = getCurrentConfiguration()
        configurationListener?.onConfigurationChanged(config)
        updateCurrentConfigDisplay()
    }

    private fun resetToDefaults() {

        spinnerSamplingRate.setSelection(4)
        switchGSR.isChecked = true
        switchTemperature.isChecked = true
        switchPPG.isChecked = false
        switchAccelerometer.isChecked = false
        spinnerGSRRange.setSelection(1)
        switchFilter.isChecked = true
        switchCalibration.isChecked = true
        
        val config = getCurrentConfiguration()
        saveSettings(config)
        
        configurationListener?.onConfigurationReset()
        updateCurrentConfigDisplay()
        
        Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
        XLog.i(TAG, "Shimmer settings reset to defaults")
    }

    private fun saveSettings(config: ShimmerConfiguration) {
        with(sharedPrefs.edit()) {
            putFloat(KEY_SAMPLING_RATE, config.samplingRate.toFloat())
            putBoolean(KEY_GSR_ENABLED, config.gsrEnabled)
            putBoolean(KEY_TEMPERATURE_ENABLED, config.temperatureEnabled)
            putBoolean(KEY_PPG_ENABLED, config.ppgEnabled)
            putBoolean(KEY_ACCEL_ENABLED, config.accelerometerEnabled)
            putInt(KEY_GSR_RANGE, config.gsrRange)
            putBoolean(KEY_FILTER_ENABLED, config.filterEnabled)
            putBoolean(KEY_CALIBRATION_ENABLED, config.calibrationEnabled)
            apply()
        }
        XLog.d(TAG, "Shimmer settings saved: $config")
    }

    private fun loadSavedSettings() {
        val samplingRate = sharedPrefs.getFloat(KEY_SAMPLING_RATE, Configuration.Shimmer3.SAMPLING_RATE_128HZ.toFloat()).toDouble()
        
        spinnerSamplingRate.setSelection(getSamplingRatePosition(samplingRate))
        switchGSR.isChecked = sharedPrefs.getBoolean(KEY_GSR_ENABLED, true)
        switchTemperature.isChecked = sharedPrefs.getBoolean(KEY_TEMPERATURE_ENABLED, true)
        switchPPG.isChecked = sharedPrefs.getBoolean(KEY_PPG_ENABLED, false)
        switchAccelerometer.isChecked = sharedPrefs.getBoolean(KEY_ACCEL_ENABLED, false)
        spinnerGSRRange.setSelection(sharedPrefs.getInt(KEY_GSR_RANGE, 1))
        switchFilter.isChecked = sharedPrefs.getBoolean(KEY_FILTER_ENABLED, true)
        switchCalibration.isChecked = sharedPrefs.getBoolean(KEY_CALIBRATION_ENABLED, true)
        
        XLog.d(TAG, "Shimmer settings loaded from preferences")
    }

    private fun updateCurrentConfigDisplay() {
        val config = getCurrentConfiguration()
        val enabledSensors = mutableListOf<String>()
        
        if (config.gsrEnabled) enabledSensors.add("GSR")
        if (config.temperatureEnabled) enabledSensors.add("Temperature")
        if (config.ppgEnabled) enabledSensors.add("PPG")
        if (config.accelerometerEnabled) enabledSensors.add("Accelerometer")
        
        val sensorsText = if (enabledSensors.isNotEmpty()) {
            enabledSensors.joinToString(", ")
        } else {
            "No sensors enabled"
        }
        
        val configText = """
            Sampling Rate: ${config.samplingRate} Hz
            Enabled Sensors: $sensorsText
            GSR Range: ${getGSRRangeText(config.gsrRange)}
            Processing: ${if (config.filterEnabled) "Filtered" else "Raw"}, ${if (config.calibrationEnabled) "Calibrated" else "Uncalibrated"}
        """.trimIndent()
        
        tvCurrentConfig.text = configText
    }

    private fun getGSRRangeText(rangeIndex: Int): String {
        return when (rangeIndex) {
            0 -> "40kΩ (High Sensitivity)"
            1 -> "287kΩ (Medium Sensitivity)"
            2 -> "1MΩ (Low Sensitivity)"
            else -> "Unknown"
        }
    }

    fun setConfigurationListener(listener: ShimmerConfigurationListener) {
        this.configurationListener = listener
    }

    fun getConfiguration(): ShimmerConfiguration {
        return getCurrentConfiguration()
    }

    fun setConfiguration(config: ShimmerConfiguration) {
        spinnerSamplingRate.setSelection(getSamplingRatePosition(config.samplingRate))
        switchGSR.isChecked = config.gsrEnabled
        switchTemperature.isChecked = config.temperatureEnabled
        switchPPG.isChecked = config.ppgEnabled
        switchAccelerometer.isChecked = config.accelerometerEnabled
        spinnerGSRRange.setSelection(config.gsrRange)
        switchFilter.isChecked = config.filterEnabled
        switchCalibration.isChecked = config.calibrationEnabled
        
        updateCurrentConfigDisplay()
    }

    fun getEnabledSensorsBitmap(): Long {
        var sensorBitmap = 0L
        
        if (switchGSR.isChecked) {
            sensorBitmap = sensorBitmap or Configuration.Shimmer3.SensorMap.GSR.mValue
        }
        
        if (switchTemperature.isChecked) {
            sensorBitmap = sensorBitmap or Configuration.Shimmer3.SensorMap.TEMPERATURE.mValue
        }
        
        if (switchPPG.isChecked) {
            sensorBitmap = sensorBitmap or Configuration.Shimmer3.SensorMap.PPG.mValue
        }
        
        if (switchAccelerometer.isChecked) {
            sensorBitmap = sensorBitmap or Configuration.Shimmer3.SensorMap.ACCEL.mValue
        }
        
        return sensorBitmap
    }
