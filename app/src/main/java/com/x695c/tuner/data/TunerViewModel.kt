package com.x695c.tuner.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TunerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private val _selectedProfile = MutableStateFlow(TuningProfile.DEFAULT)
    val selectedProfile: StateFlow<TuningProfile> = _selectedProfile.asStateFlow()

    // All configs start EMPTY — loaded directly from device files, no hardcoded defaults.
    private val _gameConfigs = MutableStateFlow<Map<String, GameTuningConfig>>(emptyMap())
    val gameConfigs: StateFlow<Map<String, GameTuningConfig>> = _gameConfigs.asStateFlow()

    private val _scenarioConfigs = MutableStateFlow<Map<String, PerformanceScenarioConfig>>(emptyMap())
    val scenarioConfigs: StateFlow<Map<String, PerformanceScenarioConfig>> = _scenarioConfigs.asStateFlow()

    private val _memoryConfig = MutableStateFlow<MemoryManagementConfig?>(null)
    val memoryConfig: StateFlow<MemoryManagementConfig?> = _memoryConfig.asStateFlow()

    // Original vendor defaults — sourced from hardcoded vendor dump (HardcodedDefaults),
    // NOT from the device's current vendor files. This ensures restore-to-default
    // always reverts to factory values, even after the user has applied modifications.
    // Persists across app restarts because HardcodedDefaults loads from APK assets.
    private var originalGameConfigs: Map<String, GameTuningConfig> = emptyMap()
    private var originalScenarioConfigs: Map<String, PerformanceScenarioConfig> = emptyMap()
    private var originalMemoryConfig: MemoryManagementConfig? = null

    // Config file availability status
    private val _configAvailability = MutableStateFlow<Map<ConfigFileDetector.ConfigType, ConfigFileDetector.ConfigStatus>>(emptyMap())
    val configAvailability: StateFlow<Map<ConfigFileDetector.ConfigType, ConfigFileDetector.ConfigStatus>> = _configAvailability.asStateFlow()

    // Root access state
    private val _rootState = MutableStateFlow(RootState())
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

    // Config change tracking state
    private val _configChangeStatus = MutableStateFlow<Map<ConfigFileDetector.ConfigType, ConfigChangeTracker.ChangeStatus>>(emptyMap())
    val configChangeStatus: StateFlow<Map<ConfigFileDetector.ConfigType, ConfigChangeTracker.ChangeStatus>> = _configChangeStatus.asStateFlow()

    // Apply state for writing configurations
    private val _applyState = MutableStateFlow(ApplyState())
    val applyState: StateFlow<ApplyState> = _applyState.asStateFlow()

    init {
        ActivityLogger.log("App", "INIT", "X695C Vendor Tuner started")
        checkRootAvailability()
        detectAndLoadConfigs()
    }

    // ==================== ROOT ACCESS METHODS ====================

    private fun checkRootAvailability() {
        viewModelScope.launch {
            // FLOW-H006: Separate su binary detection from root grant check.
            // isRootAvailable() executes `su` and checks exit code (requires user grant).
            // isSuBinaryDetected() only checks filesystem (no grant required).
            val isGranted = RootChecker.isRootAvailable()
            val suDetected = RootChecker.isSuBinaryDetected()
            _rootState.value = RootState(
                isAvailable = suDetected || isGranted,
                hasBeenRequested = false,
                isGranted = isGranted
            )
            if (isGranted) {
                ActivityLogger.log("Root", "GRANTED", "Root access detected and granted on device")
            } else if (suDetected) {
                ActivityLogger.log("Root", "SU_DETECTED", "su binary found but root not yet granted")
            } else {
                ActivityLogger.log("Root", "NOT_DETECTED", "No root access or su binary detected")
            }
        }
    }

    fun requestRootAccess() {
        viewModelScope.launch {
            _rootState.value = _rootState.value.copy(isRequesting = true)
            val granted = RootChecker.requestRootAccess()
            val suDetected = RootChecker.isSuBinaryDetected()
            _rootState.value = RootState(
                isAvailable = suDetected || granted,
                hasBeenRequested = true,
                isGranted = granted,
                isRequesting = false
            )
            if (granted) {
                ActivityLogger.log("Root", "GRANTED", "Root access granted by user")
                initializeConfigChangeBaseline()
            } else {
                ActivityLogger.log("Root", "DENIED", "Root access denied by user or not available")
            }
        }
    }

    fun dismissRootRequest() {
        _rootState.value = _rootState.value.copy(
            hasBeenRequested = true,
            isRequesting = false
        )
        ActivityLogger.log("Root", "DISMISSED", "Root request dialog dismissed")
    }

    fun shouldRequestRoot(): Boolean {
        val state = _rootState.value
        return !state.isAvailable && !state.hasBeenRequested && !state.isRequesting
    }

    // ==================== CONFIG CHANGE TRACKING METHODS ====================

    private fun initializeConfigChangeBaseline() {
        viewModelScope.launch {
            ConfigChangeTracker.initializeBaseline()
            checkConfigChanges()
        }
    }

    fun checkConfigChanges() {
        viewModelScope.launch {
            val changes = ConfigChangeTracker.checkAllForChanges()
            val statuses = ConfigChangeTracker.getAllChangeStatuses()
            _configChangeStatus.value = statuses
            changes.values.filter { it.isExternal }.forEach { result ->
                ActivityLogger.log("ConfigChange", "EXTERNAL_MODIFICATION", "${result.obfuscatedPath} was modified externally")
            }
        }
    }

    fun getFormattedLogs(): String = ActivityLogger.getFormattedLogs()

    fun hasExternalConfigChanges(): Boolean = ConfigChangeTracker.hasAnyExternalChanges()

    fun saveCurrentConfigStatesAsKnown() {
        ConfigChangeTracker.saveAllCurrentStatesAsKnown()
        checkConfigChanges()
    }

    // ==================== CONFIG LOADING (FROM DEVICE ONLY) ====================

    /**
     * Detects and loads all configuration files directly from the device.
     * No hardcoded defaults — all data comes from the vendor partition.
     */
    private fun detectAndLoadConfigs() {
        viewModelScope.launch {
            ActivityLogger.log("FileDetection", "START", "Scanning for config files...")
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Detect config files
            val configs = ConfigFileDetector.detectConfigs()
            _configAvailability.value = configs
            ActivityLogger.log("FileDetection", "COMPLETE", ConfigFileDetector.getStatusSummary())

            // Parse configs from device files directly (no hardcoded fallback)
            loadConfigsFromDevice()
        }
    }

    /**
     * Loads configuration directly from device vendor files.
     * Originals (for restore-to-default) are set from HardcodedDefaults (vendor dump),
     * NOT from the device's current files. This ensures restore always goes back to
     * factory values even after the user has applied modifications and rebooted.
     */
    private suspend fun loadConfigsFromDevice() {
        // All parsing runs on IO dispatcher to avoid blocking the main thread
        val parsedGameConfigs = withContext(Dispatchers.IO) {
            ConfigFileParser.parseGameConfigs()
        }
        if (parsedGameConfigs.isNotEmpty()) {
            _gameConfigs.value = parsedGameConfigs
            ActivityLogger.log("ConfigParser", "GAME_CONFIGS_LOADED", "Loaded ${parsedGameConfigs.size} game configs from device")
        } else {
            ActivityLogger.log("ConfigParser", "GAME_CONFIGS_EMPTY", "No game configs found on device")
        }

        val parsedScenarioConfigs = withContext(Dispatchers.IO) {
            ConfigFileParser.parseScenarioConfigs()
        }
        if (parsedScenarioConfigs.isNotEmpty()) {
            _scenarioConfigs.value = parsedScenarioConfigs
            ActivityLogger.log("ConfigParser", "SCENARIO_CONFIGS_LOADED", "Loaded ${parsedScenarioConfigs.size} scenario configs from device")
        } else {
            ActivityLogger.log("ConfigParser", "SCENARIO_CONFIGS_EMPTY", "No scenario configs found on device")
        }

        val parsedMemoryConfig = withContext(Dispatchers.IO) {
            ConfigFileParser.parseMemoryConfig()
        }
        if (parsedMemoryConfig != null) {
            _memoryConfig.value = parsedMemoryConfig
            ActivityLogger.log("ConfigParser", "MEMORY_CONFIG_LOADED", "Loaded memory config from device")
        } else {
            ActivityLogger.log("ConfigParser", "MEMORY_CONFIG_EMPTY", "No memory config found on device")
        }

        // Set originals from hardcoded vendor dump (factory defaults).
        // This ensures restore-to-default always reverts to factory values,
        // not to the device's current (possibly modified) vendor files.
        snapshotOriginalsFromDefaults()

        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    /**
     * Set the original snapshots from HardcodedDefaults (vendor dump baked into APK).
     * These are the authoritative factory values used for all restore operations.
     */
    private fun snapshotOriginalsFromDefaults() {
        val hardcodedGames = HardcodedDefaults.getGameDefaults()
        if (hardcodedGames.isNotEmpty()) {
            originalGameConfigs = hardcodedGames
            ActivityLogger.log("Defaults", "GAME_SNAPSHOT", "Original game defaults set from vendor dump (${hardcodedGames.size} entries)")
        }
        val hardcodedScenarios = HardcodedDefaults.getScenarioDefaults()
        if (hardcodedScenarios.isNotEmpty()) {
            originalScenarioConfigs = hardcodedScenarios
            ActivityLogger.log("Defaults", "SCENARIO_SNAPSHOT", "Original scenario defaults set from vendor dump (${hardcodedScenarios.size} entries)")
        }
        val hardcodedMemory = HardcodedDefaults.getMemoryDefaults()
        if (hardcodedMemory != null) {
            originalMemoryConfig = hardcodedMemory
            ActivityLogger.log("Defaults", "MEMORY_SNAPSHOT", "Original memory defaults set from vendor dump")
        }
    }

    fun isConfigAvailable(type: ConfigFileDetector.ConfigType): Boolean {
        return _configAvailability.value[type]?.available ?: false
    }

    fun isConfigReadable(type: ConfigFileDetector.ConfigType): Boolean {
        val status = _configAvailability.value[type]
        return status?.available == true && status.readable
    }

    // ==================== PROFILE MANAGEMENT ====================

    fun setProfile(profile: TuningProfile) {
        val oldProfile = _selectedProfile.value
        _selectedProfile.value = profile
        ActivityLogger.logProfileChange(oldProfile.name, profile.name)

        when (profile) {
            TuningProfile.DEFAULT -> requestDefaultProfileReset()
            TuningProfile.POWER_SAVING -> applyPowerSavingMemoryOverrides()
            TuningProfile.BALANCED -> applyBalancedMemoryOverrides()
            TuningProfile.PERFORMANCE -> applyPerformanceMemoryOverrides()
            TuningProfile.GAMING -> applyGamingMemoryOverrides()
            TuningProfile.CUSTOM -> { /* Keep current settings */ }
        }
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
    }

    /**
     * Reset to default: re-read all configs from device.
     * FLOW-L005 fix: Sets needsConfirmationReset flag instead of directly resetting.
     */
    fun requestDefaultProfileReset() {
        _uiState.value = _uiState.value.copy(needsConfirmationReset = true)
        ActivityLogger.log("Profile", "REQUEST_DEFAULT_RESET", "User requested default profile reset (confirmation pending)")
    }

    /** Confirms the default profile reset after user confirmation. FLOW-L005 fix. */
    fun confirmDefaultReset() {
        ActivityLogger.log("Profile", "CONFIRM_DEFAULT_RESET", "User confirmed default profile reset")
        _uiState.value = _uiState.value.copy(needsConfirmationReset = false, hasUnsavedChanges = false)
        viewModelScope.launch { loadConfigsFromDevice() }
    }

    /** Cancels the default profile reset. FLOW-L005 fix. */
    fun cancelDefaultReset() {
        _uiState.value = _uiState.value.copy(needsConfirmationReset = false)
        ActivityLogger.log("Profile", "CANCEL_DEFAULT_RESET", "User cancelled default profile reset")
    }

    private fun applyPowerSavingMemoryOverrides() {
        ActivityLogger.log("Profile", "LOAD_POWER_SAVING", "Applying power saving memory overrides")
        val current = _memoryConfig.value ?: return
        _memoryConfig.value = current.copy(
            features = MemoryFeatureConfig(
                appStartLimit = true, oomAdjClean = true, lowRamClean = true,
                lowSwapClean = true, oneKeyClean = true, heavyCpuClean = true,
                heavyIowClean = true, sleepClean = true, fixAdj = true,
                limit3rdStart = true, allowClean3rd = true
            )
        )
    }

    private fun applyBalancedMemoryOverrides() {
        ActivityLogger.log("Profile", "LOAD_BALANCED", "Applying balanced memory overrides")
        val current = _memoryConfig.value ?: return
        _memoryConfig.value = current.copy(
            features = MemoryFeatureConfig(
                appStartLimit = true, oomAdjClean = true, lowRamClean = true,
                lowSwapClean = true, oneKeyClean = true, heavyCpuClean = false,
                heavyIowClean = false, sleepClean = true, fixAdj = true,
                limit3rdStart = true, allowClean3rd = true
            )
        )
    }

    private fun applyPerformanceMemoryOverrides() {
        ActivityLogger.log("Profile", "LOAD_PERFORMANCE", "Applying performance memory overrides")
        val current = _memoryConfig.value ?: return
        _memoryConfig.value = current.copy(
            features = MemoryFeatureConfig(
                appStartLimit = false, oomAdjClean = true, lowRamClean = true,
                lowSwapClean = false, oneKeyClean = false, heavyCpuClean = false,
                heavyIowClean = false, sleepClean = false, fixAdj = true,
                limit3rdStart = false, allowClean3rd = false
            ),
            thresholds = MemoryThresholdConfig(adjCached = 500, freeCached = 900)
        )
    }

    private fun applyGamingMemoryOverrides() {
        ActivityLogger.log("Profile", "LOAD_GAMING", "Applying gaming memory overrides")
        val current = _memoryConfig.value ?: return
        _memoryConfig.value = current.copy(
            features = MemoryFeatureConfig(
                appStartLimit = false, oomAdjClean = true, lowRamClean = false,
                lowSwapClean = false, oneKeyClean = false, heavyCpuClean = false,
                heavyIowClean = false, sleepClean = false, fixAdj = true,
                limit3rdStart = false, allowClean3rd = false
            ),
            thresholds = MemoryThresholdConfig(adjCached = 400, freeCached = 1000),
            processLimits = ProcessMemoryConfig(
                thirdParty = 150, gms = 150, system = 150, systemBg = 150, game = 400
            )
        )
    }

    // ==================== CONFIG UPDATE METHODS ====================

    fun updateGameConfig(packageName: String, config: GameTuningConfig) {
        val oldConfig = _gameConfigs.value[packageName]
        _gameConfigs.update { configs ->
            configs.toMutableMap().apply { this[packageName] = config }
        }
        _selectedProfile.value = TuningProfile.CUSTOM
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        if (oldConfig != null) {
            ActivityLogger.logConfigChange("GameTuning", "Game[$packageName]", oldConfig.toString(), config.toString())
        }
    }

    fun updateScenarioConfig(scenarioName: String, config: PerformanceScenarioConfig) {
        val oldConfig = _scenarioConfigs.value[scenarioName]
        _scenarioConfigs.update { configs ->
            configs.toMutableMap().apply { this[scenarioName] = config }
        }
        _selectedProfile.value = TuningProfile.CUSTOM
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        if (oldConfig != null) {
            ActivityLogger.logConfigChange("PerformanceScenario", "Scenario[$scenarioName]", oldConfig.toString(), config.toString())
        }
    }

    fun updateMemoryConfig(config: MemoryManagementConfig) {
        val oldConfig = _memoryConfig.value
        _memoryConfig.value = config
        _selectedProfile.value = TuningProfile.CUSTOM
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        ActivityLogger.logConfigChange("MemoryManagement", "MemoryConfig", oldConfig?.toString() ?: "null", config.toString())
    }

    /**
     * Add a custom game by package name.
     * Validates format before accepting. Creates a default config for the new game.
     */
    fun addCustomGame(packageName: String) {
        val trimmed = packageName.trim()
        if (trimmed.isBlank() || _gameConfigs.value.containsKey(trimmed)) {
            return
        }
        if (!isValidPackageName(trimmed)) {
            ActivityLogger.logError("GameTuning", "Invalid package name format: $trimmed")
            return
        }
        val newConfig = GameTuningConfig(packageName = trimmed)
        _gameConfigs.update { configs ->
            configs.toMutableMap().apply { this[trimmed] = newConfig }
        }
        _selectedProfile.value = TuningProfile.CUSTOM
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        ActivityLogger.log("GameTuning", "ADD_GAME", "Added custom game: $trimmed")
    }

    /**
     * Remove a game configuration by package name.
     * FLOW-M002 fix: Users can now delete custom games they added.
     */
    fun removeGame(packageName: String) {
        if (!_gameConfigs.value.containsKey(packageName)) return
        _gameConfigs.update { configs ->
            configs.toMutableMap().apply { this.remove(packageName) }
        }
        _selectedProfile.value = TuningProfile.CUSTOM
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        ActivityLogger.log("GameTuning", "REMOVE_GAME", "Removed game: $packageName")
    }

    /**
     * Restore a single game config to its factory default (from vendor dump).
     */
    fun restoreGameConfig(packageName: String) {
        val original = originalGameConfigs[packageName]
        if (original != null) {
            _gameConfigs.update { configs ->
                configs.toMutableMap().apply { this[packageName] = original }
            }
            _selectedProfile.value = TuningProfile.DEFAULT
            _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
            ActivityLogger.log("GameTuning", "RESTORE_DEFAULT", "Restored game[$packageName] to factory default")
        } else {
            ActivityLogger.log("GameTuning", "RESTORE_SKIP", "No factory default for game[$packageName] — not in vendor dump")
        }
    }

    /**
     * Restore a single scenario config to its factory default (from vendor dump).
     */
    fun restoreScenarioConfig(scenarioName: String) {
        val original = originalScenarioConfigs[scenarioName]
        if (original != null) {
            _scenarioConfigs.update { configs ->
                configs.toMutableMap().apply { this[scenarioName] = original }
            }
            _selectedProfile.value = TuningProfile.DEFAULT
            _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
            ActivityLogger.log("PerformanceScenario", "RESTORE_DEFAULT", "Restored scenario[$scenarioName] to factory default")
        } else {
            ActivityLogger.log("PerformanceScenario", "RESTORE_SKIP", "No factory default for scenario[$scenarioName] — not in vendor dump")
        }
    }

    /**
     * Restore memory config to its factory default (from vendor dump).
     */
    fun restoreMemoryConfig() {
        val original = originalMemoryConfig ?: return
        _memoryConfig.value = original
        _selectedProfile.value = TuningProfile.DEFAULT
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        ActivityLogger.log("MemoryManagement", "RESTORE_DEFAULT", "Restored memory config to factory default")
    }

    /**
     * Restore ALL game configs to their factory defaults.
     * Replaces the entire game config map with the vendor dump values.
     * Games added by the user (not in the vendor dump) are preserved.
     */
    fun restoreAllGameConfigs() {
        val defaults = HardcodedDefaults.getGameDefaults()
        if (defaults.isEmpty()) {
            ActivityLogger.log("GameTuning", "RESTORE_ALL_SKIP", "No factory defaults available")
            return
        }
        // Merge: factory defaults override existing, user-added games are preserved
        val restored = _gameConfigs.value.toMutableMap()
        restored.putAll(defaults)
        _gameConfigs.value = restored
        _selectedProfile.value = TuningProfile.DEFAULT
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        ActivityLogger.log("GameTuning", "RESTORE_ALL", "Restored ${defaults.size} game configs to factory defaults")
    }

    /**
     * Restore ALL scenario configs to their factory defaults.
     * Replaces the entire scenario config map with the vendor dump values.
     */
    fun restoreAllScenarioConfigs() {
        val defaults = HardcodedDefaults.getScenarioDefaults()
        if (defaults.isEmpty()) {
            ActivityLogger.log("PerformanceScenario", "RESTORE_ALL_SKIP", "No factory defaults available")
            return
        }
        _scenarioConfigs.value = defaults.toMap()
        _selectedProfile.value = TuningProfile.DEFAULT
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
        ActivityLogger.log("PerformanceScenario", "RESTORE_ALL", "Restored ${defaults.size} scenario configs to factory defaults")
    }

    /**
     * Restore ALL configs (games, scenarios, memory) to factory defaults.
     * This is the nuclear reset option.
     */
    fun restoreAllConfigs() {
        restoreAllGameConfigs()
        restoreAllScenarioConfigs()
        if (originalMemoryConfig != null) {
            restoreMemoryConfig()
        }
        ActivityLogger.log("RestoreAll", "COMPLETE", "All configurations restored to factory defaults")
    }

    /**
     * Check whether a specific config differs from its factory default.
     */
    fun isGameConfigModified(packageName: String): Boolean {
        val current = _gameConfigs.value[packageName]
        val original = originalGameConfigs[packageName]
        return current != null && current != original
    }

    fun isScenarioConfigModified(scenarioName: String): Boolean {
        val current = _scenarioConfigs.value[scenarioName]
        val original = originalScenarioConfigs[scenarioName]
        return current != null && current != original
    }

    fun isMemoryConfigModified(): Boolean {
        return _memoryConfig.value != null && _memoryConfig.value != originalMemoryConfig
    }

    /** Check if ANY game config differs from its factory default. */
    fun hasAnyGameConfigModified(): Boolean {
        return _gameConfigs.value.any { (pkg, current) ->
            current != originalGameConfigs[pkg]
        }
    }

    /** Check if ANY scenario config differs from its factory default. */
    fun hasAnyScenarioConfigModified(): Boolean {
        return _scenarioConfigs.value.any { (name, current) ->
            current != originalScenarioConfigs[name]
        }
    }

    /**
     * Reload all configurations from device vendor files.
     * FLOW-M003 fix: Users can reload after detecting external changes.
     */
    fun reloadFromDevice() {
        ActivityLogger.log("Profile", "RELOAD", "Reloading all configs from device")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadConfigsFromDevice()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    // ==================== CONFIGURATION APPLY METHODS ====================

    fun canWriteConfigs(): Boolean {
        return _rootState.value.isGranted || RootChecker.isRootAvailable()
    }

    fun applyConfiguration() {
        if (!canWriteConfigs()) {
            ActivityLogger.logError("ConfigApply", "Cannot apply: Root access required")
            _applyState.value = ApplyState(
                isApplying = false,
                lastResult = ApplyResult(success = false, errorMessages = listOf("Root access required to modify system files")),
                showResultDialog = true
            )
            return
        }

        viewModelScope.launch {
            _applyState.value = _applyState.value.copy(isApplying = true)
            ActivityLogger.log("ConfigApply", "START", "Applying configuration changes...")

            try {
                // FLOW-H005: Only write memory config if it was actually loaded from device.
                // Do NOT fabricate defaults — MemoryManagementConfig() defaults look like
                // real vendor values (e.g. adjCached=700) but are made-up numbers.
                val memoryToWrite = _memoryConfig.value
                val results = ConfigWriter.writeAllConfigs(
                    gameConfigs = _gameConfigs.value,
                    scenarioConfigs = _scenarioConfigs.value,
                    memoryConfig = memoryToWrite
                )

                val gameResult = results.find { it.configName == "game_cfg" }
                val scenarioResult = results.find { it.configName == "scenario_cfg" }
                val memoryResult = results.find { it.configName == "mem_cfg" }

                val errors = results.filter { !it.success }.mapNotNull { it.errorMessage }
                val skipped = results.filter { it.skipped }.mapNotNull { it.errorMessage }
                val success = results.all { it.success }

                // FLOW-H005 / FLOW-L004: Distinguish written vs skipped configs.
                // "Written" = actually wrote to file; "skipped" = intentionally not written.
                val applyResult = ApplyResult(
                    success = success,
                    gameConfigWritten = (gameResult?.success == true && gameResult.skipped == false),
                    scenarioConfigWritten = (scenarioResult?.success == true && scenarioResult.skipped == false),
                    memoryConfigWritten = (memoryResult?.success == true && memoryResult.skipped == false),
                    gameConfigSkipped = gameResult?.skipped == true,
                    scenarioConfigSkipped = scenarioResult?.skipped == true,
                    memoryConfigSkipped = memoryResult?.skipped == true,
                    errorMessages = errors,
                    skippedMessages = skipped
                )

                if (success) {
                    ActivityLogger.log("ConfigApply", "SUCCESS", "All configurations applied successfully")
                    saveCurrentConfigStatesAsKnown()
                    _uiState.value = _uiState.value.copy(hasUnsavedChanges = false)
                } else {
                    ActivityLogger.logError("ConfigApply", "Apply failed: ${errors.joinToString(", ")}")
                }

                _applyState.value = ApplyState(isApplying = false, lastResult = applyResult, showResultDialog = true)
            } catch (e: Exception) {
                ActivityLogger.logError("ConfigApply", "Exception during apply: ${e.message}")
                _applyState.value = ApplyState(
                    isApplying = false,
                    lastResult = ApplyResult(success = false, errorMessages = listOf(e.message ?: "Unknown error")),
                    showResultDialog = true
                )
            }
        }
    }

    fun dismissApplyResult() {
        _applyState.value = _applyState.value.copy(showResultDialog = false)
    }

    fun hasUnsavedChanges(): Boolean = _uiState.value.hasUnsavedChanges
}

data class TunerUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val needsConfirmationReset: Boolean = false
)

data class RootState(
    val isAvailable: Boolean = false,
    val hasBeenRequested: Boolean = false,
    val isGranted: Boolean = false,
    val isRequesting: Boolean = false
)

data class ApplyState(
    val isApplying: Boolean = false,
    val lastResult: ApplyResult? = null,
    val showResultDialog: Boolean = false
)

data class ApplyResult(
    val success: Boolean,
    val gameConfigWritten: Boolean = false,
    val scenarioConfigWritten: Boolean = false,
    val memoryConfigWritten: Boolean = false,
    val gameConfigSkipped: Boolean = false,
    val scenarioConfigSkipped: Boolean = false,
    val memoryConfigSkipped: Boolean = false,
    val errorMessages: List<String> = emptyList(),
    val skippedMessages: List<String> = emptyList()
) {
    val successCount: Int
        get() = listOf(gameConfigWritten, scenarioConfigWritten, memoryConfigWritten).count { it }
    val skippedCount: Int
        get() = listOf(gameConfigSkipped, scenarioConfigSkipped, memoryConfigSkipped).count { it }
    val totalConfigs: Int
        get() = 3
}
