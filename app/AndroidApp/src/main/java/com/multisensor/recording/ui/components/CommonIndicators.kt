package com.multisensor.recording.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Unified recording indicator component used across camera and thermal previews
 */
@Composable
fun RecordingIndicator(
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = modifier.padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = alpha * 0.9f + 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = "Recording",
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Unified status overlay component for camera and device connection status
 */
@Composable
fun DeviceStatusOverlay(
    deviceName: String,
    icon: ImageVector = Icons.Default.Camera,
    isConnected: Boolean,
    isInitializing: Boolean,
    modifier: Modifier = Modifier,
    detailText: String? = null
) {
    Card(
        modifier = modifier.padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isConnected -> Color.Green.copy(alpha = 0.9f)
                isInitializing -> Color(0xFFFF9800).copy(alpha = 0.9f) // Orange
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = deviceName,
                modifier = Modifier.size(16.dp),
                tint = when {
                    isConnected -> Color.White
                    isInitializing -> Color.White
                    else -> MaterialTheme.colorScheme.primary
                }
            )
            Column {
                Text(
                    text = when {
                        isConnected -> "$deviceName Connected"
                        isInitializing -> "Initializing..."
                        else -> "$deviceName Disconnected"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isConnected -> Color.White
                        isInitializing -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                detailText?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isConnected -> Color.White.copy(alpha = 0.8f)
                            isInitializing -> Color.White.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * Common card container with consistent styling
 */
@Composable
fun PreviewCard(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
            content()
        }
    }
}