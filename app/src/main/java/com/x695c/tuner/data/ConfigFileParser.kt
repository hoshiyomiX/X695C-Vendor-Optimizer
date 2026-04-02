package com.x695c.tuner.data

import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Parses XML and JSON config files from the vendor partition.
 * Supports parsing of game tuning configs, performance scenarios,
 * and memory management configs.
 *
 * XML parsing uses Android's XmlPullParser (not DocumentBuilderFactory).
 * Reason: Android's DocumentBuilderFactory silently ignores
 * disallow-doctype-decl=false and rejects vendor XMLs with DOCTYPE.
 * XmlPullParser is a streaming parser that skips DTD processing entirely,
 * making it immune to both XXE attacks and DOCTYPE rejection.
 *
 * Supports multiple vendor file formats:
 * - Plain text XML/JSON (most common)
 * - Gzip-compressed XML/JSON (common on Infinix/MediaTek firmware)
 * - Android Binary XML (compiled XML, auto-detected via InputStream)
 *
 * Reference: https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing
 */
object ConfigFileParser {

    private val gameConfigPaths = VendorPaths.gameConfigPaths
    private val scenarioConfigPaths = VendorPaths.scenarioConfigPaths
    private val memoryConfigPaths = VendorPaths.memoryConfigPaths

    fun parseGameConfigs(): Map<String, GameTuningConfig> {
        val file = findReadableFile(gameConfigPaths)
        if (file == null) {
            ActivityLogger.log("ConfigParser", "GAME_CONFIG", "No readable game config file found")
            return emptyMap()
        }
        return try {
            val configs = mutableMapOf<String, GameTuningConfig>()
            val parser = createXmlParserFromFile(file)
            var currentPackage: String? = null
            var currentActivity = false
            val rawParams = mutableMapOf<String, Int>()
            var config = GameTuningConfig(packageName = "")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "Package" -> {
                            currentPackage = parser.getAttributeValue(null, "name")
                            rawParams.clear()
                            config = GameTuningConfig(packageName = currentPackage ?: "")
                            currentActivity = false
                        }
                        "Activity" -> {
                            currentActivity = true
                        }
                        "data" -> {
                            val cmd = parser.getAttributeValue(null, "cmd") ?: ""
                            val param1 = parser.getAttributeValue(null, "param1")?.toIntOrNull() ?: 0
                            if (currentActivity && cmd.isNotEmpty()) {
                                rawParams[cmd] = param1
                                config = when (cmd) {
                                    "PERF_RES_THERMAL_POLICY" -> config.copy(thermalPolicy = ThermalPolicy.fromValue(param1))
                                    "PERF_RES_GPU_GED_MARGIN_MODE" -> config.copy(gpuMarginMode = GpuMarginMode.fromValue(param1))
                                    "PERF_RES_SCHED_UCLAMP_MIN_TA" -> config.copy(uclampMin = UclampMin.fromValue(param1))
                                    "PERF_RES_SCHED_BOOST" -> config.copy(schedBoost = SchedBoost.fromValue(param1))
                                    "PERF_RES_FPS_FPSGO_MARGIN_MODE" -> config.copy(fpsMarginMode = FpsMarginMode.fromValue(param1))
                                    "PERF_RES_FPS_FPSGO_ADJ_LOADING" -> config.copy(fpsAdjustLoading = param1 == 1)
                                    "PERF_RES_FPS_FPSGO_LLF_TH" -> config.copy(fpsLoadingThreshold = FpsLoadingThreshold.fromValue(param1))
                                    "PERF_RES_FPS_FPSGO_GPU_BLOCK_BOOST" -> config.copy(gpuBlockBoost = GpuBlockBoost.fromValue(param1))
                                    // Frame rescue: accept both vendor names (PERF_RES_FBT_*) and legacy names
                                    "PERF_RES_FPS_FPSGO_FRAME_RESCUE_F", "PERF_RES_FBT_RESCUE_F" -> config.copy(frameRescueF = param1)
                                    "PERF_RES_FPS_FPSGO_FRAME_RESCUE_PERCENT", "PERF_RES_FBT_RESCUE_PERCENT" -> config.copy(frameRescuePercent = FrameRescuePercent.fromValue(param1))
                                    "PERF_RES_FPS_FPSGO_ULTRA_RESCUE", "PERF_RES_FBT_ULTRA_RESCUE" -> config.copy(ultraRescue = param1 == 1)
                                    "PERF_RES_NET_NETD_BOOST_UID" -> config.copy(networkBoost = NetworkBoost.fromValue(param1))
                                    "PERF_RES_NET_WIFI_LOW_LATENCY" -> config.copy(wifiLowLatency = if (param1 == 1) WifiLowLatency.ENABLED else WifiLowLatency.DISABLED)
                                    "PERF_RES_NET_MD_WEAK_SIG_OPT" -> config.copy(weakSignalOpt = if (param1 == 1) WeakSignalOpt.ENABLED else WeakSignalOpt.DISABLED)
                                    // Cold launch: accept both vendor name and legacy name
                                    "PERF_RES_COLD_LAUNCH_TIME", "PERF_RES_POWERHAL_WHITELIST_APP_LAUNCH_TIME_COLD" -> config.copy(coldLaunchTime = param1)
                                    "PERF_RES_GPU_GED_TIMER_BASE_DVFS_MARGIN" -> config.copy(gpuTimerDvfsMargin = param1)
                                    else -> config
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "Activity" -> currentActivity = false
                        "Package" -> {
                            if (currentPackage != null) {
                                configs[currentPackage] = config.copy(rawParams = rawParams.toMap())
                            }
                            currentPackage = null
                        }
                    }
                }
                eventType = parser.next()
            }
            ActivityLogger.log("ConfigParser", "GAME_CONFIG", "Parsed ${configs.size} game configurations")
            configs
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigParser", "PARSE_ERROR: Game config file found but parse FAILED: ${e.message}")
            emptyMap()
        }
    }

    fun parseScenarioConfigs(): Map<String, PerformanceScenarioConfig> {
        val file = findReadableFile(scenarioConfigPaths)
        if (file == null) {
            ActivityLogger.log("ConfigParser", "SCENARIO_CONFIG", "No readable scenario config file found")
            return emptyMap()
        }
        return try {
            val configs = mutableMapOf<String, PerformanceScenarioConfig>()
            val parser = createXmlParserFromFile(file)
            var scenarioName: String? = null
            val rawParams = mutableMapOf<String, Int>()
            var config = PerformanceScenarioConfig(scenarioName = "")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "scenario" -> {
                            scenarioName = parser.getAttributeValue(null, "powerhint")
                            rawParams.clear()
                            config = PerformanceScenarioConfig(scenarioName = scenarioName ?: "")
                        }
                        "data" -> {
                            val cmd = parser.getAttributeValue(null, "cmd") ?: ""
                            val param1 = parser.getAttributeValue(null, "param1")?.toLongOrNull() ?: 0L
                            val param1Int = param1.toInt()
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
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "scenario" && scenarioName != null) {
                        configs[scenarioName] = config.copy(rawParams = rawParams.toMap())
                        scenarioName = null
                    }
                }
                eventType = parser.next()
            }
            ActivityLogger.log("ConfigParser", "SCENARIO_CONFIG", "Parsed ${configs.size} scenario configurations")
            configs
        } catch (e: Exception) {
            ActivityLogger.logError("ConfigParser", "PARSE_ERROR: Scenario config file found but parse FAILED: ${e.message}")
            emptyMap()
        }
    }

    fun parseMemoryConfig(): MemoryManagementConfig? {
        val file = findReadableFile(memoryConfigPaths)
        if (file == null) {
            ActivityLogger.log("ConfigParser", "MEMORY_CONFIG", "No readable memory config file found")
            return null
        }
        return try {
            val content = readDecodedText(file)
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

    /**
     * Find the first readable file from a list of candidate paths.
     * Returns the File object (not content) so the caller can choose
     * the appropriate decoding method (text, gzip, or binary XML).
     */
    private fun findReadableFile(paths: List<String>): File? {
        for (path in paths) {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                return file
            }
        }
        return null
    }

    /**
     * Create an XmlPullParser from a File, auto-detecting the format:
     * 1. Gzip-compressed XML (magic bytes 0x1F 0x8B) → decompress first
     * 2. Android Binary XML (magic 0x03 0x00 0x08 0x00) → use InputStream (native support)
     * 3. Plain text XML → parse directly
     */
    private fun createXmlParserFromFile(file: File): XmlPullParser {
        val bytes = file.readBytes()
        val hexHeader = bytes.take(16).joinToString("") { "%02X".format(it) }
        ActivityLogger.log("ConfigParser", "FILE_DECODE", "File header (hex): $hexHeader (${bytes.size} bytes)")

        // Gzip compressed (magic: 0x1F 0x8B)
        if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            ActivityLogger.log("ConfigParser", "FILE_DECODE", "Format: GZIP — decompressing")
            val text = GZIPInputStream(ByteArrayInputStream(bytes))
                .bufferedReader(Charsets.UTF_8).readText()
            return createXmlPullParser(text)
        }

        // Android Binary XML (compiled XML, magic: 0x03 0x00 0x08 0x00)
        if (bytes.size >= 4 && bytes[0] == 0x03.toByte() && bytes[1] == 0x00.toByte()
            && bytes[2] == 0x08.toByte() && bytes[3] == 0x00.toByte()
        ) {
            ActivityLogger.log("ConfigParser", "FILE_DECODE", "Format: Android Binary XML")
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(ByteArrayInputStream(bytes), null)
            return parser
        }

        // Raw deflate (zlib without header, common in embedded systems)
        // Try zlib inflate first (many vendor files use zlib-wrapped deflate)
        if (bytes.size >= 2) {
            val decompressed = tryDecompress(bytes, "zlib")
                ?: tryDecompress(bytes, "deflate")
            if (decompressed != null) {
                val preview = String(decompressed.take(40).toByteArray(), Charsets.UTF_8)
                ActivityLogger.log("ConfigParser", "FILE_DECODE", "Format: zlib/deflate compressed — decompressed to ${decompressed.size} bytes, preview: $preview")
                return if (decompressed[0] == 0x03.toByte() && decompressed[1] == 0x00.toByte()) {
                    // Decompressed to binary XML
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(ByteArrayInputStream(decompressed.toByteArray()), null)
                    parser
                } else {
                    createXmlPullParser(String(decompressed.toByteArray(), Charsets.UTF_8))
                }
            }
        }

        // Plain text XML (starts with '<')
        if (bytes.isNotEmpty() && bytes[0] == '<'.code.toByte()) {
            ActivityLogger.log("ConfigParser", "FILE_DECODE", "Format: plain text XML")
            return createXmlPullParser(String(bytes, Charsets.UTF_8))
        }

        // Unknown format — log full header for diagnosis
        ActivityLogger.logError("ConfigParser", "FILE_DECODE: Unknown format! Header hex: $hexHeader — cannot parse")
        return createXmlPullParser(String(bytes, Charsets.UTF_8))
    }

    /**
     * Read a file and return its text content, auto-detecting gzip compression.
     * Used for JSON config files which are always text (never binary XML).
     */
    private fun readDecodedText(file: File): String {
        val bytes = file.readBytes()

        // Gzip compressed (magic: 0x1F 0x8B)
        if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            ActivityLogger.log("ConfigParser", "FILE_DECODE", "Gzip compression detected, decompressing")
            return GZIPInputStream(ByteArrayInputStream(bytes))
                .bufferedReader(Charsets.UTF_8).readText()
        }

        // Try zlib/deflate decompression for JSON files too
        if (bytes.isNotEmpty() && bytes[0] != '{'.code.toByte()) {
            val decompressed = tryDecompress(bytes, "zlib") ?: tryDecompress(bytes, "deflate")
            if (decompressed != null && decompressed[0] == '{'.code.toByte()) {
                return String(decompressed.toByteArray(), Charsets.UTF_8)
            }
        }

        // Plain text
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Try to decompress bytes using the specified algorithm.
     * Returns decompressed data or null if decompression fails.
     */
    private fun tryDecompress(bytes: ByteArray, algorithm: String): ByteArray? {
        return try {
            val inflater = when (algorithm) {
                "zlib" -> Inflater()
                "deflate" -> Inflater(true) // raw deflate, no zlib header
                else -> return null
            }
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var totalRead = 0
            while (totalRead < bytes.size * 20) { // safety limit
                val read = inflater.inflate(buffer)
                if (read == 0 && inflater.needsInput()) break
                output.write(buffer, 0, read)
                totalRead += read
            }
            inflater.end()
            val result = output.toByteArray()
            if (result.size < 10) null else result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create an XmlPullParser from content string.
     * Uses FEATURE_PROCESS_NAMESPACES=false for performance
     * since vendor XMLs don't use namespaces.
     */
    private fun createXmlPullParser(content: String): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(content))
        return parser
    }

    private fun parseMemoryJsonConfig(json: JSONObject): MemoryManagementConfig {
        var thresholds = MemoryThresholdConfig()
        var processLimits = ProcessMemoryConfig()
        var features = MemoryFeatureConfig()
        var recentTaskCount = 6
        var notificationCount = 4
        var cachedProcCount = 16
        var uxDetectorApp = 1

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
                allowClean3rd = featuresJson.optBoolean("allow_clean_3rd", true),
                uxDetectorProtect = featuresJson.optBoolean("ux_detector_protect", true),
                platformCloudUpdate = featuresJson.optBoolean("platform_cloud_update", true),
                tpmsCloudUpdate = featuresJson.optBoolean("tpms_cloud_update", true),
                notLimitFcmSend = featuresJson.optBoolean("not_limit_fcm_send", false),
                notLimitScheduleRun = featuresJson.optBoolean("not_limit_schedule_run", false),
                notLimitTpmsSend = featuresJson.optBoolean("not_limit_tpms_send", false)
            )
        }

        if (json.has("number")) {
            val numberJson = json.getJSONObject("number")
            recentTaskCount = numberJson.optInt("recent_task", 6)
            notificationCount = numberJson.optInt("notification", 4)
            cachedProcCount = numberJson.optInt("cached_proc", 16)
            uxDetectorApp = numberJson.optInt("ux_detector_app", 1)
        }

        return MemoryManagementConfig(
            thresholds = thresholds,
            processLimits = processLimits,
            features = features,
            recentTaskCount = recentTaskCount,
            notificationCount = notificationCount,
            cachedProcCount = cachedProcCount,
            uxDetectorApp = uxDetectorApp
        )
    }
}
