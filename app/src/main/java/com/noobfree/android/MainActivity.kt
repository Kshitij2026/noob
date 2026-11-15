package com.noobfree.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.noobfree.android.ui.AnimationScreen
import com.noobfree.android.ui.DeveloperScreen
import com.noobfree.android.ui.DeviceInfoScreen
import com.noobfree.android.ui.HomeScreen
import com.noobfree.android.ui.SettingsScreen
import com.noobfree.android.ui.theme.NoobFreeTheme
import com.noobfree.android.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NoobFreeApplication
        val factory = MainViewModel.Factory(app.shizukuRepository, app.deviceRepository)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(systemDark) }

            NoobFreeTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val mainViewModel: MainViewModel = viewModel(factory = factory)
                    NoobFreeNavHost(
                        viewModel = mainViewModel,
                        darkTheme = darkTheme,
                        onToggleDarkTheme = { darkTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoobFreeNavHost(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAllStates(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.safeDrawingPadding()
    ) {
        composable("home") {
            HomeScreen(
                uiState = uiState,
                viewModel = viewModel,
                onNavigateToDeveloper = { navController.navigate("developer") },
                onNavigateToAnimation = { navController.navigate("animation") },
                onNavigateToDeviceInfo = { navController.navigate("device") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("developer") {
            DeveloperScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("animation") {
            AnimationScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("device") {
            DeviceInfoScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                uiState = uiState,
                viewModel = viewModel,
                darkTheme = darkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
