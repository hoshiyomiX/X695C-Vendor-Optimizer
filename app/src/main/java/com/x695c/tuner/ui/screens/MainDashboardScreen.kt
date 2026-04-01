package com.x695c.tuner.ui.screens

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
import com.x695c.tuner.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    selectedProfile: TuningProfile,
    onProfileChange: (TuningProfile) -> Unit,
    gameConfigs: Map<String, GameTuningConfig>,
    scenarioConfigs: Map<String, PerformanceScenarioConfig>,
    memoryConfig: MemoryManagementConfig,
    gpuConfig: GpuDvfsConfig,
    gameConfigAvailable: Boolean = false,
    scenarioConfigAvailable: Boolean = false,
    memoryConfigAvailable: Boolean = false,
    gpuConfigAvailable: Boolean = false,
    gameConfigChanged: Boolean = false,
    scenarioConfigChanged: Boolean = false,
    memoryConfigChanged: Boolean = false,
    gpuConfigChanged: Boolean = false,
    rootState: RootState = RootState(),
    onNavigateToGames: () -> Unit,
    onNavigateToScenarios: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToGpu: () -> Unit,
    onCopyLogs: () -> Unit,
    onRequestRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card - MD3 Expressive
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "X695C Vendor Tuner",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Infinix Note 10 Pro NFC | Helio G95",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Root status indicator
                    RootStatusIndicator(
                        rootState = rootState,
                        onRequestRoot = onRequestRoot
                    )
                }
            }
        }

        // Quick Actions Row - Copy Logs Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy Logs Button
            OutlinedButton(
                onClick = onCopyLogs,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Logs")
            }

            // Root Access Button (show if not granted)
            if (!rootState.isGranted && !rootState.isAvailable) {
                Button(
                    onClick = onRequestRoot,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Root")
                }
            }
        }

        // External Changes Warning Card
        if (gameConfigChanged || scenarioConfigChanged || memoryConfigChanged || gpuConfigChanged) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "External Changes Detected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = buildChangedConfigsText(
                                gameConfigChanged, scenarioConfigChanged,
                                memoryConfigChanged, gpuConfigChanged
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Profile Selection - MD3 Expressive
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tuning Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProfile.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Active Profile") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TuningProfile.entries.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = profile.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onProfileChange(profile)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section Header
        Text(
            text = "Configuration Sections",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Config Cards - MD3 Expressive with change indicators
        ConfigCard(
            title = "Game Tuning",
            subtitle = "${gameConfigs.size} games configured",
            icon = Icons.Default.VideogameAsset,
            configAvailable = gameConfigAvailable,
            hasExternalChange = gameConfigChanged,
            onClick = onNavigateToGames
        )

        ConfigCard(
            title = "Performance Scenarios",
            subtitle = "${scenarioConfigs.size} scenarios configured",
            icon = Icons.Default.Speed,
            configAvailable = scenarioConfigAvailable,
            hasExternalChange = scenarioConfigChanged,
            onClick = onNavigateToScenarios
        )

        ConfigCard(
            title = "Memory Management",
            subtitle = "RAM: 6GB Configuration",
            icon = Icons.Default.Memory,
            configAvailable = memoryConfigAvailable,
            hasExternalChange = memoryConfigChanged,
            onClick = onNavigateToMemory
        )

        ConfigCard(
            title = "GPU DVFS Settings",
            subtitle = "Mali-G76 MC4 @ 720-900 MHz",
            icon = Icons.Default.Games,
            configAvailable = gpuConfigAvailable,
            hasExternalChange = gpuConfigChanged,
            onClick = onNavigateToGpu
        )

        // Info Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Adjust settings to optimize performance for your usage. Changes require root access to apply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RootStatusIndicator(
    rootState: RootState,
    onRequestRoot: () -> Unit
) {
    when {
        rootState.isGranted -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Root Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Root",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        rootState.isAvailable && !rootState.isGranted -> {
            IconButton(onClick = onRequestRoot) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Request Root",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        else -> {
            Icon(
                imageVector = Icons.Default.NoCell,
                contentDescription = "No Root",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun buildChangedConfigsText(
    gameChanged: Boolean,
    scenarioChanged: Boolean,
    memoryChanged: Boolean,
    gpuChanged: Boolean
): String {
    val changed = mutableListOf<String>()
    if (gameChanged) changed.add("Game")
    if (scenarioChanged) changed.add("Scenario")
    if (memoryChanged) changed.add("Memory")
    if (gpuChanged) changed.add("GPU")
    
    return "Config files modified: ${changed.joinToString(", ")}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    configAvailable: Boolean,
    hasExternalChange: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                tint = if (configAvailable) {
                    if (hasExternalChange) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                } else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!configAvailable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Config not available",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (hasExternalChange) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "External change detected",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = when {
                        hasExternalChange -> "Externally modified"
                        configAvailable -> subtitle
                        else -> "Config file not detected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        hasExternalChange -> MaterialTheme.colorScheme.error
                        configAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
