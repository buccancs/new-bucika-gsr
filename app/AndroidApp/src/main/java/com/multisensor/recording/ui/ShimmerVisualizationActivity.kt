package com.multisensor.recording.ui
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.multisensor.recording.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*
@AndroidEntryPoint
class ShimmerVisualizationActivity : AppCompatActivity() {
    private val viewModel: ShimmerConfigViewModel by viewModels()
    private lateinit var deviceStatusIcon: ImageView
    private lateinit var connectionStatusChip: Chip
    private lateinit var batteryLevelText: TextView
    private lateinit var batteryProgressBar: ProgressBar
    private lateinit var signalStrengthText: TextView
    private lateinit var signalProgressBar: ProgressBar
    private lateinit var deviceInfoText: TextView
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
    private lateinit var startStreamingButton: Button
    private lateinit var stopStreamingButton: Button
    private lateinit var realTimeDataText: TextView
    @Inject
    lateinit var logger: Logger
    private val gsrData = mutableListOf<Entry>()
    private val ppgData = mutableListOf<Entry>()
    private val accelData = mutableListOf<Entry>()
    private val gyroData = mutableListOf<Entry>()
    private var chartEntryCount = 0
    private val maxChartEntries = 500
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer_visualization)
        setupToolbar()
        initializeViews()
        setupClickListeners()
        setupCharts()
        observeViewModelState()
        logger.info("ShimmerVisualizationActivity created")
    }
    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "Shimmer Visualisation"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_shimmer_visualization, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, ShimmerSettingsActivity::class.java))
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
        startStreamingButton = findViewById(R.id.start_streaming_button)
        stopStreamingButton = findViewById(R.id.stop_streaming_button)
        realTimeDataText = findViewById(R.id.real_time_data_text)
    }
    private fun setupClickListeners() {
        startStreamingButton.setOnClickListener {
            viewModel.startStreaming()
            dataVisualizationCard.visibility = View.VISIBLE
        }
        stopStreamingButton.setOnClickListener {
            viewModel.stopStreaming()
        }
        exportDataButton.setOnClickListener {
            Toast.makeText(this, "Export functionality coming soon", Toast.LENGTH_SHORT).show()
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
        startStreamingButton.isEnabled = state.canStartRecording
        stopStreamingButton.isEnabled = state.canStopRecording
        updateDeviceStatus(state)
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
            deviceInfoText.text = "No device connected\n\nPlease configure and connect a Shimmer device in Settings to view real-time data."
        }
    }
    private fun updateDataVisualization(state: ShimmerConfigUiState) {
        dataVisualizationCard.visibility = if (state.isRecording || state.isDeviceConnected) View.VISIBLE else View.GONE
        recordingStatusChip.text = if (state.isRecording) "Recording" else "Connected"
        recordingStatusChip.setChipBackgroundColorResource(
            if (state.isRecording) R.color.success_color else R.color.warning_color
        )
        
        packetsReceivedText.text = state.dataPacketsReceived.toString()
        val duration = state.recordingDuration / 1000
        val minutes = duration / 60
        val seconds = duration % 60
        recordingDurationText.text = String.format("%02d:%02d", minutes, seconds)
        
        val dataRate = if (duration > 0) state.dataPacketsReceived.toDouble() / duration else 0.0
        dataRateText.text = String.format("%.1f Hz", dataRate)
        
        // Enhanced real-time data display
        if (state.isRecording && state.dataPacketsReceived > 0) {
            realTimeDataText.text = buildString {
                append("🔴 LIVE RECORDING SESSION\n")
                append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("⏱️ Duration: ${String.format("%02d:%02d", minutes, seconds)}\n")
                append("📦 Data Packets: ${state.dataPacketsReceived}\n")
                append("📊 Sampling Rate: ${String.format("%.1f", dataRate)} Hz\n")
                append("📶 Signal Strength: ${state.signalStrength} dBm\n")
                append("🔋 Battery Level: ${state.batteryLevel}%\n")
                append("🎯 Target Rate: ${state.samplingRate} Hz\n")
                
                // Performance metrics
                val efficiency = if (state.samplingRate > 0) (dataRate / state.samplingRate * 100).coerceAtMost(100.0) else 0.0
                append("⚡ Data Efficiency: ${String.format("%.1f", efficiency)}%\n")
                
                // Data quality indicators
                val batteryStatus = when {
                    state.batteryLevel > 50 -> "Excellent"
                    state.batteryLevel > 20 -> "Good"
                    state.batteryLevel >= 0 -> "Low"
                    else -> "Unknown"
                }
                append("💡 Battery Status: $batteryStatus\n")
                
                val signalQuality = when {
                    state.signalStrength > -60 -> "Excellent"
                    state.signalStrength > -70 -> "Good"
                    state.signalStrength > -80 -> "Fair"
                    state.signalStrength != 0 -> "Poor"
                    else -> "Unknown"
                }
                append("📡 Signal Quality: $signalQuality\n")
                
                // Session statistics
                if (state.dataPacketsReceived > 0) {
                    val avgPacketsPerSecond = if (duration > 0) state.dataPacketsReceived.toDouble() / duration else 0.0
                    append("📈 Avg. Rate: ${String.format("%.2f", avgPacketsPerSecond)} pkt/s\n")
                }
                
                append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        } else if (state.isDeviceConnected) {
            realTimeDataText.text = buildString {
                append("🟢 DEVICE READY FOR RECORDING\n")
                append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("📱 Device: ${state.selectedDevice?.name ?: "Unknown"}\n")
                append("🆔 MAC Address: ${state.selectedDevice?.macAddress ?: "Unknown"}\n")
                append("🔋 Battery: ${if (state.batteryLevel >= 0) "${state.batteryLevel}%" else "Unknown"}\n")
                append("📶 Signal: ${if (state.signalStrength != 0) "${state.signalStrength} dBm" else "Unknown"}\n")
                append("💾 Firmware: ${state.firmwareVersion.ifEmpty { "Unknown" }}\n")
                append("🔧 Hardware: ${state.hardwareVersion.ifEmpty { "Unknown" }}\n")
                append("⚙️ Sampling Rate: ${state.samplingRate} Hz\n")
                append("🎛️ Enabled Sensors: ${state.enabledSensors.size}\n")
                
                if (state.enabledSensors.isNotEmpty()) {
                    append("   └ ${state.enabledSensors.joinToString(", ")}\n")
                }
                
                // Connection quality assessment
                val connectionQuality = when {
                    state.batteryLevel > 50 && state.signalStrength > -60 -> "Excellent"
                    state.batteryLevel > 20 && state.signalStrength > -70 -> "Good"
                    state.batteryLevel > 0 || state.signalStrength > -80 -> "Fair"
                    else -> "Check Connection"
                }
                append("🌟 Connection Quality: $connectionQuality\n")
                append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("👆 Press START to begin recording")
            }
        } else {
            realTimeDataText.text = buildString {
                append("⚫ NO ACTIVE SESSION\n")
                append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("📵 Device Status: Disconnected\n")
                append("🔍 Available Devices: ${state.availableDevices.size}\n")
                
                if (state.availableDevices.isNotEmpty()) {
                    append("\n📋 Discovered Devices:\n")
                    state.availableDevices.take(3).forEachIndexed { index, device ->
                        append("   ${index + 1}. ${device.name}\n")
                        append("      MAC: ${device.macAddress}\n")
                        if (device.rssi != 0) {
                            append("      Signal: ${device.rssi}dBm\n")
                        }
                    }
                    if (state.availableDevices.size > 3) {
                        append("   ... and ${state.availableDevices.size - 3} more\n")
                    }
                }
                
                append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
                append("🔧 Go to Settings to connect a device\n")
                append("📊 Then return here to view real-time data")
            }
        }
        
        // Update chart data if recording (mock data for now - would be replaced with real sensor data)
        if (state.isRecording && state.dataPacketsReceived > 0) {
            updateChartData(state)
        }
    }
    
    private fun updateChartData(state: ShimmerConfigUiState) {
        // Generate realistic mock sensor data based on actual recording state
        val currentTime = chartEntryCount.toFloat()
        
        // Simulate GSR data (typical range 1-20 μS)
        val gsrValue = 2.5 + 0.5 * kotlin.math.sin(currentTime * 0.1) + 0.2 * kotlin.math.sin(currentTime * 0.3) + (Math.random() * 0.2 - 0.1)
        gsrData.add(Entry(currentTime, gsrValue.toFloat()))
        
        // Simulate PPG data (typical range 500-1500)
        val ppgValue = 1000 + 100 * kotlin.math.sin(currentTime * 0.05) + 50 * kotlin.math.sin(currentTime * 0.4) + (Math.random() * 20 - 10)
        ppgData.add(Entry(currentTime, ppgValue.toFloat()))
        
        // Simulate accelerometer data (typical range -2g to +2g)
        val accelValue = 0.98 + 0.1 * kotlin.math.sin(currentTime * 0.2) + (Math.random() * 0.1 - 0.05)
        accelData.add(Entry(currentTime, accelValue.toFloat()))
        
        // Simulate gyroscope data (typical range -100°/s to +100°/s)
        val gyroValue = 0.0 + 2 * kotlin.math.sin(currentTime * 0.15) + (Math.random() * 1 - 0.5)
        gyroData.add(Entry(currentTime, gyroValue.toFloat()))
        
        // Keep only the last maxChartEntries points
        if (gsrData.size > maxChartEntries) {
            gsrData.removeAt(0)
            ppgData.removeAt(0)
            accelData.removeAt(0)
            gyroData.removeAt(0)
        }
        
        chartEntryCount++
        
        // Update the visible chart
        val selectedTab = chartTabLayout.selectedTabPosition
        when (selectedTab) {
            0 -> updateChart(gsrChart, gsrData, "GSR (µS)", Color.rgb(63, 81, 181))
            1 -> updateChart(ppgChart, ppgData, "PPG", Color.rgb(233, 30, 99))
            2 -> updateChart(accelChart, accelData, "Accelerometer (g)", Color.rgb(76, 175, 80))
            3 -> updateChart(gyroChart, gyroData, "Gyroscope (°/s)", Color.rgb(255, 152, 0))
        }
    }
    
    private fun updateChart(chart: LineChart, data: MutableList<Entry>, label: String, color: Int) {
        val dataSet = LineDataSet(data.toList(), label).apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 1f
            setDrawCircleHole(false)
            valueTextSize = 0f
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }
        
        chart.data = LineData(dataSet)
        chart.notifyDataSetChanged()
        chart.invalidate()
        
        // Auto-scroll to show latest data
        chart.setVisibleXRangeMaximum(100f)
        chart.moveViewToX(data.size.toFloat())
    }
}