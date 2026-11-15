package com.noobfree.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noobfree.android.system.DeveloperManager
import com.noobfree.android.viewmodel.MainUiState
import com.noobfree.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val dev = uiState.developerOptions
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Options Manager") },
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
            item {
                ControlCard(
                    title = "Global Flag Control",
                    subtitle = "Settings.Global.DEVELOPMENT_SETTINGS_ENABLED",
                    icon = Icons.Outlined.DeveloperMode,
                    badgeText = if (dev.isEnabled) "VALUE = 1" else "VALUE = 0",
                    badgeColor = if (dev.isEnabled) Color(0xFF26A69A) else MaterialTheme.colorScheme.outline
                ) {
                    Text(
                        text = dev.verificationSource,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Write: ${DeveloperManager.CMD_ENABLE_DEV_OPTIONS}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Verify: ${DeveloperManager.CMD_GET_DEV_OPTIONS}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.enableDeveloperOptions() },
                            enabled = !uiState.isExecutingAction,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enable (1)")
                        }
                        OutlinedButton(
                            onClick = { viewModel.disableDeveloperOptions() },
                            enabled = !uiState.isExecutingAction,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Disable (0)")
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.openDeveloperOptionsSettings() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Android Developer Options Screen")
                    }
                }
            }

            item {
                ControlCard(
                    title = "OEM & Platform Compatibility",
                    subtitle = "Device-specific behavior",
                    icon = Icons.Outlined.Code
                ) {
                    Text(
                        text = dev.manufacturerNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ControlCard(
                    title = "Operation History",
                    subtitle = "${uiState.operationHistory.size} recorded operations",
                    icon = Icons.Outlined.History
                ) {
                    if (uiState.operationHistory.isEmpty()) {
                        Text(
                            text = "No shell operations executed in this session yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.operationHistory) { entry ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${entry.timestamp}] ${entry.action}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (entry.success) "OK" else "FAIL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.success) Color(0xFF26A69A) else MaterialTheme.colorScheme.error
                            )
                        }
                        if (entry.command != null) {
                            Text(
                                text = entry.command,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = entry.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
