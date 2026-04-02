package com.x695c.tuner.data

import android.content.Context

/**
 * Hardcoded factory defaults from X695C vendor partition dump.
 *
 * These values are baked into the APK as raw XML/JSON files in the assets
 * directory (vendor/power_app_cfg.xml, vendor/powerscntbl.xml,
 * vendor/policy_config_6g_ram.json). They are parsed once on first access
 * and cached in memory.
 *
 * Purpose:
 * - "Restore to Default" always reverts to the original factory vendor values,
 *   regardless of what the device's vendor files currently contain.
 * - Works across app restarts and device reboots because the defaults are
 *   compiled into the APK, not stored on the device.
 * - Even if the user has applied modified configs to the vendor partition,
 *   these hardcoded defaults remain unchanged and available for restore.
 *
 * The vendor dump was sourced from:
 * https://gitlab.com/excaliburXD/android_dump_INFINIX_Infinix-X695C.git
 */
object HardcodedDefaults {

    private const val TAG = "HardcodedDefaults"

    // Cached parsed defaults — loaded once, never modified
    @Volatile
    private var cachedGames: Map<String, GameTuningConfig>? = null

    @Volatile
    private var cachedScenarios: Map<String, PerformanceScenarioConfig>? = null

    @Volatile
    private var cachedMemory: MemoryManagementConfig? = null

    @Volatile
    private var isInitialized = false

    /**
     * Load and parse all vendor dump files from assets.
     * Safe to call multiple times — subsequent calls are no-ops.
     * Must be called with an application Context (not Activity).
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            ActivityLogger.log(TAG, "INIT", "Loading hardcoded vendor defaults from assets")

            cachedGames = loadGameDefaults(context)
            cachedScenarios = loadScenarioDefaults(context)
            cachedMemory = loadMemoryDefaults(context)

            ActivityLogger.log(TAG, "INIT_COMPLETE",
                "Loaded defaults: ${cachedGames?.size ?: 0} games, " +
                "${cachedScenarios?.size ?: 0} scenarios, " +
                "memory=${if (cachedMemory != null) "OK" else "N/A"}"
            )

            isInitialized = true
        }
    }

    /** Reset cached defaults (for testing). */
    fun reset() {
        synchronized(this) {
            cachedGames = null
            cachedScenarios = null
            cachedMemory = null
            isInitialized = false
        }
    }

    // ==================== PUBLIC ACCESSORS ====================

    /** Factory default game configs from vendor dump. */
    fun getGameDefaults(): Map<String, GameTuningConfig> = cachedGames ?: emptyMap()

    /** Factory default scenario configs from vendor dump. */
    fun getScenarioDefaults(): Map<String, PerformanceScenarioConfig> = cachedScenarios ?: emptyMap()

    /** Factory default memory config from vendor dump. */
    fun getMemoryDefaults(): MemoryManagementConfig? = cachedMemory

    /** Check if hardcoded defaults have been successfully loaded. */
    fun isLoaded(): Boolean = isInitialized

    /** Get a specific game's factory default config. */
    fun getGameDefault(packageName: String): GameTuningConfig? = cachedGames?.get(packageName)

    /** Get a specific scenario's factory default config. */
    fun getScenarioDefault(scenarioName: String): PerformanceScenarioConfig? = cachedScenarios?.get(scenarioName)

    // ==================== PRIVATE LOADERS ====================

    private fun loadGameDefaults(context: Context): Map<String, GameTuningConfig> {
        return try {
            val xml = context.assets.open("vendor/power_app_cfg.xml")
                .bufferedReader(Charsets.UTF_8).readText()
            ConfigFileParser.parseGameConfigsFromXml(xml)
        } catch (e: Exception) {
            ActivityLogger.logError(TAG, "Failed to load game defaults from assets: ${e.message}")
            emptyMap()
        }
    }

    private fun loadScenarioDefaults(context: Context): Map<String, PerformanceScenarioConfig> {
        return try {
            val xml = context.assets.open("vendor/powerscntbl.xml")
                .bufferedReader(Charsets.UTF_8).readText()
            ConfigFileParser.parseScenarioConfigsFromXml(xml)
        } catch (e: Exception) {
            ActivityLogger.logError(TAG, "Failed to load scenario defaults from assets: ${e.message}")
            emptyMap()
        }
    }

    private fun loadMemoryDefaults(context: Context): MemoryManagementConfig? {
        return try {
            val json = context.assets.open("vendor/policy_config_6g_ram.json")
                .bufferedReader(Charsets.UTF_8).readText()
            ConfigFileParser.parseMemoryConfigFromJson(json)
        } catch (e: Exception) {
            ActivityLogger.logError(TAG, "Failed to load memory defaults from assets: ${e.message}")
            null
        }
    }
}
