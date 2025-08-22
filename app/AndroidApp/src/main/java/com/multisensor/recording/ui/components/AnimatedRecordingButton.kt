package com.multisensor.recording.ui.components
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedRecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surface
            isRecording -> Color(0xFFE53E3E)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "ButtonColor"
    )
    val pulseScale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = if (isRecording) {
            infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(0)
        },
        label = "PulseScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isRecording) 16.dp else 8.dp,
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "ButtonElevation"
    )
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale * (if (isRecording) (0.95f + pulseScale * 0.05f) else 1.0f))
            .size(72.dp),
        containerColor = containerColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = elevation
        ),
        content = {
            AnimatedContent(
                targetState = isRecording,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 250,
                            delayMillis = 50,
                            easing = EaseInOutCubic
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = EaseInOutCubic
                        )
                    )
                },
                label = "ButtonIcon"
            ) { recording ->
                Icon(
                    imageVector = if (recording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (recording) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    )
}