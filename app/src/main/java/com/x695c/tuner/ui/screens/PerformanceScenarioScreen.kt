package com.x695c.tuner.ui.screens

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
import com.x695c.tuner.data.*
import com.x695c.tuner.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScenarioScreen(
    scenarioName: String,
    config: PerformanceScenarioConfig,
    onConfigChange: (PerformanceScenarioConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar - MD3 Expressive
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Performance Scenario",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scenarioName,
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
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CPU Frequency Settings
            Text(
                text = "CPU Frequency Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            LongDropdownSelector(
                label = "CPU Freq Min Cluster 0 (Little Cores - A55)",
                value = config.cpuFreqMinCluster0,
                options = listOf(
                    0L to "Default (No Override)",
                    600000L to "600 MHz",
                    800000L to "800 MHz",
                    1000000L to "1.0 GHz",
                    1200000L to "1.2 GHz",
                    1400000L to "1.4 GHz",
                    1600000L to "1.6 GHz",
                    1800000L to "1.8 GHz",
                    2000000L to "2.0 GHz (Max)"
                ),
                onValueChange = { onConfigChange(config.copy(cpuFreqMinCluster0 = it)) }
            )
            LongDropdownSelector(
                label = "CPU Freq Min Cluster 1 (Big Cores - A76)",
                value = config.cpuFreqMinCluster1,
                options = listOf(
                    0L to "Default (No Override)",
                    600000L to "600 MHz",
                    800000L to "800 MHz",
                    1000000L to "1.0 GHz",
                    1200000L to "1.2 GHz",
                    1400000L to "1.4 GHz",
                    1600000L to "1.6 GHz",
                    1800000L to "1.8 GHz",
                    2000000L to "2.0 GHz",
                    2050000L to "2.05 GHz (Max)"
                ),
                onValueChange = { onConfigChange(config.copy(cpuFreqMinCluster1 = it)) }
            )

            // DRAM Settings
            Text(
                text = "DRAM Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            DropdownSelector(
                label = "DRAM OPP (Frequency Level)",
                value = config.dramOpp,
                options = DramOpp.entries,
                onValueChange = { onConfigChange(config.copy(dramOpp = it)) },
                optionDescription = { it.description }
            )

            // Scheduler Settings
            Text(
                text = "Scheduler Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            DropdownSelector(
                label = "UCLAMP Min",
                value = config.uclampMin,
                options = UclampMin.entries,
                onValueChange = { onConfigChange(config.copy(uclampMin = it)) },
                optionDescription = { it.description }
            )
            DropdownSelector(
                label = "Scheduler Boost",
                value = config.schedBoost,
                options = SchedBoost.entries,
                onValueChange = { onConfigChange(config.copy(schedBoost = it)) },
                optionDescription = { it.description }
            )

            // Touch Boost Settings
            Text(
                text = "Touch Boost Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            DropdownSelector(
                label = "Touch Boost OPP",
                value = config.touchBoostOpp,
                options = TouchBoostOpp.entries,
                onValueChange = { onConfigChange(config.copy(touchBoostOpp = it)) },
                optionDescription = { it.description }
            )
            LongDropdownSelector(
                label = "Touch Boost Duration",
                value = config.touchBoostDuration,
                options = listOf(
                    10000000L to "10 ms (Short)",
                    50000000L to "50 ms (Default)",
                    100000000L to "100 ms",
                    200000000L to "200 ms (Long)",
                    500000000L to "500 ms (Long)"
                ),
                onValueChange = { onConfigChange(config.copy(touchBoostDuration = it)) }
            )

            // Frame Boost Settings
            Text(
                text = "Frame Boost Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IntDropdownSelector(
                label = "BHR OPP (Frame Boost Headroom)",
                value = config.bhrOpp,
                options = listOf(
                    1 to "1 (Minimal)",
                    5 to "5",
                    10 to "10",
                    15 to "15 (Default)",
                    20 to "20 (Maximum)"
                ),
                onValueChange = { onConfigChange(config.copy(bhrOpp = it)) }
            )

            // Extended Hint Settings
            Text(
                text = "Extended Hint Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IntDropdownSelector(
                label = "Extended Hint Value",
                value = config.extHint,
                options = listOf(
                    0 to "Disabled",
                    10 to "10",
                    20 to "20",
                    30 to "30 (Default)",
                    40 to "40"
                ),
                onValueChange = { onConfigChange(config.copy(extHint = it)) }
            )
            LongDropdownSelector(
                label = "Extended Hint Hold Time (ms)",
                value = config.extHintHoldTime,
                options = listOf(
                    0L to "Disabled",
                    10000L to "10 seconds",
                    20000L to "20 seconds",
                    35000L to "35 seconds (Default)",
                    50000L to "50 seconds"
                ),
                onValueChange = { onConfigChange(config.copy(extHintHoldTime = it)) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
