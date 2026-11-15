package com.noobfree.android.system

data class OperationOutcome<T>(
    val success: Boolean,
    val data: T,
    val message: String,
    val executedCommand: String? = null,
    val rawOutput: String? = null
)

data class DeveloperOptionsState(
    val isEnabled: Boolean,
    val rawGlobalValue: Int,
    val verificationSource: String,
    val manufacturerNote: String
)

data class AnimationState(
    val windowScale: Float,
    val transitionScale: Float,
    val animatorScale: Float,
    val source: String
) {
    val uniformScale: Float?
        get() = if (windowScale == transitionScale && transitionScale == animatorScale) {
            windowScale
        } else {
            null
        }
}

data class DebugState(
    val adbEnabled: Boolean,
    val wirelessAdbEnabled: Boolean,
    val wirelessDebuggingSupported: Boolean,
    val note: String
)

enum class BackgroundProcessLimit(val value: Int, val label: String, val impactDescription: String) {
    STANDARD(-1, "Standard limit (Default)", "Android manages background processes automatically based on available RAM."),
    NO_BACKGROUND(0, "No background processes", "Kills apps immediately on exit. Causes heavy reloads and delayed notifications."),
    AT_MOST_1(1, "At most 1 process", "Only 1 background app retained. Aggressive multitasking termination."),
    AT_MOST_2(2, "At most 2 processes", "Up to 2 background apps retained. Moderate battery savings on low-RAM devices."),
    AT_MOST_3(3, "At most 3 processes", "Up to 3 background apps retained."),
    AT_MOST_4(4, "At most 4 processes", "Up to 4 background apps retained.");

    companion object {
        fun fromValue(raw: Int): BackgroundProcessLimit =
            entries.find { it.value == raw } ?: STANDARD
    }
}

data class DisplayModeSummary(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRateHz: Float,
    val isActive: Boolean
)

data class RefreshRateInfo(
    val activeRefreshRateHz: Float,
    val peakSupportedHz: Float,
    val minSupportedHz: Float,
    val supportedModes: List<DisplayModeSummary>,
    val explanation: String
)

data class DeviceHardwareInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val deviceCodename: String,
    val marketName: String,
    val osSkinName: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val buildId: String,
    val totalRamFormatted: String,
    val availableRamFormatted: String,
    val cpuHardware: String,
    val supportedAbis: String,
    val cpuCoreCount: Int,
    val screenResolution: String,
    val screenDensityDpi: Int,
    val supportedRefreshRatesSummary: String,
    val manufacturerCompatibilityNotes: List<String>
)
