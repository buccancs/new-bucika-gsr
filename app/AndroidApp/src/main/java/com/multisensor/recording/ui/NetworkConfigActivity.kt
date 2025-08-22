package com.multisensor.recording.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.multisensor.recording.R
import com.multisensor.recording.network.NetworkConfiguration
import com.multisensor.recording.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NetworkConfigActivity : AppCompatActivity() {
    @Inject
    lateinit var networkConfiguration: NetworkConfiguration

    @Inject
    lateinit var logger: Logger

    private lateinit var serverIpEditText: EditText
    private lateinit var legacyPortEditText: EditText
    private lateinit var jsonPortEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_config)

        initializeViews()
        loadCurrentConfiguration()
        setupClickListeners()

        logger.info("NetworkConfigActivity created")
    }

    private fun initializeViews() {
        serverIpEditText = findViewById(R.id.edit_server_ip)
        legacyPortEditText = findViewById(R.id.edit_legacy_port)
        jsonPortEditText = findViewById(R.id.edit_json_port)
        saveButton = findViewById(R.id.btn_save_config)
        resetButton = findViewById(R.id.btn_reset_config)
    }

    private fun loadCurrentConfiguration() {
        val config = networkConfiguration.getServerConfiguration()

        serverIpEditText.setText(config.serverIp)
        legacyPortEditText.setText(config.legacyPort.toString())
        jsonPortEditText.setText(config.jsonPort.toString())

        logger.debug("Loaded current configuration: ${networkConfiguration.getConfigurationSummary()}")
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveConfiguration()
        }

        resetButton.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun saveConfiguration() {
        try {
            val serverIp = serverIpEditText.text.toString().trim()
            val legacyPortText = legacyPortEditText.text.toString().trim()
            val jsonPortText = jsonPortEditText.text.toString().trim()

            if (serverIp.isEmpty()) {
                showError("Server IP cannot be empty")
                return
            }

            if (!NetworkConfiguration.isValidIpAddress(serverIp)) {
                showError("Invalid IP address format")
                return
            }

            val legacyPort = legacyPortText.toIntOrNull()
            if (legacyPort == null || !NetworkConfiguration.isValidPort(legacyPort)) {
                showError("Legacy port must be between 1024 and 65535")
                return
            }

            val jsonPort = jsonPortText.toIntOrNull()
            if (jsonPort == null || !NetworkConfiguration.isValidPort(jsonPort)) {
                showError("JSON port must be between 1024 and 65535")
                return
            }

            if (legacyPort == jsonPort) {
                showError("Legacy and JSON ports must be different")
                return
            }

            networkConfiguration.setServerIp(serverIp)
            networkConfiguration.setLegacyPort(legacyPort)
            networkConfiguration.setJsonPort(jsonPort)

            logger.info("Network configuration saved: ${networkConfiguration.getConfigurationSummary()}")

            Toast.makeText(this, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            logger.error("Failed to save network configuration", e)
            showError("Failed to save configuration: ${e.message}")
        }
    }

    private fun resetToDefaults() {
        try {
            networkConfiguration.resetToDefaults()
            loadCurrentConfiguration()

            logger.info("Network configuration reset to defaults")
            Toast.makeText(this, "Configuration reset to defaults", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            logger.error("Failed to reset network configuration", e)
            showError("Failed to reset configuration: ${e.message}")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
