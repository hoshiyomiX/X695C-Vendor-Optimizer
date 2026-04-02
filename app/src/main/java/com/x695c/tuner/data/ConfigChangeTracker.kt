package com.x695c.tuner.data

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks changes to config files by computing and comparing checksums.
 * Detects if config files were modified outside of the APK.
 *
 * Uses SHA-256 for integrity verification (stronger than MD5).
 * Uses ConcurrentHashMap for thread-safe state management.
 */
object ConfigChangeTracker {

    private val trackedFiles = mapOf(
        ConfigFileDetector.ConfigType.GAME_WHITELIST to VendorPaths.gameConfigPaths,
        ConfigFileDetector.ConfigType.PERFORMANCE_SCENARIOS to VendorPaths.scenarioConfigPaths,
        ConfigFileDetector.ConfigType.MEMORY_MANAGEMENT to VendorPaths.memoryConfigPaths
    )

    // Thread-safe storage
    private val lastKnownChecksums = ConcurrentHashMap<ConfigFileDetector.ConfigType, String>()
    private val currentChecksums = ConcurrentHashMap<ConfigFileDetector.ConfigType, String>()
    private val changeStatus = ConcurrentHashMap<ConfigFileDetector.ConfigType, ChangeStatus>()

    data class ChangeStatus(
        val type: ConfigFileDetector.ConfigType,
        val hasChanged: Boolean,
        val lastChecked: Long,
        val fileExists: Boolean
    )

    data class ChangeCheckResult(
        val type: ConfigFileDetector.ConfigType,
        val hasChanged: Boolean,
        val isExternal: Boolean,
        val obfuscatedPath: String
    )

    /**
     * Compute SHA-256 checksum of a file's content.
     */
    private fun computeChecksum(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return null
            val content = file.readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(content)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigChangeTracker", "Failed to compute checksum: ${e.message}")
            null
        }
    }

    private fun getObfuscatedPath(type: ConfigFileDetector.ConfigType): String = when (type) {
        ConfigFileDetector.ConfigType.GAME_WHITELIST -> "[GAME_CONFIG]"
        ConfigFileDetector.ConfigType.PERFORMANCE_SCENARIOS -> "[SCENARIO_TABLE]"
        ConfigFileDetector.ConfigType.MEMORY_MANAGEMENT -> "[MEMORY_CONFIG]"
    }

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

    fun saveAllCurrentStatesAsKnown() {
        trackedFiles.keys.forEach { saveCurrentStateAsKnown(it) }
    }

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
            false
        }

        val isExternal = hasChanged && lastKnown != null
        changeStatus[type] = ChangeStatus(type, hasChanged, System.currentTimeMillis(), fileExists)

        if (isExternal) {
            ActivityLogger.log("ConfigChangeTracker", "EXTERNAL_CHANGE_DETECTED", "${getObfuscatedPath(type)} was modified externally")
        }

        return ChangeCheckResult(type, hasChanged, isExternal, getObfuscatedPath(type))
    }

    fun checkAllForChanges(): Map<ConfigFileDetector.ConfigType, ChangeCheckResult> {
        return trackedFiles.keys.associateWith { checkForChanges(it) }
    }

    fun getChangeStatus(type: ConfigFileDetector.ConfigType): ChangeStatus? = changeStatus[type]

    fun getAllChangeStatuses(): Map<ConfigFileDetector.ConfigType, ChangeStatus> = changeStatus.toMap()

    fun hasAnyExternalChanges(): Boolean = changeStatus.values.any { it.hasChanged }

    fun clearAllChecksums() {
        lastKnownChecksums.clear()
        currentChecksums.clear()
        changeStatus.clear()
        ActivityLogger.log("ConfigChangeTracker", "RESET", "All checksums cleared")
    }

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
