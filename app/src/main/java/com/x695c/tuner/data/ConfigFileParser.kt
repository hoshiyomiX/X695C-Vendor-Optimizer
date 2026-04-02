package com.x695c.tuner.data

import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses XML and JSON config files from the vendor partition.
 * Supports parsing of game tuning configs, performance scenarios,
 * and memory management configs.
 *
 * Security: XXE and DTD processing are disabled per OWASP guidelines.
 * Reference: https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing
 */
object ConfigFileParser {

    private val gameConfigPaths = VendorPaths.gameConfigPaths
    private val scenarioConfigPaths = VendorPaths.scenarioConfigPaths
    private val memoryConfigPaths = VendorPaths.memoryConfigPaths

    /**
     * Create a secure DocumentBuilderFactory with XXE protections disabled.
     * Reference: Android security best practices for XML parsing.
     */
    private fun createSecureDocumentBuilderFactory(): DocumentBuilderFactory {
        val factory = DocumentBuilderFactory.newInstance()
        // Disable DTD processing entirely (prevents XXE)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        // Disable external entities
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        // Disable external DTD loading
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        // Disable entity expansion
        factory.setExpandEntityReferences(false)
        return factory
    }

    fun parseGameConfigs(): Map<String, GameTuningConfig> {
        val content = findReadableFile(gameConfigPaths)
        if (content == null) {
            ActivityLogger.log("ConfigParser", "GAME_CONFIG", "No readable game config file found")
            return emptyMap()
        }
        return try {
            val configs = mutableMapOf<String, GameTuningConfig>()
            val doc = parseXmlDocument(content)
            val packages = doc.getElementsByTagName("Package")
            for (i in 0 until packages.length) {
                val packageElement = packages.item(i) as Element
                val packageName = packageElement.getAttribute("name")
                if (packageName.isNotEmpty()) {
                    val config = parseGamePackageConfig(packageName, packageElement)
                    configs[packageName] = config
                }
            }
            ActivityLogger.log("ConfigParser", "GAME_CONFIG", "Parsed ${configs.size} game configurations")
            configs
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigParser", "PARSE_ERROR: Game config file found but parse FAILED: ${e.message}")
            emptyMap()
        }
    }

    fun parseScenarioConfigs(): Map<String, PerformanceScenarioConfig> {
        val content = findReadableFile(scenarioConfigPaths)
        if (content == null) {
            ActivityLogger.log("ConfigParser", "SCENARIO_CONFIG", "No readable scenario config file found")
            return emptyMap()
        }
        return try {
            val configs = mutableMapOf<String, PerformanceScenarioConfig>()
            val doc = parseXmlDocument(content)
            val scenarios = doc.getElementsByTagName("scenario")
            for (i in 0 until scenarios.length) {
                val scenarioElement = scenarios.item(i) as Element
                val scenarioName = scenarioElement.getAttribute("powerhint")
                if (scenarioName.isNotEmpty()) {
                    val config = parseScenarioConfig(scenarioName, scenarioElement)
                    configs[scenarioName] = config
                }
            }
            ActivityLogger.log("ConfigParser", "SCENARIO_CONFIG", "Parsed ${configs.size} scenario configurations")
            configs
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigParser", "PARSE_ERROR: Scenario config file found but parse FAILED: ${e.message}")
            emptyMap()
        }
    }

    fun parseMemoryConfig(): MemoryManagementConfig? {
        val content = findReadableFile(memoryConfigPaths)
        if (content == null) {
            ActivityLogger.log("ConfigParser", "MEMORY_CONFIG", "No readable memory config file found")
            return null
        }
        return try {
            val json = JSONObject(content)
            val config = parseMemoryJsonConfig(json)
            ActivityLogger.log("ConfigParser", "MEMORY_CONFIG", "Successfully parsed memory configuration")
            config
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigParser", "PARSE_ERROR: Memory config file found but parse FAILED: ${e.message}")
            null
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private fun findReadableFile(paths: List<String>): String? {
        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                return file.readText()
            }
        }
        return null
    }

    private fun parseXmlDocument(content: String): Document {
        val factory = createSecureDocumentBuilderFactory()
        val builder = factory.newDocumentBuilder()
        return builder.parse(content.byteInputStream())
    }

    private fun parseGamePackageConfig(packageName: String, element: Element): GameTuningConfig {
        val rawParams = mutableMapOf<String, Int>()
        var config = GameTuningConfig(packageName = packageName)
        val activities = element.getElementsByTagName("Activity")
        for (i in 0 until activities.length) {
            val activityElement = activities.item(i) as Element
            config = parseActivityDataElements(activityElement, config, rawParams)
        }
        return config.copy(rawParams = rawParams)
    }

    /**
     * Parse all <data> elements under an <Activity>.
     * Stores every cmd/param1 pair in rawParams to preserve original values.
     * Also resolves enum fields for UI display.
     */
    private fun parseActivityDataElements(activityElement: Element, config: GameTuningConfig, rawParams: MutableMap<String, Int>): GameTuningConfig {
        var currentConfig = config
        val dataElements = activityElement.getElementsByTagName("data")
        for (i in 0 until dataElements.length) {
            val dataElement = dataElements.item(i) as Element
            val cmd = dataElement.getAttribute("cmd")
            val param1 = dataElement.getAttribute("param1").toIntOrNull() ?: 0
            // FLOW-C001 fix: always store raw value to prevent silent mutation
            rawParams[cmd] = param1
            currentConfig = when (cmd) {
                "PERF_RES_THERMAL_POLICY" -> currentConfig.copy(thermalPolicy = ThermalPolicy.fromValue(param1))
                "PERF_RES_GPU_GED_MARGIN_MODE" -> currentConfig.copy(gpuMarginMode = GpuMarginMode.fromValue(param1))
                "PERF_RES_SCHED_UCLAMP_MIN_TA" -> currentConfig.copy(uclampMin = UclampMin.fromValue(param1))
                "PERF_RES_SCHED_BOOST" -> currentConfig.copy(schedBoost = SchedBoost.fromValue(param1))
                "PERF_RES_FPS_FPSGO_MARGIN_MODE" -> currentConfig.copy(fpsMarginMode = FpsMarginMode.fromValue(param1))
                "PERF_RES_FPS_FPSGO_ADJ_LOADING" -> currentConfig.copy(fpsAdjustLoading = param1 == 1)
                "PERF_RES_FPS_FPSGO_LLF_TH" -> currentConfig.copy(fpsLoadingThreshold = FpsLoadingThreshold.fromValue(param1))
                "PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST" -> currentConfig.copy(gpuBlockBoost = GpuBlockBoost.fromValue(param1))
                "PERF_RES_FPS_FPSGO_FRAME_RESCUE_F" -> currentConfig.copy(frameRescueF = param1)
                "PERF_RES_FPS_FPSGO_FRAME_RESCUE_PERCENT" -> currentConfig.copy(frameRescuePercent = FrameRescuePercent.fromValue(param1))
                "PERF_RES_FPS_FPSGO_ULTRA_RESCUE" -> currentConfig.copy(ultraRescue = param1 == 1)
                "PERF_RES_NET_NETD_BOOST_UID" -> currentConfig.copy(networkBoost = NetworkBoost.fromValue(param1))
                "PERF_RES_NET_WIFI_LOW_LATENCY" -> currentConfig.copy(wifiLowLatency = if (param1 == 1) WifiLowLatency.ENABLED else WifiLowLatency.DISABLED)
                "PERF_RES_NET_MD_WEAK_SIG_OPT" -> currentConfig.copy(weakSignalOpt = if (param1 == 1) WeakSignalOpt.ENABLED else WeakSignalOpt.DISABLED)
                "PERF_RES_COLD_LAUNCH_TIME" -> currentConfig.copy(coldLaunchTime = param1)
                "PERF_RES_GPU_GED_TIMER_BASE_DVFS_MARGIN" -> currentConfig.copy(gpuTimerDvfsMargin = param1)
                else -> currentConfig
            }
        }
        return currentConfig
    }

    /** Parse a <scenario> element. Stores all cmd/param1 in rawParams for write integrity. */
    private fun parseScenarioConfig(scenarioName: String, element: Element): PerformanceScenarioConfig {
        val rawParams = mutableMapOf<String, Int>()
        var config = PerformanceScenarioConfig(scenarioName = scenarioName)
        val dataElements = element.getElementsByTagName("data")
        for (i in 0 until dataElements.length) {
            val dataElement = dataElements.item(i) as Element
            val cmd = dataElement.getAttribute("cmd")
            val param1 = dataElement.getAttribute("param1").toLongOrNull() ?: 0L
            val param1Int = param1.toInt()
            // FLOW-C001 fix: always store raw value
            rawParams[cmd] = param1Int
            config = when (cmd) {
                "PERF_RES_CPUFREQ_MIN_CLUSTER_0" -> config.copy(cpuFreqMinCluster0 = param1)
                "PERF_RES_CPUFREQ_MIN_CLUSTER_1" -> config.copy(cpuFreqMinCluster1 = param1)
                "PERF_RES_DRAM_OPP_MIN" -> config.copy(dramOpp = DramOpp.fromValue(param1Int))
                "PERF_RES_SCHED_UCLAMP_MIN_TA" -> config.copy(uclampMin = UclampMin.fromValue(param1Int))
                "PERF_RES_SCHED_BOOST" -> config.copy(schedBoost = SchedBoost.fromValue(param1Int))
                "PERF_RES_FPS_FBT_TOUCH_BOOST_OPP" -> config.copy(touchBoostOpp = TouchBoostOpp.fromValue(param1Int))
                "PERF_RES_FPS_FBT_TOUCH_BOOST_DURATION" -> config.copy(touchBoostDuration = param1)
                "PERF_RES_FPS_FBT_BHR_OPP" -> config.copy(bhrOpp = param1Int)
                "PERF_RES_POWER_HINT_HOLD_TIME" -> config.copy(holdTime = param1)
                "PERF_RES_POWER_HINT_EXT_HINT" -> config.copy(extHint = param1Int)
                "PERF_RES_POWER_HINT_EXT_HINT_HOLD_TIME" -> config.copy(extHintHoldTime = param1)
                else -> config
            }
        }
        return config.copy(rawParams = rawParams)
    }

    private fun parseMemoryJsonConfig(json: JSONObject): MemoryManagementConfig {
        var thresholds = MemoryThresholdConfig()
        var processLimits = ProcessMemoryConfig()
        var features = MemoryFeatureConfig()
        var recentTaskCount = 6
        var notificationCount = 4
        var cachedProcCount = 16

        if (json.has("total_mem")) {
            val thresholdsJson = json.getJSONObject("total_mem")
            thresholds = MemoryThresholdConfig(
                adjNative = thresholdsJson.optInt("adj_native", 1024),
                adjSystem = thresholdsJson.optInt("adj_system", 1024),
                adjPersist = thresholdsJson.optInt("adj_persist", 1024),
                adjForeground = thresholdsJson.optInt("adj_foreground", 200),
                adjVisible = thresholdsJson.optInt("adj_visible", 400),
                adjPerceptible = thresholdsJson.optInt("adj_perceptible", 300),
                adjBackup = thresholdsJson.optInt("adj_backup", 300),
                adjHeavyweight = thresholdsJson.optInt("adj_heavyweight", 150),
                adjService = thresholdsJson.optInt("adj_service", 200),
                adjHome = thresholdsJson.optInt("adj_home", 150),
                adjPrevious = thresholdsJson.optInt("adj_previous", 200),
                adjServiceB = thresholdsJson.optInt("adj_service_b", 200),
                adjCached = thresholdsJson.optInt("adj_cached", 700),
                swapfreeMinPercent = thresholdsJson.optInt("swapfree_min_percent", 5),
                swapfreeMaxPercent = thresholdsJson.optInt("swapfree_max_percent", 10),
                freeCached = thresholdsJson.optInt("free_cached", 700)
            )
        }

        if (json.has("proc_mem")) {
            val limitsJson = json.getJSONObject("proc_mem")
            processLimits = ProcessMemoryConfig(
                thirdParty = limitsJson.optInt("3rd", 100),
                gms = limitsJson.optInt("gms", 100),
                system = limitsJson.optInt("sys", 100),
                systemBg = limitsJson.optInt("sys_bg", 100),
                game = limitsJson.optInt("game", 300)
            )
        }

        if (json.has("feature")) {
            val featuresJson = json.getJSONObject("feature")
            features = MemoryFeatureConfig(
                appStartLimit = featuresJson.optBoolean("app_start_limit", true),
                oomAdjClean = featuresJson.optBoolean("oom_adj_clean", true),
                lowRamClean = featuresJson.optBoolean("low_ram_clean", true),
                lowSwapClean = featuresJson.optBoolean("low_swap_clean", true),
                oneKeyClean = featuresJson.optBoolean("one_key_clean", true),
                heavyCpuClean = featuresJson.optBoolean("heavy_cpu_clean", false),
                heavyIowClean = featuresJson.optBoolean("heavy_iow_clean", false),
                sleepClean = featuresJson.optBoolean("sleep_clean", true),
                fixAdj = featuresJson.optBoolean("fix_adj", true),
                limitSysStart = featuresJson.optBoolean("limit_sys_start", false),
                limitGmsStart = featuresJson.optBoolean("limit_gms_start", false),
                limit3rdStart = featuresJson.optBoolean("limit_3rd_start", true),
                allowCleanSys = featuresJson.optBoolean("allow_clean_sys", false),
                allowCleanGms = featuresJson.optBoolean("allow_clean_gms", false),
                allowClean3rd = featuresJson.optBoolean("allow_clean_3rd", true)
            )
        }

        if (json.has("number")) {
            val numberJson = json.getJSONObject("number")
            recentTaskCount = numberJson.optInt("recent_task", 6)
            notificationCount = numberJson.optInt("notification", 4)
            cachedProcCount = numberJson.optInt("cached_proc", 16)
        }

        return MemoryManagementConfig(
            thresholds = thresholds,
            processLimits = processLimits,
            features = features,
            recentTaskCount = recentTaskCount,
            notificationCount = notificationCount,
            cachedProcCount = cachedProcCount
        )
    }
}
