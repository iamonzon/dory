package com.iamonzon.dory.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.Review
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val itemId: Long,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val item: StateFlow<Item?> =
        itemRepository.observeById(itemId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reviews: StateFlow<List<Review>> =
        reviewRepository.observeReviewsForItem(itemId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val category: StateFlow<Category?> =
        item
            .flatMapLatest { loadedItem ->
                val catId = loadedItem?.categoryId
                if (catId != null) {
                    flowOf(categoryRepository.getById(catId))
                } else {
                    flowOf(null)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _reviewSubmitted = Channel<Unit>(Channel.BUFFERED)
    val reviewSubmitted: Flow<Unit> = _reviewSubmitted.receiveAsFlow()

    fun submitReview(rating: Rating, notes: String?) {
        viewModelScope.launch {
            try {
                reviewRepository.submitReview(itemId, rating, notes)
                _reviewSubmitted.send(Unit)
            } catch (_: Exception) {
                // TODO: expose error state to UI
            }
        }
    }

    companion object {
        fun factory(
            itemId: Long,
            itemRepository: ItemRepository,
            reviewRepository: ReviewRepository,
            categoryRepository: CategoryRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReviewViewModel(itemId, itemRepository, reviewRepository, categoryRepository) as T
                }
            }
    }
}
