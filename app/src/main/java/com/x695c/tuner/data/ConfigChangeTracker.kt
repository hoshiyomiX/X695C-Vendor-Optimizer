package com.x695c.tuner.data

import java.io.File
import java.security.MessageDigest

/**
 * Tracks changes to config files by computing and comparing checksums.
 * Detects if config files were modified outside of the APK.
 */
object ConfigChangeTracker {

    // Config file paths (private - never exposed in logs)
    private val trackedFiles = mapOf(
        ConfigFileDetector.ConfigType.GAME_WHITELIST to listOf(
            "/vendor/etc/power_app_cfg.xml",
            "/vendor/etc/powerhint.xml",
            "/data/vendor/power/power_app_cfg.xml"
        ),
        ConfigFileDetector.ConfigType.PERFORMANCE_SCENARIOS to listOf(
            "/vendor/etc/powerscntbl.xml",
            "/vendor/etc/powerhint_scene.xml"
        ),
        ConfigFileDetector.ConfigType.MEMORY_MANAGEMENT to listOf(
            "/vendor/etc/policy_config_6g_ram.json",
            "/vendor/etc/policy_config.json",
            "/data/vendor/lmkd/policy_config.json"
        ),
        ConfigFileDetector.ConfigType.GPU_DVFS to listOf(
            "/vendor/etc/gpu_dvfs_setting.xml",
            "/vendor/etc/hwservicectrl.json",
            "/sys/module/mali/parameters/dvfs_margin"
        )
    )

    // Store checksums from APK's last known state
    private val lastKnownChecksums = mutableMapOf<ConfigFileDetector.ConfigType, String>()

    // Store current checksums from device
    private val currentChecksums = mutableMapOf<ConfigFileDetector.ConfigType, String>()

    // Track if changes were detected
    private val changeStatus = mutableMapOf<ConfigFileDetector.ConfigType, ChangeStatus>()

    data class ChangeStatus(
        val type: ConfigFileDetector.ConfigType,
        val hasChanged: Boolean,
        val lastChecked: Long,
        val fileExists: Boolean
    )

    /**
     * Result of checking config file changes
     */
    data class ChangeCheckResult(
        val type: ConfigFileDetector.ConfigType,
        val hasChanged: Boolean,
        val isExternal: Boolean,  // Changed outside of APK
        val obfuscatedPath: String
    )

    /**
     * Compute MD5 checksum of a file's content.
     */
    private fun computeChecksum(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) {
                return null
            }

            val content = file.readBytes()
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(content)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigChangeTracker", "Failed to compute checksum: ${e.message}")
            null
        }
    }

    /**
     * Get obfuscated path name for logging
     */
    private fun getObfuscatedPath(type: ConfigFileDetector.ConfigType): String {
        return when (type) {
            ConfigFileDetector.ConfigType.GAME_WHITELIST -> "[GAME_CONFIG]"
            ConfigFileDetector.ConfigType.PERFORMANCE_SCENARIOS -> "[SCENARIO_TABLE]"
            ConfigFileDetector.ConfigType.MEMORY_MANAGEMENT -> "[MEMORY_CONFIG]"
            ConfigFileDetector.ConfigType.GPU_DVFS -> "[GPU_CONFIG]"
        }
    }

    /**
     * Save the current checksum as the "last known state" from APK.
     * Call this when the APK modifies a config file.
     */
    fun saveCurrentStateAsKnown(type: ConfigFileDetector.ConfigType) {
        val paths = trackedFiles[type] ?: return

        for (path in paths) {
            val checksum = computeChecksum(path)
            if (checksum != null) {
                lastKnownChecksums[type] = checksum
                ActivityLogger.log("ConfigChangeTracker", "STATE_SAVED", "${getObfuscatedPath(type)} checksum saved")
                return
            }
        }
    }

    /**
     * Save all current config states as known.
     */
    fun saveAllCurrentStatesAsKnown() {
        trackedFiles.keys.forEach { type ->
            saveCurrentStateAsKnown(type)
        }
    }

    /**
     * Check if a specific config type has been modified externally.
     */
    fun checkForChanges(type: ConfigFileDetector.ConfigType): ChangeCheckResult {
        val paths = trackedFiles[type] ?: return ChangeCheckResult(type, false, false, getObfuscatedPath(type))

        var fileExists = false
        var currentChecksum: String? = null

        for (path in paths) {
            val checksum = computeChecksum(path)
            if (checksum != null) {
                currentChecksum = checksum
                currentChecksums[type] = checksum
                fileExists = true
                break
            }
        }

        val lastKnown = lastKnownChecksums[type]
        val hasChanged = if (lastKnown != null && currentChecksum != null) {
            lastKnown != currentChecksum
        } else {
            false  // No baseline to compare
        }

        val isExternal = hasChanged && lastKnown != null

        // Update status
        changeStatus[type] = ChangeStatus(
            type = type,
            hasChanged = hasChanged,
            lastChecked = System.currentTimeMillis(),
            fileExists = fileExists
        )

        if (isExternal) {
            ActivityLogger.log("ConfigChangeTracker", "EXTERNAL_CHANGE_DETECTED", "${getObfuscatedPath(type)} was modified externally")
        }

        return ChangeCheckResult(
            type = type,
            hasChanged = hasChanged,
            isExternal = isExternal,
            obfuscatedPath = getObfuscatedPath(type)
        )
    }

    /**
     * Check all config files for changes.
     * Returns a map of config types to their change status.
     */
    fun checkAllForChanges(): Map<ConfigFileDetector.ConfigType, ChangeCheckResult> {
        val results = mutableMapOf<ConfigFileDetector.ConfigType, ChangeCheckResult>()

        trackedFiles.keys.forEach { type ->
            results[type] = checkForChanges(type)
        }

        return results
    }

    /**
     * Get the current change status for a config type.
     */
    fun getChangeStatus(type: ConfigFileDetector.ConfigType): ChangeStatus? {
        return changeStatus[type]
    }

    /**
     * Get all change statuses.
     */
    fun getAllChangeStatuses(): Map<ConfigFileDetector.ConfigType, ChangeStatus> {
        return changeStatus.toMap()
    }

    /**
     * Check if any config file has been modified externally.
     */
    fun hasAnyExternalChanges(): Boolean {
        return changeStatus.values.any { it.hasChanged }
    }

    /**
     * Clear all stored checksums (for reset).
     */
    fun clearAllChecksums() {
        lastKnownChecksums.clear()
        currentChecksums.clear()
        changeStatus.clear()
        ActivityLogger.log("ConfigChangeTracker", "RESET", "All checksums cleared")
    }

    /**
     * Initialize baseline checksums from current device state.
     * Call this on first run or after a fresh config load.
     */
    fun initializeBaseline() {
        trackedFiles.keys.forEach { type ->
            val paths = trackedFiles[type] ?: return@forEach

            for (path in paths) {
                val checksum = computeChecksum(path)
                if (checksum != null) {
                    lastKnownChecksums[type] = checksum
                    currentChecksums[type] = checksum
                    break
                }
            }
        }

        ActivityLogger.log("ConfigChangeTracker", "BASELINE_INIT", "Baseline checksums initialized for ${lastKnownChecksums.size} config types")
    }

    /**
     * Get a summary of all change statuses.
     */
    fun getChangeSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Config File Change Status ===")

        changeStatus.forEach { (type, status) ->
            val changeText = when {
                !status.fileExists -> "NOT FOUND"
                status.hasChanged -> "MODIFIED (External Change Detected)"
                else -> "UNCHANGED"
            }
            sb.appendLine("${type.name}: $changeText")
        }

        return sb.toString()
    }
}
