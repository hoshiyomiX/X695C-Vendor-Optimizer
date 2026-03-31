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
fun GameOptimizationScreen(
    packageName: String,
    config: GameOptimizationConfig,
    onConfigChange: (GameOptimizationConfig) -> Unit,
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
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Game Optimization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Thermal Policy
        DropdownSelector(
            label = "Thermal Policy",
            value = config.thermalPolicy,
            options = ThermalPolicy.entries,
            onValueChange = { onConfigChange(config.copy(thermalPolicy = it)) },
            optionDescription = { it.description }
        )

        // GPU Settings
        Text(
            text = "GPU Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        DropdownSelector(
            label = "GPU Margin Mode",
            value = config.gpuMarginMode,
            options = GpuMarginMode.entries,
            onValueChange = { onConfigChange(config.copy(gpuMarginMode = it)) },
            optionDescription = { it.description }
        )

        IntDropdownSelector(
            label = "GPU Timer DVFS Margin",
            value = config.gpuTimerDvfsMargin,
            options = listOf(
                1 to "1 (Fast Response)",
                5 to "5",
                10 to "10 (Default)",
                20 to "20",
                30 to "30 (Slow Response)"
            ),
            onValueChange = { onConfigChange(config.copy(gpuTimerDvfsMargin = it)) }
        )

        DropdownSelector(
            label = "GPU Block Boost",
            value = config.gpuBlockBoost,
            options = GpuBlockBoost.entries,
            onValueChange = { onConfigChange(config.copy(gpuBlockBoost = it)) },
            optionDescription = { it.description }
        )

        // CPU Settings
        Text(
            text = "CPU Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        DropdownSelector(
            label = "UCLAMP Min (Top-App)",
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

        // FPS Settings
        Text(
            text = "FPS Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        DropdownSelector(
            label = "FPS Margin Mode",
            value = config.fpsMarginMode,
            options = FpsMarginMode.entries,
            onValueChange = { onConfigChange(config.copy(fpsMarginMode = it)) },
            optionDescription = { it.description }
        )

        SwitchOption(
            label = "Adjust FPS Loading",
            checked = config.fpsAdjustLoading,
            onCheckedChange = { onConfigChange(config.copy(fpsAdjustLoading = it)) },
            description = "Enable dynamic frame adjustment"
        )

        DropdownSelector(
            label = "FPS Loading Threshold",
            value = config.fpsLoadingThreshold,
            options = FpsLoadingThreshold.entries,
            onValueChange = { onConfigChange(config.copy(fpsLoadingThreshold = it)) },
            optionDescription = { it.description }
        )

        DropdownSelector(
            label = "Frame Rescue Percent",
            value = config.frameRescuePercent,
            options = FrameRescuePercent.entries,
            onValueChange = { onConfigChange(config.copy(frameRescuePercent = it)) },
            optionDescription = { it.description }
        )

        SwitchOption(
            label = "Ultra Rescue Mode",
            checked = config.ultraRescue,
            onCheckedChange = { onConfigChange(config.copy(ultraRescue = it)) },
            description = "Emergency frame recovery for demanding games"
        )

        // Network Settings
        Text(
            text = "Network Settings",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        DropdownSelector(
            label = "Network Boost",
            value = config.networkBoost,
            options = NetworkBoost.entries,
            onValueChange = { onConfigChange(config.copy(networkBoost = it)) },
            optionDescription = { it.description }
        )

        DropdownSelector(
            label = "WiFi Low Latency",
            value = config.wifiLowLatency,
            options = WifiLowLatency.entries,
            onValueChange = { onConfigChange(config.copy(wifiLowLatency = it)) },
            optionDescription = { it.description }
        )

        DropdownSelector(
            label = "Weak Signal Optimization",
            value = config.weakSignalOpt,
            options = WeakSignalOpt.entries,
            onValueChange = { onConfigChange(config.copy(weakSignalOpt = it)) },
            optionDescription = { it.description }
        )

        // Launch Time
        IntDropdownSelector(
            label = "Cold Launch Time (ms)",
            value = config.coldLaunchTime,
            options = listOf(
                0 to "Default",
                15000 to "15 seconds",
                20000 to "20 seconds",
                25000 to "25 seconds",
                30000 to "30 seconds"
            ),
            onValueChange = { onConfigChange(config.copy(coldLaunchTime = it)) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
