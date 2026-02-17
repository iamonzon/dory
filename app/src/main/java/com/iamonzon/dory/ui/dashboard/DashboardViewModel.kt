package com.iamonzon.dory.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.repository.DashboardItem
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DashboardEvent {
    data class ItemArchived(val id: Long) : DashboardEvent
    data class ItemDeleted(val id: Long) : DashboardEvent
}

class DashboardViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val dashboardItems: StateFlow<List<DashboardItem>> =
        itemRepository.observeDashboardItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun archiveItem(id: Long) {
        viewModelScope.launch {
            try {
                itemRepository.archive(id)
                _events.send(DashboardEvent.ItemArchived(id))
            } catch (_: Exception) {
                // TODO: expose error state to UI
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                itemRepository.deleteById(id)
                _events.send(DashboardEvent.ItemDeleted(id))
            } catch (_: Exception) {
                // TODO: expose error state to UI
            }
        }
    }

    companion object {
        fun factory(itemRepository: ItemRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(itemRepository) as T
                }
            }
    }
}
