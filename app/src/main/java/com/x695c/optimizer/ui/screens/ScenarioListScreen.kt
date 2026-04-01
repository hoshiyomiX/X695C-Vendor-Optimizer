package com.x695c.optimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x695c.optimizer.data.PerformanceScenarioConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioListScreen(
    scenarioConfigs: Map<String, PerformanceScenarioConfig>,
    onScenarioSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "Performance Scenarios",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )

        // Scenario List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Select a scenario to configure performance settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(scenarioConfigs.entries.toList()) { (scenarioName, config) ->
                Card(
                    onClick = { onScenarioSelect(scenarioName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getScenarioIcon(scenarioName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getScenarioDisplayName(scenarioName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = getScenarioDescription(scenarioName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (config.cpuFreqMinCluster0 > 0) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { 
                                            Text(
                                                text = "CPU: Max",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                if (config.uclampMin.value > 0) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { 
                                            Text(
                                                text = "UCLAMP: ${config.uclampMin.value}%",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scenarios are triggered automatically by system events.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun getScenarioIcon(scenarioName: String) = when (scenarioName) {
    "LAUNCH" -> Icons.AutoMirrored.Filled.Launch
    "MTKPOWER_HINT_APP_TOUCH" -> Icons.Default.TouchApp
    "MTKPOWER_HINT_PROCESS_CREATE" -> Icons.Default.AddCircle
    "MTKPOWER_HINT_APP_ROTATE" -> Icons.Default.ScreenRotation
    "MTKPOWER_HINT_FLINGER_PRINT" -> Icons.Default.Fingerprint
    "MTKPOWER_HINT_PACK_SWITCH" -> Icons.Default.SwapHoriz
    "MTKPOWER_HINT_ACT_SWITCH" -> Icons.Default.SwapVert
    "INTERACTION" -> Icons.Default.TouchApp
    "MTKPOWER_HINT_GALLERY_BOOST" -> Icons.Default.PhotoLibrary
    "MTKPOWER_HINT_WFD" -> Icons.Default.Cast
    else -> Icons.Default.Settings
}

private fun getScenarioDisplayName(scenarioName: String) = when (scenarioName) {
    "LAUNCH" -> "App Launch"
    "MTKPOWER_HINT_APP_TOUCH" -> "Touch Response"
    "MTKPOWER_HINT_PROCESS_CREATE" -> "Process Creation"
    "MTKPOWER_HINT_APP_ROTATE" -> "Screen Rotation"
    "MTKPOWER_HINT_FLINGER_PRINT" -> "Fingerprint Scan"
    "MTKPOWER_HINT_PACK_SWITCH" -> "Package Switch"
    "MTKPOWER_HINT_ACT_SWITCH" -> "Activity Switch"
    "INTERACTION" -> "UI Interaction"
    "MTKPOWER_HINT_GALLERY_BOOST" -> "Gallery Boost"
    "MTKPOWER_HINT_WFD" -> "WiFi Display"
    else -> scenarioName.replace("MTKPOWER_HINT_", "").replace("_", " ")
}

private fun getScenarioDescription(scenarioName: String) = when (scenarioName) {
    "LAUNCH" -> "Boosts performance when launching apps"
    "MTKPOWER_HINT_APP_TOUCH" -> "Quick CPU boost on touch events"
    "MTKPOWER_HINT_PROCESS_CREATE" -> "Boosts for new process creation"
    "MTKPOWER_HINT_APP_ROTATE" -> "Smooth screen rotation animation"
    "MTKPOWER_HINT_FLINGER_PRINT" -> "Fast fingerprint authentication"
    "MTKPOWER_HINT_PACK_SWITCH" -> "Optimizes package switching"
    "MTKPOWER_HINT_ACT_SWITCH" -> "Fast activity transitions"
    "INTERACTION" -> "Smooth scrolling and fling gestures"
    "MTKPOWER_HINT_GALLERY_BOOST" -> "Fast gallery image loading"
    "MTKPOWER_HINT_WFD" -> "Wireless display optimization"
    else -> "System performance scenario"
}
