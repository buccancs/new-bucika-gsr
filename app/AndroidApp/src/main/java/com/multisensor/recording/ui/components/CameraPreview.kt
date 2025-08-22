package com.multisensor.recording.ui.components

import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multisensor.recording.ui.MainViewModel

@Composable
fun CameraPreview(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onTextureViewReady: (TextureView) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by hiltViewModel<MainViewModel>().uiState.collectAsStateWithLifecycle()
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    // Notify parent when TextureView is ready
    LaunchedEffect(textureView) {
        textureView?.let { onTextureViewReady(it) }
    }

    PreviewCard(
        modifier = modifier,
        height = 250.dp
    ) {
        // Camera TextureView
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    textureView = this
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )

        // Recording indicator overlay
        if (isRecording) {
            RecordingIndicator(
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        // Enhanced camera info overlay with better status information
        DeviceStatusOverlay(
            deviceName = "RGB Camera",
            icon = Icons.Default.Camera,
            isConnected = uiState.isCameraConnected,
            isInitializing = uiState.isConnecting,
            detailText = when {
                uiState.isCameraConnected -> "Camera ready"
                uiState.isConnecting -> "Connecting to camera..."
                !uiState.isInitialized -> "System initializing..."
                else -> "Camera not available - check permissions"
            },
            modifier = Modifier.align(Alignment.BottomStart)
        )
        
        // Add a semi-transparent overlay when camera is not connected to make status more visible
        if (!uiState.isCameraConnected && !uiState.isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "No camera",
                        modifier = Modifier.size(48.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "RGB Camera Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (uiState.isInitialized) {
                            "Camera not connected"
                        } else {
                            "Initializing system..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}





@Composable
fun ThermalPreviewSurface(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onSurfaceViewReady: (SurfaceView) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by hiltViewModel<MainViewModel>().uiState.collectAsStateWithLifecycle()
    val viewModel: MainViewModel = hiltViewModel()
    val thermalStatus by viewModel.thermalStatus.collectAsStateWithLifecycle()
    var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    // Notify parent when SurfaceView is ready
    LaunchedEffect(surfaceView) {
        surfaceView?.let { onSurfaceViewReady(it) }
    }

    PreviewCard(
        modifier = modifier,
        height = 200.dp
    ) {
        // Thermal SurfaceView
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    surfaceView = this
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Set a dark background for thermal view
                    setBackgroundColor(Color.Black.toArgb())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )

        // Enhanced thermal status overlay
        if (!thermalStatus.isAvailable || !thermalStatus.isPreviewActive) {
            DeviceStatusOverlay(
                deviceName = "Thermal",
                icon = Icons.Default.Thermostat,
                isConnected = thermalStatus.isAvailable,
                isInitializing = uiState.isConnecting,
                detailText = when {
                    thermalStatus.isAvailable && !thermalStatus.isPreviewActive -> "Camera connected - starting preview..."
                    thermalStatus.isAvailable -> "Thermal camera active"
                    uiState.isConnecting -> "Connecting to camera..."
                    else -> "Connect thermal camera"
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Thermal frame info overlay (top-left)
        if (thermalStatus.isAvailable && thermalStatus.isPreviewActive) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${thermalStatus.width}×${thermalStatus.height}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${thermalStatus.frameRate} fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (thermalStatus.frameCount > 0) {
                        Text(
                            text = "Frame: ${thermalStatus.frameCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Recording indicator overlay (top-right)
        if (isRecording && thermalStatus.isRecording) {
            RecordingIndicator(
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        
        // Device name overlay (bottom-left)
        if (thermalStatus.isAvailable) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = thermalStatus.deviceName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

