# X695C Vendor Optimizer

Android application with Jetpack Compose for configuring INFINIX X695C vendor optimization parameters.

## Device Information

- **Device**: INFINIX Hot 11S / X695C
- **SoC**: MediaTek Helio G95 (MT6785)
- **GPU**: Mali-G76 MC4
- **RAM**: 6GB

## Features

### Game Optimization
- Per-game performance profiles loaded from device vendor files
- Thermal policy configuration
- GPU DVFS margin settings
- Network optimization for gaming
- FPS and frame control settings
- Custom game addition via package name

### Performance Scenarios
- App launch boost
- Touch response optimization
- Screen rotation boost
- Fingerprint scanner optimization
- Process creation boost
- All scenarios loaded from device configuration

### Memory Management
- Memory threshold configuration
- Process memory limits
- Feature flags for memory behavior
- Swap settings

### GPU Settings
- DVFS margin configuration
- Timer-based DVFS control
- Loading-based DVFS step
- Quick presets

## Architecture Notes

All configuration data is loaded **directly from the device vendor partition** at runtime. No game package names or tuning values are hardcoded in the application. The config files are:

- `/vendor/etc/power_app_cfg.xml` — Game optimization whitelist
- `/vendor/etc/powerscntbl.xml` — Performance scenario profiles
- `/vendor/etc/performance/policy_config_6g_ram.json` — Memory management

## Optimization Profiles

| Profile | Description |
|---------|-------------|
| Default | Reload configs from device |
| Power Saving | Optimized for battery life |
| Balanced | Balance between performance and battery |
| Performance | Optimized for smooth operation |
| Gaming | Maximum performance for gaming |
| Custom | User-defined configuration |

## Building

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build APK
```bash
./gradlew assembleRelease
```

The APK will be generated at `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
app/src/main/java/com/x695c/tuner/
├── MainActivity.kt              # Main activity, theme, navigation
├── data/
│   ├── TuningConfig.kt           # Data models, enums, validation helpers
│   ├── TunerViewModel.kt         # ViewModel, state management
│   ├── ConfigWriter.kt           # Root-based file writing (XML/JSON)
│   ├── ConfigFileParser.kt       # XML/JSON parsing from vendor files
│   ├── ConfigFileDetector.kt     # Config file existence/readability check
│   ├── ConfigChangeTracker.kt    # External modification detection (SHA-256)
│   ├── RootChecker.kt            # Root access detection and execution
│   └── ActivityLogger.kt         # Audit logging with path obfuscation
└── ui/
    ├── components/
    │   └── DropdownComponents.kt  # Reusable dropdown components
    └── screens/
        ├── MainDashboardScreen.kt
        ├── GameListScreen.kt
        ├── GameTuningScreen.kt
        ├── ScenarioListScreen.kt
        ├── PerformanceScenarioScreen.kt
        └── MemoryManagementScreen.kt
```

## Technology Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModel + StateFlow
- **Kotlin**: 1.9.22
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## ⚠️ Disclaimer

This application modifies configuration files on the vendor partition of a rooted device. Modifying vendor files can:

- Void your warranty
- Cause system instability
- Lead to bootloops if misconfigured
- Increase device temperature and power consumption

**Use at your own risk. Always backup your original configuration files before making changes.**

## License

This project is provided for educational and research purposes.

## Credits

- Device vendor analysis based on INFINIX X695C firmware
- MediaTek MT6785 performance tuning documentation
