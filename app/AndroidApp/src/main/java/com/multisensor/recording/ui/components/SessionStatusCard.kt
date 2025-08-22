package com.multisensor.recording.ui.components
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multisensor.recording.recording.DeviceStatus
@Composable
fun SessionStatusCard(
    sessionStatus: String,
    deviceConnections: Map<String, DeviceStatus>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Session Status",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                SessionStatusBadge(status = sessionStatus)
            }
            if (deviceConnections.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connected Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(deviceConnections.entries.toList()) { (deviceName, status) ->
                            DeviceStatusChip(
                                deviceName = deviceName,
                                status = status
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SessionStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (colour, icon) = when (status.lowercase()) {
        "recording" -> Color(0xFFE53E3E) to Icons.Default.FiberManualRecord
        "connected" -> Color(0xFF38A169) to Icons.Default.CheckCircle
        "ready" -> Color(0xFF3182CE) to Icons.Default.CheckCircle
        "disconnected" -> Color(0xFF718096) to Icons.Default.Cancel
        "error" -> Color(0xFFE53E3E) to Icons.Default.Error
        else -> Color(0xFFED8936) to Icons.Default.Warning
    }
    val animatedColor by animateColorAsState(
        targetValue = colour,
        animationSpec = tween(300),
        label = "StatusColor"
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = animatedColor.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelLarge,
                color = animatedColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
@Composable
private fun DeviceStatusChip(
    deviceName: String,
    status: DeviceStatus,
    modifier: Modifier = Modifier
) {
    val (statusColor, icon) = when (status) {
        DeviceStatus.CONNECTED -> Color(0xFF38A169) to Icons.Default.CheckCircle
        DeviceStatus.DISCONNECTED -> Color(0xFF718096) to Icons.Default.Cancel
        DeviceStatus.CONNECTING -> Color(0xFFED8936) to Icons.Default.Sync
        DeviceStatus.ERROR -> Color(0xFFE53E3E) to Icons.Default.Error
    }
    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(300),
        label = "DeviceColor"
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getDeviceIcon(deviceName),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = animatedColor,
                            shape = CircleShape
                        )
                )
                Text(
                    text = status.displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = animatedColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@Composable
private fun getDeviceIcon(deviceName: String): ImageVector {
    return when (deviceName.lowercase()) {
        "camera", "rgb camera" -> Icons.Default.Videocam
        "thermal", "thermal camera" -> Icons.Default.Thermostat
        "gsr", "shimmer", "sensor" -> Icons.Default.Sensors
        "pc", "computer", "pc connection" -> Icons.Default.Computer
        "network" -> Icons.Default.Wifi
        else -> Icons.Default.DeviceHub
    }
}