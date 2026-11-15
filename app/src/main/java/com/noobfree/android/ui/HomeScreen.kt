package com.noobfree.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noobfree.android.data.ShizukuConnectionState
import com.noobfree.android.system.AnimationManager
import com.noobfree.android.system.BackgroundProcessLimit
import com.noobfree.android.viewmodel.MainUiState
import com.noobfree.android.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToDeveloper: () -> Unit,
    onNavigateToAnimation: () -> Unit,
    onNavigateToDeviceInfo: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val pendingBg = uiState.pendingBackgroundLimitConfirmation
    if (pendingBg != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackgroundProcessLimitDialog() },
            title = { Text("Confirm Background Process Limit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Change background process limit to: ${pendingBg.label}?")
                    Text(
                        text = pendingBg.impactDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Note: Android resets this value back to Standard upon device reboot.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmBackgroundProcessLimitChange() }) {
                    Text("Apply Limit")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBackgroundProcessLimitDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOOBFREE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Android Developer Control Center",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { viewModel.refreshAllStates(showLoading = true) }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh status")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings and About")
                    }
                }
            }
        }

        if (uiState.isLoading || uiState.isExecutingAction) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            AnimatedVisibility(visible = uiState.bannerMessage != null) {
                val msg = uiState.bannerMessage.orEmpty()
                val isErr = uiState.bannerIsError
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isErr) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isErr) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        TextButton(onClick = { viewModel.dismissBanner() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }

        item {
            val status = uiState.shizukuStatus
            val badgeColor = when (status.state) {
                ShizukuConnectionState.CONNECTED -> Color(0xFF26A69A)
                ShizukuConnectionState.PERMISSION_REQUIRED,
                ShizukuConnectionState.BINDING_SERVICE -> Color(0xFFFFB74D)
                else -> MaterialTheme.colorScheme.error
            }
            val connectLabel = when (status.state) {
                ShizukuConnectionState.NOT_INSTALLED -> "Get Shizuku"
                ShizukuConnectionState.SERVICE_STOPPED -> "Open Shizuku"
                ShizukuConnectionState.PERMISSION_REQUIRED -> "Authorize"
                ShizukuConnectionState.PERMISSION_DENIED_PERMANENTLY -> "Open Shizuku"
                ShizukuConnectionState.BINDING_SERVICE -> "Connecting..."
                ShizukuConnectionState.CONNECTED -> "Re-verify"
            }

            ControlCard(
                title = "Shizuku Bridge",
                subtitle = "Privilege: ${status.privilegeLabel}",
                icon = Icons.Outlined.Security,
                badgeText = status.statusTitle,
                badgeColor = badgeColor
            ) {
                Text(
                    text = status.statusDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (status.errorDetail != null) {
                    Text(
                        text = status.errorDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.onConnectOrAuthorizeShizuku() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(connectLabel)
                    }
                    OutlinedButton(
                        onClick = { viewModel.refreshAllStates(showLoading = true) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh")
                    }
                }
            }
        }

        item {
            val dev = uiState.developerOptions
            ControlCard(
                title = "Developer Options",
                subtitle = dev.verificationSource,
                icon = Icons.Outlined.DeveloperMode,
                badgeText = if (dev.isEnabled) "ENABLED" else "DISABLED",
                badgeColor = if (dev.isEnabled) Color(0xFF26A69A) else MaterialTheme.colorScheme.outline
            ) {
                Text(
                    text = if (dev.isEnabled) {
                        "Developer Options are active on this device. No manual 7-tap Build Number gesture required."
                    } else {
                        "Activate Android Developer Options with one tap via Shizuku shell authorization."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.enableDeveloperOptions() },
                        enabled = !uiState.isExecutingAction && !dev.isEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (dev.isEnabled) "Already Enabled" else "Enable Developer Options")
                    }
                    OutlinedButton(
                        onClick = { viewModel.openDeveloperOptionsSettings() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Settings")
                    }
                }
                TextButton(
                    onClick = onNavigateToDeveloper,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Advanced verification, disable toggle & OEM notes ->")
                }
            }
        }

        item {
            val anim = uiState.animationState
            val currentLabel = anim.uniformScale?.let { "${AnimationManager.formatScaleForShell(it)}x" }
                ?: "Custom (${anim.windowScale}x / ${anim.transitionScale}x / ${anim.animatorScale}x)"
            ControlCard(
                title = "Animation Scale",
                subtitle = "Current: $currentLabel",
                icon = Icons.Outlined.Animation,
                badgeText = currentLabel,
                badgeColor = MaterialTheme.colorScheme.primary
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimationManager.PRESET_SCALES.forEach { preset ->
                        val selected = anim.uniformScale != null && abs(anim.uniformScale - preset) < 0.01f
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setAnimationScale(preset) },
                            label = {
                                Text("${AnimationManager.formatScaleForShell(preset)}x")
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.setAnimationScale(1f) }) {
                        Text("Restore 1x Default")
                    }
                    TextButton(onClick = onNavigateToAnimation) {
                        Text("Inspect individual scales ->")
                    }
                }
            }
        }

        item {
            val dbg = uiState.debugState
            ControlCard(
                title = "USB & Wireless Debugging",
                subtitle = "Guided Debugging Access",
                icon = Icons.Outlined.Usb,
                badgeText = if (dbg.adbEnabled) "USB ON" else "USB OFF",
                badgeColor = if (dbg.adbEnabled) Color(0xFF26A69A) else MaterialTheme.colorScheme.outline
            ) {
                KeyValueRow("USB Debugging (ADB)", if (dbg.adbEnabled) "Enabled" else "Disabled")
                KeyValueRow(
                    "Wireless Debugging",
                    if (!dbg.wirelessDebuggingSupported) "Requires Android 11+"
                    else if (dbg.wirelessAdbEnabled) "Enabled" else "Disabled"
                )
                Text(
                    text = dbg.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { viewModel.openWirelessDebuggingSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Debugging Settings")
                }
            }
        }

        item {
            val rr = uiState.refreshRateInfo
            ControlCard(
                title = "Refresh Rate Monitor",
                subtitle = "Peak Panel: ${rr.peakSupportedHz.roundToInt()} Hz",
                icon = Icons.Outlined.Speed,
                badgeText = "${rr.activeRefreshRateHz.roundToInt()} Hz ACTIVE",
                badgeColor = Color(0xFF26A69A)
            ) {
                Text(
                    text = rr.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { viewModel.openDisplaySettings() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Open Display & Refresh Rate Settings")
                }
            }
        }

        item {
            val currentLimit = uiState.backgroundProcessLimit
            ControlCard(
                title = "Background Processes",
                subtitle = currentLimit.label,
                icon = Icons.Outlined.Memory,
                badgeText = if (currentLimit == BackgroundProcessLimit.STANDARD) "DEFAULT" else "CUSTOM",
                badgeColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = currentLimit.impactDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundProcessLimit.entries.forEach { option ->
                        FilterChip(
                            selected = option == currentLimit,
                            onClick = { viewModel.requestBackgroundProcessLimitChange(option) },
                            label = {
                                Text(
                                    when (option) {
                                        BackgroundProcessLimit.STANDARD -> "Standard"
                                        BackgroundProcessLimit.NO_BACKGROUND -> "None"
                                        else -> "${option.value}"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            val hw = uiState.deviceHardwareInfo
            ControlCard(
                title = "Device & System Info",
                subtitle = hw?.marketName ?: "Reading hardware...",
                icon = Icons.Outlined.PhoneAndroid
            ) {
                if (hw != null) {
                    KeyValueRow("OS / Skin", "${hw.osSkinName} (Android ${hw.androidVersion})")
                    KeyValueRow("Model / SDK", "${hw.model} • API ${hw.sdkInt}")
                    KeyValueRow("Memory (RAM)", "${hw.availableRamFormatted} free / ${hw.totalRamFormatted}")
                    KeyValueRow("Display", "${hw.screenResolution} (${hw.supportedRefreshRatesSummary})")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToDeviceInfo,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Full Specs & GPU Shortcuts")
                    }
                    OutlinedButton(
                        onClick = { viewModel.openDeviceInfoSettings() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "System About Phone", modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
