package com.x695c.tuner.data

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Detects and reads vendor configuration files from the device.
 * All paths are obfuscated in logs for security.
 * Uses ConcurrentHashMap for thread-safe state management.
 */
object ConfigFileDetector {

    private val gameConfigPaths = VendorPaths.gameConfigPaths
    private val scenarioConfigPaths = VendorPaths.scenarioConfigPaths
    private val memoryConfigPaths = VendorPaths.memoryConfigPaths

    enum class ConfigType {
        GAME_WHITELIST,
        PERFORMANCE_SCENARIOS,
        MEMORY_MANAGEMENT
    }

    data class ConfigStatus(
        val type: ConfigType,
        val available: Boolean,
        val readable: Boolean
    ) {
        override fun toString(): String = "ConfigStatus(type=$type, available=$available, readable=$readable)"
    }

    // Thread-safe storage
    private val detectedConfigs = ConcurrentHashMap<ConfigType, ConfigStatus>()

    fun detectConfigs(): Map<ConfigType, ConfigStatus> {
        detectConfig(ConfigType.GAME_WHITELIST, gameConfigPaths)
        detectConfig(ConfigType.PERFORMANCE_SCENARIOS, scenarioConfigPaths)
        detectConfig(ConfigType.MEMORY_MANAGEMENT, memoryConfigPaths)
        return detectedConfigs.toMap()
    }

    private fun detectConfig(type: ConfigType, paths: List<String>) {
        var detected = false
        var readable = false
        for (path in paths) {
            val file = File(path)
            if (file.exists()) {
                detected = true
                readable = file.canRead()
                ActivityLogger.logFileDetection(path, true)
                if (readable) break
            } else {
                ActivityLogger.logFileDetection(path, false)
            }
        }
        detectedConfigs[type] = ConfigStatus(type, detected, readable)
    }

    fun getConfigStatus(type: ConfigType): ConfigStatus =
        detectedConfigs[type] ?: ConfigStatus(type, false, false)

    fun isConfigAvailable(type: ConfigType): Boolean =
        detectedConfigs[type]?.available ?: false

    fun isConfigReadable(type: ConfigType): Boolean {
        val status = detectedConfigs[type]
        return status?.available == true && status.readable
    }

    fun readConfigFile(type: ConfigType): String? {
        val paths = when (type) {
            ConfigType.GAME_WHITELIST -> gameConfigPaths
            ConfigType.PERFORMANCE_SCENARIOS -> scenarioConfigPaths
            ConfigType.MEMORY_MANAGEMENT -> memoryConfigPaths
        }
        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                return try {
                    file.readText()
                } catch (e: Exception) {
                    ActivityLogger.logError("ConfigFileDetector", "Failed to read config: ${e.message}")
                    null
                }
            }
        }
        return null
    }

    fun getStatusSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Config File Detection Status ===")
        detectedConfigs.forEach { (type, status) ->
            val statusText = when {
                !status.available -> "NOT FOUND"
                !status.readable -> "FOUND (No Read Permission)"
                else -> "AVAILABLE"
            }
            sb.appendLine("${type.name}: $statusText")
        }
        return sb.toString()
    }
}
