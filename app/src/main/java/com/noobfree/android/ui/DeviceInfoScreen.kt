package com.noobfree.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noobfree.android.viewmodel.MainUiState
import com.noobfree.android.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val hw = uiState.deviceHardwareInfo
    val rr = uiState.refreshRateInfo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device, Display & GPU") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (hw != null) {
                item {
                    ControlCard(
                        title = hw.marketName,
                        subtitle = "${hw.osSkinName} • Android ${hw.androidVersion}",
                        icon = Icons.Outlined.PhoneAndroid
                    ) {
                        KeyValueRow("Manufacturer", hw.manufacturer)
                        KeyValueRow("Model / Codename", "${hw.model} (${hw.deviceCodename})")
                        KeyValueRow("Android / SDK", "Android ${hw.androidVersion} (API ${hw.sdkInt})")
                        KeyValueRow("Security Patch", hw.securityPatch)
                        KeyValueRow("Build ID", hw.buildId)
                    }
                }

                item {
                    ControlCard(
                        title = "SoC, Memory & Display",
                        subtitle = hw.cpuHardware,
                        icon = Icons.Outlined.DeveloperBoard
                    ) {
                        KeyValueRow("SoC / CPU", hw.cpuHardware)
                        KeyValueRow("CPU Cores", "${hw.cpuCoreCount} cores")
                        KeyValueRow("ABIs", hw.supportedAbis)
                        KeyValueRow("Total RAM", hw.totalRamFormatted)
                        KeyValueRow("Available RAM", hw.availableRamFormatted)
                        KeyValueRow("Resolution", hw.screenResolution)
                        KeyValueRow("Density", "${hw.screenDensityDpi} dpi")
                    }
                }
            }

            item {
                ControlCard(
                    title = "Display Refresh Modes",
                    subtitle = "Active: ${rr.activeRefreshRateHz.roundToInt()} Hz",
                    icon = Icons.Outlined.Speed
                ) {
                    rr.supportedModes.forEach { mode ->
                        KeyValueRow(
                            label = "Mode #${mode.modeId} (${mode.width}×${mode.height})",
                            value = "${mode.refreshRateHz.roundToInt()} Hz${if (mode.isActive) " [Active]" else ""}"
                        )
                    }
                    Text(
                        text = rr.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ControlCard(
                    title = "GPU Rendering & Profiling",
                    subtitle = "Hardware-accelerated UI diagnostics",
                    icon = Icons.Outlined.GraphicEq
                ) {
                    Text(
                        text = "Android exposes Profile HWUI Rendering, Show GPU View Updates, and Force 4x MSAA inside system Developer Options. " +
                            "NoobFree provides direct navigation rather than modifying restricted SurfaceFlinger debug flags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { viewModel.openDeveloperOptionsSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Developer Options (Hardware Accelerated Rendering)")
                    }
                }
            }
        }
    }
}
