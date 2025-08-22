package com.multisensor.recording.ui.compose.screens
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multisensor.recording.recording.DeviceStatus
import com.multisensor.recording.ui.MainUiState
import com.multisensor.recording.ui.MainViewModel
import com.multisensor.recording.ui.components.AnimatedRecordingButton
import com.multisensor.recording.ui.components.CameraPreview
import com.multisensor.recording.ui.components.ColorPaletteSelector
import com.multisensor.recording.ui.components.ThermalPreviewSurface
import com.multisensor.recording.ui.components.ThermalCameraStatusCard
import com.multisensor.recording.ui.components.ThermalControlsPanel
import com.multisensor.recording.ui.components.SessionStatusCard
import com.multisensor.recording.ui.components.ShimmerDashboard
import com.multisensor.recording.ui.theme.ConnectionGreen
import com.multisensor.recording.ui.theme.DisconnectedRed
import com.multisensor.recording.ui.theme.RecordingActive
import com.multisensor.recording.ui.theme.RecordingInactive
import kotlin.math.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onNavigateToPreview: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val thermalStatus by viewModel.thermalStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Track preview components readiness
    var cameraTextureView by remember { mutableStateOf<TextureView?>(null) }
    var thermalSurfaceView by remember { mutableStateOf<SurfaceView?>(null) }
    var initializationAttempted by remember { mutableStateOf(false) }
    
    // Camera switching state - true for thermal/IR, false for RGB
    var showThermalCamera by remember { mutableStateOf(false) }
    
    // Show thermal camera status panel state
    var showThermalStatus by remember { mutableStateOf(false) }
    
    // Show thermal controls panel state  
    var showThermalControls by remember { mutableStateOf(false) }

    // Initialize system when both preview components are ready
    LaunchedEffect(cameraTextureView, thermalSurfaceView) {
        if (cameraTextureView != null && !initializationAttempted) {
            initializationAttempted = true
            android.util.Log.d("RecordingScreen", "Starting device initialization with TextureView and SurfaceView")
            
            // Initialize the system with the actual views
            // Note: We need both views for full system initialization even if only one is displayed
            viewModel.initializeSystem(cameraTextureView!!, thermalSurfaceView)
            
            // Also try to connect to PC server automatically
            viewModel.connectToPC()
        }
    }

    // Show any errors that occur during initialization
    if (uiState.showErrorDialog && !uiState.errorMessage.isNullOrBlank()) {
        LaunchedEffect(uiState.errorMessage) {
            // Log the error for debugging
            android.util.Log.e("RecordingScreen", "Initialization error: ${uiState.errorMessage}")
        }
    }
    Scaffold(
        floatingActionButton = {
            AnimatedRecordingButton(
                isRecording = uiState.isRecording,
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
                enabled = uiState.canStartRecording || uiState.canStopRecording
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(4.dp), // Reduced padding from 16.dp to 4.dp
            verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing from 16.dp to 8.dp
        ) {
            // Compact session status display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        uiState.isRecording -> "Recording"
                        uiState.isInitialized -> "Ready"
                        else -> "Initializing"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.isRecording) RecordingActive else MaterialTheme.colorScheme.onSurface
                )
                
                // Camera controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Thermal controls button
                    IconButton(
                        onClick = { showThermalControls = !showThermalControls },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Thermal Controls",
                            modifier = Modifier.size(16.dp),
                            tint = if (showThermalControls) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Thermal status button
                    IconButton(
                        onClick = { showThermalStatus = !showThermalStatus },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = "Thermal Status",
                            modifier = Modifier.size(16.dp),
                            tint = if (thermalStatus.isAvailable) ConnectionGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Camera switch
                    Text(
                        text = "RGB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!showThermalCamera) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = showThermalCamera,
                        onCheckedChange = { showThermalCamera = it },
                        modifier = Modifier.scale(0.8f) // Smaller switch
                    )
                    Text(
                        text = "Thermal",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showThermalCamera) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Thermal Camera Controls Panel (collapsible)
            AnimatedVisibility(
                visible = showThermalControls,
                enter = slideInVertically() + expandVertically() + fadeIn(),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                ThermalControlsPanel(
                    status = thermalStatus,
                    temperatureRange = uiState.temperatureRange,
                    onTemperatureRangeChange = { /* TODO: Implement temperature range change */ },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Thermal Camera Status Card (collapsible)
            AnimatedVisibility(
                visible = showThermalStatus,
                enter = slideInVertically() + expandVertically() + fadeIn(),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                ThermalCameraStatusCard(
                    status = thermalStatus,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Camera Preview - Always create both views for initialization, but only display one
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take up all remaining space
            ) {
                // RGB Camera Preview - Always present for initialization
                CameraPreview(
                    isRecording = uiState.isRecording,
                    onTextureViewReady = { textureView ->
                        cameraTextureView = textureView
                    },
                    modifier = if (!showThermalCamera) Modifier.fillMaxSize() else Modifier.size(0.dp)
                )

                // Thermal Camera Preview - Always present for initialization
                ThermalPreviewSurface(
                    isRecording = uiState.isRecording,
                    onSurfaceViewReady = { surfaceView ->
                        thermalSurfaceView = surfaceView
                    },
                    modifier = if (showThermalCamera) Modifier.fillMaxSize() else Modifier.size(0.dp)
                )
            }

            // Shimmer Device Dashboard
            ShimmerDashboard(
                onNavigateToSettings = {
                    // Navigate to Shimmer settings
                    android.content.Intent(context, com.multisensor.recording.ui.ShimmerSettingsActivity::class.java).also {
                        context.startActivity(it)
                    }
                },
                onNavigateToVisualization = {
                    // Navigate to Shimmer visualization
                    android.content.Intent(context, com.multisensor.recording.ui.ShimmerVisualizationActivity::class.java).also {
                        context.startActivity(it)
                    }
                }
            )
            
            // Quick access to comprehensive Shimmer Control
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Advanced Shimmer Controls",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Access full device configuration and diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { onNavigateToPreview() } // This will navigate to Shimmer Control
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeviceHub,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Control Panel")
                    }
                }
            }

            // ColorPaletteSelector(
            //     currentPalette = uiState.colorPalette,
            //     onPaletteSelect = {  }
            // )
        }
    }
}