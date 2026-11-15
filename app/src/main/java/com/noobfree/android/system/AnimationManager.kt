package com.noobfree.android.system

import android.content.ContentResolver
import android.provider.Settings
import com.noobfree.android.data.CommandExecutor
import kotlin.math.abs

class AnimationManager(
    private val contentResolver: ContentResolver,
    private val commandExecutor: CommandExecutor
) {
    companion object {
        val PRESET_SCALES = listOf(0f, 0.5f, 1f, 1.5f, 2f)

        const val KEY_WINDOW = Settings.Global.WINDOW_ANIMATION_SCALE
        const val KEY_TRANSITION = Settings.Global.TRANSITION_ANIMATION_SCALE
        const val KEY_ANIMATOR = Settings.Global.ANIMATOR_DURATION_SCALE

        fun formatScaleForShell(scale: Float): String {
            return when {
                abs(scale - 0f) < 0.01f -> "0"
                abs(scale - 0.5f) < 0.01f -> "0.5"
                abs(scale - 1f) < 0.01f -> "1"
                abs(scale - 1.5f) < 0.01f -> "1.5"
                abs(scale - 2f) < 0.01f -> "2"
                else -> scale.toString()
            }
        }
    }

    suspend fun getAnimationState(): AnimationState {
        if (commandExecutor.isReady()) {
            val wRes = commandExecutor.execute("settings get global $KEY_WINDOW")
            val tRes = commandExecutor.execute("settings get global $KEY_TRANSITION")
            val aRes = commandExecutor.execute("settings get global $KEY_ANIMATOR")

            val w = wRes.stdout.trim().toFloatOrNull()
            val t = tRes.stdout.trim().toFloatOrNull()
            val a = aRes.stdout.trim().toFloatOrNull()

            if (w != null && t != null && a != null) {
                return AnimationState(
                    windowScale = w,
                    transitionScale = t,
                    animatorScale = a,
                    source = "Verified via Shizuku Shell"
                )
            }
        }

        val w = runCatching { Settings.Global.getFloat(contentResolver, KEY_WINDOW, 1f) }.getOrDefault(1f)
        val t = runCatching { Settings.Global.getFloat(contentResolver, KEY_TRANSITION, 1f) }.getOrDefault(1f)
        val a = runCatching { Settings.Global.getFloat(contentResolver, KEY_ANIMATOR, 1f) }.getOrDefault(1f)

        return AnimationState(
            windowScale = w,
            transitionScale = t,
            animatorScale = a,
            source = "Android Settings.Global"
        )
    }

    suspend fun setAllAnimationScales(scale: Float): OperationOutcome<AnimationState> {
        if (!commandExecutor.isReady()) {
            return OperationOutcome(
                success = false,
                data = getAnimationState(),
                message = "Shizuku is not connected. Authorize Shizuku to modify animation scales."
            )
        }

        val shellVal = formatScaleForShell(scale)
        val keys = listOf(KEY_WINDOW, KEY_TRANSITION, KEY_ANIMATOR)
        val errors = mutableListOf<String>()

        for (key in keys) {
            val res = commandExecutor.execute("settings put global $key $shellVal")
            if (!res.isSuccess) {
                errors.add("$key: ${res.stderr.ifBlank { "exit ${res.exitCode}" }}")
            }
        }

        val verified = getAnimationState()
        val matches = abs(verified.windowScale - scale) < 0.01f &&
            abs(verified.transitionScale - scale) < 0.01f &&
            abs(verified.animatorScale - scale) < 0.01f

        val batchCommand = keys.joinToString(" && ") { "settings put global $it $shellVal" }

        return if (errors.isEmpty() && matches) {
            OperationOutcome(
                success = true,
                data = verified,
                message = "All 3 animation scales set to ${shellVal}x and verified.",
                executedCommand = batchCommand,
                rawOutput = "window=${verified.windowScale}, transition=${verified.transitionScale}, animator=${verified.animatorScale}"
            )
        } else {
            OperationOutcome(
                success = false,
                data = verified,
                message = if (errors.isNotEmpty()) {
                    "Failed updating animation scales: ${errors.joinToString("; ")}"
                } else {
                    "Verification mismatch after applying ${shellVal}x (got ${verified.windowScale}x / ${verified.transitionScale}x / ${verified.animatorScale}x)."
                },
                executedCommand = batchCommand,
                rawOutput = errors.joinToString("; ")
            )
        }
    }
}
