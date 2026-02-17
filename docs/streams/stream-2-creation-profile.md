# Stream 2: Creation + Profile ViewModels

## Branch
`stream/creation-profile`

## Goal
Create ViewModels for the Item Creation and Profile screens. CreationViewModel handles the multi-step creation form with validation. ProfileViewModel computes mastery statistics from repository data.

## Critical Constraint
**DO NOT modify any existing files.** Only create new files listed below.

## Files to Create

```
app/src/main/java/com/iamonzon/dory/ui/creation/CreationViewModel.kt
app/src/main/java/com/iamonzon/dory/ui/profile/ProfileViewModel.kt
app/src/test/java/com/iamonzon/dory/ui/creation/CreationViewModelTest.kt
app/src/test/java/com/iamonzon/dory/ui/profile/ProfileViewModelTest.kt
```

## ViewModel Contracts

### CreationViewModel

```kotlin
package com.iamonzon.dory.ui.creation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.StateFlow

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

    val uiState: StateFlow<CreationUiState>

    fun updateTitle(title: String)
    fun updateSource(source: String)
    fun selectCategory(id: Long, name: String)
    fun updateNotes(notes: String)

    /**
     * Advance to the next step. No-op if on NOTES.
     */
    fun nextStep()

    /**
     * Go back to the previous step. No-op if on TITLE.
     */
    fun previousStep()

    /**
     * Returns true if the current step's required fields are filled.
     * TITLE: title.isNotBlank()
     * SOURCE: always true (source is optional per spec? — check: source is required based on the Item model field)
     * CATEGORY: always true (category is optional)
     * NOTES: always true
     */
    fun canAdvance(): Boolean

    /**
     * Save the item. Sets isSaving=true, calls itemRepository.insert(),
     * then sets savedSuccessfully=true.
     */
    fun saveItem()

    companion object {
        fun factory(
            itemRepository: ItemRepository,
            categoryRepository: CategoryRepository
        ): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Load categories via `categoryRepository.observeAll()` into the uiState
- `saveItem()` should create an `Item(title, source, categoryId, notes)` and call `itemRepository.insert()`
- After save, set `savedSuccessfully = true` — integration stream uses this to navigate back
- Validate: title must be non-blank before allowing save. Source is required per the `Item` model (it's a non-nullable `String`), so the current UI behavior of requiring non-blank source on step 2 is correct
- The `CreationStep` enum in this ViewModel replaces the private one in `ItemCreationScreen.kt` — the integration stream will update the screen to use this one

### ProfileViewModel

```kotlin
package com.iamonzon.dory.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.repository.DashboardItem
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import com.iamonzon.dory.data.model.ReviewUrgency
import kotlinx.coroutines.flow.StateFlow

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

    val uiState: StateFlow<ProfileUiState>

    /**
     * Update the daily notification time.
     */
    fun setNotificationTime(hour: Int, minute: Int)

    companion object {
        fun factory(
            itemRepository: ItemRepository,
            categoryRepository: CategoryRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Combine `itemRepository.observeDashboardItems()` with `settingsRepository.observeNotificationHour()` and `settingsRepository.observeNotificationMinute()` to produce `ProfileUiState`
- **Mastered**: items with `urgency == NotDue` (their stability is high enough that they're not due)
- **Struggling**: items with `urgency == Overdue` (they've lapsed or are significantly overdue)
- **By category**: group dashboard items by `categoryName`, count each. Use "Uncategorized" for items with `categoryName == null`
- `setNotificationTime` calls `settingsRepository.setNotificationTime(hour, minute)` in viewModelScope

## Existing Code Reference

These existing repository methods are consumed (do NOT modify):

- `ItemRepository.insert(item: Item): Long` — in `data/repository/ItemRepository.kt`
- `ItemRepository.observeDashboardItems(): Flow<List<DashboardItem>>` — in `data/repository/ItemRepository.kt`
- `CategoryRepository.observeAll(): Flow<List<Category>>` — in `data/repository/CategoryRepository.kt`
- `SettingsRepository.observeNotificationHour(): Flow<Int>` — in `data/repository/SettingsRepository.kt`
- `SettingsRepository.observeNotificationMinute(): Flow<Int>` — in `data/repository/SettingsRepository.kt`
- `SettingsRepository.setNotificationTime(hour: Int, minute: Int)` — in `data/repository/SettingsRepository.kt`

The `DashboardItem` data class is defined in `data/repository/ItemRepository.kt`:
```kotlin
data class DashboardItem(
    val item: Item,
    val urgency: ReviewUrgency,
    val categoryName: String?
)
```

The `Item` domain model is in `data/model/Item.kt`:
```kotlin
data class Item(
    val id: Long = 0,
    val title: String,
    val source: String,
    val categoryId: Long? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false
)
```

## Testing Requirements

Write unit tests using:
- `kotlinx.coroutines.test.runTest` and `TestDispatcher`
- Fake repositories (create inline in test files)

**CreationViewModelTest:**
- Test step navigation (nextStep, previousStep, boundary conditions)
- Test field updates reflect in uiState
- Test canAdvance logic per step
- Test saveItem creates the correct Item and sets savedSuccessfully
- Test categories are loaded from repository

**ProfileViewModelTest:**
- Test stats computation: total, mastered (NotDue), struggling (Overdue)
- Test byCategory grouping
- Test notification time read/write
- Test that stats update reactively when repository data changes

## Acceptance Criteria

1. Both ViewModel files compile independently
2. All unit tests pass
3. Each ViewModel has a `companion object { fun factory(...): ViewModelProvider.Factory }`
4. No existing files were modified
5. `./gradlew test` passes with the new tests included
