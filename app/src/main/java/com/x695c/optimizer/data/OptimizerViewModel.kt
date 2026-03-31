package com.x695c.optimizer.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OptimizerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizerUiState())
    val uiState: StateFlow<OptimizerUiState> = _uiState.asStateFlow()

    private val _selectedProfile = MutableStateFlow(OptimizationProfile.DEFAULT)
    val selectedProfile: StateFlow<OptimizationProfile> = _selectedProfile.asStateFlow()

    private val _gameConfigs = MutableStateFlow(getDefaultGameConfigs())
    val gameConfigs: StateFlow<Map<String, GameOptimizationConfig>> = _gameConfigs.asStateFlow()

    private val _scenarioConfigs = MutableStateFlow(getDefaultScenarioConfigs())
    val scenarioConfigs: StateFlow<Map<String, PerformanceScenarioConfig>> = _scenarioConfigs.asStateFlow()

    private val _memoryConfig = MutableStateFlow(MemoryManagementConfig())
    val memoryConfig: StateFlow<MemoryManagementConfig> = _memoryConfig.asStateFlow()

    private val _gpuConfig = MutableStateFlow(GpuDvfsConfig())
    val gpuConfig: StateFlow<GpuDvfsConfig> = _gpuConfig.asStateFlow()

    fun setProfile(profile: OptimizationProfile) {
        _selectedProfile.value = profile
        when (profile) {
            OptimizationProfile.DEFAULT -> loadDefaultProfile()
            OptimizationProfile.POWER_SAVING -> loadPowerSavingProfile()
            OptimizationProfile.BALANCED -> loadBalancedProfile()
            OptimizationProfile.PERFORMANCE -> loadPerformanceProfile()
            OptimizationProfile.GAMING -> loadGamingProfile()
            OptimizationProfile.CUSTOM -> { /* Keep current settings */ }
        }
    }

    private fun loadDefaultProfile() {
        _gameConfigs.value = getDefaultGameConfigs()
        _scenarioConfigs.value = getDefaultScenarioConfigs()
        _memoryConfig.value = MemoryManagementConfig()
        _gpuConfig.value = GpuDvfsConfig()
    }

    private fun loadPowerSavingProfile() {
        _gpuConfig.value = GpuDvfsConfig(
            marginMode = GpuMarginMode.MINIMUM,
            timerBaseDvfsMargin = 10,
            loadingBaseDvfsStep = 2
        )
        _memoryConfig.value = MemoryManagementConfig(
            features = MemoryFeatureConfig(
                appStartLimit = true,
                oomAdjClean = true,
                lowRamClean = true,
                lowSwapClean = true,
                oneKeyClean = true,
                heavyCpuClean = true,
                heavyIowClean = true,
                sleepClean = true,
                fixAdj = true,
                limit3rdStart = true,
                allowClean3rd = true
            )
        )
    }

    private fun loadBalancedProfile() {
        _gpuConfig.value = GpuDvfsConfig(
            marginMode = GpuMarginMode.BALANCED,
            timerBaseDvfsMargin = 10,
            loadingBaseDvfsStep = 4
        )
        _memoryConfig.value = MemoryManagementConfig(
            features = MemoryFeatureConfig(
                appStartLimit = true,
                oomAdjClean = true,
                lowRamClean = true,
                lowSwapClean = true,
                oneKeyClean = true,
                heavyCpuClean = false,
                heavyIowClean = false,
                sleepClean = true,
                fixAdj = true,
                limit3rdStart = true,
                allowClean3rd = true
            )
        )
    }

    private fun loadPerformanceProfile() {
        _gpuConfig.value = GpuDvfsConfig(
            marginMode = GpuMarginMode.HIGH,
            timerBaseDvfsMargin = 20,
            loadingBaseDvfsStep = 6
        )
        _memoryConfig.value = MemoryManagementConfig(
            features = MemoryFeatureConfig(
                appStartLimit = false,
                oomAdjClean = true,
                lowRamClean = true,
                lowSwapClean = false,
                oneKeyClean = false,
                heavyCpuClean = false,
                heavyIowClean = false,
                sleepClean = false,
                fixAdj = true,
                limit3rdStart = false,
                allowClean3rd = false
            ),
            thresholds = MemoryThresholdConfig(
                adjCached = 500,
                freeCached = 900
            )
        )
    }

    private fun loadGamingProfile() {
        _gpuConfig.value = GpuDvfsConfig(
            marginMode = GpuMarginMode.MAXIMUM,
            timerBaseDvfsMargin = 30,
            loadingBaseDvfsStep = 8
        )
        _memoryConfig.value = MemoryManagementConfig(
            features = MemoryFeatureConfig(
                appStartLimit = false,
                oomAdjClean = true,
                lowRamClean = false,
                lowSwapClean = false,
                oneKeyClean = false,
                heavyCpuClean = false,
                heavyIowClean = false,
                sleepClean = false,
                fixAdj = true,
                limit3rdStart = false,
                allowClean3rd = false
            ),
            thresholds = MemoryThresholdConfig(
                adjCached = 400,
                freeCached = 1000
            ),
            processLimits = ProcessMemoryConfig(
                thirdParty = 150,
                gms = 150,
                system = 150,
                systemBg = 150,
                game = 400
            )
        )
    }

    fun updateGameConfig(packageName: String, config: GameOptimizationConfig) {
        _gameConfigs.update { configs ->
            configs.toMutableMap().apply {
                this[packageName] = config
            }
        }
        _selectedProfile.value = OptimizationProfile.CUSTOM
    }

    fun updateScenarioConfig(scenarioName: String, config: PerformanceScenarioConfig) {
        _scenarioConfigs.update { configs ->
            configs.toMutableMap().apply {
                this[scenarioName] = config
            }
        }
        _selectedProfile.value = OptimizationProfile.CUSTOM
    }

    fun updateMemoryConfig(config: MemoryManagementConfig) {
        _memoryConfig.value = config
        _selectedProfile.value = OptimizationProfile.CUSTOM
    }

    fun updateGpuConfig(config: GpuDvfsConfig) {
        _gpuConfig.value = config
        _selectedProfile.value = OptimizationProfile.CUSTOM
    }

    fun exportConfiguration(): String {
        val config = FullOptimizationConfig(
            profile = _selectedProfile.value,
            gameConfigs = _gameConfigs.value,
            scenarioConfigs = _scenarioConfigs.value,
            memoryConfig = _memoryConfig.value,
            gpuConfig = _gpuConfig.value
        )
        return buildXmlConfiguration(config)
    }

    private fun buildXmlConfiguration(config: FullOptimizationConfig): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<!-- X695C Vendor Optimization Configuration -->")
        sb.appendLine("<!-- Profile: ${config.profile.displayName} -->")
        sb.appendLine()
        
        // Game configurations
        sb.appendLine("<WHITELIST>")
        config.gameConfigs.forEach { (packageName, gameConfig) ->
            sb.appendLine("    <Package name=\"$packageName\">")
            sb.appendLine("        <Activity name=\"Common\">")
            
            if (gameConfig.thermalPolicy != ThermalPolicy.DEFAULT) {
                sb.appendLine("            <data cmd=\"PERF_RES_THERMAL_POLICY\" param1=\"${gameConfig.thermalPolicy.value}\"></data>")
            }
            if (gameConfig.gpuMarginMode != GpuMarginMode.BALANCED) {
                sb.appendLine("            <data cmd=\"PERF_RES_GPU_GED_MARGIN_MODE\" param1=\"${gameConfig.gpuMarginMode.value}\"></data>")
            }
            if (gameConfig.uclampMin != UclampMin.NONE) {
                sb.appendLine("            <data cmd=\"PERF_RES_SCHED_UCLAMP_MIN_TA\" param1=\"${gameConfig.uclampMin.value}\"></data>")
            }
            if (gameConfig.schedBoost != SchedBoost.DISABLED) {
                sb.appendLine("            <data cmd=\"PERF_RES_SCHED_BOOST\" param1=\"${gameConfig.schedBoost.value}\"></data>")
            }
            if (gameConfig.fpsMarginMode != FpsMarginMode.DISABLED) {
                sb.appendLine("            <data cmd=\"PERF_RES_FPS_FPSGO_MARGIN_MODE\" param1=\"${gameConfig.fpsMarginMode.value}\"></data>")
            }
            if (gameConfig.fpsAdjustLoading) {
                sb.appendLine("            <data cmd=\"PERF_RES_FPS_FPSGO_ADJ_LOADING\" param1=\"1\"></data>")
            }
            if (gameConfig.fpsLoadingThreshold != FpsLoadingThreshold.STANDARD) {
                sb.appendLine("            <data cmd=\"PERF_RES_FPS_FPSGO_LLF_TH\" param1=\"${gameConfig.fpsLoadingThreshold.value}\"></data>")
            }
            if (gameConfig.gpuBlockBoost != GpuBlockBoost.DISABLED) {
                sb.appendLine("            <data cmd=\"PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST\" param1=\"${gameConfig.gpuBlockBoost.value}\"></data>")
            }
            if (gameConfig.networkBoost != NetworkBoost.DISABLED) {
                sb.appendLine("            <data cmd=\"PERF_RES_NET_NETD_BOOST_UID\" param1=\"${gameConfig.networkBoost.value}\"></data>")
            }
            if (gameConfig.wifiLowLatency == WifiLowLatency.ENABLED) {
                sb.appendLine("            <data cmd=\"PERF_RES_NET_WIFI_LOW_LATENCY\" param1=\"1\"></data>")
            }
            if (gameConfig.weakSignalOpt == WeakSignalOpt.ENABLED) {
                sb.appendLine("            <data cmd=\"PERF_RES_NET_MD_WEAK_SIG_OPT\" param1=\"1\"></data>")
            }
            
            sb.appendLine("        </Activity>")
            sb.appendLine("    </Package>")
        }
        sb.appendLine("</WHITELIST>")
        
        return sb.toString()
    }
}

data class OptimizerUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val exportResult: String? = null
)
