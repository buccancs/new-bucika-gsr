package com.multisensor.recording.ui.compose.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {

    object Recording : Screen("recording", "Recording", Icons.Filled.RadioButtonChecked)
    object ThermalPreview : Screen("thermal_preview", "Preview", Icons.Filled.Visibility)
    object Devices : Screen("devices", "Devices", Icons.Filled.Devices)
    object Calibration : Screen("calibration", "Calibration", Icons.Filled.Tune)
    object Files : Screen("files", "Files", Icons.Filled.Folder)

    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object About : Screen("about", "About", Icons.Filled.Info)
    object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Filled.BugReport)
    object ShimmerSettings : Screen("shimmer_settings", "Shimmer", Icons.Filled.Bluetooth)
    object ShimmerVisualization : Screen("shimmer_viz", "Visualisation", Icons.Filled.Analytics)
    object ShimmerControl : Screen("shimmer_control", "Shimmer Control", Icons.Filled.DeviceHub)
}