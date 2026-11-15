package com.noobfree.android

import android.content.ContentResolver
import com.noobfree.android.data.CommandExecutor
import com.noobfree.android.service.ShizukuUserService
import com.noobfree.android.system.AnimationManager
import com.noobfree.android.system.BackgroundProcessLimit
import com.noobfree.android.system.DebugManager
import com.noobfree.android.system.DeveloperManager
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperManagerTest {

    private class FakeCommandExecutor(
        var ready: Boolean = true,
        private val initialSettings: MutableMap<String, String> = mutableMapOf(
            "development_settings_enabled" to "0",
            "window_animation_scale" to "1",
            "transition_animation_scale" to "1",
            "animator_duration_scale" to "1",
            "background_process_limit" to "-1",
            "adb_enabled" to "0",
            "adb_wifi_enabled" to "0"
        ),
        var failWrites: Boolean = false
    ) : CommandExecutor {
        val executedCommands = mutableListOf<String>()

        override fun isReady(): Boolean = ready

        override suspend fun execute(command: String, timeoutMs: Long): ShellCommandResult {
            executedCommands.add(command)
            if (!ready) {
                return ShellCommandResult(-100, "", "Shizuku not connected")
            }
            val parts = command.trim().split(" ")
            if (parts.size >= 4 && parts[0] == "settings" && parts[1] == "get" && parts[2] == "global") {
                val key = parts[3]
                return ShellCommandResult(0, initialSettings[key] ?: "null", "")
            }
            if (parts.size >= 5 && parts[0] == "settings" && parts[1] == "put" && parts[2] == "global") {
                if (failWrites) {
                    return ShellCommandResult(1, "", "SecurityException: Permission denial")
                }
                val key = parts[3]
                val value = parts[4]
                initialSettings[key] = value
                return ShellCommandResult(0, "", "")
            }
            return ShellCommandResult(1, "", "Unsupported command")
        }
    }

    @Test
    fun `enableDeveloperOptions writes and verifies development_settings_enabled`() = runTest {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val executor = FakeCommandExecutor(ready = true)
        val manager = DeveloperManager(resolver, executor)

        val outcome = manager.enableDeveloperOptions()

        assertTrue(outcome.success)
        assertTrue(outcome.data.isEnabled)
        assertEquals(1, outcome.data.rawGlobalValue)
        assertTrue(executor.executedCommands.contains("settings put global development_settings_enabled 1"))
        assertTrue(executor.executedCommands.contains("settings get global development_settings_enabled"))
    }

    @Test
    fun `enableDeveloperOptions reports failure when Shizuku is disconnected`() = runTest {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val executor = FakeCommandExecutor(ready = false)
        val manager = DeveloperManager(resolver, executor)

        val outcome = manager.enableDeveloperOptions()

        assertFalse(outcome.success)
        assertFalse(outcome.data.isEnabled)
    }

    @Test
    fun `enableDeveloperOptions reports failure when OEM blocks write`() = runTest {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val executor = FakeCommandExecutor(ready = true, failWrites = true)
        val manager = DeveloperManager(resolver, executor)

        val outcome = manager.enableDeveloperOptions()

        assertFalse(outcome.success)
        assertFalse(outcome.data.isEnabled)
    }

    @Test
    fun `animationManager updates and verifies all three animation scales`() = runTest {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val executor = FakeCommandExecutor(ready = true)
        val animManager = AnimationManager(resolver, executor)

        val outcome = animManager.setAllAnimationScales(0.5f)

        assertTrue(outcome.success)
        assertEquals(0.5f, outcome.data.windowScale, 0.001f)
        assertEquals(0.5f, outcome.data.transitionScale, 0.001f)
        assertEquals(0.5f, outcome.data.animatorScale, 0.001f)
    }

    @Test
    fun `debugManager sets and verifies background process limit`() = runTest {
        val resolver = mockk<ContentResolver>(relaxed = true)
        val executor = FakeCommandExecutor(ready = true)
        val debugManager = DebugManager(resolver, executor)

        val outcome = debugManager.setBackgroundProcessLimit(BackgroundProcessLimit.AT_MOST_2)

        assertTrue(outcome.success)
        assertEquals(BackgroundProcessLimit.AT_MOST_2, outcome.data)
    }

    @Test
    fun `userService command allowlist blocks arbitrary shell commands`() {
        assertTrue(ShizukuUserService.isCommandAllowed("settings put global development_settings_enabled 1"))
        assertTrue(ShizukuUserService.isCommandAllowed("settings put global window_animation_scale 0.5"))
        assertFalse(ShizukuUserService.isCommandAllowed("rm -rf /sdcard"))
        assertFalse(ShizukuUserService.isCommandAllowed("settings put global development_settings_enabled 1; reboot"))
    }
}
