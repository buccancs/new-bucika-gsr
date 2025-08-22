package com.multisensor.recording.ui.compose.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.multisensor.recording.ui.compose.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentScreen = getScreenFromRoute(navBackStackEntry?.destination?.route)
                    Text(currentScreen?.title ?: "Multi-Sensor Recording")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Add Shimmer Control button for easy access
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.ShimmerControl.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeviceHub,
                            contentDescription = "Shimmer Control"
                        )
                    }

                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        // bottomBar = {
        //     EnhancedBottomNavigation(navController = navController)
        // },
        floatingActionButton = {
            // Only show FAB for screens that need one
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            when (currentRoute) {
                Screen.Files.route -> {
                    FloatingActionButton(
                        onClick = { /* Refresh files functionality could be added here */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
                // Recording screen has its own AnimatedRecordingButton
                // No FAB needed for other screens
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recording.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(Screen.Recording.route) {
                RecordingScreen(
                    onNavigateToPreview = {
                        navController.navigate(Screen.ThermalPreview.route)
                    }
                )
            }

            composable(Screen.ThermalPreview.route) {
                ThermalPreviewScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Devices.route) {
                DevicesScreen()
            }

            composable(Screen.Calibration.route) {
                CalibrationScreen()
            }

            composable(Screen.Files.route) {
                FilesScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.ShimmerSettings.route) {
                ShimmerSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.ShimmerVisualization.route) {
                ShimmerVisualizationScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.ShimmerControl.route) {
                ShimmerControlScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun EnhancedBottomNavigation(navController: NavHostController) {
    val items = listOf(
        Screen.Recording,
        Screen.Devices,
        Screen.Calibration,
        Screen.Files,
        Screen.About
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {

                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }

                        launchSingleTop = true

                        restoreState = true
                    }
                }
            )
        }
    }
}

private fun getScreenFromRoute(route: String?): Screen? {
    return when (route) {
        Screen.Recording.route -> Screen.Recording
        Screen.ThermalPreview.route -> Screen.ThermalPreview
        Screen.Devices.route -> Screen.Devices
        Screen.Calibration.route -> Screen.Calibration
        Screen.Files.route -> Screen.Files
        Screen.Settings.route -> Screen.Settings
        Screen.About.route -> Screen.About
        Screen.Diagnostics.route -> Screen.Diagnostics
        Screen.ShimmerSettings.route -> Screen.ShimmerSettings
        Screen.ShimmerVisualization.route -> Screen.ShimmerVisualization
        Screen.ShimmerControl.route -> Screen.ShimmerControl
        else -> null
    }
}