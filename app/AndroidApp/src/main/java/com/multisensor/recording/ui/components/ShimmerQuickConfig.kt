package com.multisensor.recording.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multisensor.recording.recording.DeviceConfiguration.SensorChannel
import com.multisensor.recording.ui.ShimmerConfigUiState
import com.multisensor.recording.ui.ShimmerConfigViewModel
import com.multisensor.recording.ui.ShimmerDeviceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShimmerQuickConfig(
    modifier: Modifier = Modifier,
    viewModel: ShimmerConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Quick Device Status Card
            QuickDeviceStatusCard(
                uiState = uiState,
                viewModel = viewModel
            )
        }

        if (uiState.isDeviceConnected) {
            item {
                // Quick Sensor Configuration
                QuickSensorConfigCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            item {
                // Quick Recording Controls
                QuickRecordingControlsCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            item {
                // Device Diagnostics
                DeviceDiagnosticsCard(
                    uiState = uiState
                )
            }
        }

        if (uiState.availableDevices.isNotEmpty()) {
            item {
                // Available Devices List
                AvailableDevicesCard(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun QuickDeviceStatusCard(
    uiState: ShimmerConfigUiState,
    viewModel: ShimmerConfigViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "Device Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                StatusIndicator(
                    isConnected = uiState.isDeviceConnected,
                    isConnecting = uiState.isLoadingConnection
                )
            }

            if (uiState.isDeviceConnected && uiState.selectedDevice != null) {
                DeviceInfoRow(
                    icon = Icons.Default.DeviceHub,
                    label = "Device",
                    value = uiState.selectedDevice!!.name
                )
                
                DeviceInfoRow(
                    icon = Icons.Default.Fingerprint,
                    label = "MAC",
                    value = uiState.selectedDevice!!.macAddress
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BatteryIndicator(
                        batteryLevel = uiState.batteryLevel,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SignalIndicator(
                        signalStrength = uiState.signalStrength,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!uiState.isDeviceConnected) {
                    Button(
                        onClick = { viewModel.scanForDevices() },
                        enabled = !uiState.isScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.disconnectFromDevice() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BluetoothDisabled, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSensorConfigCard(
    uiState: ShimmerConfigUiState,
    viewModel: ShimmerConfigViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sensor Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Sampling Rate Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sampling Rate",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val samplingRates = listOf(25.6, 51.2, 128.0, 256.0, 512.0)
                var expanded by remember { mutableStateOf(false) }
                
                Box {
                    OutlinedTextField(
                        value = "${uiState.samplingRate} Hz",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { 
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .width(120.dp)
                            .clickable { expanded = !expanded }
                    )
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        samplingRates.forEach { rate ->
                            DropdownMenuItem(
                                text = { Text("${rate} Hz") },
                                onClick = {
                                    viewModel.updateSamplingRate(rate.toInt())
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick sensor toggles
            Text(
                text = "Active Sensors",
                style = MaterialTheme.typography.bodyMedium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sensors = listOf(
                    "GSR" to Icons.Default.Sensors,
                    "PPG" to Icons.Default.Favorite,
                    "ACCEL" to Icons.Default.Speed,
                    "GYRO" to Icons.Default.RotateRight,
                    "MAG" to Icons.Default.Explore,
                    "ECG" to Icons.Default.Monitor,
                    "EMG" to Icons.Default.GraphicEq
                )
                
                items(sensors) { (sensorName, icon) ->
                    SensorToggleChip(
                        sensorName = sensorName,
                        icon = icon,
                        isEnabled = sensorName in uiState.enabledSensors,
                        onToggle = { enabled ->
                            val newSensors = if (enabled) {
                                uiState.enabledSensors + sensorName
                            } else {
                                uiState.enabledSensors - sensorName
                            }
                            viewModel.updateSensorConfiguration(newSensors)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickRecordingControlsCard(
    uiState: ShimmerConfigUiState,
    viewModel: ShimmerConfigViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isRecording) Color(0xFFE8F5E8) else MaterialTheme.colorScheme.surface
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
                    text = "Recording Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (uiState.isRecording) {
                    Text(
                        text = "🔴 LIVE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (uiState.isRecording) {
                            viewModel.stopStreaming()
                        } else {
                            viewModel.startStreaming()
                        }
                    },
                    enabled = uiState.canStartRecording || uiState.canStopRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRecording) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.isRecording) "Stop Recording" else "Start Recording")
                }
            }

            if (uiState.isRecording) {
                RecordingStatsRow(uiState = uiState)
            }
        }
    }
}

@Composable
private fun RecordingStatsRow(uiState: ShimmerConfigUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${uiState.dataPacketsReceived}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Text(
                text = "Packets",
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val duration = uiState.recordingDuration / 1000
            val minutes = duration / 60
            val seconds = duration % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelSmall
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val duration = uiState.recordingDuration / 1000
            val dataRate = if (duration > 0) uiState.dataPacketsReceived.toDouble() / duration else 0.0
            Text(
                text = String.format("%.1f", dataRate),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "Hz",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DeviceDiagnosticsCard(uiState: ShimmerConfigUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Device Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DeviceInfoRow(
                icon = Icons.Default.Memory,
                label = "Firmware",
                value = uiState.firmwareVersion.ifEmpty { "Unknown" }
            )
            
            DeviceInfoRow(
                icon = Icons.Default.Computer,
                label = "Hardware",
                value = uiState.hardwareVersion.ifEmpty { "Unknown" }
            )
            
            if (uiState.enabledSensors.isNotEmpty()) {
                DeviceInfoRow(
                    icon = Icons.Default.Sensors,
                    label = "Active Sensors",
                    value = "${uiState.enabledSensors.size} sensors"
                )
            }
        }
    }
}

@Composable
private fun AvailableDevicesCard(
    uiState: ShimmerConfigUiState,
    viewModel: ShimmerConfigViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Available Devices (${uiState.availableDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            uiState.availableDevices.forEachIndexed { index, device ->
                DeviceListItem(
                    device = device,
                    isSelected = index == uiState.selectedDeviceIndex,
                    onClick = { viewModel.onDeviceSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: ShimmerDeviceItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = device.macAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (device.rssi != 0) {
            Text(
                text = "${device.rssi}dBm",
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    device.rssi > -60 -> Color(0xFF4CAF50)
                    device.rssi > -80 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
        }
    }
}

@Composable
private fun StatusIndicator(
    isConnected: Boolean,
    isConnecting: Boolean
) {
    val color = when {
        isConnected -> Color(0xFF4CAF50)
        isConnecting -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isConnecting) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StatusAlpha"
    )
    
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                color.copy(alpha = if (isConnecting) animatedAlpha else 1f),
                CircleShape
            )
    )
}

@Composable
private fun DeviceInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BatteryIndicator(
    batteryLevel: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when {
                    batteryLevel > 80 -> Icons.Default.BatteryFull
                    batteryLevel > 60 -> Icons.Default.Battery6Bar
                    batteryLevel > 40 -> Icons.Default.Battery4Bar
                    batteryLevel > 20 -> Icons.Default.Battery2Bar
                    batteryLevel >= 0 -> Icons.Default.Battery1Bar
                    else -> Icons.Default.BatteryUnknown
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    batteryLevel > 50 -> Color(0xFF4CAF50)
                    batteryLevel > 20 -> Color(0xFFFF9800)
                    batteryLevel >= 0 -> Color(0xFFF44336)
                    else -> Color.Gray
                }
            )
            Text(
                text = if (batteryLevel >= 0) "${batteryLevel}%" else "--",
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = "Battery",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignalIndicator(
    signalStrength: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when {
                    signalStrength > -60 -> Icons.Default.SignalWifi4Bar
                    signalStrength > -70 -> Icons.Default.Wifi
                    signalStrength > -80 -> Icons.Default.Wifi
                    signalStrength != 0 -> Icons.Default.WifiOff
                    else -> Icons.Default.SignalWifiOff
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    signalStrength > -60 -> Color(0xFF4CAF50)
                    signalStrength > -80 -> Color(0xFFFF9800)
                    signalStrength != 0 -> Color(0xFFF44336)
                    else -> Color.Gray
                }
            )
            Text(
                text = if (signalStrength != 0) "${signalStrength}dBm" else "--",
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = "Signal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SensorToggleChip(
    sensorName: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    FilterChip(
        onClick = { onToggle(!isEnabled) },
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = sensorName,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        selected = isEnabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}