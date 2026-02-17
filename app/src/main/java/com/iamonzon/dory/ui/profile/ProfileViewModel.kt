package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.model.ReviewUrgency
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileStats(
    val totalItems: Int = 0,
    val masteredCount: Int = 0,
    val strugglingCount: Int = 0,
    val byCategory: Map<String, Int> = emptyMap()
)

data class ProfileUiState(
    val stats: ProfileStats = ProfileStats(),
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0
)

class ProfileViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                itemRepository.observeDashboardItems(),
                settingsRepository.observeNotificationHour(),
                settingsRepository.observeNotificationMinute()
            ) { dashboardItems, hour, minute ->
                val stats = ProfileStats(
                    totalItems = dashboardItems.size,
                    masteredCount = dashboardItems.count { it.urgency == ReviewUrgency.NotDue },
                    strugglingCount = dashboardItems.count { it.urgency == ReviewUrgency.Overdue },
                    byCategory = dashboardItems.groupBy { it.categoryName ?: "Uncategorized" }
                        .mapValues { it.value.size }
                )
                ProfileUiState(
                    stats = stats,
                    notificationHour = hour,
                    notificationMinute = minute
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setNotificationTime(hour, minute)
        }
    }

    companion object {
        fun factory(
            itemRepository: ItemRepository,
            categoryRepository: CategoryRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(itemRepository, categoryRepository, settingsRepository) as T
            }
        }
    }
}
