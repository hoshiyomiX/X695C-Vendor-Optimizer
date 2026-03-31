package com.x695c.optimizer.ui.screens

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
import com.x695c.optimizer.data.*
import com.x695c.optimizer.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpuSettingsScreen(
    config: GpuDvfsConfig,
    onConfigChange: (GpuDvfsConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            colors = CardDefaults.cardColors(
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
                    imageVector = Icons.Default.Games,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GPU DVFS Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mali-G76 MC4 GPU",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Warning Card
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "High GPU settings may increase battery drain and device temperature.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // GPU DVFS Settings
        Text(
            text = "DVFS Configuration",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        DropdownSelector(
            label = "GPU Margin Mode",
            value = config.marginMode,
            options = GpuMarginMode.entries,
            onValueChange = { onConfigChange(config.copy(marginMode = it)) },
            optionDescription = { it.description }
        )

        IntDropdownSelector(
            label = "Timer-Based DVFS Margin",
            value = config.timerBaseDvfsMargin,
            options = listOf(
                1 to "1 (Most Aggressive)",
                5 to "5",
                10 to "10 (Default)",
                20 to "20",
                30 to "30",
                50 to "50",
                80 to "80 (Most Conservative)"
            ),
            onValueChange = { onConfigChange(config.copy(timerBaseDvfsMargin = it)) }
        )

        IntDropdownSelector(
            label = "Loading-Based DVFS Step",
            value = config.loadingBaseDvfsStep,
            options = listOf(
                0 to "0 (Disabled)",
                2 to "2 (Slow)",
                4 to "4 (Default)",
                6 to "6",
                8 to "8",
                10 to "10 (Fast)"
            ),
            onValueChange = { onConfigChange(config.copy(loadingBaseDvfsStep = it)) }
        )

        IntDropdownSelector(
            label = "GPU CWAITG",
            value = config.cwaitg,
            options = listOf(
                0 to "0 (Default)",
                20 to "20",
                40 to "40",
                60 to "60",
                80 to "80",
                100 to "100 (Maximum)"
            ),
            onValueChange = { onConfigChange(config.copy(cwaitg = it)) }
        )

        // Presets
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    onConfigChange(GpuDvfsConfig(
                        marginMode = GpuMarginMode.MINIMUM,
                        timerBaseDvfsMargin = 10,
                        loadingBaseDvfsStep = 2,
                        cwaitg = 0
                    ))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Power Saving")
            }
            OutlinedButton(
                onClick = { 
                    onConfigChange(GpuDvfsConfig(
                        marginMode = GpuMarginMode.BALANCED,
                        timerBaseDvfsMargin = 10,
                        loadingBaseDvfsStep = 4,
                        cwaitg = 0
                    ))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Balanced")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    onConfigChange(GpuDvfsConfig(
                        marginMode = GpuMarginMode.HIGH,
                        timerBaseDvfsMargin = 20,
                        loadingBaseDvfsStep = 6,
                        cwaitg = 50
                    ))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Performance")
            }
            Button(
                onClick = { 
                    onConfigChange(GpuDvfsConfig(
                        marginMode = GpuMarginMode.MAXIMUM,
                        timerBaseDvfsMargin = 30,
                        loadingBaseDvfsStep = 8,
                        cwaitg = 80
                    ))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Gaming")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
