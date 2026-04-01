package com.x695c.optimizer.data

/**
 * Data models for X695C Vendor Optimization Configuration
 * Device: Infinix Note 10 Pro NFC
 * SoC: MediaTek Helio G95 (MT6785)
 * 
 * Helio G95 Specifications:
 * - CPU: 2x Cortex-A76 @ 2.05 GHz + 6x Cortex-A55 @ 2.0 GHz
 * - GPU: Mali-G76 MC4 @ 720 MHz (up to 900 MHz boost)
 * - Memory: LPDDR4X up to 2133 MHz
 * - Process: 12nm FinFET
 */

// ==================== ENUMS FOR DROPDOWN OPTIONS ====================

enum class ThermalPolicy(val value: Int, val description: String) {
    DEFAULT(0, "Default (No Override)"),
    CONSERVATIVE(1, "Conservative (Cool)"),
    BALANCED(4, "Balanced"),
    PERFORMANCE(8, "Performance (Gaming)"),
    AGGRESSIVE(12, "Aggressive (Max Performance)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DEFAULT
    }
}

enum class GpuMarginMode(val value: Int, val description: String) {
    MINIMUM(10, "Minimum (Power Saving)"),
    LOW(30, "Low"),
    BALANCED(50, "Balanced"),
    HIGH(80, "High"),
    MAXIMUM(110, "Maximum (Performance)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: BALANCED
    }
}

enum class UclampMin(val value: Int, val description: String) {
    NONE(0, "None (0%)"),
    LOW(20, "Low (20%)"),
    MEDIUM(40, "Medium (40%)"),
    HIGH(60, "High (60%)"),
    VERY_HIGH(80, "Very High (80%)"),
    MAXIMUM(100, "Maximum (100%)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: NONE
    }
}

enum class SchedBoost(val value: Int, val description: String) {
    DISABLED(0, "Disabled"),
    ENABLED(1, "Enabled"),
    AGGRESSIVE(2, "Aggressive");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DISABLED
    }
}

enum class FpsMarginMode(val value: Int, val description: String) {
    DISABLED(0, "Disabled"),
    STANDARD(1, "Standard"),
    AGGRESSIVE(2, "Aggressive");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DISABLED
    }
}

enum class NetworkBoost(val value: Int, val description: String) {
    DISABLED(0, "Disabled"),
    STANDARD(1, "Standard Boost"),
    HIGH_PRIORITY(2, "High Priority");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DISABLED
    }
}

enum class DramOpp(val value: Int, val description: String) {
    HIGH_PERFORMANCE(0, "High Performance (2133 MHz)"),
    BALANCED(1, "Balanced (1600 MHz)"),
    POWER_SAVING(2, "Power Saving (1066 MHz)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: BALANCED
    }
}

enum class TouchBoostOpp(val value: Int, val description: String) {
    MINIMAL(0, "Minimal"),
    LOW(1, "Low"),
    STANDARD(2, "Standard"),
    HIGH(3, "High"),
    MAXIMUM(5, "Maximum");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: STANDARD
    }
}

enum class FpsLoadingThreshold(val value: Int, val description: String) {
    VERY_LOW(10, "Very Low (10%)"),
    LOW(15, "Low (15%)"),
    STANDARD(25, "Standard (25%)"),
    HIGH(35, "High (35%)"),
    VERY_HIGH(50, "Very High (50%)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: STANDARD
    }
}

enum class GpuBlockBoost(val value: Int, val description: String) {
    DISABLED(-1, "Disabled"),
    LOW(30, "Low (30%)"),
    MEDIUM(50, "Medium (50%)"),
    HIGH(80, "High (80%)"),
    MAXIMUM(100, "Maximum (100%)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DISABLED
    }
}

enum class FrameRescuePercent(val value: Int, val description: String) {
    NONE(0, "None"),
    LOW(25, "Low (25%)"),
    MEDIUM(50, "Medium (50%)"),
    HIGH(75, "High (75%)"),
    MAXIMUM(100, "Maximum (100%)");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: NONE
    }
}

enum class WifiLowLatency(val value: Int, val description: String) {
    DISABLED(0, "Disabled"),
    ENABLED(1, "Enabled");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DISABLED
    }
}

enum class WeakSignalOpt(val value: Int, val description: String) {
    DISABLED(0, "Disabled"),
    ENABLED(1, "Enabled");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: DISABLED
    }
}

// ==================== CONFIGURATION DATA CLASSES ====================

data class GameOptimizationConfig(
    val packageName: String,
    val thermalPolicy: ThermalPolicy = ThermalPolicy.DEFAULT,
    val gpuMarginMode: GpuMarginMode = GpuMarginMode.BALANCED,
    val gpuTimerDvfsMargin: Int = 10,
    val uclampMin: UclampMin = UclampMin.NONE,
    val schedBoost: SchedBoost = SchedBoost.DISABLED,
    val fpsMarginMode: FpsMarginMode = FpsMarginMode.DISABLED,
    val fpsLoadingThreshold: FpsLoadingThreshold = FpsLoadingThreshold.STANDARD,
    val fpsAdjustLoading: Boolean = false,
    val gpuBlockBoost: GpuBlockBoost = GpuBlockBoost.DISABLED,
    val frameRescueF: Int = 0,
    val frameRescuePercent: FrameRescuePercent = FrameRescuePercent.NONE,
    val ultraRescue: Boolean = false,
    val networkBoost: NetworkBoost = NetworkBoost.DISABLED,
    val wifiLowLatency: WifiLowLatency = WifiLowLatency.DISABLED,
    val weakSignalOpt: WeakSignalOpt = WeakSignalOpt.DISABLED,
    val coldLaunchTime: Int = 0
)

data class PerformanceScenarioConfig(
    val scenarioName: String,
    val cpuFreqMinCluster0: Long = 0,  // Little cores (A55) - Max 2.0 GHz
    val cpuFreqMinCluster1: Long = 0,  // Big cores (A76) - Max 2.05 GHz
    val dramOpp: DramOpp = DramOpp.BALANCED,
    val uclampMin: UclampMin = UclampMin.NONE,
    val schedBoost: SchedBoost = SchedBoost.DISABLED,
    val touchBoostOpp: TouchBoostOpp = TouchBoostOpp.STANDARD,
    val touchBoostDuration: Long = 100000000,
    val bhrOpp: Int = 1,
    val holdTime: Long = 0,
    val extHint: Int = 0,
    val extHintHoldTime: Long = 0
)

data class MemoryThresholdConfig(
    val adjNative: Int = 1024,
    val adjSystem: Int = 1024,
    val adjPersist: Int = 1024,
    val adjForeground: Int = 200,
    val adjVisible: Int = 400,
    val adjPerceptible: Int = 300,
    val adjBackup: Int = 300,
    val adjHeavyweight: Int = 150,
    val adjService: Int = 200,
    val adjHome: Int = 150,
    val adjPrevious: Int = 200,
    val adjServiceB: Int = 200,
    val adjCached: Int = 700,
    val swapfreeMinPercent: Int = 5,
    val swapfreeMaxPercent: Int = 10,
    val freeCached: Int = 700
)

data class ProcessMemoryConfig(
    val thirdParty: Int = 100,
    val gms: Int = 100,
    val system: Int = 100,
    val systemBg: Int = 100,
    val game: Int = 300
)

data class MemoryFeatureConfig(
    val appStartLimit: Boolean = true,
    val oomAdjClean: Boolean = true,
    val lowRamClean: Boolean = true,
    val lowSwapClean: Boolean = true,
    val oneKeyClean: Boolean = true,
    val heavyCpuClean: Boolean = false,
    val heavyIowClean: Boolean = false,
    val sleepClean: Boolean = true,
    val fixAdj: Boolean = true,
    val limitSysStart: Boolean = false,
    val limitGmsStart: Boolean = false,
    val limit3rdStart: Boolean = true,
    val allowCleanSys: Boolean = false,
    val allowCleanGms: Boolean = false,
    val allowClean3rd: Boolean = true
)

data class MemoryManagementConfig(
    val thresholds: MemoryThresholdConfig = MemoryThresholdConfig(),
    val processLimits: ProcessMemoryConfig = ProcessMemoryConfig(),
    val features: MemoryFeatureConfig = MemoryFeatureConfig(),
    val recentTaskCount: Int = 6,
    val notificationCount: Int = 4,
    val cachedProcCount: Int = 16
)

data class GpuDvfsConfig(
    val marginMode: GpuMarginMode = GpuMarginMode.BALANCED,
    val timerBaseDvfsMargin: Int = 10,
    val loadingBaseDvfsStep: Int = 4,
    val cwaitg: Int = 0
)

// ==================== PRESET PROFILES ====================

enum class OptimizationProfile(val displayName: String, val description: String) {
    DEFAULT("Default", "Stock configuration"),
    POWER_SAVING("Power Saving", "Optimized for battery life"),
    BALANCED("Balanced", "Balance between performance and battery"),
    PERFORMANCE("Performance", "Optimized for smooth operation"),
    GAMING("Gaming", "Maximum performance for gaming"),
    CUSTOM("Custom", "User-defined configuration")
}

data class FullOptimizationConfig(
    val profile: OptimizationProfile = OptimizationProfile.DEFAULT,
    val gameConfigs: Map<String, GameOptimizationConfig> = emptyMap(),
    val scenarioConfigs: Map<String, PerformanceScenarioConfig> = emptyMap(),
    val memoryConfig: MemoryManagementConfig = MemoryManagementConfig(),
    val gpuConfig: GpuDvfsConfig = GpuDvfsConfig()
)

// ==================== HELPER FUNCTIONS ====================

/**
 * Default game configurations optimized for Helio G95
 * GPU: Mali-G76 MC4 @ 720-900 MHz
 * CPU: 2x A76 @ 2.05 GHz + 6x A55 @ 2.0 GHz
 */
fun getDefaultGameConfigs(): Map<String, GameOptimizationConfig> {
    return mapOf(
        // Honor of Kings - High intensity MOBA
        "com.tencent.tmgp.sgame" to GameOptimizationConfig(
            packageName = "com.tencent.tmgp.sgame",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.HIGH,
            gpuTimerDvfsMargin = 10,
            uclampMin = UclampMin.HIGH,
            networkBoost = NetworkBoost.STANDARD,
            wifiLowLatency = WifiLowLatency.ENABLED,
            weakSignalOpt = WeakSignalOpt.ENABLED,
            coldLaunchTime = 25000
        ),
        // PUBG Mobile - Battle Royale
        "com.tencent.ig" to GameOptimizationConfig(
            packageName = "com.tencent.ig",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.HIGH,
            uclampMin = UclampMin.HIGH,
            networkBoost = NetworkBoost.STANDARD,
            weakSignalOpt = WeakSignalOpt.ENABLED
        ),
        // Free Fire - Less demanding, optimize for smoothness
        "com.dts.freefireth" to GameOptimizationConfig(
            packageName = "com.dts.freefireth",
            thermalPolicy = ThermalPolicy.BALANCED,
            gpuMarginMode = GpuMarginMode.BALANCED,
            uclampMin = UclampMin.MEDIUM
        ),
        // PUBG Mobile HD - More GPU intensive
        "com.tencent.tmgp.pubgmhd" to GameOptimizationConfig(
            packageName = "com.tencent.tmgp.pubmh",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.MAXIMUM,
            uclampMin = UclampMin.VERY_HIGH,
            networkBoost = NetworkBoost.STANDARD,
            weakSignalOpt = WeakSignalOpt.ENABLED,
            wifiLowLatency = WifiLowLatency.ENABLED
        ),
        // Genshin Impact - Very demanding
        "com.miHoYo.enterprise.NGHSoD" to GameOptimizationConfig(
            packageName = "com.miHoYo.enterprise.NGHSoD",
            thermalPolicy = ThermalPolicy.AGGRESSIVE,
            gpuMarginMode = GpuMarginMode.MAXIMUM,
            uclampMin = UclampMin.MAXIMUM,
            fpsAdjustLoading = true,
            fpsLoadingThreshold = FpsLoadingThreshold.HIGH,
            frameRescuePercent = FrameRescuePercent.HIGH,
            networkBoost = NetworkBoost.STANDARD,
            weakSignalOpt = WeakSignalOpt.ENABLED
        ),
        // Mobile Legends
        "com.mobile.legends" to GameOptimizationConfig(
            packageName = "com.mobile.legends",
            thermalPolicy = ThermalPolicy.BALANCED,
            gpuMarginMode = GpuMarginMode.BALANCED,
            uclampMin = UclampMin.MEDIUM,
            networkBoost = NetworkBoost.STANDARD
        ),
        // Call of Duty Mobile
        "com.activision.callofduty.shooter" to GameOptimizationConfig(
            packageName = "com.activision.callofduty.shooter",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.HIGH,
            uclampMin = UclampMin.HIGH,
            networkBoost = NetworkBoost.STANDARD,
            wifiLowLatency = WifiLowLatency.ENABLED
        )
    )
}

/**
 * Default scenario configurations optimized for Helio G95
 * 
 * CPU Frequency Limits (in kHz):
 * - Cluster 0 (Little - Cortex-A55): Max 2000000 (2.0 GHz)
 * - Cluster 1 (Big - Cortex-A76): Max 2050000 (2.05 GHz)
 */
fun getDefaultScenarioConfigs(): Map<String, PerformanceScenarioConfig> {
    return mapOf(
        // App Launch - Maximum boost for fast startup
        "LAUNCH" to PerformanceScenarioConfig(
            scenarioName = "LAUNCH",
            cpuFreqMinCluster0 = 2000000,  // 2.0 GHz (A55 max)
            cpuFreqMinCluster1 = 2050000,  // 2.05 GHz (A76 max)
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.MAXIMUM
        ),
        // Touch Response - Quick boost for responsiveness
        "MTKPOWER_HINT_APP_TOUCH" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_APP_TOUCH",
            cpuFreqMinCluster0 = 1200000,  // 1.2 GHz
            cpuFreqMinCluster1 = 1500000,  // 1.5 GHz
            touchBoostOpp = TouchBoostOpp.HIGH,
            touchBoostDuration = 100000000
        ),
        // Process Creation - Boost for new app spawns
        "MTKPOWER_HINT_PROCESS_CREATE" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_PROCESS_CREATE",
            cpuFreqMinCluster0 = 2000000,  // 2.0 GHz
            cpuFreqMinCluster1 = 2050000,  // 2.05 GHz
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.VERY_HIGH,
            bhrOpp = 15,
            holdTime = 6000,
            extHint = 30,
            extHintHoldTime = 35000
        ),
        // Screen Rotation - Smooth animation
        "MTKPOWER_HINT_APP_ROTATE" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_APP_ROTATE",
            cpuFreqMinCluster0 = 1600000,  // 1.6 GHz
            cpuFreqMinCluster1 = 1800000,  // 1.8 GHz
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.HIGH,
            schedBoost = SchedBoost.ENABLED
        ),
        // Fingerprint - Quick authentication
        "MTKPOWER_HINT_FLINGER_PRINT" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_FLINGER_PRINT",
            cpuFreqMinCluster0 = 1500000,  // 1.5 GHz
            cpuFreqMinCluster1 = 1800000,  // 1.8 GHz
            dramOpp = DramOpp.HIGH_PERFORMANCE
        ),
        // UI Interaction - Smooth scrolling
        "INTERACTION" to PerformanceScenarioConfig(
            scenarioName = "INTERACTION",
            cpuFreqMinCluster0 = 1000000,  // 1.0 GHz
            cpuFreqMinCluster1 = 1200000,  // 1.2 GHz
            bhrOpp = 15,
            touchBoostOpp = TouchBoostOpp.STANDARD
        ),
        // Package Switch - Fast app switching
        "MTKPOWER_HINT_PACK_SWITCH" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_PACK_SWITCH",
            cpuFreqMinCluster0 = 1400000,  // 1.4 GHz
            cpuFreqMinCluster1 = 1600000,  // 1.6 GHz
            dramOpp = DramOpp.BALANCED,
            uclampMin = UclampMin.MEDIUM
        ),
        // Activity Switch
        "MTKPOWER_HINT_ACT_SWITCH" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_ACT_SWITCH",
            cpuFreqMinCluster0 = 1200000,  // 1.2 GHz
            cpuFreqMinCluster1 = 1500000,  // 1.5 GHz
            uclampMin = UclampMin.MEDIUM
        ),
        // Gallery Boost - Fast image loading
        "MTKPOWER_HINT_GALLERY_BOOST" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_GALLERY_BOOST",
            cpuFreqMinCluster0 = 1200000,  // 1.2 GHz
            cpuFreqMinCluster1 = 1500000,  // 1.5 GHz
            dramOpp = DramOpp.HIGH_PERFORMANCE
        ),
        // WiFi Display
        "MTKPOWER_HINT_WFD" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_WFD",
            cpuFreqMinCluster0 = 1600000,  // 1.6 GHz
            cpuFreqMinCluster1 = 1800000,  // 1.8 GHz
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.HIGH
        )
    )
}
