package com.x695c.optimizer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x695c.optimizer.data.*
import com.x695c.optimizer.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryManagementScreen(
    config: MemoryManagementConfig,
    configAvailable: Boolean = false,
    onConfigChange: (MemoryManagementConfig) -> Unit
    onBack: () -> Unit
    modifier: Modifier.fillMaxSize()
) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar - MD3 Expressive
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Memory Management",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RAM: 6GB Configuration",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Config status warning
                if (!configAvailable) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Memory config file not detected. Settings are for reference only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Memory Thresholds
                Text(
                    text = "Memory Thresholds (Pages)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IntDropdownSelector(
                    label = "Native Process Threshold",
                    value = config.thresholds.adjNative,
                    options = listOf(
                        512 to "512 (Aggressive)",
                        768 to "768",
                        1024 to "1024 (Default)",
                        1536 to "1536",
                        2048 to "2048 (Conservative)"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            thresholds = config.thresholds.copy(adjNative = it)
                    )) 
                }
                IntDropdownSelector(
                    label = "System Process Threshold",
                    value = config.thresholds.adjSystem,
                    options = listOf(
                        512 to "512 (Aggressive)",
                        768 to "768",
                        1024 to "1024 (Default)",
                        1536 to "1536",
                        2048 to "2048 (Conservative)"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            thresholds = config.thresholds.copy(adjSystem = it)
                    )) 
                }
                IntDropdownSelector(
                    label = "Cached App Threshold",
                    value = config.thresholds.adjCached,
                    options = listOf(
                        400 to "400 (More Cache)",
                        500 to "500",
                        600 to "600",
                        700 to "700 (Default)",
                        800 to "800 (Less Cache)"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            thresholds = config.thresholds.copy(adjCached = it)
                    )) 
                }
                IntDropdownSelector(
                    label = "Foreground App Threshold",
                    value = config.thresholds.adjForeground,
                    options = listOf(
                        100 to "100 (Protect)",
                        200 to "200 (Default)",
                        300 to "300",
                        400 to "400"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            thresholds = config.thresholds.copy(adjForeground = it)
                    )) 
                }
                // Swap Settings
                Text(
                    text = "Swap Settings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IntDropdownSelector(
                    label = "Swap Free Min Percent",
                    value = config.thresholds.swapfreeMinPercent,
                    options = listOf(
                        2 to "2%",
                        5 to "5% (Default)",
                        8 to "8%",
                        10 to "10%"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            thresholds = config.thresholds.copy(swapfreeMinPercent = it)
                    )) 
                }
                IntDropdownSelector(
                    label = "Swap Free Max Percent",
                    value = config.thresholds.swapfreeMaxPercent,
                    options = listOf(
                        5 to "5%",
                        8 to "8%",
                        10 to "10% (Default)",
                        15 to "15%"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            thresholds = config.thresholds.copy(swapfreeMaxPercent = it)
                    )) 
                }
                // Process Memory Limits
                Text(
                    text = "Process Memory Limits (MB)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IntDropdownSelector(
                    label = "3rd Party Apps Limit",
                    value = config.processLimits.thirdParty,
                    options = listOf(
                        50 to "50 MB",
                        75 to "75 MB",
                        100 to "100 MB (Default)",
                        150 to "150 MB",
                        200 to "200 MB"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            processLimits = config.processLimits.copy(thirdParty = it)
                    )) 
                }
                IntDropdownSelector(
                    label = "Game Apps Limit",
                    value = config.processLimits.game,
                    options = listOf(
                        200 to "200 MB",
                        250 to "250 MB",
                        300 to "300 MB (Default)",
                        400 to "400 MB",
                        500 to "500 MB"
                    ),
                    onValueChange = { 
                        onConfigChange(config.copy(
                            processLimits = config.processLimits.copy(game = it)
                    )) 
                }
                // Task Counts
                Text(
                    text = "Task Counts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IntDropdownSelector(
                    label = "Recent Task Count",
                    value = config.recentTaskCount,
                    options = listOf(
                        4 to "4",
                        6 to "6 (Default)",
                        8 to "8",
                        10 to "10",
                        12 to "12"
                    ),
                    onValueChange = { onConfigChange(config.copy(recentTaskCount = it)) }
                )
                IntDropdownSelector(
                    label = "Cached Process Count",
                    value = config.cachedProcCount,
                    options = listOf(
                        8 to "8",
                        12 to "12",
                        16 to "16 (Default)",
                        24 to "24",
                        32 to "32"
                    ),
                    onValueChange = { onConfigChange(config.copy(cachedProcCount = it)) }
                }
                // Feature Flags
                Text(
                    text = "Feature Flags",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                SwitchOption(
                    label = "App Start Limit",
                    checked = config.features.appStartLimit,
                    onCheckedChange = { 
                        onConfigChange(config.copy(
                            features = config.features.copy(appStartLimit = it)
                    )) 
                },
                    description = "Restrict simultaneous app starts"
                )
                SwitchOption(
                    label = "OOM Adj Clean",
                    checked = config.features.oomAdjClean,
                    onCheckedChange = { 
                        onConfigChange(config.copy(
                            features = config.features.copy(oomAdjClean = it)
                    )) 
                },
                    description = "Clean OOM adj values periodically"
                )
                SwitchOption(
                    label = "Sleep Clean",
                    checked = config.features.sleepClean,
                    onCheckedChange = { 
                        onConfigChange(config.copy(
                            features = config.features.copy(sleepClean = it)
                    )) 
                },
                    description = "Clean memory during device sleep"
                )
                SwitchOption(
                    label = "Limit 3rd Party Start",
                    checked = config.features.limit3rdStart,
                    onCheckedChange = { 
                        onConfigChange(config.copy(
                            features = config.features.copy(limit3rdStart = it)
                    )) 
                },
                    description = "Restrict 3rd party app auto-start"
                )
                SwitchOption(
                    label = "Heavy CPU Clean",
                    checked = config.features.heavyCpuClean,
                    onCheckedChange = { 
                        onConfigChange(config.copy(
                            features = config.features.copy(heavyCpuClean = it)
                    )) 
                },
                    description = "Kill CPU-heavy background apps"
                )
                SwitchOption(
                    label = "Allow Clean 3rd Party",
                    checked = config.features.allowClean3rd
                    onCheckedChange = { 
                        onConfigChange(config.copy(
                            features = config.features.copy(allowClean3rd = it)
                    )) 
                },
                    description = "Allow killing 3rd party apps under memory pressure"
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private val gpuTimerOptions = listOf(
    1 to "1 (Most Aggressive)",
    5 to "5",
    10 to "10 (Default)",
    20 to "20",
    30 to "30",
    50 to "50",
    80 to "80 (Most Conservative)"
)

private val coldLaunchOptions = listOf(
    0 to "Default",
    15000 to "15 seconds",
    20000 to "20 seconds",
    25000 to "25 seconds",
    30000 to "30 seconds"
)

private val FpsLoadingThreshold.entries {
    VERY_LOW(10, "Very Low (10%)"),
    LOW(15, "Low (15%)"),
    STANDARD(25, "Standard (25%)"),
    HIGH(35, "High (35%)"),
    VERY_HIGH(50, "Very High (50%)")
}
