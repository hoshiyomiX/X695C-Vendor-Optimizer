package com.x695c.tuner.data

import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Writes configuration files to the vendor partition using root access.
 * All write operations require root privileges to modify /vendor/etc/ files.
 *
 * IMPORTANT: Without root access, NO configuration changes can be applied!
 *
 * Backup/Restore strategy:
 * - NO .bak files are created. All restore operations use hardcoded factory
 *   defaults from [HardcodedDefaults] (baked into APK assets from vendor dump).
 * - On partial write failure in [writeAllConfigs], successfully-written configs
 *   are rolled back to factory defaults (not to previous user-modified state).
 * - This eliminates all external file reliance — zero files created outside /vendor.
 */
object ConfigWriter {
    private val GAME_CONFIG_PATH = VendorPaths.GAME_CONFIG_PATH
    private val SCENARIO_CONFIG_PATH = VendorPaths.SCENARIO_CONFIG_PATH
    private val MEMORY_CONFIG_PATH = VendorPaths.MEMORY_CONFIG_PATH
    private const val GAME_CONFIG_NAME = "game_cfg"
    private const val SCENARIO_CONFIG_NAME = "scenario_cfg"
    private const val MEMORY_CONFIG_NAME = "mem_cfg"

    data class WriteResult(
        val success: Boolean,
        val configName: String,
        val errorMessage: String? = null,
        /** True when write was intentionally skipped (e.g. empty map, no device config loaded). */
        val skipped: Boolean = false
    )

    private fun checkRootOrLog(): Boolean {
        if (!RootChecker.isRootAvailable()) {
            ActivityLogger.logError("ConfigWriter", "Root access required for write operations")
            return false
        }
        return true
    }

    /**
     * Execute a command with root privileges using ProcessBuilder.
     * Consumes stdout and stderr on background threads to prevent deadlock.
     * Returns the exit code and combined output.
     */
    private fun executeRootCommand(command: String): Pair<Int, String> {
        return try {
            val processBuilder = ProcessBuilder("su")
            val process = processBuilder.start()
            val outputStream = DataOutputStream(process.outputStream)

            outputStream.writeBytes("$command\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()

            // Read stdout and stderr on separate threads to prevent buffer deadlock
            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutThread = Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdoutBuilder.appendLine(line)
                    }
                    reader.close()
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderrBuilder.appendLine(line)
                    }
                    reader.close()
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()
            stdoutThread.join(30000)  // 30s timeout per thread
            stderrThread.join(30000)

            val exitCode = process.waitFor()
            val output = stdoutBuilder.toString() + stderrBuilder.toString()
            exitCode to output
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigWriter", "Failed to execute root command: ${e.message}")
            -1 to (e.message ?: "Unknown error")
        }
    }

    /**
     * Write content to a file using root via ProcessBuilder pipe.
     * Pipes content directly through stdin to avoid shell escaping issues.
     *
     * No backup is created — restore uses hardcoded factory defaults from APK assets.
     */
    private fun writeToFileWithRoot(filePath: String, content: String, configName: String): WriteResult {
        if (!checkRootOrLog()) {
            return WriteResult(success = false, configName = configName, errorMessage = "Root access not available")
        }

        return try {
            val processBuilder = ProcessBuilder("su", "-c", "cat > \"$filePath\"")
            val process = processBuilder.start()
            val outputStream = DataOutputStream(process.outputStream)

            outputStream.write(content.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            // Drain streams
            val stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).readText()
                } catch (_: Exception) {}
            }
            val stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).readText()
                } catch (_: Exception) {}
            }
            stdoutThread.start()
            stderrThread.start()
            stdoutThread.join(30000)
            stderrThread.join(30000)

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                // Verify written file is not empty
                val (_, verifyOutput) = executeRootCommand("wc -c \"$filePath\" 2>/dev/null")
                val fileSize = verifyOutput.trim().filter { it.isDigit() }.toIntOrNull() ?: 0
                if (fileSize == 0) {
                    ActivityLogger.logError("ConfigWriter", "Write verification failed: file is empty after write for $configName")
                    WriteResult(success = false, configName = configName, errorMessage = "Write verification failed: file is empty after write")
                } else {
                    WriteResult(success = true, configName = configName)
                }
            } else {
                WriteResult(success = false, configName = configName, errorMessage = "Write failed (exit $exitCode)")
            }
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigWriter", "Failed to write file: ${e.message}")
            WriteResult(success = false, configName = configName, errorMessage = e.message)
        }
    }

    // ==================== USER WRITE OPERATIONS ====================

    fun writeGameConfigs(configs: Map<String, GameTuningConfig>): WriteResult {
        if (!checkRootOrLog()) {
            return WriteResult(success = false, configName = GAME_CONFIG_NAME, errorMessage = "Root access required")
        }
        // FLOW-W001: Guard against empty config map that would overwrite vendor data
        if (configs.isEmpty()) {
            ActivityLogger.logError("ConfigWriter", "Skipped: no game configs to write (empty map would destroy vendor data)")
            return WriteResult(success = true, skipped = true, configName = GAME_CONFIG_NAME, errorMessage = "Skipped: no game configs to write (empty map would destroy vendor data)")
        }
        ActivityLogger.log("ConfigWriter", "WRITE_START", "Writing $GAME_CONFIG_NAME with ${configs.size} entries")
        return try {
            val xmlContent = buildGameConfigXml(configs)
            val result = writeToFileWithRoot(GAME_CONFIG_PATH, xmlContent, GAME_CONFIG_NAME)
            if (result.success) {
                ActivityLogger.log("ConfigWriter", "WRITE_SUCCESS", "$GAME_CONFIG_NAME written successfully")
            } else {
                ActivityLogger.logError("ConfigWriter", "Failed to write $GAME_CONFIG_NAME: ${result.errorMessage}")
            }
            result
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigWriter", "Exception writing $GAME_CONFIG_NAME: ${e.message}")
            WriteResult(success = false, configName = GAME_CONFIG_NAME, errorMessage = e.message)
        }
    }

    fun writeScenarioConfigs(configs: Map<String, PerformanceScenarioConfig>): WriteResult {
        if (!checkRootOrLog()) {
            return WriteResult(success = false, configName = SCENARIO_CONFIG_NAME, errorMessage = "Root access required")
        }
        // FLOW-W001: Guard against empty config map that would overwrite vendor data
        if (configs.isEmpty()) {
            ActivityLogger.logError("ConfigWriter", "Skipped: no scenario configs to write (empty map would destroy vendor data)")
            return WriteResult(success = true, skipped = true, configName = SCENARIO_CONFIG_NAME, errorMessage = "Skipped: no scenario configs to write (empty map would destroy vendor data)")
        }
        ActivityLogger.log("ConfigWriter", "WRITE_START", "Writing $SCENARIO_CONFIG_NAME with ${configs.size} entries")
        return try {
            val xmlContent = buildScenarioConfigXml(configs)
            val result = writeToFileWithRoot(SCENARIO_CONFIG_PATH, xmlContent, SCENARIO_CONFIG_NAME)
            if (result.success) {
                ActivityLogger.log("ConfigWriter", "WRITE_SUCCESS", "$SCENARIO_CONFIG_NAME written successfully")
            } else {
                ActivityLogger.logError("ConfigWriter", "Failed to write $SCENARIO_CONFIG_NAME: ${result.errorMessage}")
            }
            result
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigWriter", "Exception writing $SCENARIO_CONFIG_NAME: ${e.message}")
            WriteResult(success = false, configName = SCENARIO_CONFIG_NAME, errorMessage = e.message)
        }
    }

    fun writeMemoryConfig(config: MemoryManagementConfig): WriteResult {
        if (!checkRootOrLog()) {
            return WriteResult(success = false, configName = MEMORY_CONFIG_NAME, errorMessage = "Root access required")
        }
        ActivityLogger.log("ConfigWriter", "WRITE_START", "Writing $MEMORY_CONFIG_NAME")
        return try {
            val jsonContent = buildMemoryConfigJson(config)
            val result = writeToFileWithRoot(MEMORY_CONFIG_PATH, jsonContent, MEMORY_CONFIG_NAME)
            if (result.success) {
                ActivityLogger.log("ConfigWriter", "WRITE_SUCCESS", "$MEMORY_CONFIG_NAME written successfully")
            } else {
                ActivityLogger.logError("ConfigWriter", "Failed to write $MEMORY_CONFIG_NAME: ${result.errorMessage}")
            }
            result
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigWriter", "Exception writing $MEMORY_CONFIG_NAME: ${e.message}")
            WriteResult(success = false, configName = MEMORY_CONFIG_NAME, errorMessage = e.message)
        }
    }

    fun writeAllConfigs(
        gameConfigs: Map<String, GameTuningConfig>,
        scenarioConfigs: Map<String, PerformanceScenarioConfig>,
        memoryConfig: MemoryManagementConfig?
    ): List<WriteResult> {
        if (!checkRootOrLog()) {
            ActivityLogger.logError("ConfigWriter", "Cannot write configs: Root access required")
            return listOf(
                WriteResult(false, GAME_CONFIG_NAME, "Root access required"),
                WriteResult(false, SCENARIO_CONFIG_NAME, "Root access required"),
                WriteResult(false, MEMORY_CONFIG_NAME, "Root access required")
            )
        }
        ActivityLogger.log("ConfigWriter", "WRITE_ALL_START", "Starting full configuration write")
        val results = mutableListOf<WriteResult>()
        val writtenConfigs = mutableListOf<String>()

        // Write game configs
        val gameResult = writeGameConfigs(gameConfigs)
        results.add(gameResult)
        if (gameResult.success && !gameResult.skipped) writtenConfigs.add(GAME_CONFIG_NAME)

        // Write scenario configs
        val scenarioResult = writeScenarioConfigs(scenarioConfigs)
        results.add(scenarioResult)
        if (scenarioResult.success && !scenarioResult.skipped) writtenConfigs.add(SCENARIO_CONFIG_NAME)

        // FLOW-H005: Skip memory config write if it was never loaded from device.
        // Do NOT fabricate defaults — that would write made-up values to vendor partition.
        val memoryResult = if (memoryConfig != null) {
            writeMemoryConfig(memoryConfig)
        } else {
            ActivityLogger.log("ConfigWriter", "MEMORY_SKIP", "No memory config loaded from device — skipping write to avoid fabricating defaults")
            WriteResult(success = true, skipped = true, configName = MEMORY_CONFIG_NAME, errorMessage = "Skipped: no memory config loaded from device")
        }
        results.add(memoryResult)
        if (memoryResult.success && !memoryResult.skipped) writtenConfigs.add(MEMORY_CONFIG_NAME)

        // FLOW-W003: Rollback on partial failure — restore from hardcoded factory defaults
        // (no .bak files; factory defaults are baked into APK assets)
        val failedConfigs = results.filter { !it.success }
        if (failedConfigs.isNotEmpty() && writtenConfigs.isNotEmpty()) {
            ActivityLogger.logError("ConfigWriter", "PARTIAL FAILURE: ${failedConfigs.size} configs failed, rolling back ${writtenConfigs.size} successful writes to factory defaults")
            writtenConfigs.forEach { cfgName ->
                val restoreResult = restoreFromDefaults(cfgName)
                ActivityLogger.log("ConfigWriter", "ROLLBACK", "$cfgName factory restore: ${if (restoreResult.success) "SUCCESS" else "FAILED"}")
            }
        }

        val successCount = results.count { it.success }
        ActivityLogger.log("ConfigWriter", "WRITE_ALL_COMPLETE", "$successCount/${results.size} configs written successfully")
        return results
    }

    // ==================== RESTORE FROM HARDCODED FACTORY DEFAULTS ====================

    /**
     * Restore a config to its factory default by writing hardcoded vendor dump values
     * from [HardcodedDefaults] (APK assets) to the vendor partition.
     *
     * This replaces the old .bak-based [restoreFromBackup] approach, eliminating
     * all external file reliance. The factory defaults are baked into the APK
     * binary and cannot be lost or corrupted on the device.
     *
     * @param configName One of GAME_CONFIG_NAME, SCENARIO_CONFIG_NAME, MEMORY_CONFIG_NAME
     * @return WriteResult indicating success or failure of the restore
     */
    fun restoreFromDefaults(configName: String): WriteResult {
        if (!checkRootOrLog()) {
            return WriteResult(success = false, configName = configName, errorMessage = "Root access required")
        }

        return when (configName) {
            GAME_CONFIG_NAME -> {
                val defaults = HardcodedDefaults.getGameDefaults()
                if (defaults.isEmpty()) {
                    ActivityLogger.logError("ConfigWriter", "No factory defaults available for game configs")
                    return WriteResult(success = false, configName = configName, errorMessage = "No factory defaults available (HardcodedDefaults not initialized)")
                }
                val xmlContent = buildGameConfigXml(defaults)
                ActivityLogger.log("ConfigWriter", "RESTORE_DEFAULTS", "Restoring $GAME_CONFIG_NAME from hardcoded factory defaults (${defaults.size} entries)")
                val result = writeToFileWithRoot(GAME_CONFIG_PATH, xmlContent, GAME_CONFIG_NAME)
                if (result.success) {
                    ActivityLogger.log("ConfigWriter", "RESTORE_SUCCESS", "$GAME_CONFIG_NAME restored to factory defaults")
                } else {
                    ActivityLogger.logError("ConfigWriter", "Failed to restore $GAME_CONFIG_NAME: ${result.errorMessage}")
                }
                result
            }
            SCENARIO_CONFIG_NAME -> {
                val defaults = HardcodedDefaults.getScenarioDefaults()
                if (defaults.isEmpty()) {
                    ActivityLogger.logError("ConfigWriter", "No factory defaults available for scenario configs")
                    return WriteResult(success = false, configName = configName, errorMessage = "No factory defaults available (HardcodedDefaults not initialized)")
                }
                val xmlContent = buildScenarioConfigXml(defaults)
                ActivityLogger.log("ConfigWriter", "RESTORE_DEFAULTS", "Restoring $SCENARIO_CONFIG_NAME from hardcoded factory defaults (${defaults.size} entries)")
                val result = writeToFileWithRoot(SCENARIO_CONFIG_PATH, xmlContent, SCENARIO_CONFIG_NAME)
                if (result.success) {
                    ActivityLogger.log("ConfigWriter", "RESTORE_SUCCESS", "$SCENARIO_CONFIG_NAME restored to factory defaults")
                } else {
                    ActivityLogger.logError("ConfigWriter", "Failed to restore $SCENARIO_CONFIG_NAME: ${result.errorMessage}")
                }
                result
            }
            MEMORY_CONFIG_NAME -> {
                val defaults = HardcodedDefaults.getMemoryDefaults()
                if (defaults == null) {
                    ActivityLogger.logError("ConfigWriter", "No factory defaults available for memory config")
                    return WriteResult(success = false, configName = configName, errorMessage = "No factory defaults available (HardcodedDefaults not initialized)")
                }
                val jsonContent = buildMemoryConfigJson(defaults)
                ActivityLogger.log("ConfigWriter", "RESTORE_DEFAULTS", "Restoring $MEMORY_CONFIG_NAME from hardcoded factory defaults")
                val result = writeToFileWithRoot(MEMORY_CONFIG_PATH, jsonContent, MEMORY_CONFIG_NAME)
                if (result.success) {
                    ActivityLogger.log("ConfigWriter", "RESTORE_SUCCESS", "$MEMORY_CONFIG_NAME restored to factory defaults")
                } else {
                    ActivityLogger.logError("ConfigWriter", "Failed to restore $MEMORY_CONFIG_NAME: ${result.errorMessage}")
                }
                result
            }
            else -> WriteResult(success = false, configName = configName, errorMessage = "Unknown config: $configName")
        }
    }

    /**
     * Restore ALL configs (games, scenarios, memory) to hardcoded factory defaults.
     * Writes vendor dump values directly to the vendor partition.
     */
    fun restoreAllDefaults(): List<WriteResult> {
        return listOf(
            restoreFromDefaults(GAME_CONFIG_NAME),
            restoreFromDefaults(SCENARIO_CONFIG_NAME),
            restoreFromDefaults(MEMORY_CONFIG_NAME)
        )
    }

    // ==================== XML/JSON BUILDERS ====================

    /**
     * FLOW-C001 fix: Resolve the param1 value for a cmd from rawParams if available.
     * If the raw value differs from the enum's resolved value, prefer the raw value
     * to prevent silent mutation of vendor-specific tuning parameters.
     */
    private fun resolveGameParam(config: GameTuningConfig, cmd: String, enumValue: Int): Int {
        val raw = config.rawParams[cmd]
        return if (raw != null && raw != enumValue) raw else enumValue
    }

    private fun buildGameConfigXml(configs: Map<String, GameTuningConfig>): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<WHITELIST>")

        configs.forEach { (packageName, config) ->
            // Use xmlEscape for all user-provided/package strings
            val safeName = xmlEscape(packageName)
            sb.appendLine("  <Package name=\"$safeName\">")
            sb.appendLine("    <Activity name=\"${xmlEscape(config.activityName)}\">")

            // FLOW-C001 fix: use rawParams to preserve original vendor values
            // Thermal policy
            sb.appendLine("      <data cmd=\"PERF_RES_THERMAL_POLICY\" param1=\"${resolveGameParam(config, "PERF_RES_THERMAL_POLICY", config.thermalPolicy.value)}\"/>")
            // GPU margin mode
            sb.appendLine("      <data cmd=\"PERF_RES_GPU_GED_MARGIN_MODE\" param1=\"${resolveGameParam(config, "PERF_RES_GPU_GED_MARGIN_MODE", config.gpuMarginMode.value)}\"/>")
            // GPU timer DVFS margin
            sb.appendLine("      <data cmd=\"PERF_RES_GPU_GED_TIMER_BASE_DVFS_MARGIN\" param1=\"${config.gpuTimerDvfsMargin}\"/>")
            // Uclamp min
            sb.appendLine("      <data cmd=\"PERF_RES_SCHED_UCLAMP_MIN_TA\" param1=\"${resolveGameParam(config, "PERF_RES_SCHED_UCLAMP_MIN_TA", config.uclampMin.value)}\"/>")
            // Sched boost
            sb.appendLine("      <data cmd=\"PERF_RES_SCHED_BOOST\" param1=\"${resolveGameParam(config, "PERF_RES_SCHED_BOOST", config.schedBoost.value)}\"/>")

            // FPS margin mode (only write when enabled)
            if (config.fpsMarginMode != FpsMarginMode.DISABLED) {
                sb.appendLine("      <data cmd=\"PERF_RES_FPS_FPSGO_MARGIN_MODE\" param1=\"${resolveGameParam(config, "PERF_RES_FPS_FPSGO_MARGIN_MODE", config.fpsMarginMode.value)}\"/>")
            }
            // FPS adjust loading
            if (config.fpsAdjustLoading) {
                sb.appendLine("      <data cmd=\"PERF_RES_FPS_FPSGO_ADJ_LOADING\" param1=\"1\"/>")
            }
            // FPS loading threshold (only write when FPS features are used)
            if (config.fpsMarginMode != FpsMarginMode.DISABLED || config.fpsAdjustLoading) {
                sb.appendLine("      <data cmd=\"PERF_RES_FPS_FPSGO_LLF_TH\" param1=\"${resolveGameParam(config, "PERF_RES_FPS_FPSGO_LLF_TH", config.fpsLoadingThreshold.value)}\"/>")
            }
            // GPU block boost
            if (config.gpuBlockBoost != GpuBlockBoost.DISABLED) {
                sb.appendLine("      <data cmd=\"PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST\" param1=\"${resolveGameParam(config, "PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST", config.gpuBlockBoost.value)}\"/>")
            }
            // Frame rescue (vendor uses PERF_RES_FBT_* names)
            if (config.frameRescuePercent != FrameRescuePercent.NONE) {
                sb.appendLine("      <data cmd=\"PERF_RES_FBT_RESCUE_F\" param1=\"${config.frameRescueF}\"/>")
                sb.appendLine("      <data cmd=\"PERF_RES_FBT_RESCUE_PERCENT\" param1=\"${resolveGameParam(config, "PERF_RES_FBT_RESCUE_PERCENT", config.frameRescuePercent.value)}\"/>")
            }
            // Ultra rescue (vendor uses PERF_RES_FBT_ULTRA_RESCUE)
            if (config.ultraRescue) {
                sb.appendLine("      <data cmd=\"PERF_RES_FBT_ULTRA_RESCUE\" param1=\"1\"/>")
            }
            // Network boost
            if (config.networkBoost != NetworkBoost.DISABLED) {
                sb.appendLine("      <data cmd=\"PERF_RES_NET_NETD_BOOST_UID\" param1=\"${resolveGameParam(config, "PERF_RES_NET_NETD_BOOST_UID", config.networkBoost.value)}\"/>")
            }
            // WiFi low latency
            if (config.wifiLowLatency == WifiLowLatency.ENABLED) {
                sb.appendLine("      <data cmd=\"PERF_RES_NET_WIFI_LOW_LATENCY\" param1=\"1\"/>")
            }
            // Weak signal optimization
            if (config.weakSignalOpt == WeakSignalOpt.ENABLED) {
                sb.appendLine("      <data cmd=\"PERF_RES_NET_MD_WEAK_SIG_OPT\" param1=\"1\"/>")
            }
            // Cold launch time (vendor uses PERF_RES_POWERHAL_WHITELIST_APP_LAUNCH_TIME_COLD)
            if (config.coldLaunchTime > 0) {
                sb.appendLine("      <data cmd=\"PERF_RES_POWERHAL_WHITELIST_APP_LAUNCH_TIME_COLD\" param1=\"${config.coldLaunchTime}\"/>")
            }

            // FLOW-C001 fix: write any additional rawParams that were in the original XML
            // but not covered by the structured fields above (preserves vendor-specific commands)
            val coveredCmds = setOf(
                "PERF_RES_THERMAL_POLICY", "PERF_RES_GPU_GED_MARGIN_MODE", "PERF_RES_GPU_GED_TIMER_BASE_DVFS_MARGIN",
                "PERF_RES_SCHED_UCLAMP_MIN_TA", "PERF_RES_SCHED_BOOST", "PERF_RES_FPS_FPSGO_MARGIN_MODE",
                "PERF_RES_FPS_FPSGO_ADJ_LOADING", "PERF_RES_FPS_FPSGO_LLF_TH", "PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST",
                "PERF_RES_FBT_RESCUE_F", "PERF_RES_FBT_RESCUE_PERCENT",
                "PERF_RES_FBT_ULTRA_RESCUE", "PERF_RES_NET_NETD_BOOST_UID",
                "PERF_RES_NET_WIFI_LOW_LATENCY", "PERF_RES_NET_MD_WEAK_SIG_OPT",
                "PERF_RES_POWERHAL_WHITELIST_APP_LAUNCH_TIME_COLD",
                // Legacy aliases (preserved for rawParams dedup)
                "PERF_RES_FPS_FPSGO_FRAME_RESCUE_F", "PERF_RES_FPS_FPSGO_FRAME_RESCUE_PERCENT",
                "PERF_RES_FPS_FPSGO_ULTRA_RESCUE", "PERF_RES_COLD_LAUNCH_TIME"
            )
            config.rawParams.forEach { (cmd, param1) ->
                if (cmd !in coveredCmds) {
                    sb.appendLine("      <data cmd=\"$cmd\" param1=\"$param1\"/>")
                }
            }

            sb.appendLine("    </Activity>")
            sb.appendLine("  </Package>")
        }

        sb.appendLine("</WHITELIST>")
        return sb.toString()
    }

    private fun buildScenarioConfigXml(configs: Map<String, PerformanceScenarioConfig>): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<SCNTABLE>")

        configs.forEach { (_, config) ->
            val safeName = xmlEscape(config.scenarioName)
            sb.appendLine("  <scenario powerhint=\"$safeName\">")

            if (config.cpuFreqMinCluster0 > 0) {
                sb.appendLine("    <data cmd=\"PERF_RES_CPUFREQ_MIN_CLUSTER_0\" param1=\"${config.cpuFreqMinCluster0}\"></data>")
            }
            if (config.cpuFreqMinCluster1 > 0) {
                sb.appendLine("    <data cmd=\"PERF_RES_CPUFREQ_MIN_CLUSTER_1\" param1=\"${config.cpuFreqMinCluster1}\"></data>")
            }
            // FLOW-C001 fix: prefer rawParams for enum-typed fields
            val dramVal = config.rawParams["PERF_RES_DRAM_OPP_MIN"] ?: config.dramOpp.value
            sb.appendLine("    <data cmd=\"PERF_RES_DRAM_OPP_MIN\" param1=\"$dramVal\"></data>")
            if (config.uclampMin != UclampMin.NONE) {
                val uclampVal = config.rawParams["PERF_RES_SCHED_UCLAMP_MIN_TA"] ?: config.uclampMin.value
                sb.appendLine("    <data cmd=\"PERF_RES_SCHED_UCLAMP_MIN_TA\" param1=\"$uclampVal\"></data>")
            }
            if (config.schedBoost != SchedBoost.DISABLED) {
                val schedVal = config.rawParams["PERF_RES_SCHED_BOOST"] ?: config.schedBoost.value
                sb.appendLine("    <data cmd=\"PERF_RES_SCHED_BOOST\" param1=\"$schedVal\"></data>")
            }
            // Touch boost OPP — only emit if present in original vendor data
            if (config.rawParams.containsKey("PERF_RES_FPS_FBT_TOUCH_BOOST_OPP") || config.touchBoostOpp != TouchBoostOpp.STANDARD) {
                val touchOppVal = config.rawParams["PERF_RES_FPS_FBT_TOUCH_BOOST_OPP"] ?: config.touchBoostOpp.value
                sb.appendLine("    <data cmd=\"PERF_RES_FPS_FBT_TOUCH_BOOST_OPP\" param1=\"$touchOppVal\"></data>")
            }
            if (config.touchBoostDuration != 0L) {
                sb.appendLine("    <data cmd=\"PERF_RES_FPS_FBT_TOUCH_BOOST_DURATION\" param1=\"${config.touchBoostDuration}\"></data>")
            }
            if (config.bhrOpp > 0) {
                sb.appendLine("    <data cmd=\"PERF_RES_FPS_FBT_BHR_OPP\" param1=\"${config.bhrOpp}\"></data>")
            }
            if (config.holdTime > 0) {
                sb.appendLine("    <data cmd=\"PERF_RES_POWER_HINT_HOLD_TIME\" param1=\"${config.holdTime}\"></data>")
            }
            if (config.extHint > 0) {
                sb.appendLine("    <data cmd=\"PERF_RES_POWER_HINT_EXT_HINT\" param1=\"${config.extHint}\"></data>")
            }
            if (config.extHintHoldTime > 0) {
                sb.appendLine("    <data cmd=\"PERF_RES_POWER_HINT_EXT_HINT_HOLD_TIME\" param1=\"${config.extHintHoldTime}\"></data>")
            }

            // FLOW-C001 fix: preserve any additional raw vendor-specific commands
            val coveredCmds = setOf(
                "PERF_RES_CPUFREQ_MIN_CLUSTER_0", "PERF_RES_CPUFREQ_MIN_CLUSTER_1",
                "PERF_RES_DRAM_OPP_MIN", "PERF_RES_SCHED_UCLAMP_MIN_TA", "PERF_RES_SCHED_BOOST",
                "PERF_RES_FPS_FBT_TOUCH_BOOST_OPP", "PERF_RES_FPS_FBT_TOUCH_BOOST_DURATION",
                "PERF_RES_FPS_FBT_BHR_OPP", "PERF_RES_POWER_HINT_HOLD_TIME",
                "PERF_RES_POWER_HINT_EXT_HINT", "PERF_RES_POWER_HINT_EXT_HINT_HOLD_TIME"
            )
            config.rawParams.forEach { (cmd, param1) ->
                if (cmd !in coveredCmds) {
                    sb.appendLine("    <data cmd=\"$cmd\" param1=\"$param1\"></data>")
                }
            }

            sb.appendLine("  </scenario>")
        }

        sb.appendLine("</SCNTABLE>")
        return sb.toString()
    }

    /**
     * Build memory config JSON using READ-MODIFY-WRITE pattern.
     * Reads the original file, merges our changes into it, writes back.
     * This preserves ALL vendor sections we don't explicitly manage
     * (platform_list, game_list, social_list, tracker, interval, provider_filter, etc.)
     *
     * Falls back to creating a new JSON if the original file is unreadable.
     */
    private fun buildMemoryConfigJson(config: MemoryManagementConfig): String {
        // Try read-modify-write: read original JSON, merge our sections, preserve everything else
        val originalJson = try {
            val content = readVendorFileWithRoot(MEMORY_CONFIG_PATH)
            if (content != null) JSONObject(content) else null
        } catch (_: Exception) { null }

        val json = originalJson ?: JSONObject()

        // Always overwrite the 4 sections we manage — preserve all others
        val thresholds = JSONObject().apply {
            put("adj_native", config.thresholds.adjNative)
            put("adj_system", config.thresholds.adjSystem)
            put("adj_persist", config.thresholds.adjPersist)
            put("adj_foreground", config.thresholds.adjForeground)
            put("adj_visible", config.thresholds.adjVisible)
            put("adj_perceptible", config.thresholds.adjPerceptible)
            put("adj_backup", config.thresholds.adjBackup)
            put("adj_heavyweight", config.thresholds.adjHeavyweight)
            put("adj_service", config.thresholds.adjService)
            put("adj_home", config.thresholds.adjHome)
            put("adj_previous", config.thresholds.adjPrevious)
            put("adj_service_b", config.thresholds.adjServiceB)
            put("adj_cached", config.thresholds.adjCached)
            put("swapfree_min_percent", config.thresholds.swapfreeMinPercent)
            put("swapfree_max_percent", config.thresholds.swapfreeMaxPercent)
            put("free_cached", config.thresholds.freeCached)
        }
        json.put("total_mem", thresholds)

        val processLimits = JSONObject().apply {
            put("3rd", config.processLimits.thirdParty)
            put("gms", config.processLimits.gms)
            put("sys", config.processLimits.system)
            put("sys_bg", config.processLimits.systemBg)
            put("game", config.processLimits.game)
        }
        json.put("proc_mem", processLimits)

        val features = JSONObject().apply {
            put("app_start_limit", config.features.appStartLimit)
            put("oom_adj_clean", config.features.oomAdjClean)
            put("low_ram_clean", config.features.lowRamClean)
            put("low_swap_clean", config.features.lowSwapClean)
            put("one_key_clean", config.features.oneKeyClean)
            put("heavy_cpu_clean", config.features.heavyCpuClean)
            put("heavy_iow_clean", config.features.heavyIowClean)
            put("sleep_clean", config.features.sleepClean)
            put("fix_adj", config.features.fixAdj)
            put("limit_sys_start", config.features.limitSysStart)
            put("limit_gms_start", config.features.limitGmsStart)
            put("limit_3rd_start", config.features.limit3rdStart)
            put("allow_clean_sys", config.features.allowCleanSys)
            put("allow_clean_gms", config.features.allowCleanGms)
            put("allow_clean_3rd", config.features.allowClean3rd)
            put("ux_detector_protect", config.features.uxDetectorProtect)
            put("platform_cloud_update", config.features.platformCloudUpdate)
            put("tpms_cloud_update", config.features.tpmsCloudUpdate)
            put("not_limit_fcm_send", config.features.notLimitFcmSend)
            put("not_limit_schedule_run", config.features.notLimitScheduleRun)
            put("not_limit_tpms_send", config.features.notLimitTpmsSend)
        }
        json.put("feature", features)

        val number = JSONObject().apply {
            put("recent_task", config.recentTaskCount)
            put("notification", config.notificationCount)
            put("cached_proc", config.cachedProcCount)
            put("ux_detector_app", config.uxDetectorApp)
        }
        json.put("number", number)

        return json.toString(2)
    }

    /** Read a file from vendor partition using root. Returns null if unreadable. */
    private fun readVendorFileWithRoot(filePath: String): String? {
        return try {
            val (exitCode, output) = executeRootCommand("cat \"$filePath\" 2>/dev/null")
            if (exitCode == 0 && output.isNotBlank()) output.trim() else null
        } catch (_: Exception) { null }
    }
}
