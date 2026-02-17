package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedItemsViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {

    val archivedItems: StateFlow<List<Item>> =
        itemRepository.observeAllArchived()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    fun restoreItem(id: Long) {
        viewModelScope.launch {
            itemRepository.unarchive(id)
        }
    }

    companion object {
        fun factory(itemRepository: ItemRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ArchivedItemsViewModel(itemRepository) as T
                }
            }
    }
}
