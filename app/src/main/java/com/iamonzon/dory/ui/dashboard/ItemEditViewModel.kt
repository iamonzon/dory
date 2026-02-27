package com.iamonzon.dory.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.Review
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemEditUiState(
    val title: String = "",
    val source: String = "",
    val notes: String = "",
    val savedSuccessfully: Boolean = false
)

class ItemEditViewModel(
    private val itemId: Long,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    val item: StateFlow<Item?> =
        itemRepository.observeById(itemId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _editState = MutableStateFlow(ItemEditUiState())
    val editState: StateFlow<ItemEditUiState> = _editState.asStateFlow()

    private val _recentReviews = MutableStateFlow<List<Review>>(emptyList())
    val recentReviews: StateFlow<List<Review>> = _recentReviews.asStateFlow()

    init {
        viewModelScope.launch {
            val loadedItem = itemRepository.getById(itemId)
            if (loadedItem != null) {
                _editState.update {
                    it.copy(
                        title = loadedItem.title,
                        source = loadedItem.source,
                        notes = loadedItem.notes ?: ""
                    )
                }
            }
            _recentReviews.value = reviewRepository.getLatestReviewsForItem(itemId, 3)
        }
    }

    fun updateTitle(title: String) {
        _editState.update { it.copy(title = title) }
    }

    fun updateSource(source: String) {
        _editState.update { it.copy(source = source) }
    }

    fun updateNotes(notes: String) {
        _editState.update { it.copy(notes = notes) }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val currentItem = item.value ?: return@launch
            val state = _editState.value
            itemRepository.update(
                currentItem.copy(
                    title = state.title,
                    source = state.source,
                    notes = state.notes.ifBlank { null }
                )
            )
            _editState.update { it.copy(savedSuccessfully = true) }
        }
    }

    companion object {
        fun factory(
            itemId: Long,
            itemRepository: ItemRepository,
            reviewRepository: ReviewRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ItemEditViewModel(itemId, itemRepository, reviewRepository) as T
            }
        }
    }
}
