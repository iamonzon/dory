# Stream 3: Settings + Support Screen ViewModels

## Branch
`stream/settings-support`

## Goal
Create ViewModels for the Category Management, Archived Items, and Advanced Settings screens. These handle CRUD operations for categories, item restoration, and FSRS parameter configuration.

## Critical Constraint
**DO NOT modify any existing files.** Only create new files listed below.

## Files to Create

```
app/src/main/java/com/iamonzon/dory/ui/profile/CategoryManagementViewModel.kt
app/src/main/java/com/iamonzon/dory/ui/profile/ArchivedItemsViewModel.kt
app/src/main/java/com/iamonzon/dory/ui/profile/AdvancedSettingsViewModel.kt
app/src/test/java/com/iamonzon/dory/ui/profile/CategoryManagementViewModelTest.kt
app/src/test/java/com/iamonzon/dory/ui/profile/ArchivedItemsViewModelTest.kt
app/src/test/java/com/iamonzon/dory/ui/profile/AdvancedSettingsViewModelTest.kt
```

## ViewModel Contracts

### CategoryManagementViewModel

```kotlin
package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.repository.CategoryDeleteStrategy
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.model.Item
import kotlinx.coroutines.flow.StateFlow

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

    val uiState: StateFlow<CategoryManagementUiState>

    /**
     * Create a new category with the given name.
     */
    fun createCategory(name: String)

    /**
     * Rename a category.
     */
    fun renameCategory(id: Long, newName: String)

    /**
     * Delete a category with the specified strategy for its items.
     * Strategy is one of: UncategorizeItems, ArchiveItems, DeleteItems.
     */
    fun deleteCategory(id: Long, strategy: CategoryDeleteStrategy)

    companion object {
        fun factory(
            categoryRepository: CategoryRepository,
            itemRepository: ItemRepository
        ): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Combine `categoryRepository.observeAll()` with `itemRepository.observeAllActive()` to compute item counts per category
- `createCategory` should create a `Category(name = name)` and call `categoryRepository.insert()`
- `renameCategory` calls `categoryRepository.rename(id, newName)`
- `deleteCategory` calls `categoryRepository.delete(id, strategy)` — the repository already handles the three strategies (uncategorize, archive, delete items)

### ArchivedItemsViewModel

```kotlin
package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.StateFlow

class ArchivedItemsViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {

    /**
     * All archived items, observed reactively.
     */
    val archivedItems: StateFlow<List<Item>>

    /**
     * Restore (unarchive) an item by ID.
     */
    fun restoreItem(id: Long)

    companion object {
        fun factory(itemRepository: ItemRepository): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Use `itemRepository.observeAllArchived()` collected into StateFlow
- `restoreItem` calls `itemRepository.unarchive(id)` in viewModelScope

### AdvancedSettingsViewModel

```kotlin
package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

data class AdvancedSettingsUiState(
    val desiredRetention: Double = 0.9,
    val fsrsWeights: List<Double> = emptyList()
)

class AdvancedSettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AdvancedSettingsUiState>

    /**
     * Update the desired retention value (0.70 to 0.97).
     */
    fun setDesiredRetention(value: Double)

    companion object {
        fun factory(settingsRepository: SettingsRepository): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Load desired retention from `settingsRepository.observeDesiredRetention()`
- FSRS weights are read-only display from `FsrsParameters.DEFAULT_WEIGHTS` (they are not user-editable in this version — the spec says they'll be personalized automatically in the future)
- `setDesiredRetention` calls `settingsRepository.setDesiredRetention(value)` in viewModelScope

## Existing Code Reference

These existing repository methods are consumed (do NOT modify):

- `CategoryRepository.observeAll(): Flow<List<Category>>` — in `data/repository/CategoryRepository.kt`
- `CategoryRepository.insert(category: Category): Long`
- `CategoryRepository.rename(id: Long, newName: String)`
- `CategoryRepository.delete(categoryId: Long, strategy: CategoryDeleteStrategy)`
- `CategoryDeleteStrategy` enum: `UncategorizeItems`, `ArchiveItems`, `DeleteItems` — in `data/repository/CategoryRepository.kt`
- `ItemRepository.observeAllActive(): Flow<List<Item>>` — in `data/repository/ItemRepository.kt`
- `ItemRepository.observeAllArchived(): Flow<List<Item>>` — in `data/repository/ItemRepository.kt`
- `ItemRepository.unarchive(id: Long)` — in `data/repository/ItemRepository.kt`
- `SettingsRepository.observeDesiredRetention(): Flow<Double>` — in `data/repository/SettingsRepository.kt`
- `SettingsRepository.setDesiredRetention(value: Double)` — in `data/repository/SettingsRepository.kt`
- `FsrsParameters.DEFAULT_WEIGHTS: List<Double>` — in `algorithm/FsrsParameters.kt`

The `Category` domain model is in `data/model/Category.kt`:
```kotlin
data class Category(
    val id: Long = 0,
    val name: String,
    val fsrsParameters: FsrsParameters? = null,
    val desiredRetention: Double? = null
)
```

## Testing Requirements

Write unit tests using `kotlinx.coroutines.test.runTest` and `TestDispatcher` with fake repositories.

**CategoryManagementViewModelTest:**
- Test categories with counts are loaded correctly
- Test createCategory calls repository
- Test renameCategory calls repository
- Test deleteCategory with each strategy calls repository correctly

**ArchivedItemsViewModelTest:**
- Test archived items are loaded from repository
- Test restoreItem calls unarchive on repository

**AdvancedSettingsViewModelTest:**
- Test desired retention is loaded from settings
- Test setDesiredRetention persists to settings
- Test FSRS weights are displayed from defaults

## Acceptance Criteria

1. All three ViewModel files compile independently
2. All unit tests pass
3. Each ViewModel has a `companion object { fun factory(...): ViewModelProvider.Factory }`
4. No existing files were modified
5. `./gradlew test` passes with the new tests included
