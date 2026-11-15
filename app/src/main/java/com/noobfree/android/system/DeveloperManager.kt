package com.noobfree.android.system

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import com.noobfree.android.data.CommandExecutor

class DeveloperManager(
    private val contentResolver: ContentResolver,
    private val commandExecutor: CommandExecutor
) {
    companion object {
        const val CMD_GET_DEV_OPTIONS = "settings get global development_settings_enabled"
        const val CMD_ENABLE_DEV_OPTIONS = "settings put global development_settings_enabled 1"
        const val CMD_DISABLE_DEV_OPTIONS = "settings put global development_settings_enabled 0"
    }

    suspend fun getDeveloperOptionsState(): DeveloperOptionsState {
        val publicValue = runCatching {
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        }.getOrDefault(0)

        if (commandExecutor.isReady()) {
            val shellRes = commandExecutor.execute(CMD_GET_DEV_OPTIONS)
            val parsedShell = shellRes.stdout.trim().toIntOrNull()
            if (shellRes.isSuccess && parsedShell != null) {
                return DeveloperOptionsState(
                    isEnabled = parsedShell == 1,
                    rawGlobalValue = parsedShell,
                    verificationSource = "Verified via Shizuku Shell (settings get global)",
                    manufacturerNote = getManufacturerCompatibilityNote()
                )
            }
        }

        return DeveloperOptionsState(
            isEnabled = publicValue == 1,
            rawGlobalValue = publicValue,
            verificationSource = "Verified via Android Settings.Global",
            manufacturerNote = getManufacturerCompatibilityNote()
        )
    }

    suspend fun enableDeveloperOptions(): OperationOutcome<DeveloperOptionsState> {
        if (!commandExecutor.isReady()) {
            val current = getDeveloperOptionsState()
            return OperationOutcome(
                success = false,
                data = current,
                message = "Shizuku is not connected. Start Shizuku and grant permission first.",
                executedCommand = CMD_ENABLE_DEV_OPTIONS
            )
        }

        val writeResult = commandExecutor.execute(CMD_ENABLE_DEV_OPTIONS)
        if (!writeResult.isSuccess) {
            val current = getDeveloperOptionsState()
            val reason = writeResult.stderr.ifBlank { "Exit code ${writeResult.exitCode}" }
            return OperationOutcome(
                success = false,
                data = current,
                message = "Command failed: $reason. ${getManufacturerRestrictionHint()}",
                executedCommand = CMD_ENABLE_DEV_OPTIONS,
                rawOutput = reason
            )
        }

        val verifyResult = commandExecutor.execute(CMD_GET_DEV_OPTIONS)
        val verifiedInt = verifyResult.stdout.trim().toIntOrNull()
            ?: runCatching {
                Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
            }.getOrDefault(0)

        val updatedState = DeveloperOptionsState(
            isEnabled = verifiedInt == 1,
            rawGlobalValue = verifiedInt,
            verificationSource = "Verified via `settings get global development_settings_enabled` -> $verifiedInt",
            manufacturerNote = getManufacturerCompatibilityNote()
        )

        return if (verifiedInt == 1) {
            OperationOutcome(
                success = true,
                data = updatedState,
                message = "Developer Options enabled and verified (development_settings_enabled = 1).",
                executedCommand = "$CMD_ENABLE_DEV_OPTIONS && $CMD_GET_DEV_OPTIONS",
                rawOutput = "1"
            )
        } else {
            OperationOutcome(
                success = false,
                data = updatedState,
                message = "Verification failed: value remained '$verifiedInt'. ${getManufacturerRestrictionHint()}",
                executedCommand = "$CMD_ENABLE_DEV_OPTIONS && $CMD_GET_DEV_OPTIONS",
                rawOutput = verifyResult.stdout.ifBlank { verifyResult.stderr }
            )
        }
    }

    suspend fun disableDeveloperOptions(): OperationOutcome<DeveloperOptionsState> {
        if (!commandExecutor.isReady()) {
            val current = getDeveloperOptionsState()
            return OperationOutcome(
                success = false,
                data = current,
                message = "Shizuku is not connected.",
                executedCommand = CMD_DISABLE_DEV_OPTIONS
            )
        }

        val writeResult = commandExecutor.execute(CMD_DISABLE_DEV_OPTIONS)
        val verifyResult = commandExecutor.execute(CMD_GET_DEV_OPTIONS)
        val verifiedInt = verifyResult.stdout.trim().toIntOrNull() ?: -1
        val state = getDeveloperOptionsState()

        return if (writeResult.isSuccess && verifiedInt == 0) {
            OperationOutcome(
                success = true,
                data = state,
                message = "Developer Options disabled and verified (development_settings_enabled = 0).",
                executedCommand = "$CMD_DISABLE_DEV_OPTIONS && $CMD_GET_DEV_OPTIONS",
                rawOutput = "0"
            )
        } else {
            OperationOutcome(
                success = false,
                data = state,
                message = "Could not disable Developer Options (verified value: $verifiedInt).",
                executedCommand = CMD_DISABLE_DEV_OPTIONS,
                rawOutput = writeResult.stderr
            )
        }
    }

    fun getManufacturerCompatibilityNote(): String {
        val mfr = Build.MANUFACTURER.lowercase()
        return when {
            mfr.contains("nothing") ->
                "Nothing OS (e.g., Nothing Phone 3a): Standard AOSP Settings.Global flag supported cleanly via Shizuku."
            mfr.contains("samsung") ->
                "Samsung One UI: Supported via Shizuku. Knox enterprise policies on managed devices may lock Developer Options."
            mfr.contains("xiaomi") || mfr.contains("redmi") || mfr.contains("poco") ->
                "Xiaomi HyperOS / MIUI: Global flag writes cleanly; sub-toggles like 'USB Debugging (Security settings)' require Mi Account verification."
            mfr.contains("oneplus") || mfr.contains("oppo") || mfr.contains("realme") ->
                "OxygenOS / ColorOS: Supported via Shizuku. Some builds auto-hide Developer Options after reboot unless re-toggled."
            mfr.contains("google") ->
                "Pixel / Stock Android: Full native compatibility with Settings.Global and Shizuku UserService."
            else ->
                "${Build.MANUFACTURER}: Uses standard AOSP development_settings_enabled flag; OEM MDM policies may apply."
        }
    }

    private fun getManufacturerRestrictionHint(): String {
        val mfr = Build.MANUFACTURER.lowercase()
        return when {
            mfr.contains("xiaomi") || mfr.contains("redmi") ->
                "On HyperOS/MIUI, ensure 'Disable permission monitoring' is enabled or no MDM lock is active."
            mfr.contains("samsung") ->
                "On Samsung One UI, check if Knox Guard or work profile MDM restricts Developer Options."
            else ->
                "Your device OEM or work profile policy may restrict global settings writes."
        }
    }
}
