package com.noobfree.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noobfree.android.BuildConfig
import com.noobfree.android.viewmodel.MainUiState
import com.noobfree.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & About") },
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
                    title = "Appearance",
                    subtitle = "Material 3 Theme",
                    icon = Icons.Outlined.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "High-contrast dark developer interface",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = onToggleDarkTheme
                        )
                    }
                }
            }

            item {
                ControlCard(
                    title = "Why Shizuku Is Required",
                    subtitle = "Legitimate Android Privilege Model",
                    icon = Icons.Outlined.Security
                ) {
                    Text(
                        text = "Standard Android apps cannot modify Settings.Global keys such as development_settings_enabled or animation scales " +
                            "because Android protects them with WRITE_SECURE_SETTINGS. Shizuku bridges ADB shell permissions (UID 2000) " +
                            "authorized explicitly by you, avoiding root exploits or security bypasses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openShizukuApp() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Shizuku Manager")
                        }
                        OutlinedButton(
                            onClick = { viewModel.openAppDetailsSettings() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("App Info")
                        }
                    }
                }
            }

            item {
                ControlCard(
                    title = "Security & Privacy Guarantee",
                    subtitle = "Zero telemetry • Offline-first",
                    icon = Icons.Outlined.PrivacyTip
                ) {
                    Text(
                        text = "• No INTERNET permission requested in AndroidManifest.xml.
" +
                            "• No analytics, trackers, or background daemons.
" +
                            "• Shizuku UserService enforces a strict command allow-list and terminates when unbound.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ControlCard(
                    title = "About NoobFree",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    icon = Icons.Outlined.Info
                ) {
                    KeyValueRow("Application", "NoobFree")
                    KeyValueRow("Package", BuildConfig.APPLICATION_ID)
                    KeyValueRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    KeyValueRow("Target Platform", "Android 8.0+ (API 26–35)")
                    uiState.deviceHardwareInfo?.manufacturerCompatibilityNotes?.forEach { note ->
                        Text(
                            text = "• $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
