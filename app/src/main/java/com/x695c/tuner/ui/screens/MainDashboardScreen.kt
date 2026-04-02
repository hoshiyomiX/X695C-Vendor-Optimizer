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
    gameConfigAvailable: Boolean = false,
    scenarioConfigAvailable: Boolean = false,
    memoryConfigAvailable: Boolean = false,
    gameConfigChanged: Boolean = false,
    scenarioConfigChanged: Boolean = false,
    memoryConfigChanged: Boolean = false,
    rootState: RootState = RootState(),
    applyState: ApplyState = ApplyState(),
    hasUnsavedChanges: Boolean = false,
    onNavigateToGames: () -> Unit,
    onNavigateToScenarios: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onCopyLogs: () -> Unit,
    onRequestRoot: () -> Unit = {},
    onApplyConfiguration: () -> Unit,
    onDismissApplyResult: () -> Unit,
    onRebootDevice: () -> Unit = {},
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

        // No Root Warning Banner
        if (!rootState.isGranted && !rootState.isAvailable) {
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Root Access Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Configuration changes cannot be applied without root. The APK can only READ configs without root.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Quick Actions Row - Copy Logs & Apply Buttons
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

            // Apply Configuration Button
            Button(
                onClick = onApplyConfiguration,
                enabled = hasUnsavedChanges && (rootState.isGranted || rootState.isAvailable),
                modifier = Modifier.weight(1f)
            ) {
                if (applyState.isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (applyState.isApplying) "Applying..." else "Apply")
            }
        }

        // Unsaved Changes Indicator
        if (hasUnsavedChanges) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Unsaved changes - Press Apply to write to system",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // External Changes Warning Card
        if (gameConfigChanged || scenarioConfigChanged || memoryConfigChanged) {
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
                        imageVector = Icons.Default.Edit,
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
                                memoryConfigChanged
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

    // Apply Result Dialog
    if (applyState.showResultDialog && applyState.lastResult != null) {
        ApplyResultDialog(
            result = applyState.lastResult,
            onDismiss = onDismissApplyResult,
            onReboot = onRebootDevice
        )
    }
}

/**
 * Dialog showing the result of configuration apply operation.
 */
@Composable
private fun ApplyResultDialog(
    result: ApplyResult,
    onDismiss: () -> Unit,
    onReboot: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = if (result.success) "Success" else "Apply Failed",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Configuration write results:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ResultRow("Game Config", result.gameConfigWritten, result.gameConfigSkipped)
                ResultRow("Scenario Config", result.scenarioConfigWritten, result.scenarioConfigSkipped)
                ResultRow("Memory Config", result.memoryConfigWritten, result.memoryConfigSkipped)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${result.successCount} written, ${result.skippedCount} skipped / ${result.totalConfigs} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (result.skippedMessages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Skipped:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    result.skippedMessages.forEach { msg ->
                        Text(
                            text = "• $msg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (result.errorMessages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Errors:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    result.errorMessages.forEach { error ->
                        Text(
                            text = "• $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (result.success) {
                    TextButton(onClick = onReboot) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reboot")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    )
}

@Composable
private fun ResultRow(name: String, written: Boolean, skipped: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        when {
            skipped -> {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Skipped",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(skipped)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            written -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Written",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RootStatusIndicator(
    rootState: RootState,
    onRequestRoot: () -> Unit
) {
    when {
        rootState.isRequesting -> {
            // FLOW-H006: Show loading spinner while root grant is in progress
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Root",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
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
            // FLOW-H006: This state is now reachable — su binary detected but root not yet granted.
            // The shield button triggers the su grant prompt.
            IconButton(onClick = onRequestRoot) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Grant Root Access",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Block,
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
    memoryChanged: Boolean
): String {
    val changed = mutableListOf<String>()
    if (gameChanged) changed.add("Game")
    if (scenarioChanged) changed.add("Scenario")
    if (memoryChanged) changed.add("Memory")
    
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
