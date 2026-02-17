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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
        itemRepository.observeById(itemId)
            .flatMapLatest { loadedItem ->
                val catId = loadedItem?.categoryId
                if (catId != null) {
                    flowOf(categoryRepository.getById(catId))
                } else {
                    flowOf(null)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _reviewSubmitted = MutableStateFlow(false)
    val reviewSubmitted: StateFlow<Boolean> = _reviewSubmitted.asStateFlow()

    fun submitReview(rating: Rating, notes: String?) {
        viewModelScope.launch {
            reviewRepository.submitReview(itemId, rating, notes)
            _reviewSubmitted.value = true
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
