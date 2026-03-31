package com.x695c.optimizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x695c.optimizer.data.*
import com.x695c.optimizer.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            X695COptimizerTheme {
                OptimizerApp()
            }
        }
    }
}

@Composable
fun X695COptimizerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = md_theme_primary,
            onPrimary = md_theme_onPrimary,
            primaryContainer = md_theme_primaryContainer,
            onPrimaryContainer = md_theme_onPrimaryContainer,
            secondary = md_theme_secondary,
            onSecondary = md_theme_onSecondary,
            secondaryContainer = md_theme_secondaryContainer,
            onSecondaryContainer = md_theme_onSecondaryContainer,
            tertiary = md_theme_tertiary,
            onTertiary = md_theme_onTertiary,
            tertiaryContainer = md_theme_tertiaryContainer,
            onTertiaryContainer = md_theme_onTertiaryContainer,
            error = md_theme_error,
            onError = md_theme_onError,
            errorContainer = md_theme_errorContainer,
            onErrorContainer = md_theme_onErrorContainer,
            background = md_theme_background,
            onBackground = md_theme_onBackground,
            surface = md_theme_surface,
            onSurface = md_theme_onSurface,
            surfaceVariant = md_theme_surfaceVariant,
            onSurfaceVariant = md_theme_onSurfaceVariant
        ),
        content = content
    )
}

// Dark theme colors
private val md_theme_primary = androidx.compose.ui.graphics.Color(0xFF6BB5FF)
private val md_theme_onPrimary = androidx.compose.ui.graphics.Color(0xFF003258)
private val md_theme_primaryContainer = androidx.compose.ui.graphics.Color(0xFF1F4E79)
private val md_theme_onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD1E4FF)
private val md_theme_secondary = androidx.compose.ui.graphics.Color(0xFFBAC6DC)
private val md_theme_onSecondary = androidx.compose.ui.graphics.Color(0xFF243043)
private val md_theme_secondaryContainer = androidx.compose.ui.graphics.Color(0xFF3A465A)
private val md_theme_onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E2F9)
private val md_theme_tertiary = androidx.compose.ui.graphics.Color(0xFFD9BDE6)
private val md_theme_onTertiary = androidx.compose.ui.graphics.Color(0xFF3C2849)
private val md_theme_tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF533E60)
private val md_theme_onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFf5D9FF)
private val md_theme_error = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
private val md_theme_onError = androidx.compose.ui.graphics.Color(0xFF690005)
private val md_theme_errorContainer = androidx.compose.ui.graphics.Color(0xFF93000A)
private val md_theme_onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6)
private val md_theme_background = androidx.compose.ui.graphics.Color(0xFF0A1118)
private val md_theme_onBackground = androidx.compose.ui.graphics.Color(0xFFE1E2E8)
private val md_theme_surface = androidx.compose.ui.graphics.Color(0xFF0A1118)
private val md_theme_onSurface = androidx.compose.ui.graphics.Color(0xFFE1E2E8)
private val md_theme_surfaceVariant = androidx.compose.ui.graphics.Color(0xFF42474E)
private val md_theme_onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC2C7CF)

@Composable
fun OptimizerApp(
    viewModel: OptimizerViewModel = viewModel()
) {
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val gameConfigs by viewModel.gameConfigs.collectAsState()
    val scenarioConfigs by viewModel.scenarioConfigs.collectAsState()
    val memoryConfig by viewModel.memoryConfig.collectAsState()
    val gpuConfig by viewModel.gpuConfig.collectAsState()

    var currentScreen by remember { mutableStateOf("main") }
    var selectedGame by remember { mutableStateOf<String?>(null) }
    var selectedScenario by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            "main" -> MainDashboardScreen(
                selectedProfile = selectedProfile,
                onProfileChange = { viewModel.setProfile(it) },
                gameConfigs = gameConfigs,
                scenarioConfigs = scenarioConfigs,
                memoryConfig = memoryConfig,
                gpuConfig = gpuConfig,
                onExport = { viewModel.exportConfiguration() },
                onNavigateToGames = { currentScreen = "games" },
                onNavigateToScenarios = { currentScreen = "scenarios" },
                onNavigateToMemory = { currentScreen = "memory" },
                onNavigateToGpu = { currentScreen = "gpu" }
            )
            "games" -> GameListScreen(
                gameConfigs = gameConfigs,
                onGameSelect = { 
                    selectedGame = it
                    currentScreen = "game_detail"
                },
                onBack = { currentScreen = "main" }
            )
            "game_detail" -> {
                val packageName = selectedGame
                val config = packageName?.let { gameConfigs[it] }
                if (packageName != null && config != null) {
                    GameOptimizationScreen(
                        packageName = packageName,
                        config = config,
                        onConfigChange = { newConfig ->
                            viewModel.updateGameConfig(packageName, newConfig)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // Back button handled in top bar
                    LaunchedEffect(Unit) {
                        // Add back handler if needed
                    }
                } else {
                    LaunchedEffect(Unit) {
                        currentScreen = "games"
                    }
                }
            }
            "scenarios" -> ScenarioListScreen(
                scenarioConfigs = scenarioConfigs,
                onScenarioSelect = {
                    selectedScenario = it
                    currentScreen = "scenario_detail"
                },
                onBack = { currentScreen = "main" }
            )
            "scenario_detail" -> {
                val scenarioName = selectedScenario
                val config = scenarioName?.let { scenarioConfigs[it] }
                if (scenarioName != null && config != null) {
                    PerformanceScenarioScreen(
                        scenarioName = scenarioName,
                        config = config,
                        onConfigChange = { newConfig ->
                            viewModel.updateScenarioConfig(scenarioName, newConfig)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LaunchedEffect(Unit) {
                        currentScreen = "scenarios"
                    }
                }
            }
            "memory" -> MemoryManagementScreen(
                config = memoryConfig,
                onConfigChange = { viewModel.updateMemoryConfig(it) },
                modifier = Modifier.fillMaxSize()
            )
            "gpu" -> GpuSettingsScreen(
                config = gpuConfig,
                onConfigChange = { viewModel.updateGpuConfig(it) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
