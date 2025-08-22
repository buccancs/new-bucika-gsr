package com.multisensor.recording.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
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
import com.multisensor.recording.recording.ThermalRecorder
import com.multisensor.recording.util.TemperatureRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalControlsPanel(
    status: ThermalRecorder.ThermalCameraStatus,
    temperatureRange: TemperatureRange = TemperatureRange.BODY_TEMPERATURE,
    onTemperatureRangeChange: (TemperatureRange) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            // Header with expand/collapse button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Thermal Controls",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Thermal Camera Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                IconButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically() + expandVertically() + fadeIn(),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Temperature Range Controls
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Temperature Range",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TemperatureRangeButton(
                                    range = TemperatureRange.BODY_TEMPERATURE,
                                    current = temperatureRange,
                                    onClick = onTemperatureRangeChange,
                                    modifier = Modifier.weight(1f)
                                )
                                TemperatureRangeButton(
                                    range = TemperatureRange.ROOM_TEMPERATURE,
                                    current = temperatureRange,
                                    onClick = onTemperatureRangeChange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TemperatureRangeButton(
                                    range = TemperatureRange.INDUSTRIAL,
                                    current = temperatureRange,
                                    onClick = onTemperatureRangeChange,
                                    modifier = Modifier.weight(1f)
                                )
                                TemperatureRangeButton(
                                    range = TemperatureRange.AUTOMOTIVE,
                                    current = temperatureRange,
                                    onClick = onTemperatureRangeChange,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    


                    
                    // Current Status Summary
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Status Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Range:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${temperatureRange.min.toInt()}°C - ${temperatureRange.max.toInt()}°C",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            
                            if (status.frameCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Frames:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${status.frameCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureRangeButton(
    range: TemperatureRange,
    current: TemperatureRange,
    onClick: (TemperatureRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = range == current
    
    if (isSelected) {
        Button(
            onClick = { onClick(range) },
            modifier = modifier
        ) {
            Text(
                text = range.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = { onClick(range) },
            modifier = modifier
        ) {
            Text(
                text = range.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}