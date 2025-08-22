package com.multisensor.recording.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.multisensor.recording.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private lateinit var originalPreferences: Map<String, Any?>
    private var hasUnsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        originalPreferences = preferences.all.toMap()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Settings"
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        setupActionButtons()
        setupBackPressedCallback()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    showCancelConfirmationDialog()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupActionButtons() {
        val cancelButton = findViewById<MaterialButton>(R.id.cancelButton)
        val resetButton = findViewById<MaterialButton>(R.id.resetButton)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)

        cancelButton.setOnClickListener {
            if (hasUnsavedChanges) {
                showCancelConfirmationDialog()
            } else {
                finish()
            }
        }

        resetButton.setOnClickListener {
            showResetConfirmationDialog()
        }

        saveButton.setOnClickListener {
            saveAndApplySettings()
        }
    }

    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ ->
                restoreOriginalPreferences()
                finish()
            }
            .setNegativeButton("Continue Editing", null)
            .show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset All Settings?")
            .setMessage("This will reset all settings to their default values. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAndApplySettings() {
        if (validateAllSettings()) {
            hasUnsavedChanges = false
            originalPreferences = preferences.all.toMap()

            Toast.makeText(this, "Settings saved and applied successfully", Toast.LENGTH_SHORT).show()

            notifySettingsChanged()
        }
    }

    private fun validateAllSettings(): Boolean {
        val shimmerMac = preferences.getString("shimmer_mac_address", "")
        val serverIp = preferences.getString("server_ip", "")
        val serverPort = preferences.getString("server_port", "")
        val jsonServerPort = preferences.getString("json_server_port", "")
        val emissivity = preferences.getString("thermal_emissivity", "")

        if (!shimmerMac.isNullOrEmpty() && !isValidMacAddress(shimmerMac)) {
            Toast.makeText(this, "Invalid Shimmer MAC address format", Toast.LENGTH_LONG).show()
            return false
        }

        if (!serverIp.isNullOrEmpty() && !isValidIpAddress(serverIp)) {
            Toast.makeText(this, "Invalid server IP address format", Toast.LENGTH_LONG).show()
            return false
        }

        try {
            serverPort?.toInt()?.let { port ->
                if (port !in 1024..65535) {
                    Toast.makeText(this, "Server port must be between 1024 and 65535", Toast.LENGTH_LONG).show()
                    return false
                }
            }

            jsonServerPort?.toInt()?.let { port ->
                if (port !in 1024..65535) {
                    Toast.makeText(this, "JSON server port must be between 1024 and 65535", Toast.LENGTH_LONG).show()
                    return false
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid port number format", Toast.LENGTH_LONG).show()
            return false
        }

        try {
            emissivity?.toFloat()?.let { value ->
                if (value !in 0.1f..1.0f) {
                    Toast.makeText(this, "Emissivity must be between 0.1 and 1.0", Toast.LENGTH_LONG).show()
                    return false
                }
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid emissivity value", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun restoreOriginalPreferences() {
        val editor = preferences.edit()
        editor.clear()

        originalPreferences.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }

        editor.apply()
        hasUnsavedChanges = false
    }

    private fun resetToDefaults() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()

        recreate()

        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }

    private fun notifySettingsChanged() {
        sendBroadcast(android.content.Intent("com.multisensor.recording.SETTINGS_CHANGED"))
    }

    fun markAsChanged() {
        hasUnsavedChanges = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                if (hasUnsavedChanges) {
                    showCancelConfirmationDialog()
                } else {
                    finish()
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            setupPreferenceListeners()
        }

        private fun setupPreferenceListeners() {
            findPreference<androidx.preference.EditTextPreference>("shimmer_mac_address")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val macAddress = newValue as String
                    if (isValidMacAddress(macAddress)) {
                        summary = "MAC Address: $macAddress"
                        (activity as? SettingsActivity)?.markAsChanged()
                        true
                    } else {
                        Toast
                            .makeText(
                                context,
                                "Invalid MAC address format. Use format: XX:XX:XX:XX:XX:XX",
                                Toast.LENGTH_LONG,
                            ).show()
                        false
                    }
                }

                text?.let { summary = "MAC Address: $it" }
            }

            findPreference<androidx.preference.ListPreference>("video_resolution")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val resolution = newValue as String
                    summary = "Resolution: $resolution"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let { summary = "Resolution: $it" }
            }

            findPreference<androidx.preference.ListPreference>("frame_rate")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val frameRate = newValue as String
                    summary = "Frame Rate: ${frameRate}fps"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let { summary = "Frame Rate: ${it}fps" }
            }

            findPreference<androidx.preference.ListPreference>("thermal_frame_rate")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val frameRate = newValue as String
                    summary = "Thermal Frame Rate: ${frameRate}fps"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let { summary = "Thermal Frame Rate: ${it}fps" }
            }

            findPreference<androidx.preference.ListPreference>("thermal_color_palette")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val palette = newValue as String
                    val displayName = when (palette) {
                        "iron" -> "Iron"
                        "rainbow" -> "Rainbow"
                        "grayscale" -> "Grayscale"
                        "hot_metal" -> "Hot Metal"
                        "arctic" -> "Arctic"
                        "medical" -> "Medical"
                        else -> palette
                    }
                    summary = "Color Palette: $displayName"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let {
                    val displayName = when (it) {
                        "iron" -> "Iron"
                        "rainbow" -> "Rainbow"
                        "grayscale" -> "Grayscale"
                        "hot_metal" -> "Hot Metal"
                        "arctic" -> "Arctic"
                        "medical" -> "Medical"
                        else -> it
                    }
                    summary = "Color Palette: $displayName"
                }
            }

            findPreference<androidx.preference.ListPreference>("thermal_temperature_range")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val range = newValue as String
                    val displayName = when (range) {
                        "auto" -> "Auto Range"
                        "-20_150" -> "-20°C to 150°C"
                        "0_100" -> "0°C to 100°C"
                        "15_45" -> "15°C to 45°C (Human Body)"
                        "20_40" -> "20°C to 40°C (Room Temp)"
                        "custom" -> "Custom Range"
                        else -> range
                    }
                    summary = "Temperature Range: $displayName"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let {
                    val displayName = when (it) {
                        "auto" -> "Auto Range"
                        "-20_150" -> "-20°C to 150°C"
                        "0_100" -> "0°C to 100°C"
                        "15_45" -> "15°C to 45°C (Human Body)"
                        "20_40" -> "20°C to 40°C (Room Temp)"
                        "custom" -> "Custom Range"
                        else -> it
                    }
                    summary = "Temperature Range: $displayName"
                }
            }

            findPreference<androidx.preference.EditTextPreference>("thermal_emissivity")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val emissivityStr = newValue as String
                    try {
                        val emissivity = emissivityStr.toFloat()
                        if (emissivity in 0.1f..1.0f) {
                            summary = "Emissivity: $emissivity"
                            (activity as? SettingsActivity)?.markAsChanged()
                            true
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    "Emissivity must be between 0.1 and 1.0",
                                    Toast.LENGTH_LONG,
                                ).show()
                            false
                        }
                    } catch (e: NumberFormatException) {
                        Toast
                            .makeText(
                                context,
                                "Invalid emissivity value",
                                Toast.LENGTH_LONG,
                            ).show()
                        false
                    }
                }

                text?.let { summary = "Emissivity: $it" }
            }

            findPreference<androidx.preference.ListPreference>("thermal_temperature_units")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val units = newValue as String
                    val displayName = when (units) {
                        "celsius" -> "Celsius (°C)"
                        "fahrenheit" -> "Fahrenheit (°F)"
                        "kelvin" -> "Kelvin (K)"
                        else -> units
                    }
                    summary = "Temperature Units: $displayName"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let {
                    val displayName = when (it) {
                        "celsius" -> "Celsius (°C)"
                        "fahrenheit" -> "Fahrenheit (°F)"
                        "kelvin" -> "Kelvin (K)"
                        else -> it
                    }
                    summary = "Temperature Units: $displayName"
                }
            }

            findPreference<androidx.preference.ListPreference>("thermal_data_format")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val format = newValue as String
                    val displayName = when (format) {
                        "radiometric" -> "Radiometric (Full Temperature Data)"
                        "visual" -> "Visual (Image Only)"
                        "combined" -> "Combined (Image + Temperature)"
                        "raw" -> "Raw (Sensor Data)"
                        else -> format
                    }
                    summary = "Data Format: $displayName"
                    (activity as? SettingsActivity)?.markAsChanged()
                    true
                }

                value?.let {
                    val displayName = when (it) {
                        "radiometric" -> "Radiometric (Full Temperature Data)"
                        "visual" -> "Visual (Image Only)"
                        "combined" -> "Combined (Image + Temperature)"
                        "raw" -> "Raw (Sensor Data)"
                        else -> it
                    }
                    summary = "Data Format: $displayName"
                }
            }

            findPreference<androidx.preference.EditTextPreference>("server_ip")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val ipAddress = newValue as String
                    if (isValidIpAddress(ipAddress)) {
                        summary = "Server IP: $ipAddress"
                        (activity as? SettingsActivity)?.markAsChanged()
                        true
                    } else {
                        Toast
                            .makeText(
                                context,
                                "Invalid IP address format",
                                Toast.LENGTH_LONG,
                            ).show()
                        false
                    }
                }

                text?.let { summary = "Server IP: $it" }
            }

            findPreference<androidx.preference.EditTextPreference>("server_port")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val portStr = newValue as String
                    try {
                        val port = portStr.toInt()
                        if (port in 1024..65535) {
                            summary = "Server Port: $port"
                            (activity as? SettingsActivity)?.markAsChanged()
                            true
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    "Port must be between 1024 and 65535",
                                    Toast.LENGTH_LONG,
                                ).show()
                            false
                        }
                    } catch (e: NumberFormatException) {
                        Toast
                            .makeText(
                                context,
                                "Invalid port number",
                                Toast.LENGTH_LONG,
                            ).show()
                        false
                    }
                }

                text?.let { summary = "Server Port: $it" }
            }

            findPreference<androidx.preference.EditTextPreference>("json_server_port")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    val portStr = newValue as String
                    try {
                        val port = portStr.toInt()
                        if (port in 1024..65535) {
                            summary = "JSON Server Port: $port"
                            (activity as? SettingsActivity)?.markAsChanged()
                            true
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    "Port must be between 1024 and 65535",
                                    Toast.LENGTH_LONG,
                                ).show()
                            false
                        }
                    } catch (e: NumberFormatException) {
                        Toast
                            .makeText(
                                context,
                                "Invalid port number",
                                Toast.LENGTH_LONG,
                            ).show()
                        false
                    }
                }

                text?.let { summary = "JSON Server Port: $it" }
            }

            preferenceScreen.let { screen ->
                for (i in 0 until screen.preferenceCount) {
                    setupGenericChangeListener(screen.getPreference(i))
                }
            }
        }

        private fun setupGenericChangeListener(preference: androidx.preference.Preference) {
            when (preference) {
                is androidx.preference.PreferenceCategory -> {
                    for (i in 0 until preference.preferenceCount) {
                        setupGenericChangeListener(preference.getPreference(i))
                    }
                }

                is androidx.preference.SwitchPreferenceCompat -> {
                    preference.setOnPreferenceChangeListener { _, _ ->
                        (activity as? SettingsActivity)?.markAsChanged()
                        true
                    }
                }

                is androidx.preference.ListPreference -> {
                    if (preference.onPreferenceChangeListener == null) {
                        preference.setOnPreferenceChangeListener { _, _ ->
                            (activity as? SettingsActivity)?.markAsChanged()
                            true
                        }
                    }
                }

                is androidx.preference.EditTextPreference -> {
                    if (preference.onPreferenceChangeListener == null) {
                        preference.setOnPreferenceChangeListener { _, _ ->
                            (activity as? SettingsActivity)?.markAsChanged()
                            true
                        }
                    }
                }
            }
        }

        private fun isValidMacAddress(macAddress: String): Boolean {
            val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
            return macAddress.matches(macPattern.toRegex())
        }

        private fun isValidIpAddress(ipAddress: String): Boolean {
            val parts = ipAddress.split(".")
            if (parts.size != 4) return false

            return parts.all { part ->
                try {
                    val num = part.toInt()
                    num in 0..255
                } catch (e: NumberFormatException) {
                    false
                }
            }
        }
    }

    companion object {
        fun isValidMacAddress(macAddress: String): Boolean {
            val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"
            return macAddress.matches(macPattern.toRegex())
        }

        fun isValidIpAddress(ipAddress: String): Boolean {
            val parts = ipAddress.split(".")
            if (parts.size != 4) return false

            return parts.all { part ->
                try {
                    val num = part.toInt()
                    num in 0..255
                } catch (e: NumberFormatException) {
                    false
                }
            }
        }
    }
}
