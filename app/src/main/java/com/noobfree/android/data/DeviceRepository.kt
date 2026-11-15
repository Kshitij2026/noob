package com.noobfree.android.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.noobfree.android.system.AnimationManager
import com.noobfree.android.system.AnimationState
import com.noobfree.android.system.BackgroundProcessLimit
import com.noobfree.android.system.DebugManager
import com.noobfree.android.system.DebugState
import com.noobfree.android.system.DeveloperManager
import com.noobfree.android.system.DeveloperOptionsState
import com.noobfree.android.system.DeviceHardwareInfo
import com.noobfree.android.system.DeviceInfoManager
import com.noobfree.android.system.OperationOutcome
import com.noobfree.android.system.RefreshRateInfo
import com.noobfree.android.system.RefreshRateManager

class DeviceRepository(
    private val context: Context,
    val developerManager: DeveloperManager,
    val animationManager: AnimationManager,
    val debugManager: DebugManager,
    val refreshRateManager: RefreshRateManager,
    val deviceInfoManager: DeviceInfoManager
) {
    suspend fun readDeveloperOptionsState(): DeveloperOptionsState =
        developerManager.getDeveloperOptionsState()

    suspend fun enableDeveloperOptions(): OperationOutcome<DeveloperOptionsState> =
        developerManager.enableDeveloperOptions()

    suspend fun disableDeveloperOptions(): OperationOutcome<DeveloperOptionsState> =
        developerManager.disableDeveloperOptions()

    suspend fun readAnimationState(): AnimationState =
        animationManager.getAnimationState()

    suspend fun applyAnimationScale(scale: Float): OperationOutcome<AnimationState> =
        animationManager.setAllAnimationScales(scale)

    suspend fun readDebugState(): DebugState =
        debugManager.getDebugState()

    suspend fun readBackgroundProcessLimit(): BackgroundProcessLimit =
        debugManager.getBackgroundProcessLimit()

    suspend fun setBackgroundProcessLimit(limit: BackgroundProcessLimit): OperationOutcome<BackgroundProcessLimit> =
        debugManager.setBackgroundProcessLimit(limit)

    fun readRefreshRateInfo(): RefreshRateInfo =
        refreshRateManager.getRefreshRateInfo()

    fun readDeviceHardwareInfo(): DeviceHardwareInfo =
        deviceInfoManager.getDeviceInfo()

    fun openDeveloperOptionsSettings(): Boolean = launchSettingsIntent(
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openDeviceInfoSettings(): Boolean = launchSettingsIntent(
        Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openDisplaySettings(): Boolean = launchSettingsIntent(
        Intent(Settings.ACTION_DISPLAY_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openWirelessDebuggingOrDevSettings(): Boolean = launchSettingsIntent(
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openSystemSettings(): Boolean = launchSettingsIntent(
        Intent(Settings.ACTION_SETTINGS)
    )

    fun openAppDetailsSettings(): Boolean = launchSettingsIntent(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        },
        Intent(Settings.ACTION_SETTINGS)
    )

    private fun launchSettingsIntent(vararg intents: Intent): Boolean {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolved = intent.resolveActivity(context.packageManager)
            if (resolved != null) {
                val started = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (started) return true
            }
        }
        return false
    }
}
