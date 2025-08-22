package com.multisensor.recording.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multisensor.recording.ui.ShimmerConfigUiState
import com.multisensor.recording.ui.ShimmerConfigViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShimmerDashboard(
    modifier: Modifier = Modifier,
    viewModel: ShimmerConfigViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVisualization: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with device status
            ShimmerDashboardHeader(
                uiState = uiState,
                onSettingsClick = onNavigateToSettings,
                onVisualizationClick = onNavigateToVisualization
            )

            // Quick status cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    QuickStatusCard(
                        title = "Connection",
                        value = if (uiState.isDeviceConnected) "Connected" else "Disconnected",
                        icon = if (uiState.isDeviceConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        color = if (uiState.isDeviceConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                        onClick = { viewModel.scanForDevices() }
                    )
                }
                
                item {
                    QuickStatusCard(
                        title = "Battery",
                        value = if (uiState.batteryLevel >= 0) "${uiState.batteryLevel}%" else "--",
                        icon = Icons.Default.Battery1Bar,
                        color = when {
                            uiState.batteryLevel > 50 -> Color(0xFF4CAF50)
                            uiState.batteryLevel > 20 -> Color(0xFFFF9800)
                            uiState.batteryLevel >= 0 -> Color(0xFFF44336)
                            else -> Color.Gray
                        }
                    )
                }
                
                item {
                    QuickStatusCard(
                        title = "Recording",
                        value = if (uiState.isRecording) "Active" else "Stopped",
                        icon = if (uiState.isRecording) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        color = if (uiState.isRecording) Color(0xFFE91E63) else Color.Gray,
                        onClick = { 
                            if (uiState.isRecording) {
                                viewModel.stopStreaming()
                            } else if (uiState.canStartRecording) {
                                viewModel.startStreaming()
                            }
                        }
                    )
                }
                
                if (uiState.isRecording) {
                    item {
                        QuickStatusCard(
                            title = "Data Rate",
                            value = "${String.format("%.1f", uiState.dataPacketsReceived.toDouble() / max(1, uiState.recordingDuration / 1000))} Hz",
                            icon = Icons.Default.Speed,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }

            // Device controls (only show when connected)
            if (uiState.isDeviceConnected) {
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
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (uiState.isRecording) "Stop" else "Start")
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateToVisualization,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Visualize")
                    }
                }
            } else {
                // Show scan/connect button when not connected
                Button(
                    onClick = { viewModel.scanForDevices() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isScanning
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
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan for Devices")
                    }
                }
            }

            // Real-time sensor data preview (when recording)
            if (uiState.isRecording) {
                SensorDataPreview(uiState = uiState)
            }
        }
    }
}

@Composable
private fun ShimmerDashboardHeader(
    uiState: ShimmerConfigUiState,
    onSettingsClick: () -> Unit,
    onVisualizationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Shimmer GSR Device",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (uiState.selectedDevice != null) {
                Text(
                    text = uiState.selectedDevice!!.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = onVisualizationClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Visualization",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickStatusCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SensorDataPreview(uiState: ShimmerConfigUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Live Sensor Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Mock real-time data visualization - replace with actual sensor data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorValueDisplay(
                    label = "GSR",
                    value = "2.45 µS",
                    color = Color(0xFF3F51B5)
                )
                SensorValueDisplay(
                    label = "PPG",
                    value = "1024",
                    color = Color(0xFFE91E63)
                )
                SensorValueDisplay(
                    label = "Accel",
                    value = "0.98 g",
                    color = Color(0xFF4CAF50)
                )
                SensorValueDisplay(
                    label = "Gyro",
                    value = "0.1°/s",
                    color = Color(0xFFFF9800)
                )
            }
            
            // Recording session info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration: ${formatDuration(uiState.recordingDuration)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Packets: ${uiState.dataPacketsReceived}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SensorValueDisplay(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}