package com.noobfree.android.system

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import com.noobfree.android.data.CommandExecutor

class DebugManager(
    private val contentResolver: ContentResolver,
    private val commandExecutor: CommandExecutor
) {
    companion object {
        const val CMD_GET_ADB = "settings get global adb_enabled"
        const val CMD_GET_WIFI_ADB = "settings get global adb_wifi_enabled"
        const val CMD_GET_BG_LIMIT = "settings get global background_process_limit"
    }

    suspend fun getDebugState(): DebugState {
        var adbInt = runCatching {
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0)
        }.getOrDefault(0)

        var wifiAdbInt = runCatching {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0)
        }.getOrDefault(0)

        if (commandExecutor.isReady()) {
            commandExecutor.execute(CMD_GET_ADB).stdout.trim().toIntOrNull()?.let { adbInt = it }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                commandExecutor.execute(CMD_GET_WIFI_ADB).stdout.trim().toIntOrNull()?.let { wifiAdbInt = it }
            }
        }

        val wirelessSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val note = if (wirelessSupported) {
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) supports native Wireless Debugging in Developer Options. " +
                "For security, Android requires manual user confirmation to toggle USB/Wireless debugging."
        } else {
            "Native Wireless Debugging UI requires Android 11 (API 30) or higher."
        }

        return DebugState(
            adbEnabled = adbInt == 1,
            wirelessAdbEnabled = wifiAdbInt == 1,
            wirelessDebuggingSupported = wirelessSupported,
            note = note
        )
    }

    suspend fun getBackgroundProcessLimit(): BackgroundProcessLimit {
        var raw = runCatching {
            Settings.Global.getInt(contentResolver, "background_process_limit", -1)
        }.getOrDefault(-1)

        if (commandExecutor.isReady()) {
            val res = commandExecutor.execute(CMD_GET_BG_LIMIT)
            res.stdout.trim().toIntOrNull()?.let { raw = it }
        }
        return BackgroundProcessLimit.fromValue(raw)
    }

    suspend fun setBackgroundProcessLimit(
        limit: BackgroundProcessLimit
    ): OperationOutcome<BackgroundProcessLimit> {
        if (!commandExecutor.isReady()) {
            return OperationOutcome(
                success = false,
                data = getBackgroundProcessLimit(),
                message = "Shizuku is not connected. Authorize Shizuku to configure background process limits."
            )
        }

        val cmd = "settings put global background_process_limit ${limit.value}"
        val writeRes = commandExecutor.execute(cmd)
        val verified = getBackgroundProcessLimit()

        return if (writeRes.isSuccess && verified == limit) {
            OperationOutcome(
                success = true,
                data = verified,
                message = "Background process limit set to '${limit.label}' and verified.",
                executedCommand = "$cmd && $CMD_GET_BG_LIMIT",
                rawOutput = limit.value.toString()
            )
        } else {
            OperationOutcome(
                success = false,
                data = verified,
                message = "Failed to apply background process limit: ${writeRes.stderr.ifBlank { "OEM restriction or value reverted" }}",
                executedCommand = cmd,
                rawOutput = writeRes.stderr
            )
        }
    }
}
