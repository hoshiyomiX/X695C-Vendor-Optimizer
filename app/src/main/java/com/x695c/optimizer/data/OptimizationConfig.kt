package com.x695c.optimizer.data

/**
 * Data models for X695C Vendor Optimization Configuration
 * Based on analysis of INFINIX X695C vendor partition files
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
    HIGH_PERFORMANCE(0, "High Performance (Max Frequency)"),
    BALANCED(1, "Balanced"),
    POWER_SAVING(2, "Power Saving (Low Frequency)");

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
    val cpuFreqMinCluster0: Long = 0,
    val cpuFreqMinCluster1: Long = 0,
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

fun getDefaultGameConfigs(): Map<String, GameOptimizationConfig> {
    return mapOf(
        "com.tencent.tmgp.sgame" to GameOptimizationConfig(
            packageName = "com.tencent.tmgp.sgame",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.MAXIMUM,
            gpuTimerDvfsMargin = 10,
            networkBoost = NetworkBoost.STANDARD,
            wifiLowLatency = WifiLowLatency.ENABLED,
            weakSignalOpt = WeakSignalOpt.ENABLED,
            coldLaunchTime = 25000
        ),
        "com.tencent.ig" to GameOptimizationConfig(
            packageName = "com.tencent.ig",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.MAXIMUM,
            networkBoost = NetworkBoost.STANDARD,
            weakSignalOpt = WeakSignalOpt.ENABLED
        ),
        "com.dts.freefireth" to GameOptimizationConfig(
            packageName = "com.dts.freefireth",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.MAXIMUM
        ),
        "com.tencent.tmgp.pubgmhd" to GameOptimizationConfig(
            packageName = "com.tencent.tmgp.pubgmhd",
            thermalPolicy = ThermalPolicy.PERFORMANCE,
            gpuMarginMode = GpuMarginMode.MAXIMUM,
            networkBoost = NetworkBoost.STANDARD,
            weakSignalOpt = WeakSignalOpt.ENABLED,
            wifiLowLatency = WifiLowLatency.ENABLED
        ),
        "com.miHoYo.enterprise.NGHSoD" to GameOptimizationConfig(
            packageName = "com.miHoYo.enterprise.NGHSoD",
            fpsAdjustLoading = true,
            fpsLoadingThreshold = FpsLoadingThreshold.STANDARD,
            networkBoost = NetworkBoost.STANDARD,
            weakSignalOpt = WeakSignalOpt.ENABLED
        )
    )
}

fun getDefaultScenarioConfigs(): Map<String, PerformanceScenarioConfig> {
    return mapOf(
        "LAUNCH" to PerformanceScenarioConfig(
            scenarioName = "LAUNCH",
            cpuFreqMinCluster0 = 3000000,
            cpuFreqMinCluster1 = 3000000,
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.MAXIMUM
        ),
        "MTKPOWER_HINT_APP_TOUCH" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_APP_TOUCH",
            cpuFreqMinCluster0 = 1500000,
            cpuFreqMinCluster1 = 1419000
        ),
        "MTKPOWER_HINT_PROCESS_CREATE" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_PROCESS_CREATE",
            cpuFreqMinCluster0 = 3000000,
            cpuFreqMinCluster1 = 3000000,
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.MAXIMUM,
            bhrOpp = 15,
            holdTime = 6000,
            extHint = 30,
            extHintHoldTime = 35000
        ),
        "MTKPOWER_HINT_APP_ROTATE" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_APP_ROTATE",
            cpuFreqMinCluster0 = 3000000,
            cpuFreqMinCluster1 = 3000000,
            dramOpp = DramOpp.HIGH_PERFORMANCE,
            uclampMin = UclampMin.MAXIMUM,
            schedBoost = SchedBoost.ENABLED
        ),
        "MTKPOWER_HINT_FLINGER_PRINT" to PerformanceScenarioConfig(
            scenarioName = "MTKPOWER_HINT_FLINGER_PRINT",
            cpuFreqMinCluster0 = 3000000,
            cpuFreqMinCluster1 = 3000000,
            dramOpp = DramOpp.HIGH_PERFORMANCE
        ),
        "INTERACTION" to PerformanceScenarioConfig(
            scenarioName = "INTERACTION",
            bhrOpp = 15
        )
    )
}
