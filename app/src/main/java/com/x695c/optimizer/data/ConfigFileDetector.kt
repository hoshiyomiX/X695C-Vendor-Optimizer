package com.x695c.optimizer.data

import java.io.File

/**
 * Detects and reads vendor configuration files from the device.
 * Requires root access for actual file reading on production devices.
 */
object ConfigFileDetector {

    // Vendor partition paths for X695C (no trailing slash to avoid double slash)
    private const val VENDOR_PATH = "/vendor/etc"
    private const val DATA_VENDOR_PATH = "/data/vendor"

    // Config file paths
    val CONFIG_FILES = mapOf(
        ConfigType.GAME_WHITELIST to listOf(
            "$VENDOR_PATH/power_app_cfg.xml",
            "$VENDOR_PATH/powerhint.xml",
            "$DATA_VENDOR_PATH/power/power_app_cfg.xml"
        ),
        ConfigType.PERFORMANCE_SCENARIOS to listOf(
            "$VENDOR_PATH/powerscntbl.xml",
            "$VENDOR_PATH/powerhint_scene.xml"
        ),
        ConfigType.MEMORY_MANAGEMENT to listOf(
            "$VENDOR_PATH/policy_config_6g_ram.json",
            "$VENDOR_PATH/policy_config.json",
            "$DATA_VENDOR_PATH/lmkd/policy_config.json"
        ),
        ConfigType.GPU_DVFS to listOf(
            "$VENDOR_PATH/gpu_dvfs_setting.xml",
            "$VENDOR_PATH/hwservicectrl.json",
            "/sys/module/mali/parameters/dvfs_margin"
        )
    )

    enum class ConfigType {
        GAME_WHITELIST,
        PERFORMANCE_SCENARIOS,
        MEMORY_MANAGEMENT,
        GPU_DVFS
    }

    data class ConfigStatus(
        val type: ConfigType,
        val available: Boolean,
        val detectedPath: String?,
        val readable: Boolean
    )

    private val detectedConfigs = mutableMapOf<ConfigType, ConfigStatus>()

    /**
     * Check if configuration files exist on the device.
     * Should be called during app initialization.
     */
    fun detectConfigs(): Map<ConfigType, ConfigStatus> {
        CONFIG_FILES.forEach { (type, paths) ->
            var detected = false
            var detectedPath: String? = null
            var readable = false

            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    detected = true
                    detectedPath = path
                    readable = file.canRead()

                    // Log the detection
                    ActivityLogger.logFileDetection(path, true)

                    if (readable) {
                        break // Use first readable file
                    }
                } else {
                    ActivityLogger.logFileDetection(path, false)
                }
            }

            val status = ConfigStatus(type, detected, detectedPath, readable)
            detectedConfigs[type] = status
        }

        return detectedConfigs.toMap()
    }

    /**
     * Get the status of a specific config type.
     */
    fun getConfigStatus(type: ConfigType): ConfigStatus {
        return detectedConfigs[type] ?: ConfigStatus(type, false, null, false)
    }

    /**
     * Check if a config type is available and readable.
     */
    fun isConfigAvailable(type: ConfigType): Boolean {
        return detectedConfigs[type]?.available ?: false
    }

    /**
     * Check if a config type is readable (file exists and can be read).
     */
    fun isConfigReadable(type: ConfigType): Boolean {
        val status = detectedConfigs[type]
        return status?.available == true && status.readable
    }

    /**
     * Get all detected config paths.
     */
    fun getDetectedPaths(): Map<ConfigType, String?> {
        return detectedConfigs.mapValues { it.value.detectedPath }
    }

    /**
     * Read the content of a config file if available and readable.
     * Returns null if file is not available or not readable.
     */
    fun readConfigFile(type: ConfigType): String? {
        val status = detectedConfigs[type] ?: return null

        if (!status.available || !status.readable || status.detectedPath == null) {
            return null
        }

        return try {
            File(status.detectedPath).readText()
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigFileDetector", "Failed to read ${status.detectedPath}: ${e.message}")
            null
        }
    }

    /**
     * Get a summary of all config file statuses.
     */
    fun getStatusSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Config File Detection Status ===")

        detectedConfigs.forEach { (type, status) ->
            val statusText = when {
                !status.available -> "NOT FOUND"
                !status.readable -> "FOUND (No Read Permission)"
                else -> "AVAILABLE: ${status.detectedPath}"
            }
            sb.appendLine("${type.name}: $statusText")
        }

        return sb.toString()
    }
}
