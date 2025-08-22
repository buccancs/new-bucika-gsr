package com.multisensor.recording.ui
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.multisensor.recording.R
import com.multisensor.recording.recording.DeviceConfiguration.SensorChannel
import com.multisensor.recording.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
@AndroidEntryPoint
class ShimmerConfigActivity : AppCompatActivity() {
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
    private lateinit var startStreamingButton: Button
    private lateinit var stopStreamingButton: Button
    private lateinit var deviceListView: ListView
    private lateinit var configurationPresetSpinner: Spinner
    private lateinit var realTimeDataText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var selectDeviceButton: Button
    private lateinit var crcSpinner: Spinner
    private lateinit var deviceInfoText: TextView
    private lateinit var gsrRangeSpinner: Spinner
    private lateinit var accelRangeSpinner: Spinner
    private lateinit var dataVisualizationCard: View
    private lateinit var chartTabLayout: TabLayout
    private lateinit var gsrChart: LineChart
    private lateinit var ppgChart: LineChart
    private lateinit var accelChart: LineChart
    private lateinit var gyroChart: LineChart
    private lateinit var packetsReceivedText: TextView
    private lateinit var recordingDurationText: TextView
    private lateinit var dataRateText: TextView
    private lateinit var recordingStatusChip: Chip
    private lateinit var exportDataButton: Button
    @Inject
    lateinit var logger: Logger
    private val gsrData = mutableListOf<Entry>()
    private val ppgData = mutableListOf<Entry>()
    private val accelData = mutableListOf<Entry>()
    private val gyroData = mutableListOf<Entry>()
    private var chartEntryCount = 0
    private val maxChartEntries = 500
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
        private const val STATUS_UPDATE_INTERVAL_MS = 2000L
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer_config)
        initializeViews()
        setupClickListeners()
        setupSpinners()
        setupSensorCheckboxes()
        setupCharts()
        observeViewModelState()
        checkBluetoothPermissions()
        logger.info("Enhanced ShimmerConfigActivity created with real-time charts")
    }
    override fun onDestroy() {
        super.onDestroy()
        logger.info("ShimmerConfigActivity destroyed")
    }
    private fun initializeViews() {
        deviceStatusIcon = findViewById(R.id.device_status_icon)
        connectionStatusChip = findViewById(R.id.connection_status_chip)
        batteryLevelText = findViewById(R.id.battery_level_text)
        batteryProgressBar = findViewById(R.id.battery_progress_bar)
        signalStrengthText = findViewById(R.id.signal_strength_text)
        signalProgressBar = findViewById(R.id.signal_progress_bar)
        deviceInfoText = findViewById(R.id.device_info_text)
        dataVisualizationCard = findViewById(R.id.data_visualization_card)
        chartTabLayout = findViewById(R.id.chart_tab_layout)
        gsrChart = findViewById(R.id.gsr_chart)
        ppgChart = findViewById(R.id.ppg_chart)
        accelChart = findViewById(R.id.accel_chart)
        gyroChart = findViewById(R.id.gyro_chart)
        packetsReceivedText = findViewById(R.id.packets_received_text)
        recordingDurationText = findViewById(R.id.recording_duration_text)
        dataRateText = findViewById(R.id.data_rate_text)
        recordingStatusChip = findViewById(R.id.recording_status_chip)
        exportDataButton = findViewById(R.id.export_data_button)
        samplingRateSpinner = findViewById(R.id.sampling_rate_spinner)
        connectButton = findViewById(R.id.connect_button)
        disconnectButton = findViewById(R.id.disconnect_button)
        scanButton = findViewById(R.id.scan_button)
        startStreamingButton = findViewById(R.id.start_streaming_button)
        stopStreamingButton = findViewById(R.id.stop_streaming_button)
        deviceListView = findViewById(R.id.device_list_view)
        configurationPresetSpinner = findViewById(R.id.configuration_preset_spinner)
        realTimeDataText = findViewById(R.id.real_time_data_text)
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
        startStreamingButton.setOnClickListener {
            viewModel.startStreaming()
            dataVisualizationCard.visibility = View.VISIBLE
        }
        stopStreamingButton.setOnClickListener {
            viewModel.stopStreaming()
        }
        selectDeviceButton.setOnClickListener {
            showDeviceSelectionDialog()
        }
        exportDataButton.setOnClickListener {
            Toast.makeText(this, "Export functionality coming soon", Toast.LENGTH_SHORT).show()
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
        chartTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showChart(gsrChart)
                    1 -> showChart(ppgChart)
                    2 -> showChart(accelChart)
                    3 -> showChart(gyroChart)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    private fun showChart(chartToShow: LineChart) {
        gsrChart.visibility = View.GONE
        ppgChart.visibility = View.GONE
        accelChart.visibility = View.GONE
        gyroChart.visibility = View.GONE
        chartToShow.visibility = View.VISIBLE
    }
    private fun setupCharts() {
        chartTabLayout.addTab(chartTabLayout.newTab().setText("GSR"))
        chartTabLayout.addTab(chartTabLayout.newTab().setText("PPG"))
        chartTabLayout.addTab(chartTabLayout.newTab().setText("Accel"))
        chartTabLayout.addTab(chartTabLayout.newTab().setText("Gyro"))
        configureChart(gsrChart, "GSR (µS)", Color.rgb(63, 81, 181))
        configureChart(ppgChart, "PPG", Color.rgb(233, 30, 99))
        configureChart(accelChart, "Accelerometer (g)", Color.rgb(76, 175, 80))
        configureChart(gyroChart, "Gyroscope (°/s)", Color.rgb(255, 152, 0))
        showChart(gsrChart)
    }
    private fun configureChart(chart: LineChart, label: String, colour: Int) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
            val dataSet = LineDataSet(mutableListOf(), label).apply {
                this.color = colour
                setCircleColor(colour)
                lineWidth = 2f
                circleRadius = 3f
                setDrawCircleHole(false)
                valueTextSize = 0f
                setDrawFilled(false)
            }
            data = LineData(dataSet)
            invalidate()
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
        startStreamingButton.isEnabled = state.canStartRecording
        stopStreamingButton.isEnabled = state.canStopRecording
        scanButton.isEnabled = state.canStartScan
        progressBar.visibility = if (state.isScanning || state.isLoadingConnection) View.VISIBLE else View.GONE
        progressText.visibility = if (state.isScanning || state.isLoadingConnection) View.VISIBLE else View.GONE
        progressText.text = when {
            state.isScanning -> "Scanning for devices..."
            state.isLoadingConnection -> "Connecting to device..."
            else -> ""
        }
        updateDeviceStatus(state)
        val deviceNames = state.availableDevices.map { "${it.name} (${it.macAddress})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, deviceNames)
        deviceListView.adapter = adapter
        deviceListView.choiceMode = ListView.CHOICE_MODE_SINGLE
        if (state.selectedDeviceIndex >= 0 && state.selectedDeviceIndex < state.availableDevices.size) {
            deviceListView.setItemChecked(state.selectedDeviceIndex, true)
        }
        sensorCheckboxes.values.forEach { checkbox ->
            checkbox.isEnabled = state.isDeviceConnected && !state.isConfiguring
        }
        gsrRangeSpinner.isEnabled = state.isDeviceConnected && !state.isConfiguring
        accelRangeSpinner.isEnabled = state.isDeviceConnected && !state.isConfiguring
        crcSpinner.isEnabled = state.isDeviceConnected && !state.isConfiguring
        if (state.isDeviceConnected && state.firmwareVersion.isNotEmpty()) {
            val firmwareVersionCode = state.firmwareVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
            crcSpinner.isEnabled = firmwareVersionCode >= 8
        }
        findViewById<View>(R.id.configuration_section)?.visibility =
            if (state.showConfigurationPanel) View.VISIBLE else View.GONE
        findViewById<View>(R.id.streaming_section)?.visibility =
            if (state.showRecordingControls) View.VISIBLE else View.GONE
        updateDataVisualization(state)
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
        if (state.isDeviceConnected) {
            deviceInfoText.text = buildString {
                append("Device Information:\n")
                append("• Firmware: ${state.firmwareVersion}\n")
                append("• Hardware: ${state.hardwareVersion}\n")
                append("• MAC: ${state.selectedDevice?.macAddress ?: "Unknown"}\n")
                append("• Battery: ${state.batteryLevel}%\n")
                append("• Signal: ${state.signalStrength}dBm")
            }
        } else {
            deviceInfoText.text = "No device connected\n\nPlease connect a Shimmer device to view detailed information."
        }
    }
    private fun updateDataVisualization(state: ShimmerConfigUiState) {
        dataVisualizationCard.visibility = if (state.isRecording) View.VISIBLE else View.GONE
        recordingStatusChip.text = if (state.isRecording) "Recording" else "Stopped"
        recordingStatusChip.setChipBackgroundColorResource(
            if (state.isRecording) R.color.success_color else R.color.error_color
        )
        packetsReceivedText.text = state.dataPacketsReceived.toString()
        val duration = state.recordingDuration / 1000
        val minutes = duration / 60
        val seconds = duration % 60
        recordingDurationText.text = String.format("%02d:%02d", minutes, seconds)
        val dataRate = if (duration > 0) state.dataPacketsReceived.toDouble() / duration else 0.0
        dataRateText.text = String.format("%.1f", dataRate)
        if (state.isRecording && state.dataPacketsReceived > 0) {
            realTimeDataText.text = buildString {
                append("Active Recording Session\n")
                append("Duration: ${String.format("%02d:%02d", minutes, seconds)}\n")
                append("Packets: ${state.dataPacketsReceived}\n")
                append("Rate: ${String.format("%.1f", dataRate)} Hz\n")
                append("Signal: ${state.signalStrength} dBm\n")
                append("Battery: ${state.batteryLevel}%")
            }
        } else if (state.isDeviceConnected) {
            realTimeDataText.text = buildString {
                append("Device Ready\n")
                append("Status: Connected\n")
                append("Battery: ${if (state.batteryLevel >= 0) "${state.batteryLevel}%" else "Unknown"}\n")
                append("Signal: ${state.signalStrength} dBm\n")
                append("Firmware: ${state.firmwareVersion}")
            }
        } else {
            realTimeDataText.text = "No active session\n\nConnect a device and start recording to view real-time data."
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
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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