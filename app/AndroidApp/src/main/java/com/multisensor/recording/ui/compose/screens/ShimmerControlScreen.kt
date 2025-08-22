package com.multisensor.recording.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multisensor.recording.ui.ShimmerConfigUiState
import com.multisensor.recording.ui.ShimmerConfigViewModel
import com.multisensor.recording.ui.components.ShimmerDashboard
import com.multisensor.recording.ui.components.ShimmerQuickConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShimmerControlScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ShimmerConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Shimmer GSR Control",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Navigate to detailed settings
                            android.content.Intent(context, com.multisensor.recording.ui.ShimmerSettingsActivity::class.java).also {
                                context.startActivity(it)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Detailed Settings")
                    }
                    
                    IconButton(
                        onClick = {
                            // Navigate to visualization
                            android.content.Intent(context, com.multisensor.recording.ui.ShimmerVisualizationActivity::class.java).also {
                                context.startActivity(it)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = "Data Visualization")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Shimmer Dashboard
            ShimmerDashboard(
                modifier = Modifier.fillMaxWidth(),
                onNavigateToSettings = {
                    android.content.Intent(context, com.multisensor.recording.ui.ShimmerSettingsActivity::class.java).also {
                        context.startActivity(it)
                    }
                },
                onNavigateToVisualization = {
                    android.content.Intent(context, com.multisensor.recording.ui.ShimmerVisualizationActivity::class.java).also {
                        context.startActivity(it)
                    }
                }
            )

            // Quick Configuration Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (uiState.isDeviceConnected) {
                            AssistChip(
                                onClick = { /* Show help */ },
                                label = { Text("Device Ready") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CheckCircle, 
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ShimmerQuickConfig()
                }
            }

            // API Usage Guide Card
            if (!uiState.isDeviceConnected) {
                ApiUsageGuideCard()
            }
            
            // Recording Session Info (when recording)
            if (uiState.isRecording) {
                RecordingSessionCard(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ApiUsageGuideCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🚀 Shimmer API Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val features = listOf(
                "📡 Real-time device discovery and connection",
                "🎛️ Complete sensor configuration (GSR, PPG, Accel, Gyro, Mag, ECG, EMG)",
                "📊 Live data streaming with customizable sampling rates",
                "💾 SD card logging with time synchronization",
                "🔋 Battery monitoring and device diagnostics",
                "📈 Real-time data visualization and export",
                "🔧 Advanced configuration (CRC, ranges, calibration)",
                "🔄 Multi-device support and automatic reconnection"
            )
            
            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "To get started:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "1. Ensure Bluetooth is enabled\n2. Tap 'Scan for Devices' above\n3. Select your Shimmer device\n4. Configure sensors and start recording",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordingSessionCard(uiState: ShimmerConfigUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔴 Active Recording Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                
                val duration = uiState.recordingDuration / 1000
                val minutes = duration / 60
                val seconds = duration % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SessionMetric(
                    label = "Data Packets",
                    value = "${uiState.dataPacketsReceived}",
                    icon = Icons.Default.DataObject
                )
                
                SessionMetric(
                    label = "Sampling Rate",
                    value = "${uiState.samplingRate} Hz",
                    icon = Icons.Default.Speed
                )
                
                val duration = uiState.recordingDuration / 1000
                val dataRate = if (duration > 0) uiState.dataPacketsReceived.toDouble() / duration else 0.0
                SessionMetric(
                    label = "Actual Rate",
                    value = String.format("%.1f Hz", dataRate),
                    icon = Icons.Default.Analytics
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SessionMetric(
                    label = "Battery",
                    value = if (uiState.batteryLevel >= 0) "${uiState.batteryLevel}%" else "--",
                    icon = Icons.Default.Battery1Bar
                )
                
                SessionMetric(
                    label = "Signal",
                    value = if (uiState.signalStrength != 0) "${uiState.signalStrength}dBm" else "--",
                    icon = Icons.Default.SignalWifi4Bar
                )
                
                SessionMetric(
                    label = "Active Sensors",
                    value = "${uiState.enabledSensors.size}",
                    icon = Icons.Default.Sensors
                )
            }
            
            HorizontalDivider()
            
            Text(
                text = "Active Sensors: ${uiState.enabledSensors.joinToString(", ").ifEmpty { "None" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}