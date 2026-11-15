package com.noobfree.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noobfree.android.data.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeveloperViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DeveloperUiState())
    val state: StateFlow<DeveloperUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            val dev = deviceRepository.readDeveloperOptionsState()
            val dbg = deviceRepository.readDebugState()
            val bg = deviceRepository.readBackgroundProcessLimit()
            _state.update {
                it.copy(
                    isBusy = false,
                    developerOptions = dev,
                    debugState = dbg,
                    backgroundProcessLimit = bg
                )
            }
        }
    }

    fun verifyNow() {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            val dev = deviceRepository.readDeveloperOptionsState()
            _state.update {
                it.copy(
                    isBusy = false,
                    developerOptions = dev,
                    lastCommandExecuted = "settings get global development_settings_enabled",
                    lastVerificationOutput = "development_settings_enabled = ${dev.rawGlobalValue}"
                )
            }
        }
    }

    class Factory(
        private val deviceRepository: DeviceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeveloperViewModel(deviceRepository) as T
        }
    }
}
