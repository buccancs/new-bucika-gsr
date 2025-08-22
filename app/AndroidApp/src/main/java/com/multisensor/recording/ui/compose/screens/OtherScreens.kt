package com.multisensor.recording.ui.compose.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.multisensor.recording.ui.MainViewModel
import com.multisensor.recording.ui.DevicesUiState
import com.multisensor.recording.ui.DevicesViewModel
import com.multisensor.recording.ui.CalibrationViewModel
import com.multisensor.recording.ui.FileViewViewModel
import com.multisensor.recording.ui.SessionItem
import com.multisensor.recording.ui.SessionStatus
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devicesViewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by devicesViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Device Status Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.totalConnectedDevices} device(s) connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (uiState.totalConnectedDevices > 0)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.allDevicesHealthy) {
                    Text(
                        text = "All devices healthy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DeviceCard(
            title = "PC Controller",
            isConnected = uiState.isPcConnected,
            status = uiState.pcConnectionStatus,
            details = if (uiState.isPcConnected)
                "IP: ${uiState.pcIpAddress}\nPort: ${uiState.pcPort}\nLast seen: ${uiState.pcLastSeen}"
            else "Not connected",
            onConnect = { devicesViewModel.connectPc() },
            onDisconnect = { devicesViewModel.disconnectPc() },
            onTest = { devicesViewModel.testPcConnection() },
            isConnecting = uiState.isConnecting && !uiState.isPcConnected,
            isTesting = uiState.isTesting
        )

        Spacer(modifier = Modifier.height(8.dp))

        DeviceCard(
            title = "Shimmer Device",
            isConnected = uiState.isShimmerConnected,
            status = if (uiState.isShimmerConnected) "Connected" else "Disconnected",
            details = if (uiState.isShimmerConnected)
                "MAC: ${uiState.shimmerMacAddress}\nBattery: ${uiState.shimmerBatteryLevel}%\nSensors: ${uiState.shimmerActiveSensors}\nSample Rate: ${uiState.shimmerSampleRate} Hz\nLast seen: ${uiState.shimmerLastSeen}"
            else "Not connected",
            onConnect = { devicesViewModel.connectShimmer() },
            onDisconnect = { devicesViewModel.disconnectShimmer() },
            onTest = { devicesViewModel.testShimmerConnection() },
            isConnecting = uiState.isConnecting && !uiState.isShimmerConnected,
            isTesting = uiState.isTesting
        )

        Spacer(modifier = Modifier.height(8.dp))

        DeviceCard(
            title = "Thermal Camera",
            isConnected = uiState.isThermalConnected,
            status = if (uiState.isThermalConnected) "Connected" else "Disconnected",
            details = if (uiState.isThermalConnected)
                "Model: ${uiState.thermalCameraModel}\nTemp: ${uiState.thermalCurrentTemp}°C\nResolution: ${uiState.thermalResolution}\nFrame Rate: ${uiState.thermalFrameRate} fps\nLast seen: ${uiState.thermalLastSeen}"
            else "Not connected",
            onConnect = { devicesViewModel.connectThermal() },
            onDisconnect = { devicesViewModel.disconnectThermal() },
            onTest = { devicesViewModel.testThermalConnection() },
            isConnecting = uiState.isConnecting && !uiState.isThermalConnected,
            isTesting = uiState.isTesting
        )

        Spacer(modifier = Modifier.height(8.dp))

        DeviceCard(
            title = "Network",
            isConnected = uiState.isNetworkConnected,
            status = if (uiState.isNetworkConnected) "Connected" else "Disconnected",
            details = if (uiState.isNetworkConnected)
                "SSID: ${uiState.networkSsid}\nIP: ${uiState.networkIpAddress}\nSignal: ${uiState.networkSignalStrength}%\nType: ${uiState.networkType}"
            else "Not connected",
            onConnect = { devicesViewModel.connectNetwork() },
            onDisconnect = { devicesViewModel.disconnectNetwork() },
            onTest = { devicesViewModel.testNetworkConnection() },
            isConnecting = uiState.isConnecting && !uiState.isNetworkConnected,
            isTesting = uiState.isTesting
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { devicesViewModel.refreshAllDevices() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isConnecting
                ) {
                    if (uiState.isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Refresh All Devices")
                }
            }
        }

        if (uiState.testResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Test Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.testResults.takeLast(5).forEach { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = if (result.contains("PASSED"))
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    calibrationViewModel: CalibrationViewModel = hiltViewModel()
) {
    val uiState by calibrationViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Calibration Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val calibratedCount = listOf(
                    uiState.isCameraCalibrated,
                    uiState.isThermalCalibrated,
                    uiState.isShimmerCalibrated
                ).count { it }

                Text(
                    text = "$calibratedCount of 3 devices calibrated",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (calibratedCount == 3)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.isSystemValid) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "System ready for recording",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (uiState.validationErrors.isNotEmpty()) {
                    Column {
                        uiState.validationErrors.forEach { error ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CalibrationCard(
            title = "Camera Calibration",
            isCalibrated = uiState.isCameraCalibrated,
            isCalibrating = uiState.isCameraCalibrating,
            progress = uiState.cameraCalibrationProgress,
            details = if (uiState.isCameraCalibrated)
                "Calibration Error: ${uiState.cameraCalibrationError} pixels\nDate: ${uiState.cameraCalibrationDate}"
            else "Not calibrated",
            onStart = { calibrationViewModel.startCameraCalibration() },
            onReset = { calibrationViewModel.resetCameraCalibration() },
            canStart = uiState.canStartCalibration && !uiState.isCameraCalibrating
        )

        Spacer(modifier = Modifier.height(8.dp))

        CalibrationCard(
            title = "Thermal Camera Calibration",
            isCalibrated = uiState.isThermalCalibrated,
            isCalibrating = uiState.isThermalCalibrating,
            progress = uiState.thermalCalibrationProgress,
            details = if (uiState.isThermalCalibrated)
                "Temperature Range: ${uiState.thermalTempRange}\nEmissivity: ${uiState.thermalEmissivity}\nColor Palette: ${uiState.thermalColorPalette}\nDate: ${uiState.thermalCalibrationDate}"
            else "Not calibrated",
            onStart = { calibrationViewModel.startThermalCalibration() },
            onReset = { calibrationViewModel.resetThermalCalibration() },
            canStart = uiState.canStartCalibration && !uiState.isThermalCalibrating
        )

        Spacer(modifier = Modifier.height(8.dp))

        CalibrationCard(
            title = "Shimmer Device Calibration",
            isCalibrated = uiState.isShimmerCalibrated,
            isCalibrating = uiState.isShimmerCalibrating,
            progress = uiState.shimmerCalibrationProgress,
            details = if (uiState.isShimmerCalibrated)
                "MAC: ${uiState.shimmerMacAddress}\nSensors: ${uiState.shimmerSensorConfig}\nSample Rate: ${uiState.shimmerSamplingRate} Hz\nDate: ${uiState.shimmerCalibrationDate}"
            else "Not calibrated",
            onStart = { calibrationViewModel.startShimmerCalibration() },
            onReset = { calibrationViewModel.resetShimmerCalibration() },
            canStart = uiState.canStartCalibration && !uiState.isShimmerCalibrating
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { calibrationViewModel.validateSystem() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isValidating && !uiState.isAnyCalibrating
                ) {
                    if (uiState.isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Validate System")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { calibrationViewModel.saveCalibrationData() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAnyCalibrating
                    ) {
                        Text("Save")
                    }

                    OutlinedButton(
                        onClick = { calibrationViewModel.loadCalibrationData() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAnyCalibrating
                    ) {
                        Text("Load")
                    }

                    OutlinedButton(
                        onClick = { calibrationViewModel.exportCalibrationData() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isAnyCalibrating
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    fileViewModel: FileViewViewModel = hiltViewModel()
) {
    val uiState by fileViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Storage Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${uiState.sessions.size} sessions",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${uiState.totalFileCount} files total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.showStorageWarning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Storage Low",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uiState.storageUsagePercentage },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (uiState.showStorageWarning)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { fileViewModel.onSearchQueryChanged(it) },
            label = { Text("Search sessions") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { fileViewModel.refreshSessions() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoadingSessions
            ) {
                if (uiState.isLoadingSessions) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Refresh")
            }

            OutlinedButton(
                onClick = { fileViewModel.deleteAllSessions() },
                modifier = Modifier.weight(1f),
                enabled = uiState.sessions.isNotEmpty() && !uiState.isLoadingSessions,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.showEmptyState) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No sessions found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty())
                            "Try adjusting your search"
                        else "Record a session to see files here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn {
                items(uiState.filteredSessions) { session ->
                    SessionCard(
                        session = session,
                        isSelected = session == uiState.selectedSession,
                        onClick = { fileViewModel.selectSession(session) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        uiState.errorMessage?.let { message ->
            LaunchedEffect(message) {

                fileViewModel.clearError()
            }
        }

        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {

                fileViewModel.clearSuccess()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: SessionItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
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
                    text = session.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                SessionStatusChip(session.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Duration: ${session.formattedDuration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Files: ${session.fileCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = formatFileSize(session.totalSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(session.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (session.deviceTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    session.deviceTypes.forEach { deviceType ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    deviceType,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStatusChip(status: SessionStatus) {
    val (colour, text) = when (status) {
        SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary to "Completed"
        SessionStatus.INTERRUPTED -> MaterialTheme.colorScheme.error to "Interrupted"
        SessionStatus.CORRUPTED -> MaterialTheme.colorScheme.error to "Corrupted"
        SessionStatus.PROCESSING -> MaterialTheme.colorScheme.secondary to "Processing"
    }

    AssistChip(
        onClick = { },
        label = {
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = colour
            )
        },
        modifier = Modifier.height(24.dp)
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

@Composable
private fun CalibrationCard(
    title: String,
    isCalibrated: Boolean,
    isCalibrating: Boolean,
    progress: Int,
    details: String,
    onStart: () -> Unit,
    onReset: () -> Unit,
    canStart: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isCalibrated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isCalibrating -> "Calibrating..."
                            isCalibrated -> "Calibrated"
                            else -> "Not calibrated"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCalibrated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isCalibrating) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    enabled = canStart && !isCalibrating
                ) {
                    if (isCalibrating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isCalibrated) "Recalibrate" else "Start Calibration")
                }

                if (isCalibrated) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        enabled = !isCalibrating
                    ) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    title: String,
    isConnected: Boolean,
    status: String,
    details: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onTest: () -> Unit,
    isConnecting: Boolean = false,
    isTesting: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isConnected) {
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Connect")
                    }
                }

                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting && !isConnecting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test")
                }
            }
        }
    }
}