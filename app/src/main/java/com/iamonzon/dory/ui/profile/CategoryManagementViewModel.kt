package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.repository.CategoryDeleteStrategy
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryWithCount(
    val category: Category,
    val itemCount: Int
)

data class CategoryManagementUiState(
    val categories: List<CategoryWithCount> = emptyList()
)

class CategoryManagementViewModel(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    val uiState: StateFlow<CategoryManagementUiState> =
        combine(
            categoryRepository.observeAll(),
            itemRepository.observeAllActive()
        ) { categories, items ->
            val countsByCategory = items.groupingBy { it.categoryId }.eachCount()
            CategoryManagementUiState(
                categories = categories.map { category ->
                    CategoryWithCount(
                        category = category,
                        itemCount = countsByCategory[category.id] ?: 0
                    )
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CategoryManagementUiState()
        )

    fun createCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insert(Category(name = name))
        }
    }

    fun renameCategory(id: Long, newName: String) {
        viewModelScope.launch {
            categoryRepository.rename(id, newName)
        }
    }

    fun deleteCategory(id: Long, strategy: CategoryDeleteStrategy) {
        viewModelScope.launch {
            categoryRepository.delete(id, strategy)
        }
    }

    companion object {
        fun factory(
            categoryRepository: CategoryRepository,
            itemRepository: ItemRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CategoryManagementViewModel(categoryRepository, itemRepository) as T
            }
        }
    }
}
