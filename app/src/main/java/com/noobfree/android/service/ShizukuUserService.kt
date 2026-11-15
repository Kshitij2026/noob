package com.noobfree.android.service

import android.os.Process
import androidx.annotation.Keep
import com.noobfree.android.IShizukuShellService
import com.noobfree.android.ShellCommandResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@Keep
class ShizukuUserService : IShizukuShellService.Stub() {

    override fun executeCommand(command: String, timeoutMs: Long): ShellCommandResult {
        if (!isCommandAllowed(command)) {
            return ShellCommandResult(
                exitCode = -403,
                stdout = "",
                stderr = "Security policy blocked unapproved command: $command"
            )
        }

        var process: java.lang.Process? = null
        return try {
            val builder = ProcessBuilder("sh", "-c", command)
            process = builder.start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val outThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdoutBuilder.append(line).append('\n')
                    }
                }
            }
            val errThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderrBuilder.append(line).append('\n')
                    }
                }
            }

            outThread.start()
            errThread.start()

            val effectiveTimeout = timeoutMs.coerceIn(500L, 15_000L)
            val finished = process.waitFor(effectiveTimeout, TimeUnit.MILLISECONDS)

            if (!finished) {
                process.destroyForcibly()
                outThread.join(250)
                errThread.join(250)
                ShellCommandResult(
                    exitCode = -1,
                    stdout = stdoutBuilder.toString().trim(),
                    stderr = "Command timed out after ${effectiveTimeout}ms",
                    timedOut = true
                )
            } else {
                outThread.join(500)
                errThread.join(500)
                ShellCommandResult(
                    exitCode = process.exitValue(),
                    stdout = stdoutBuilder.toString().trim(),
                    stderr = stderrBuilder.toString().trim(),
                    timedOut = false
                )
            }
        } catch (e: Exception) {
            process?.destroyForcibly()
            ShellCommandResult(
                exitCode = -2,
                stdout = "",
                stderr = e.message ?: "Unknown UserService execution failure"
            )
        }
    }

    override fun getUid(): Int = Process.myUid()

    override fun destroy() {
        exitProcess(0)
    }

    companion object {
        private val ALLOWED_PATTERNS = listOf(
            Regex("^settings get global development_settings_enabled$"),
            Regex("^settings put global development_settings_enabled [01]$"),
            Regex("^settings get global adb_enabled$"),
            Regex("^settings get global adb_wifi_enabled$"),
            Regex("^settings get global (window_animation_scale|transition_animation_scale|animator_duration_scale)$"),
            Regex("^settings put global (window_animation_scale|transition_animation_scale|animator_duration_scale) (0|0\.5|1|1\.0|1\.5|2|2\.0)$"),
            Regex("^settings get global background_process_limit$"),
            Regex("^settings put global background_process_limit (-1|0|1|2|3|4)$")
        )

        fun isCommandAllowed(command: String): Boolean {
            val trimmed = command.trim()
            return ALLOWED_PATTERNS.any { it.matches(trimmed) }
        }
    }
}
