# Stream 1: Dashboard + Review ViewModels

## Branch
`stream/dashboard-review`

## Goal
Create ViewModels for the Dashboard, Review, and Review Session screens. These ViewModels expose reactive state from the repository layer and handle user actions (archive, delete, submit review).

## Critical Constraint
**DO NOT modify any existing files.** Only create new files listed below.

## Files to Create

```
app/src/main/java/com/iamonzon/dory/ui/dashboard/DashboardViewModel.kt
app/src/main/java/com/iamonzon/dory/ui/review/ReviewViewModel.kt
app/src/main/java/com/iamonzon/dory/ui/review/ReviewSessionViewModel.kt
app/src/test/java/com/iamonzon/dory/ui/dashboard/DashboardViewModelTest.kt
app/src/test/java/com/iamonzon/dory/ui/review/ReviewViewModelTest.kt
app/src/test/java/com/iamonzon/dory/ui/review/ReviewSessionViewModelTest.kt
```

## ViewModel Contracts

### DashboardViewModel

```kotlin
package com.iamonzon.dory.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iamonzon.dory.data.repository.DashboardItem
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {

    /**
     * All active items sorted by urgency (Overdue > DueToday > NotDue).
     * Emitted from ItemRepository.observeDashboardItems().
     */
    val dashboardItems: StateFlow<List<DashboardItem>>
        // = itemRepository.observeDashboardItems()
        //     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Archive an item by ID. Shows toast via one-shot event.
     */
    fun archiveItem(id: Long)
        // = viewModelScope.launch { itemRepository.archive(id) }

    /**
     * Delete an item by ID. Shows toast via one-shot event.
     */
    fun deleteItem(id: Long)
        // = viewModelScope.launch { itemRepository.deleteById(id) }

    companion object {
        fun factory(itemRepository: ItemRepository): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- `dashboardItems` should use `stateIn` with `WhileSubscribed(5000)` and initial value `emptyList()`
- Archive and delete should emit a one-shot event (use `SharedFlow` or `Channel`) for toast/snackbar feedback so the integration stream can observe it
- Expose a `ViewModelProvider.Factory` companion so the integration stream can instantiate it from `AppContainer`

### ReviewViewModel

```kotlin
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
import kotlinx.coroutines.flow.StateFlow

class ReviewViewModel(
    private val itemId: Long,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    /**
     * The item being reviewed. null while loading or if not found.
     */
    val item: StateFlow<Item?>

    /**
     * All reviews for this item, ordered by date descending.
     */
    val reviews: StateFlow<List<Review>>

    /**
     * The item's category, if any.
     */
    val category: StateFlow<Category?>

    /**
     * Submit a review with the given rating and optional notes.
     * Calls ReviewRepository.submitReview() which computes S/D via FSRS.
     * Emits a one-shot "review submitted" event for navigation/toast.
     */
    fun submitReview(rating: Rating, notes: String?)

    /**
     * One-shot event: true when review was submitted successfully.
     * Integration stream uses this to trigger navigation back.
     */
    val reviewSubmitted: StateFlow<Boolean>

    companion object {
        fun factory(
            itemId: Long,
            itemRepository: ItemRepository,
            reviewRepository: ReviewRepository,
            categoryRepository: CategoryRepository
        ): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Load item via `itemRepository.observeById(itemId)`, collect into StateFlow
- Load reviews via `reviewRepository.observeReviewsForItem(itemId)`
- Load category by observing the item's `categoryId` and calling `categoryRepository.getById()`
- `submitReview` should call `reviewRepository.submitReview(itemId, rating, notes)` in viewModelScope
- After successful submission, set `reviewSubmitted` to true

### ReviewSessionViewModel

```kotlin
package com.iamonzon.dory.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iamonzon.dory.data.repository.DashboardItem
import com.iamonzon.dory.data.repository.ItemRepository
import kotlinx.coroutines.flow.StateFlow

class ReviewSessionViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {

    /**
     * Items that are due or overdue, from ItemRepository.observeDueItems().
     */
    val dueItems: StateFlow<List<DashboardItem>>

    companion object {
        fun factory(itemRepository: ItemRepository): ViewModelProvider.Factory
    }
}
```

**Implementation notes:**
- Use `itemRepository.observeDueItems()` collected into StateFlow
- This list updates reactively: when a review is submitted via ReviewViewModel, the item disappears from this list automatically (its urgency changes)

## Existing Code Reference

The ViewModels consume these existing repository methods (do NOT modify these files):

- `ItemRepository.observeDashboardItems(): Flow<List<DashboardItem>>` — in `data/repository/ItemRepository.kt`
- `ItemRepository.observeDueItems(): Flow<List<DashboardItem>>` — in `data/repository/ItemRepository.kt`
- `ItemRepository.observeById(id: Long): Flow<Item?>` — in `data/repository/ItemRepository.kt`
- `ItemRepository.archive(id: Long)` — in `data/repository/ItemRepository.kt`
- `ItemRepository.deleteById(id: Long)` — in `data/repository/ItemRepository.kt`
- `ReviewRepository.observeReviewsForItem(itemId: Long): Flow<List<Review>>` — in `data/repository/ReviewRepository.kt`
- `ReviewRepository.submitReview(itemId: Long, rating: Rating, notes: String?): Review` — in `data/repository/ReviewRepository.kt`
- `CategoryRepository.getById(id: Long): Category?` — in `data/repository/CategoryRepository.kt`

## Testing Requirements

Write unit tests for each ViewModel using:
- `kotlinx.coroutines.test.runTest` and `TestDispatcher`
- Fake/mock repositories (create simple fakes inline in the test file, or use a shared test-fixtures approach)
- Test that `dashboardItems` emits correctly from repository flow
- Test that `archiveItem` / `deleteItem` call the repository
- Test that `submitReview` calls repository and sets `reviewSubmitted`
- Test that `dueItems` filters correctly

## Acceptance Criteria

1. All three ViewModel files compile independently (no references to files outside the existing codebase)
2. All unit tests pass
3. Each ViewModel has a `companion object { fun factory(...): ViewModelProvider.Factory }` for DI
4. No existing files were modified
5. `./gradlew test` passes with the new tests included
