package com.noobfree.android.data

enum class ShizukuConnectionState {
    NOT_INSTALLED,
    SERVICE_STOPPED,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED_PERMANENTLY,
    BINDING_SERVICE,
    CONNECTED
}

data class ShizukuStatus(
    val state: ShizukuConnectionState,
    val shizukuVersion: Int = -1,
    val serviceUid: Int = -1,
    val statusTitle: String,
    val statusDescription: String,
    val errorDetail: String? = null
) {
    val isReady: Boolean
        get() = state == ShizukuConnectionState.CONNECTED

    val privilegeLabel: String
        get() = when (serviceUid) {
            0 -> "Root (UID 0)"
            2000 -> "ADB Shell (UID 2000)"
            in 1..Int.MAX_VALUE -> "UID $serviceUid"
            else -> "Unbound"
        }
}

data class OperationLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val category: String,
    val action: String,
    val command: String?,
    val success: Boolean,
    val detail: String
)
