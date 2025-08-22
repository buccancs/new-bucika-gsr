package com.multisensor.recording.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.multisensor.recording.databinding.ActivityDevicesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DevicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDevicesBinding
    private lateinit var viewModel: DevicesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            viewModel = ViewModelProvider(this)[DevicesViewModel::class.java]
        } catch (e: SecurityException) {
            showError("Permission error initializing devices: ${e.message}")
            return
        } catch (e: IllegalStateException) {
            showError("Invalid state initializing devices: ${e.message}")
            return
        } catch (e: RuntimeException) {
            showError("Runtime error initializing devices: ${e.message}")
            return
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Device Management"
            subtitle = "Monitor and configure all devices"
        }

        setupDeviceControls()
        setupConfigurationButtons()
        setupDiagnosticButtons()
    }

    private fun setupDeviceControls() {
        binding.pcConnectButton.setOnClickListener {
            try {
                val currentState = viewModel.uiState.value
                if (currentState.isPcConnected) {
                    viewModel.disconnectPc()
                } else {
                    viewModel.connectPc()
                }
            } catch (e: IllegalStateException) {
                showError("Invalid PC connection state: ${e.message}")
            } catch (e: RuntimeException) {
                showError("PC connection failed: ${e.message}")
            }
        }

        binding.shimmerConnectButton.setOnClickListener {
            try {
                val currentState = viewModel.uiState.value
                if (currentState.isShimmerConnected) {
                    viewModel.disconnectShimmer()
                } else {
                    viewModel.connectShimmer()
                }
            } catch (e: Exception) {
                showError("Shimmer connection failed: ${e.message}")
            }
        }

        binding.thermalConnectButton.setOnClickListener {
            try {
                val currentState = viewModel.uiState.value
                if (currentState.isThermalConnected) {
                    viewModel.disconnectThermal()
                } else {
                    viewModel.connectThermal()
                }
            } catch (e: Exception) {
                showError("Thermal connection failed: ${e.message}")
            }
        }

        binding.networkConnectButton.setOnClickListener {
            try {
                val currentState = viewModel.uiState.value
                if (currentState.isNetworkConnected) {
                    viewModel.disconnectNetwork()
                } else {
                    viewModel.connectNetwork()
                }
            } catch (e: Exception) {
                showError("Network connection failed: ${e.message}")
            }
        }

        binding.refreshAllButton.setOnClickListener {
            try {
                viewModel.refreshAllDevices()
                showMessage("Refreshing all devices...")
            } catch (e: Exception) {
                showError("Refresh failed: ${e.message}")
            }
        }
    }

    private fun setupConfigurationButtons() {
        binding.shimmerConfigButton.setOnClickListener {
            try {
                val intent = Intent(this, ShimmerConfigActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                showError("Failed to open Shimmer config: ${e.message}")
            }
        }

        binding.networkConfigButton.setOnClickListener {
            try {
                val intent = Intent(this, NetworkConfigActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                showError("Failed to open network config: ${e.message}")
            }
        }

        binding.generalSettingsButton.setOnClickListener {
            try {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                showError("Failed to open settings: ${e.message}")
            }
        }
    }

    private fun setupDiagnosticButtons() {
        binding.testPcButton.setOnClickListener {
            try {
                viewModel.testPcConnection()
                showMessage("Testing PC connection...")
            } catch (e: Exception) {
                showError("PC test failed: ${e.message}")
            }
        }

        binding.testShimmerButton.setOnClickListener {
            try {
                viewModel.testShimmerConnection()
                showMessage("Testing Shimmer connection...")
            } catch (e: Exception) {
                showError("Shimmer test failed: ${e.message}")
            }
        }

        binding.testThermalButton.setOnClickListener {
            try {
                viewModel.testThermalConnection()
                showMessage("Testing thermal connection...")
            } catch (e: Exception) {
                showError("Thermal test failed: ${e.message}")
            }
        }

        binding.testNetworkButton.setOnClickListener {
            try {
                viewModel.testNetworkConnection()
                showMessage("Testing network connection...")
            } catch (e: Exception) {
                showError("Network test failed: ${e.message}")
            }
        }
    }

    private fun observeViewModel() {
        try {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.uiState.collect { uiState ->
                        updateUI(uiState)
                    }
                }
            }
        } catch (e: Exception) {
            showError("Failed to setup monitoring: ${e.message}")
        }
    }

    private fun updateUI(uiState: DevicesUiState) {
        updateConnectionStatus(uiState)
        updateDeviceDetails(uiState)
        updateConnectionButtons(uiState)
        updateTestResults(uiState)
    }

    private fun updateConnectionStatus(uiState: DevicesUiState) {
        updateConnectionIndicator(
            binding.pcStatusIndicator,
            binding.pcStatusText,
            uiState.isPcConnected,
            "PC Connected",
            "PC Disconnected"
        )

        updateConnectionIndicator(
            binding.shimmerStatusIndicator,
            binding.shimmerStatusText,
            uiState.isShimmerConnected,
            "Shimmer Connected",
            "Shimmer Disconnected"
        )

        updateConnectionIndicator(
            binding.thermalStatusIndicator,
            binding.thermalStatusText,
            uiState.isThermalConnected,
            "Thermal Connected",
            "Thermal Disconnected"
        )

        updateConnectionIndicator(
            binding.networkStatusIndicator,
            binding.networkStatusText,
            uiState.isNetworkConnected,
            "Network Connected",
            "Network Disconnected"
        )

        updateConnectionIndicator(
            binding.gsrStatusIndicator,
            binding.gsrStatusText,
            uiState.isGsrConnected,
            "GSR Connected",
            "GSR Disconnected"
        )
    }

    private fun updateConnectionIndicator(
        indicator: android.widget.ImageView,
        textView: android.widget.TextView,
        isConnected: Boolean,
        connectedText: String,
        disconnectedText: String
    ) {
        if (isConnected) {
            indicator.setImageResource(com.multisensor.recording.R.drawable.ic_check_circle)
            indicator.setColorFilter(getColor(com.multisensor.recording.R.color.md_theme_primary))
            textView.text = connectedText
            textView.setTextColor(getColor(com.multisensor.recording.R.color.md_theme_primary))
        } else {
            indicator.setImageResource(com.multisensor.recording.R.drawable.ic_close)
            indicator.setColorFilter(getColor(com.multisensor.recording.R.color.md_theme_error))
            textView.text = disconnectedText
            textView.setTextColor(getColor(com.multisensor.recording.R.color.md_theme_error))
        }
    }

    private fun updateDeviceDetails(uiState: DevicesUiState) {
        binding.pcDetailsText.text = buildString {
            if (uiState.isPcConnected) {
                appendLine("IP Address: ${uiState.pcIpAddress}")
                appendLine("Port: ${uiState.pcPort}")
                appendLine("Status: ${uiState.pcConnectionStatus}")
                appendLine("Last Seen: ${uiState.pcLastSeen}")
            } else {
                append("Not connected")
            }
        }

        binding.shimmerDetailsText.text = buildString {
            if (uiState.isShimmerConnected) {
                appendLine("MAC Address: ${uiState.shimmerMacAddress}")
                appendLine("Battery: ${uiState.shimmerBatteryLevel}%")
                appendLine("Sensors: ${uiState.shimmerActiveSensors}")
                appendLine("Sample Rate: ${uiState.shimmerSampleRate}Hz")
                appendLine("Last Seen: ${uiState.shimmerLastSeen}")
            } else {
                append("Not connected")
            }
        }

        binding.thermalDetailsText.text = buildString {
            if (uiState.isThermalConnected) {
                appendLine("Model: ${uiState.thermalCameraModel}")
                appendLine("Temperature: ${uiState.thermalCurrentTemp}°C")
                appendLine("Resolution: ${uiState.thermalResolution}")
                appendLine("Frame Rate: ${uiState.thermalFrameRate}fps")
                appendLine("Last Seen: ${uiState.thermalLastSeen}")
            } else {
                append("Not connected")
            }
        }

        binding.networkDetailsText.text = buildString {
            if (uiState.isNetworkConnected) {
                appendLine("WiFi SSID: ${uiState.networkSsid}")
                appendLine("IP Address: ${uiState.networkIpAddress}")
                appendLine("Signal: ${uiState.networkSignalStrength}%")
                appendLine("Type: ${uiState.networkType}")
            } else {
                append("Not connected")
            }
        }

        binding.gsrDetailsText.text = buildString {
            if (uiState.isGsrConnected) {
                appendLine("Current Value: ${uiState.gsrCurrentValue}")
                appendLine("Range: ${uiState.gsrRange}")
                appendLine("Sample Rate: ${uiState.gsrSampleRate}Hz")
                appendLine("Last Reading: ${uiState.gsrLastReading}")
            } else {
                append("Not connected")
            }
        }
    }

    private fun updateConnectionButtons(uiState: DevicesUiState) {
        binding.pcConnectButton.text = if (uiState.isPcConnected) "Disconnect PC" else "Connect PC"
        binding.pcConnectButton.isEnabled = !uiState.isConnecting

        binding.shimmerConnectButton.text = if (uiState.isShimmerConnected) "Disconnect Shimmer" else "Connect Shimmer"
        binding.shimmerConnectButton.isEnabled = !uiState.isConnecting

        binding.thermalConnectButton.text = if (uiState.isThermalConnected) "Disconnect Thermal" else "Connect Thermal"
        binding.thermalConnectButton.isEnabled = !uiState.isConnecting

        binding.networkConnectButton.text = if (uiState.isNetworkConnected) "Disconnect Network" else "Connect Network"
        binding.networkConnectButton.isEnabled = !uiState.isConnecting

        binding.shimmerConfigButton.isEnabled = !uiState.isConnecting
        binding.networkConfigButton.isEnabled = !uiState.isConnecting
        binding.generalSettingsButton.isEnabled = !uiState.isConnecting

        binding.refreshAllButton.isEnabled = !uiState.isConnecting

        binding.testPcButton.isEnabled = uiState.isPcConnected && !uiState.isTesting
        binding.testShimmerButton.isEnabled = uiState.isShimmerConnected && !uiState.isTesting
        binding.testThermalButton.isEnabled = uiState.isThermalConnected && !uiState.isTesting
        binding.testNetworkButton.isEnabled = uiState.isNetworkConnected && !uiState.isTesting
    }

    private fun updateTestResults(uiState: DevicesUiState) {
        if (uiState.testResults.isNotEmpty()) {
            binding.testResultsText.text = "Test Results:\n${uiState.testResults.joinToString("\n")}"
        } else {
            binding.testResultsText.text = "No test results available"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(error: String) {
        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        android.util.Log.e("DevicesActivity", error)
    }
}
