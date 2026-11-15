package com.noobfree.android.system

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class DeviceInfoManager(private val context: Context) {

    fun getDeviceInfo(): DeviceHardwareInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRam = formatBytes(memInfo.totalMem)
        val availRam = formatBytes(memInfo.availMem)

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics)

        val resolution = if (display != null) {
            "${display.mode.physicalWidth} × ${display.mode.physicalHeight} px"
        } else {
            "${metrics.widthPixels} × ${metrics.heightPixels} px"
        }

        val ratesSummary = display?.supportedModes
            ?.map { "${it.refreshRate.roundToInt()} Hz" }
            ?.distinct()
            ?.joinToString(" • ")
            ?: "60 Hz"

        val osSkin = detectOsSkin()
        val marketName = detectMarketName()

        return DeviceHardwareInfo(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.titlecase(Locale.US) },
            brand = Build.BRAND.replaceFirstChar { it.titlecase(Locale.US) },
            model = Build.MODEL,
            deviceCodename = Build.DEVICE,
            marketName = marketName,
            osSkinName = osSkin,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            buildId = Build.DISPLAY,
            totalRamFormatted = totalRam,
            availableRamFormatted = availRam,
            cpuHardware = readCpuHardware(),
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            screenResolution = resolution,
            screenDensityDpi = metrics.densityDpi,
            supportedRefreshRatesSummary = ratesSummary,
            manufacturerCompatibilityNotes = buildCompatibilityNotes()
        )
    }

    private fun detectMarketName(): String {
        val model = Build.MODEL
        val device = Build.DEVICE.lowercase(Locale.US)
        val mfr = Build.MANUFACTURER.lowercase(Locale.US)
        return when {
            mfr.contains("nothing") && (device.contains("asteroids") || model.contains("A059", ignoreCase = true)) ->
                "Nothing Phone (3a)"
            mfr.contains("nothing") ->
                "Nothing $model ($device)"
            else -> "${Build.MANUFACTURER} $model"
        }
    }

    private fun detectOsSkin(): String {
        val mfr = Build.MANUFACTURER.lowercase(Locale.US)
        return when {
            mfr.contains("nothing") -> "Nothing OS"
            mfr.contains("samsung") -> "Samsung One UI"
            mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco") -> "Xiaomi HyperOS / MIUI"
            mfr.contains("oneplus") -> "OxygenOS"
            mfr.contains("oppo") -> "ColorOS"
            mfr.contains("realme") -> "realme UI"
            mfr.contains("google") -> "Pixel / Stock AOSP"
            else -> "Android (${Build.MANUFACTURER})"
        }
    }

    private fun readCpuHardware(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            val socMfr = Build.SOC_MANUFACTURER
            if (!socModel.isNullOrBlank() && socModel != Build.UNKNOWN) {
                return if (!socMfr.isNullOrBlank() && socMfr != Build.UNKNOWN) {
                    "$socMfr $socModel"
                } else {
                    socModel
                }
            }
        }
        return runCatching {
            File("/proc/cpuinfo").useLines { lines ->
                lines.firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
                    ?.substringAfter(":")
                    ?.trim()
            }
        }.getOrNull() ?: Build.HARDWARE
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Unknown"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f GB", gb)
    }

    private fun buildCompatibilityNotes(): List<String> = listOf(
        "Nothing OS (Nothing Phone 3a): Full compatibility with Shizuku Wireless Debugging, Developer Options toggle, and 0x–2x animation scales.",
        "Samsung One UI: Developer Options & animation scales supported. Knox MDM profiles may restrict global settings writes.",
        "Xiaomi HyperOS / MIUI: Global flags supported; 'USB Debugging (Security settings)' requires manual Mi Account verification in Settings.",
        "OnePlus OxygenOS: Supported via Shizuku; disable 'Permission monitoring' in Developer Options if shell writes are blocked.",
        "Stock Android / Pixel: Reference AOSP behavior with full Settings.Global support."
    )
}
