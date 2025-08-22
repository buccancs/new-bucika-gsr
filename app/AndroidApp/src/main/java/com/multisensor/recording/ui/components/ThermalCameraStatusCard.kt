package com.multisensor.recording.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.ui.theme.ConnectionGreen
import com.multisensor.recording.ui.theme.DisconnectedRed
import com.multisensor.recording.ui.theme.RecordingActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalCameraStatusCard(
    status: ThermalRecorder.ThermalCameraStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status indicator
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
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = "Thermal Camera",
                        tint = if (status.isAvailable) ConnectionGreen else DisconnectedRed
                    )
                    Text(
                        text = "Thermal Camera",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Connection status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (status.isAvailable) ConnectionGreen else DisconnectedRed
                            )
                    )
                    Text(
                        text = if (status.isAvailable) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (status.isAvailable) ConnectionGreen else DisconnectedRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Device information
            if (status.isAvailable) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Device name and recording status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Device: ${status.deviceName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (status.isRecording) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Animated recording indicator
                                val infiniteTransition = rememberInfiniteTransition(label = "recording")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "recording_alpha"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RecordingActive.copy(alpha = alpha))
                                )
                                Text(
                                    text = "Recording",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RecordingActive,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Technical specifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Resolution",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${status.width}×${status.height}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Frame Rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${status.frameRate} fps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Frame Count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${status.frameCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Preview status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (status.isPreviewActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Preview Status",
                                modifier = Modifier.size(16.dp),
                                tint = if (status.isPreviewActive) ConnectionGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (status.isPreviewActive) "Preview Active" else "Preview Inactive",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (status.isPreviewActive) ConnectionGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // No device available message
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UsbOff,
                        contentDescription = "No device",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No thermal camera detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Connect Topdon TC001/TC001+ via USB-C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}