package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdvancedSettingsUiState(
    val desiredRetention: Double = 0.9,
    val fsrsWeights: List<Double> = emptyList()
)

class AdvancedSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AdvancedSettingsUiState> =
        settingsRepository.observeDesiredRetention()
            .map { retention ->
                AdvancedSettingsUiState(
                    desiredRetention = retention,
                    fsrsWeights = FsrsParameters.DEFAULT_WEIGHTS.toList()
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AdvancedSettingsUiState(
                    fsrsWeights = FsrsParameters.DEFAULT_WEIGHTS.toList()
                )
            )

    fun setDesiredRetention(value: Double) {
        viewModelScope.launch {
            settingsRepository.setDesiredRetention(value)
        }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdvancedSettingsViewModel(settingsRepository) as T
                }
            }
    }
}
