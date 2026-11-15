package com.noobfree.android.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import com.noobfree.android.BuildConfig
import com.noobfree.android.IShizukuShellService
import com.noobfree.android.ShellCommandResult
import com.noobfree.android.service.ShizukuUserService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

interface CommandExecutor {
    suspend fun execute(command: String, timeoutMs: Long = 4000L): ShellCommandResult
    fun isReady(): Boolean
}

class ShizukuRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CommandExecutor {

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_SHIZUKU = 1001
    }

    private val _status = MutableStateFlow(
        ShizukuStatus(
            state = ShizukuConnectionState.SERVICE_STOPPED,
            statusTitle = "Checking Shizuku...",
            statusDescription = "Inspecting Shizuku service and binder availability."
        )
    )
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val bindMutex = Mutex()
    @Volatile
    private var userService: IShizukuShellService? = null
    @Volatile
    private var isBinding = false

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShizukuUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBinding = false
            if (service != null && service.pingBinder()) {
                val stub = IShizukuShellService.Stub.asInterface(service)
                userService = stub
                val uid = runCatching { stub.uid }.getOrDefault(-1)
                val ver = runCatching { Shizuku.getVersion() }.getOrDefault(-1)
                _status.value = ShizukuStatus(
                    state = ShizukuConnectionState.CONNECTED,
                    shizukuVersion = ver,
                    serviceUid = uid,
                    statusTitle = "Connected & Authorized",
                    statusDescription = "Shizuku UserService active (${if (uid == 0) "Root UID 0" else "ADB Shell UID $uid"})."
                )
            } else {
                userService = null
                refreshStatus()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBinding = false
            userService = null
            refreshStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isBinding = false
        userService = null
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    refreshStatus()
                } else {
                    val deniedForever = runCatching { Shizuku.shouldShowRequestPermissionRationale() }
                        .getOrDefault(false)
                    _status.value = ShizukuStatus(
                        state = if (deniedForever) {
                            ShizukuConnectionState.PERMISSION_DENIED_PERMANENTLY
                        } else {
                            ShizukuConnectionState.PERMISSION_REQUIRED
                        },
                        statusTitle = "Permission Denied",
                        statusDescription = "Shizuku authorization was declined. Open Shizuku Manager to allow NoobFree.",
                        errorDetail = "Grant permission inside Shizuku -> Authorized applications."
                    )
                }
            }
        }

    fun registerListeners() {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        refreshStatus()
    }

    fun unregisterListeners() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            if (userService != null) {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
                userService = null
            }
        }
    }

    fun refreshStatus() {
        if (!isShizukuInstalled()) {
            userService = null
            _status.value = ShizukuStatus(
                state = ShizukuConnectionState.NOT_INSTALLED,
                statusTitle = "Shizuku Not Installed",
                statusDescription = "Install the official Shizuku app to enable privileged developer controls without root."
            )
            return
        }

        val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binderAlive) {
            userService = null
            _status.value = ShizukuStatus(
                state = ShizukuConnectionState.SERVICE_STOPPED,
                statusTitle = "Shizuku Service Stopped",
                statusDescription = "Shizuku is installed but not running. Start it via Wireless Debugging or ADB."
            )
            return
        }

        if (Shizuku.isPreV11()) {
            _status.value = ShizukuStatus(
                state = ShizukuConnectionState.SERVICE_STOPPED,
                statusTitle = "Outdated Shizuku Version",
                statusDescription = "Please update Shizuku to v11 or newer."
            )
            return
        }

        val permissionGranted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        if (!permissionGranted) {
            userService = null
            val shouldShowRationale = runCatching {
                Shizuku.shouldShowRequestPermissionRationale()
            }.getOrDefault(false)
            _status.value = ShizukuStatus(
                state = if (shouldShowRationale) {
                    ShizukuConnectionState.PERMISSION_DENIED_PERMANENTLY
                } else {
                    ShizukuConnectionState.PERMISSION_REQUIRED
                },
                shizukuVersion = runCatching { Shizuku.getVersion() }.getOrDefault(-1),
                statusTitle = "Authorization Required",
                statusDescription = "Shizuku is running. Grant NoobFree permission to execute shell settings commands."
            )
            return
        }

        val currentService = userService
        if (currentService != null && currentService.asBinder().pingBinder()) {
            val uid = runCatching { currentService.uid }.getOrDefault(-1)
            val ver = runCatching { Shizuku.getVersion() }.getOrDefault(-1)
            _status.value = ShizukuStatus(
                state = ShizukuConnectionState.CONNECTED,
                shizukuVersion = ver,
                serviceUid = uid,
                statusTitle = "Connected & Authorized",
                statusDescription = "Shizuku UserService active (${if (uid == 0) "Root UID 0" else "ADB Shell UID $uid"})."
            )
        } else {
            bindUserService()
        }
    }

    fun requestPermissionOrConnect() {
        refreshStatus()
        when (_status.value.state) {
            ShizukuConnectionState.NOT_INSTALLED -> openShizukuDownloadPage()
            ShizukuConnectionState.SERVICE_STOPPED -> openShizukuApp()
            ShizukuConnectionState.PERMISSION_REQUIRED -> {
                runCatching {
                    Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                }.onFailure { err ->
                    _status.value = _status.value.copy(
                        errorDetail = "Failed to launch permission dialog: ${err.message}"
                    )
                }
            }
            ShizukuConnectionState.PERMISSION_DENIED_PERMANENTLY -> openShizukuApp()
            ShizukuConnectionState.BINDING_SERVICE,
            ShizukuConnectionState.CONNECTED -> bindUserService()
        }
    }

    private fun bindUserService() {
        if (isBinding) return
        isBinding = true
        _status.value = ShizukuStatus(
            state = ShizukuConnectionState.BINDING_SERVICE,
            shizukuVersion = runCatching { Shizuku.getVersion() }.getOrDefault(-1),
            statusTitle = "Binding UserService...",
            statusDescription = "Establishing isolated Shizuku shell binder channel."
        )
        runCatching {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        }.onFailure { err ->
            isBinding = false
            _status.value = ShizukuStatus(
                state = ShizukuConnectionState.SERVICE_STOPPED,
                statusTitle = "UserService Bind Failed",
                statusDescription = "Could not bind Shizuku UserService.",
                errorDetail = err.message
            )
        }
    }

    override fun isReady(): Boolean {
        val service = userService ?: return false
        return runCatching { service.asBinder().pingBinder() }.getOrDefault(false)
    }

    override suspend fun execute(command: String, timeoutMs: Long): ShellCommandResult =
        withContext(ioDispatcher) {
            bindMutex.withLock {
                val service = userService
                if (service == null || !runCatching { service.asBinder().pingBinder() }.getOrDefault(false)) {
                    refreshStatus()
                    return@withContext ShellCommandResult(
                        exitCode = -100,
                        stdout = "",
                        stderr = "Shizuku UserService is not connected. Verify Shizuku is running and authorized."
                    )
                }
                try {
                    service.executeCommand(command, timeoutMs)
                } catch (e: Exception) {
                    userService = null
                    refreshStatus()
                    ShellCommandResult(
                        exitCode = -101,
                        stdout = "",
                        stderr = "Binder IPC error: ${e.message ?: "Service disconnected"}"
                    )
                }
            }
        }

    fun isShizukuInstalled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    SHIZUKU_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openShizukuApp(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            openShizukuDownloadPage()
            false
        }
    }

    fun openShizukuDownloadPage() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://shizuku.rikka.app/download/")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
