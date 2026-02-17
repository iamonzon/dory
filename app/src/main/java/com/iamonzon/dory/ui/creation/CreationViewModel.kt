package com.iamonzon.dory.ui.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreationStep { TITLE, SOURCE, CATEGORY, NOTES }

data class CreationUiState(
    val currentStep: CreationStep = CreationStep.TITLE,
    val title: String = "",
    val source: String = "",
    val selectedCategoryId: Long? = null,
    val selectedCategoryName: String? = null,
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

class CreationViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreationUiState())
    val uiState: StateFlow<CreationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeAll().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateSource(source: String) {
        _uiState.update { it.copy(source = source) }
    }

    fun selectCategory(id: Long, name: String) {
        _uiState.update { it.copy(selectedCategoryId = id, selectedCategoryName = name) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun nextStep() {
        _uiState.update { state ->
            val next = when (state.currentStep) {
                CreationStep.TITLE -> CreationStep.SOURCE
                CreationStep.SOURCE -> CreationStep.CATEGORY
                CreationStep.CATEGORY -> CreationStep.NOTES
                CreationStep.NOTES -> return@update state
            }
            state.copy(currentStep = next)
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val prev = when (state.currentStep) {
                CreationStep.TITLE -> return@update state
                CreationStep.SOURCE -> CreationStep.TITLE
                CreationStep.CATEGORY -> CreationStep.SOURCE
                CreationStep.NOTES -> CreationStep.CATEGORY
            }
            state.copy(currentStep = prev)
        }
    }

    fun canAdvance(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            CreationStep.TITLE -> state.title.isNotBlank()
            CreationStep.SOURCE -> state.source.isNotBlank()
            CreationStep.CATEGORY -> true
            CreationStep.NOTES -> true
        }
    }

    fun saveItem() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank() || state.source.isBlank()) return@launch
            _uiState.update { it.copy(isSaving = true) }
            try {
                val item = Item(
                    title = state.title,
                    source = state.source,
                    categoryId = state.selectedCategoryId,
                    notes = state.notes.ifBlank { null }
                )
                itemRepository.insert(item)
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        fun factory(
            itemRepository: ItemRepository,
            categoryRepository: CategoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreationViewModel(itemRepository, categoryRepository) as T
            }
        }
    }
}
