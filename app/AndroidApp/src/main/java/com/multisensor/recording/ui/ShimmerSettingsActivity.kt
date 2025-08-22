package com.multisensor.recording.ui
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.multisensor.recording.R
import com.multisensor.recording.recording.DeviceConfiguration.SensorChannel
import com.multisensor.recording.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class ShimmerSettingsActivity : AppCompatActivity() {
    private val viewModel: ShimmerConfigViewModel by viewModels()
    private lateinit var deviceStatusIcon: ImageView
    private lateinit var connectionStatusChip: Chip
    private lateinit var batteryLevelText: TextView
    private lateinit var batteryProgressBar: ProgressBar
    private lateinit var signalStrengthText: TextView
    private lateinit var signalProgressBar: ProgressBar
    private lateinit var samplingRateSpinner: Spinner
    private lateinit var sensorCheckboxes: Map<SensorChannel, CheckBox>
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var scanButton: Button
    private lateinit var deviceListView: ListView
    private lateinit var configurationPresetSpinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var selectDeviceButton: Button
    private lateinit var crcSpinner: Spinner
    private lateinit var deviceInfoText: TextView
    private lateinit var gsrRangeSpinner: Spinner
    private lateinit var accelRangeSpinner: Spinner
    @Inject
    lateinit var logger: Logger
    private val bluetoothPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 1001
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer_settings)
        setupToolbar()
        initializeViews()
        setupClickListeners()
        setupSpinners()
        setupSensorCheckboxes()
        observeViewModelState()
        checkBluetoothPermissions()
        logger.info("ShimmerSettingsActivity created")
    }
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "Shimmer Settings"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_shimmer_settings, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_visualization -> {
                startActivity(Intent(this, ShimmerVisualizationActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun initializeViews() {
        deviceStatusIcon = findViewById(R.id.device_status_icon)
        connectionStatusChip = findViewById(R.id.connection_status_chip)
        batteryLevelText = findViewById(R.id.battery_level_text)
        batteryProgressBar = findViewById(R.id.battery_progress_bar)
        signalStrengthText = findViewById(R.id.signal_strength_text)
        signalProgressBar = findViewById(R.id.signal_progress_bar)
        deviceInfoText = findViewById(R.id.device_info_text)
        samplingRateSpinner = findViewById(R.id.sampling_rate_spinner)
        connectButton = findViewById(R.id.connect_button)
        disconnectButton = findViewById(R.id.disconnect_button)
        scanButton = findViewById(R.id.scan_button)
        deviceListView = findViewById(R.id.device_list_view)
        configurationPresetSpinner = findViewById(R.id.configuration_preset_spinner)
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progress_text)
        selectDeviceButton = findViewById(R.id.select_device_button)
        crcSpinner = findViewById(R.id.crc_spinner)
        gsrRangeSpinner = findViewById(R.id.gsr_range_spinner)
        accelRangeSpinner = findViewById(R.id.accel_range_spinner)
        sensorCheckboxes =
            mapOf(
                SensorChannel.GSR to findViewById(R.id.checkbox_gsr),
                SensorChannel.PPG to findViewById(R.id.checkbox_ppg),
                SensorChannel.ACCEL to findViewById(R.id.checkbox_accel),
                SensorChannel.GYRO to findViewById(R.id.checkbox_gyro),
                SensorChannel.MAG to findViewById(R.id.checkbox_mag),
                SensorChannel.ECG to findViewById(R.id.checkbox_ecg),
                SensorChannel.EMG to findViewById(R.id.checkbox_emg),
            )
    }
    private fun setupClickListeners() {
        connectButton.setOnClickListener { viewModel.connectToDevice() }
        disconnectButton.setOnClickListener { viewModel.disconnectFromDevice() }
        scanButton.setOnClickListener { viewModel.scanForDevices() }
        selectDeviceButton.setOnClickListener {
            showDeviceSelectionDialog()
        }
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val state = viewModel.uiState.value
            if (position < state.availableDevices.size) {
                viewModel.onDeviceSelected(position)
            }
        }
        sensorCheckboxes.forEach { (channel, checkbox) ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                val enabledSensors = sensorCheckboxes.filter { it.value.isChecked }.keys.map { it.name }.toSet()
                viewModel.updateSensorConfiguration(enabledSensors)
            }
        }
    }
    private fun observeViewModelState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }
    private fun render(state: ShimmerConfigUiState) {
        connectButton.isEnabled = state.canConnectToDevice
        disconnectButton.isEnabled = state.canDisconnectDevice
        scanButton.isEnabled = state.canStartScan
        progressBar.visibility = if (state.isScanning || state.isLoadingConnection) View.VISIBLE else View.GONE
        progressText.visibility = if (state.isScanning || state.isLoadingConnection) View.VISIBLE else View.GONE
        progressText.text = when {
            state.isScanning -> "Scanning for devices..."
            state.isLoadingConnection -> "Connecting to device..."
            else -> ""
        }
        updateDeviceStatus(state)
        
        // Enhanced device list with better information
        val deviceNames = state.availableDevices.map { device ->
            buildString {
                append(device.name)
                append(" (${device.macAddress})")
                if (device.rssi != 0) {
                    append("\nSignal: ${device.rssi}dBm")
                }
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, deviceNames)
        deviceListView.adapter = adapter
        deviceListView.choiceMode = ListView.CHOICE_MODE_SINGLE
        if (state.selectedDeviceIndex >= 0 && state.selectedDeviceIndex < state.availableDevices.size) {
            deviceListView.setItemChecked(state.selectedDeviceIndex, true)
        }
        
        // Update sensor configuration controls based on connection state
        sensorCheckboxes.values.forEach { checkbox ->
            checkbox.isEnabled = state.isDeviceConnected && !state.isConfiguring
        }
        gsrRangeSpinner.isEnabled = state.isDeviceConnected && !state.isConfiguring
        accelRangeSpinner.isEnabled = state.isDeviceConnected && !state.isConfiguring
        crcSpinner.isEnabled = state.isDeviceConnected && !state.isConfiguring
        
        // Enable CRC only for firmware version 8+
        if (state.isDeviceConnected && state.firmwareVersion.isNotEmpty()) {
            val firmwareVersionCode = state.firmwareVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
            crcSpinner.isEnabled = firmwareVersionCode >= 8 && !state.isConfiguring
        }
        
        // Show/hide configuration panel based on device connection
        findViewById<View>(R.id.configuration_section)?.visibility =
            if (state.showConfigurationPanel) View.VISIBLE else View.GONE
        
        // Update sensor checkboxes based on current configuration
        if (state.isDeviceConnected) {
            state.enabledSensors.forEach { sensorName ->
                val sensorChannel = when (sensorName) {
                    "GSR" -> SensorChannel.GSR
                    "PPG" -> SensorChannel.PPG
                    "ACCEL" -> SensorChannel.ACCEL
                    "GYRO" -> SensorChannel.GYRO
                    "MAG" -> SensorChannel.MAG
                    "ECG" -> SensorChannel.ECG
                    "EMG" -> SensorChannel.EMG
                    else -> null
                }
                sensorChannel?.let { channel ->
                    sensorCheckboxes[channel]?.isChecked = true
                }
            }
        }
        
        // Show error messages
        state.errorMessage?.let { message ->
            if (state.showErrorDialog) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.onErrorMessageShown()
            }
        }
    }
    private fun updateDeviceStatus(state: ShimmerConfigUiState) {
        when {
            state.isDeviceConnected -> {
                connectionStatusChip.text = "Connected"
                connectionStatusChip.setChipBackgroundColorResource(R.color.success_color)
                deviceStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
            }
            state.isLoadingConnection -> {
                connectionStatusChip.text = "Connecting..."
                connectionStatusChip.setChipBackgroundColorResource(R.color.warning_color)
                deviceStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning_color))
            }
            else -> {
                connectionStatusChip.text = "Disconnected"
                connectionStatusChip.setChipBackgroundColorResource(R.color.error_color)
                deviceStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_color))
            }
        }
        
        // Enhanced battery display with more detailed status
        if (state.batteryLevel >= 0) {
            batteryLevelText.text = "${state.batteryLevel}%"
            batteryProgressBar.progress = state.batteryLevel
            batteryProgressBar.progressTintList = ContextCompat.getColorStateList(
                this,
                when {
                    state.batteryLevel > 50 -> R.color.success_color
                    state.batteryLevel > 20 -> R.color.warning_color
                    else -> R.color.error_color
                }
            )
        } else {
            batteryLevelText.text = "--"
            batteryProgressBar.progress = 0
        }
        
        // Enhanced signal strength display
        val signalStrength = state.signalStrength
        if (signalStrength != 0) {
            signalStrengthText.text = "${signalStrength}dBm"
            val signalPercent = when {
                signalStrength > -50 -> 100
                signalStrength > -60 -> 80
                signalStrength > -70 -> 60
                signalStrength > -80 -> 40
                signalStrength > -90 -> 20
                else -> 0
            }
            signalProgressBar.progress = signalPercent
            signalProgressBar.progressTintList = ContextCompat.getColorStateList(
                this,
                when {
                    signalPercent > 60 -> R.color.success_color
                    signalPercent > 30 -> R.color.warning_color
                    else -> R.color.error_color
                }
            )
        } else {
            signalStrengthText.text = "--"
            signalProgressBar.progress = 0
        }
        
        // Enhanced device information display
        if (state.isDeviceConnected) {
            deviceInfoText.text = buildString {
                append("Device Information:\n")
                append("• Name: ${state.selectedDevice?.name ?: "Unknown"}\n")
                append("• MAC Address: ${state.selectedDevice?.macAddress ?: "Unknown"}\n")
                append("• Firmware: ${state.firmwareVersion.ifEmpty { "Unknown" }}\n")
                append("• Hardware: ${state.hardwareVersion.ifEmpty { "Unknown" }}\n")
                append("• Battery: ${if (state.batteryLevel >= 0) "${state.batteryLevel}%" else "Unknown"}\n")
                append("• Signal: ${if (state.signalStrength != 0) "${state.signalStrength}dBm" else "Unknown"}\n")
                append("• Sampling Rate: ${state.samplingRate}Hz\n")
                append("• Enabled Sensors: ${state.enabledSensors.joinToString(", ").ifEmpty { "None" }}\n")
                if (state.isRecording) {
                    append("• Recording: Active (${state.dataPacketsReceived} packets)")
                } else {
                    append("• Recording: Inactive")
                }
            }
        } else {
            deviceInfoText.text = buildString {
                append("No device connected\n\n")
                append("Please scan and connect a Shimmer device to configure settings and start recording.\n\n")
                append("Available devices: ${state.availableDevices.size}\n")
                if (state.availableDevices.isNotEmpty()) {
                    append("\nFound devices:\n")
                    state.availableDevices.forEachIndexed { index, device ->
                        append("${index + 1}. ${device.name} (${device.macAddress})")
                        if (device.rssi != 0) {
                            append(" - ${device.rssi}dBm")
                        }
                        append("\n")
                    }
                }
            }
        }
    }
    private fun setupSpinners() {
        val samplingRates = arrayOf("25.6 Hz", "51.2 Hz", "128.0 Hz", "256.0 Hz", "512.0 Hz")
        val samplingRateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, samplingRates)
        samplingRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        samplingRateSpinner.adapter = samplingRateAdapter
        samplingRateSpinner.setSelection(1)
        samplingRateSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val samplingRateValues = arrayOf(25.6, 51.2, 128.0, 256.0, 512.0)
                    if (position < samplingRateValues.size) {
                        viewModel.updateSamplingRate(samplingRateValues[position].toInt())
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        val presets = viewModel.getAvailablePresets().toTypedArray()
        val presetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presets)
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        configurationPresetSpinner.adapter = presetAdapter
        configurationPresetSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val selectedPreset = presets[position]
                    viewModel.applyConfigurationPreset(selectedPreset)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        val crcOptions = arrayOf("Disable CRC", "Enable 1-byte CRC", "Enable 2-byte CRC")
        val crcAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, crcOptions)
        crcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        crcSpinner.adapter = crcAdapter
        crcSpinner.isEnabled = false
        crcSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (crcSpinner.isEnabled) {
                    viewModel.updateCrcConfiguration(position)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val gsrRanges = arrayOf("40kΩ to 4MΩ", "10kΩ to 1MΩ", "3.2kΩ to 0.32MΩ", "1kΩ to 0.1MΩ", "0.3kΩ to 0.03MΩ")
        val gsrRangeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gsrRanges)
        gsrRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gsrRangeSpinner.adapter = gsrRangeAdapter
        gsrRangeSpinner.setSelection(4)
        gsrRangeSpinner.isEnabled = false
        gsrRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (gsrRangeSpinner.isEnabled) {
                    viewModel.updateGsrRange(position)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val accelRanges = arrayOf("±2g", "±4g", "±8g", "±16g")
        val accelRangeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accelRanges)
        accelRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        accelRangeSpinner.adapter = accelRangeAdapter
        accelRangeSpinner.setSelection(0)
        accelRangeSpinner.isEnabled = false
        accelRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (accelRangeSpinner.isEnabled) {
                    val ranges = arrayOf(2, 4, 8, 16)
                    viewModel.updateAccelRange(ranges[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    private fun setupSensorCheckboxes() {
        sensorCheckboxes[SensorChannel.GSR]?.isChecked = true
        sensorCheckboxes[SensorChannel.PPG]?.isChecked = true
        sensorCheckboxes[SensorChannel.ACCEL]?.isChecked = true
    }
    private fun checkBluetoothPermissions() {
        val missingPermissions =
            bluetoothPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        } else {
            logger.info("Bluetooth permissions already granted.")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                logger.info("Bluetooth permissions granted by user.")
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permissions are required for shimmer device functionality",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun showDeviceSelectionDialog() {
        val dialog = ShimmerDeviceSelectionDialog.newInstance()
        dialog.setDeviceSelectionListener(object : ShimmerDeviceSelectionDialog.DeviceSelectionListener {
            override fun onDeviceSelected(macAddress: String, deviceName: String) {
                logger.info("Device selected: $deviceName ($macAddress)")
                viewModel.connectToSpecificDevice(macAddress, deviceName)
            }
            override fun onSelectionCancelled() {
                logger.info("Device selection cancelled")
            }
        })
        dialog.show(supportFragmentManager, "device_selection")
    }
}