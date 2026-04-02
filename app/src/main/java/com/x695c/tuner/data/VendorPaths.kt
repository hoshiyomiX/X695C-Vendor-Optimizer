package com.x695c.tuner.data

/**
 * Centralized vendor configuration file paths.
 * FLOW-M004 fix: Eliminates path duplication across 4 files (DRY principle).
 * All path references must come from here.
 *
 * Reference: MediaTek Perf Service vendor config paths for Helio G95 (MT6785)
 */
object VendorPaths {
    const val GAME_CONFIG_PATH = "/vendor/etc/power_app_cfg.xml"
    const val SCENARIO_CONFIG_PATH = "/vendor/etc/powerscntbl.xml"
    const val MEMORY_CONFIG_PATH = "/vendor/etc/performance/policy_config_6g_ram.json"

    val gameConfigPaths: List<String> get() = listOf(GAME_CONFIG_PATH)
    val scenarioConfigPaths: List<String> get() = listOf(SCENARIO_CONFIG_PATH)
    val memoryConfigPaths: List<String> get() = listOf(MEMORY_CONFIG_PATH)

    /** Obfuscate vendor paths for secure logging. */
    fun obfuscatePath(path: String): String = when {
        path.contains("power_app_cfg") -> "[GAME_CONFIG]"
        path.contains("powerscntbl") -> "[SCENARIO_TABLE]"
        path.contains("policy_config") -> "[MEMORY_CONFIG]"
        path.contains("/vendor/") -> "[VENDOR_FILE]"
        else -> "[CONFIG_FILE]"
    }

    /** Map ConfigType to its vendor config file path(s). */
    fun pathsForType(type: ConfigFileDetector.ConfigType): List<String> = when (type) {
        ConfigFileDetector.ConfigType.GAME_WHITELIST -> gameConfigPaths
        ConfigFileDetector.ConfigType.PERFORMANCE_SCENARIOS -> scenarioConfigPaths
        ConfigFileDetector.ConfigType.MEMORY_MANAGEMENT -> memoryConfigPaths
    }
}
