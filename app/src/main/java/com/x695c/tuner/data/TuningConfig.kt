package com.x695c.tuner.data

/**
 * Data models for X695C Vendor Tuning Configuration
 * Device: Infinix Note 10 Pro NFC
 * SoC: MediaTek Helio G95 (MT6785)
 *
 * Helio G95 Specifications:
 * - CPU: 2x Cortex-A76 @ 2.05 GHz + 6x Cortex-A55 @ 2.0 GHz
 * - GPU: Mali-G76 MC4 @ 720 MHz (up to 900 MHz boost)
 * - Memory: LPDDR4X up to 2133 MHz
 * - Process: 12nm FinFET
 *
 * IMPORTANT: No game package names or scenario configs are hardcoded.
 * All configuration data is loaded directly from the device's vendor
 * partition files at runtime. If no config files exist on the device,
 * the app shows empty states where the user can add custom entries.
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

/**
 * Game tuning configuration loaded from device vendor files.
 * Enum fields (thermalPolicy, gpuMarginMode, etc.) are resolved for UI display.
 * rawParams preserves the original PERF_RES_* command values from the XML file.
 * During write, rawParams takes precedence to prevent silent value mutation.
 *
 * Example: If XML has PERF_RES_THERMAL_POLICY param1="99",
 *   thermalPolicy resolves to DEFAULT(0) for UI display,
 *   rawParams["PERF_RES_THERMAL_POLICY"] = 99 preserves the original value.
 */
data class GameTuningConfig(
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
    val coldLaunchTime: Int = 0,
    /** Preserves original PERF_RES_* param1 values from the XML file.
     *  Key = cmd (e.g. "PERF_RES_THERMAL_POLICY"), Value = raw param1 int.
     *  Used during write to avoid silent enum default mutation (FLOW-C001 fix). */
    val rawParams: Map<String, Int> = emptyMap()
)

/**
 * Performance scenario configuration loaded from device vendor files.
 * rawParams preserves original PERF_RES_* command values from the XML file.
 */
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
    val extHintHoldTime: Long = 0,
    /** Preserves original PERF_RES_* param1 values from the XML file.
     *  Key = cmd (e.g. "PERF_RES_DRAM_OPP_MIN"), Value = raw param1 int.
     *  Used during write to avoid silent enum default mutation (FLOW-C001 fix). */
    val rawParams: Map<String, Int> = emptyMap()
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
    val allowClean3rd: Boolean = true,
    // Vendor-specific feature flags (preserved on read/write)
    val uxDetectorProtect: Boolean = true,
    val platformCloudUpdate: Boolean = true,
    val tpmsCloudUpdate: Boolean = true,
    val notLimitFcmSend: Boolean = false,
    val notLimitScheduleRun: Boolean = false,
    val notLimitTpmsSend: Boolean = false
)

data class MemoryManagementConfig(
    val thresholds: MemoryThresholdConfig = MemoryThresholdConfig(),
    val processLimits: ProcessMemoryConfig = ProcessMemoryConfig(),
    val features: MemoryFeatureConfig = MemoryFeatureConfig(),
    val recentTaskCount: Int = 6,
    val notificationCount: Int = 4,
    val cachedProcCount: Int = 16,
    val uxDetectorApp: Int = 1
)

// ==================== PRESET PROFILES ====================

enum class TuningProfile(val displayName: String, val description: String) {
    DEFAULT("Default", "Stock configuration"),
    POWER_SAVING("Power Saving", "Tuned for battery life"),
    BALANCED("Balanced", "Balance between performance and battery"),
    PERFORMANCE("Performance", "Tuned for smooth operation"),
    GAMING("Gaming", "Maximum performance for gaming"),
    CUSTOM("Custom", "User-defined configuration")
}

data class FullTuningConfig(
    val profile: TuningProfile = TuningProfile.DEFAULT,
    val gameConfigs: Map<String, GameTuningConfig> = emptyMap(),
    val scenarioConfigs: Map<String, PerformanceScenarioConfig> = emptyMap(),
    val memoryConfig: MemoryManagementConfig = MemoryManagementConfig()
)

// ==================== HELPER FUNCTIONS ====================

/**
 * Package name validation regex (Android package name format).
 * Reference: https://developer.android.com/build/application-id
 */
private val PACKAGE_NAME_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

/**
 * Validate an Android package name format.
 * Returns true if the name matches the standard reverse-domain format.
 */
fun isValidPackageName(packageName: String): Boolean {
    if (packageName.isBlank() || packageName.length > 255) return false
    return PACKAGE_NAME_REGEX.matches(packageName)
}

/**
 * Escape a string for safe inclusion in XML attribute values.
 * Handles the five predefined XML entity references per XML 1.0 spec.
 */
fun xmlEscape(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
