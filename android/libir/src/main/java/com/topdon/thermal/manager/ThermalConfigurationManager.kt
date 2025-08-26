package com.topdon.thermal.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.IRCMDType
import com.topdon.lib.core.common.SharedManager
import com.topdon.thermal.repository.ConfigRepository
import com.topdon.thermal.utils.IRConfigData
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ThermalConfigurationManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private val configRepository = ConfigRepository()
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private var currentEmissivity: Float = DEFAULT_EMISSIVITY
    private var currentTemperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
    private var currentPseudoColorPalette: String = DEFAULT_PALETTE
    private var isAutoShutterEnabled: Boolean = true
    private var currentMeasurementMode: MeasurementMode = MeasurementMode.TEMPERATURE
    
    enum class TemperatureUnit(val symbol: String, val factor: Float, val offset: Float) {
        CELSIUS("°C", 1.0f, 0.0f),
        FAHRENHEIT("°F", 1.8f, 32.0f),
        KELVIN("K", 1.0f, 273.15f)
    }
    
    enum class MeasurementMode {
        TEMPERATURE,
        OBSERVATION
    }
    
    fun initializeConfiguration() {
        lifecycleOwner.lifecycleScope.launch {
            try {
                loadSavedSettings()
                applyInitialConfiguration()
                Log.d(TAG, "Configuration manager initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Configuration initialization failed", e)
                applyDefaultConfiguration()
            }
        }
    }
    
    private suspend fun loadSavedSettings() = withContext(Dispatchers.IO) {
        currentEmissivity = sharedPreferences.getFloat(KEY_EMISSIVITY, DEFAULT_EMISSIVITY)
        currentTemperatureUnit = TemperatureUnit.values()[
            sharedPreferences.getInt(KEY_TEMP_UNIT, TemperatureUnit.CELSIUS.ordinal)
        ]
        currentPseudoColorPalette = sharedPreferences.getString(KEY_PALETTE, DEFAULT_PALETTE) ?: DEFAULT_PALETTE
        isAutoShutterEnabled = sharedPreferences.getBoolean(KEY_AUTO_SHUTTER, true)
        currentMeasurementMode = MeasurementMode.values()[
            sharedPreferences.getInt(KEY_MEASUREMENT_MODE, MeasurementMode.TEMPERATURE.ordinal)
        ]
        
        Log.d(TAG, "Settings loaded - Emissivity: $currentEmissivity, Unit: $currentTemperatureUnit")
    }
    
    private suspend fun applyInitialConfiguration() {
        applyEmissivity(currentEmissivity)
        applyTemperatureUnit(currentTemperatureUnit)
        applyPseudoColorPalette(currentPseudoColorPalette)
        applyAutoShutterSetting(isAutoShutterEnabled)
        applyMeasurementMode(currentMeasurementMode)
    }
    
    private suspend fun applyDefaultConfiguration() {
        currentEmissivity = DEFAULT_EMISSIVITY
        currentTemperatureUnit = TemperatureUnit.CELSIUS
        currentPseudoColorPalette = DEFAULT_PALETTE
        isAutoShutterEnabled = true
        currentMeasurementMode = MeasurementMode.TEMPERATURE
        
        applyInitialConfiguration()
        saveCurrentSettings()
    }
    
    fun setEmissivity(emissivity: Float) {
        if (emissivity !in 0.01f..1.0f) {
            Log.w(TAG, "Invalid emissivity value: $emissivity. Must be between 0.01 and 1.0")
            return
        }
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                currentEmissivity = emissivity
                applyEmissivity(emissivity)
                saveEmissivity(emissivity)
                Log.d(TAG, "Emissivity set to: $emissivity")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set emissivity", e)
            }
        }
    }
    
    private suspend fun applyEmissivity(emissivity: Float) = withContext(Dispatchers.Main) {
        IRCMD.getInstance()?.let { ircmd ->
            val emissivityInt = (emissivity * 100).toInt()
            ircmd.setParam(IRCMDType.EMISSIVITY, emissivityInt)
        }
    }
    
    fun setTemperatureUnit(unit: TemperatureUnit) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                currentTemperatureUnit = unit
                applyTemperatureUnit(unit)
                saveTemperatureUnit(unit)
                Log.d(TAG, "Temperature unit set to: ${unit.symbol}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set temperature unit", e)
            }
        }
    }
    
    private suspend fun applyTemperatureUnit(unit: TemperatureUnit) = withContext(Dispatchers.Main) {

        SharedManager.setTemperatureUnit(context, unit.ordinal)
    }
    
    fun convertTemperature(value: Float, fromUnit: TemperatureUnit, toUnit: TemperatureUnit): Float {
        if (fromUnit == toUnit) return value
        
        val celsiusValue = when (fromUnit) {
            TemperatureUnit.CELSIUS -> value
            TemperatureUnit.FAHRENHEIT -> (value - 32.0f) / 1.8f
            TemperatureUnit.KELVIN -> value - 273.15f
        }
        
        return when (toUnit) {
            TemperatureUnit.CELSIUS -> celsiusValue
            TemperatureUnit.FAHRENHEIT -> celsiusValue * 1.8f + 32.0f
            TemperatureUnit.KELVIN -> celsiusValue + 273.15f
        }
    }
    
    fun setPseudoColorPalette(palette: String) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                currentPseudoColorPalette = palette
                applyPseudoColorPalette(palette)
                savePseudoColorPalette(palette)
                Log.d(TAG, "Pseudo-color palette set to: $palette")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set pseudo-color palette", e)
            }
        }
    }
    
    private suspend fun applyPseudoColorPalette(palette: String) = withContext(Dispatchers.Main) {
        IRCMD.getInstance()?.let { ircmd ->
            val paletteIndex = getPaletteIndex(palette)
            ircmd.setParam(IRCMDType.PSEUDO_COLOR, paletteIndex)
        }
    }
    
    private fun getPaletteIndex(palette: String): Int {
        return when (palette.lowercase()) {
            "iron" -> 0
            "rainbow" -> 1
            "arctic" -> 2
            "hotmetal" -> 3
            "medical" -> 4
            "grayscale" -> 5
            else -> 0
        }
    }
    
    fun setAutoShutterEnabled(enabled: Boolean) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                isAutoShutterEnabled = enabled
                applyAutoShutterSetting(enabled)
                saveAutoShutterSetting(enabled)
                Log.d(TAG, "Auto shutter enabled: $enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set auto shutter", e)
            }
        }
    }
    
    private suspend fun applyAutoShutterSetting(enabled: Boolean) = withContext(Dispatchers.Main) {
        IRCMD.getInstance()?.setParam(IRCMDType.AUTO_SHUTTER, if (enabled) 1 else 0)
    }
    
    fun setMeasurementMode(mode: MeasurementMode) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                currentMeasurementMode = mode
                applyMeasurementMode(mode)
                saveMeasurementMode(mode)
                Log.d(TAG, "Measurement mode set to: $mode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set measurement mode", e)
            }
        }
    }
    
    private suspend fun applyMeasurementMode(mode: MeasurementMode) = withContext(Dispatchers.Main) {
        val modeValue = when (mode) {
            MeasurementMode.TEMPERATURE -> 1
            MeasurementMode.OBSERVATION -> 0
        }
        IRCMD.getInstance()?.setParam(IRCMDType.MEASUREMENT_MODE, modeValue)
    }
    
    private fun saveCurrentSettings() {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            sharedPreferences.edit().apply {
                putFloat(KEY_EMISSIVITY, currentEmissivity)
                putInt(KEY_TEMP_UNIT, currentTemperatureUnit.ordinal)
                putString(KEY_PALETTE, currentPseudoColorPalette)
                putBoolean(KEY_AUTO_SHUTTER, isAutoShutterEnabled)
                putInt(KEY_MEASUREMENT_MODE, currentMeasurementMode.ordinal)
                apply()
            }
        }
    }
    
    private fun saveEmissivity(emissivity: Float) {
        sharedPreferences.edit().putFloat(KEY_EMISSIVITY, emissivity).apply()
    }
    
    private fun saveTemperatureUnit(unit: TemperatureUnit) {
        sharedPreferences.edit().putInt(KEY_TEMP_UNIT, unit.ordinal).apply()
    }
    
    private fun savePseudoColorPalette(palette: String) {
        sharedPreferences.edit().putString(KEY_PALETTE, palette).apply()
    }
    
    private fun saveAutoShutterSetting(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_SHUTTER, enabled).apply()
    }
    
    private fun saveMeasurementMode(mode: MeasurementMode) {
        sharedPreferences.edit().putInt(KEY_MEASUREMENT_MODE, mode.ordinal).apply()
    }
    
    fun getCurrentConfiguration(): ConfigurationState {
        return ConfigurationState(
            emissivity = currentEmissivity,
            temperatureUnit = currentTemperatureUnit,
            pseudoColorPalette = currentPseudoColorPalette,
            isAutoShutterEnabled = isAutoShutterEnabled,
            measurementMode = currentMeasurementMode
        )
    }
    
    fun resetToDefaults() {
        lifecycleOwner.lifecycleScope.launch {
            applyDefaultConfiguration()
            Log.d(TAG, "Configuration reset to defaults")
        }
    }
    
    data class ConfigurationState(
        val emissivity: Float,
        val temperatureUnit: TemperatureUnit,
        val pseudoColorPalette: String,
        val isAutoShutterEnabled: Boolean,
        val measurementMode: MeasurementMode
    )
    
    companion object {
        private const val TAG = "ThermalConfigManager"
        private const val PREFS_NAME = "thermal_config_prefs"
        
        private const val KEY_EMISSIVITY = "emissivity"
        private const val KEY_TEMP_UNIT = "temperature_unit"
        private const val KEY_PALETTE = "pseudo_color_palette"
        private const val KEY_AUTO_SHUTTER = "auto_shutter_enabled"
        private const val KEY_MEASUREMENT_MODE = "measurement_mode"
        
        private const val DEFAULT_EMISSIVITY = 0.95f
        private const val DEFAULT_PALETTE = "iron"
    }
