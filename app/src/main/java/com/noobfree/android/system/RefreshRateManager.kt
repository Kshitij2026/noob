package com.noobfree.android.system

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import kotlin.math.roundToInt

class RefreshRateManager(private val context: Context) {

    fun getRefreshRateInfo(): RefreshRateInfo {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display: Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

        if (display == null) {
            return RefreshRateInfo(
                activeRefreshRateHz = 60f,
                peakSupportedHz = 60f,
                minSupportedHz = 60f,
                supportedModes = emptyList(),
                explanation = "Default display metrics unavailable."
            )
        }

        val activeMode = display.mode
        val activeHz = activeMode.refreshRate
        val modes = display.supportedModes.map { mode ->
            DisplayModeSummary(
                modeId = mode.modeId,
                width = mode.physicalWidth,
                height = mode.physicalHeight,
                refreshRateHz = mode.refreshRate,
                isActive = mode.modeId == activeMode.modeId
            )
        }.sortedByDescending { it.refreshRateHz }

        val peakHz = modes.maxOfOrNull { it.refreshRateHz } ?: activeHz
        val minHz = modes.minOfOrNull { it.refreshRateHz } ?: activeHz

        val explanation = buildString {
            append("Currently active: ${activeHz.roundToInt()} Hz (Peak panel capability: ${peakHz.roundToInt()} Hz). ")
            if (peakHz > activeHz + 1f) {
                append("LTPO / Adaptive Sync or Battery Saver dynamically lowers the active refresh rate when idle or displaying static UI.")
            } else {
                append("Panel is running at its peak supported refresh rate.")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                append(" Android 11+ manages per-window frame rate hints automatically.")
            }
        }

        return RefreshRateInfo(
            activeRefreshRateHz = activeHz,
            peakSupportedHz = peakHz,
            minSupportedHz = minHz,
            supportedModes = modes,
            explanation = explanation
        )
    }
}
