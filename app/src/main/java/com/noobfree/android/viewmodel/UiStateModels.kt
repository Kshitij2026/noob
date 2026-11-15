package com.noobfree.android.viewmodel

import com.noobfree.android.data.OperationLogEntry
import com.noobfree.android.data.ShizukuConnectionState
import com.noobfree.android.data.ShizukuStatus
import com.noobfree.android.system.AnimationState
import com.noobfree.android.system.BackgroundProcessLimit
import com.noobfree.android.system.DebugState
import com.noobfree.android.system.DeveloperOptionsState
import com.noobfree.android.system.DeviceHardwareInfo
import com.noobfree.android.system.RefreshRateInfo

data class MainUiState(
    val isLoading: Boolean = true,
    val isExecutingAction: Boolean = false,
    val shizukuStatus: ShizukuStatus = ShizukuStatus(
        state = ShizukuConnectionState.SERVICE_STOPPED,
        statusTitle = "Initializing...",
        statusDescription = "Checking Shizuku service status."
    ),
    val developerOptions: DeveloperOptionsState = DeveloperOptionsState(
        isEnabled = false,
        rawGlobalValue = 0,
        verificationSource = "Pending",
        manufacturerNote = ""
    ),
    val animationState: AnimationState = AnimationState(1f, 1f, 1f, "Pending"),
    val debugState: DebugState = DebugState(
        adbEnabled = false,
        wirelessAdbEnabled = false,
        wirelessDebuggingSupported = true,
        note = ""
    ),
    val backgroundProcessLimit: BackgroundProcessLimit = BackgroundProcessLimit.STANDARD,
    val refreshRateInfo: RefreshRateInfo = RefreshRateInfo(60f, 60f, 60f, emptyList(), ""),
    val deviceHardwareInfo: DeviceHardwareInfo? = null,
    val pendingBackgroundLimitConfirmation: BackgroundProcessLimit? = null,
    val bannerMessage: String? = null,
    val bannerIsError: Boolean = false,
    val operationHistory: List<OperationLogEntry> = emptyList()
)

data class DeveloperUiState(
    val isBusy: Boolean = false,
    val developerOptions: DeveloperOptionsState = DeveloperOptionsState(
        isEnabled = false,
        rawGlobalValue = 0,
        verificationSource = "Pending",
        manufacturerNote = ""
    ),
    val debugState: DebugState = DebugState(
        adbEnabled = false,
        wirelessAdbEnabled = false,
        wirelessDebuggingSupported = true,
        note = ""
    ),
    val backgroundProcessLimit: BackgroundProcessLimit = BackgroundProcessLimit.STANDARD,
    val lastCommandExecuted: String? = null,
    val lastVerificationOutput: String? = null
)
