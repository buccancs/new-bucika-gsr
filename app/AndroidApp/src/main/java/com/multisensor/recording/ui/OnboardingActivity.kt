package com.multisensor.recording.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.multisensor.recording.MainActivity
import com.multisensor.recording.R
import com.multisensor.recording.ui.theme.MultiSensorTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        setContent {
            MultiSensorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingFlow(
                        onComplete = { completeOnboarding() },
                        onRequestPermissions = { requestPermissions(it) }
                    )
                }
            }
        }
    }

    private fun completeOnboarding() {
        sharedPreferences.edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun requestPermissions(permissions: List<String>) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    companion object {
        fun shouldShowOnboarding(sharedPreferences: SharedPreferences): Boolean {
            return !sharedPreferences.getBoolean("onboarding_completed", false)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onRequestPermissions: (List<String>) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> FeaturePage()
                2 -> PermissionsPage(onRequestPermissions = onRequestPermissions)
                3 -> SetupPage()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            if (pagerState.currentPage < 3) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onComplete
                ) {
                    Text("Get Started")
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Sensors,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Multi-Sensor Recording",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Advanced multi-modal sensor recording system for contactless GSR prediction research.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeaturePage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Powerful Features",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val features = listOf(
            Triple(Icons.Filled.Camera, "Multi-Camera Recording", "Synchronous recording from multiple camera sources with precise timing"),
            Triple(Icons.Filled.Thermostat, "Thermal Imaging", "Real-time thermal camera integration for temperature monitoring"),
            Triple(Icons.Filled.Sensors, "GSR Sensing", "Galvanic skin response measurement via Shimmer devices"),
            Triple(Icons.Filled.Computer, "PC Integration", "Seamless communication with master controller systems"),
            Triple(Icons.Filled.Cloud, "Offline-First", "Local recording with optional cloud synchronisation"),
            Triple(Icons.Filled.Analytics, "Research Ready", "Built specifically for academic research workflows")
        )

        items(features) { (icon, title, description) ->
            FeatureCard(
                icon = icon,
                title = title,
                description = description
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PermissionsPage(
    onRequestPermissions: (List<String>) -> Unit
) {
    val permissions = listOf(
        Triple(Manifest.permission.CAMERA, "Camera", "Required for video recording and computer vision processing"),
        Triple(Manifest.permission.RECORD_AUDIO, "Microphone", "Optional for audio synchronisation with video streams"),
        Triple(Manifest.permission.ACCESS_FINE_LOCATION, "Location", "Required for Bluetooth device discovery and connection"),
        Triple(Manifest.permission.BLUETOOTH_SCAN, "Bluetooth", "Required for connecting to Shimmer GSR sensors"),
        Triple(Manifest.permission.POST_NOTIFICATIONS, "Notifications", "For recording status and system alerts")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "The app needs these permissions to function properly:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(permissions) { (permission, name, description) ->
            PermissionCard(
                permission = permission,
                name = name,
                description = description
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onRequestPermissions(permissions.map { it.first })
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
fun PermissionCard(
    permission: String,
    name: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (permission) {
                    Manifest.permission.CAMERA -> Icons.Filled.Camera
                    Manifest.permission.RECORD_AUDIO -> Icons.Filled.Mic
                    Manifest.permission.ACCESS_FINE_LOCATION -> Icons.Filled.LocationOn
                    Manifest.permission.BLUETOOTH_SCAN -> Icons.Filled.Bluetooth
                    Manifest.permission.POST_NOTIFICATIONS -> Icons.Filled.Notifications
                    else -> Icons.Filled.Security
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SetupPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your multi-sensor recording system is ready to use. You can now:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        val setupItems = listOf(
            "Connect and configure your devices",
            "Calibrate cameras and sensors",
            "Start recording sessions",
            "Manage and export your data"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            setupItems.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
