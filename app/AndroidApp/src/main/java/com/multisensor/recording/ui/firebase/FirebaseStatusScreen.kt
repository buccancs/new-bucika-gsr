package com.multisensor.recording.ui.firebase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Firebase status UI component showing integration status and statistics
 * Enhanced with authentication management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseStatusScreen(
    viewModel: FirebaseStatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFirebaseStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Firebase Integration Status",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Firebase services status cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Authentication status
            item {
                AuthenticationServiceCard(
                    isEnabled = uiState.authenticationEnabled,
                    isAuthenticated = uiState.isAuthenticated,
                    currentUser = uiState.currentUser,
                    onLogout = { viewModel.logout() }
                )
            }

            item {
                FirebaseServiceCard(
                    title = "Analytics",
                    description = "Event tracking and user analytics",
                    icon = Icons.Default.Analytics,
                    isEnabled = uiState.analyticsEnabled,
                    eventsLogged = uiState.analyticsEventsCount
                )
            }

            item {
                FirebaseServiceCard(
                    title = "Firestore",
                    description = "Research data and session metadata",
                    icon = Icons.Default.Storage,
                    isEnabled = uiState.firestoreEnabled,
                    documentsStored = uiState.firestoreDocumentCount
                )
            }

            item {
                FirebaseServiceCard(
                    title = "Storage",
                    description = "Video and sensor data files",
                    icon = Icons.Default.CloudUpload,
                    isEnabled = uiState.storageEnabled,
                    bytesUploaded = uiState.storageBytesUploaded
                )
            }

            // Usage statistics (only shown when authenticated)
            if (uiState.isAuthenticated) {
                item {
                    UsageStatisticsCard(
                        totalSessions = uiState.totalSessions,
                        totalDataSize = uiState.totalDataSize,
                        averageSessionDuration = uiState.averageSessionDuration
                    )
                }
            }

            // Test Firebase functionality buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Test Firebase Integration",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.testAnalytics()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Analytics")
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.testFirestore()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.isAuthenticated
                        ) {
                            Text("Test Firestore")
                        }
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.testAuthentication()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isAuthenticated
                    ) {
                        Text("Test Authentication")
                    }
                }
            }

            // Recent activities
            if (uiState.recentActivities.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Recent Firebase Activities",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(uiState.recentActivities.take(5)) { activity ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = activity.action,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = activity.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthenticationServiceCard(
    isEnabled: Boolean,
    isAuthenticated: Boolean,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAuthenticated) Icons.Default.Person else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Authentication",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isAuthenticated) 
                        "Multi-researcher access control" 
                    else 
                        "Sign in required for full features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isAuthenticated && currentUser != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Signed in as: ${currentUser.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (currentUser.displayName?.isNotBlank() == true) {
                        Text(
                            text = currentUser.displayName!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (isAuthenticated) {
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sign out",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isAuthenticated) {
                                MaterialTheme.colorScheme.primary
                            } else if (isEnabled) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun UsageStatisticsCard(
    totalSessions: Int,
    totalDataSize: Long,
    averageSessionDuration: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Usage Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Sessions",
                    value = totalSessions.toString()
                )
                
                StatisticItem(
                    label = "Data Size",
                    value = formatBytes(totalDataSize)
                )
                
                StatisticItem(
                    label = "Avg Duration",
                    value = formatDuration(averageSessionDuration)
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

@Composable
fun FirebaseServiceCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    eventsLogged: Int? = null,
    documentsStored: Int? = null,
    bytesUploaded: Long? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
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
                modifier = Modifier.size(40.dp),
                tint = if (isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show relevant statistics
                when {
                    eventsLogged != null -> {
                        Text(
                            text = "$eventsLogged events logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    documentsStored != null -> {
                        Text(
                            text = "$documentsStored documents stored",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    bytesUploaded != null -> {
                        val sizeStr = when {
                            bytesUploaded < 1024 -> "$bytesUploaded B"
                            bytesUploaded < 1024 * 1024 -> "${bytesUploaded / 1024} KB"
                            else -> "${bytesUploaded / (1024 * 1024)} MB"
                        }
                        Text(
                            text = "$sizeStr uploaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            CircleShape
                        )
                )
            }
        }
    }
}