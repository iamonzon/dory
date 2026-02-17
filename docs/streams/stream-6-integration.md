# Stream 6: Integration — Wire Everything Together

## Branch
`stream/integration`

## Goal
Wire the ViewModels (created in Streams 1-3) into the screen composables, update `AppContainer` with ViewModel factories, update `DoryNavGraph` to instantiate ViewModels, set up notification initialization (Stream 4), and handle the onboarding flow.

## Prerequisite
**This stream runs AFTER Streams 1-5 are all merged to `main`.** It is the only stream that modifies shared files.

## Current State (Post-Merge)

All streams 1-5 are merged. The codebase has:
- **All 8 ViewModels** created and compiling (`Dashboard`, `Creation`, `Review`, `ReviewSession`, `Profile`, `CategoryManagement`, `ArchivedItems`, `AdvancedSettings`)
- **No `ItemEditViewModel`** — this was not part of any stream and must be created in Stream 6
- **All screens** still read from `MockData` — no screen is wired to its ViewModel yet
- **`DoryApplication.onCreate()`** initializes `AppContainer` only — does NOT call `NotificationChannels.createChannels()` or schedule the daily digest
- **`MainActivity`** uses `IS_FIRST_LAUNCH = false` (hardcoded constant) instead of reading from `SettingsRepository`
- **`lifecycle-runtime-compose`** dependency is NOT in `build.gradle.kts` — needed for `collectAsStateWithLifecycle()`

## Files to Modify

```
app/build.gradle.kts                                          (add lifecycle-runtime-compose)
app/src/main/java/com/iamonzon/dory/AppContainer.kt            (add ViewModel factories)
app/src/main/java/com/iamonzon/dory/DoryApplication.kt         (notification channel + scheduler init)
app/src/main/java/com/iamonzon/dory/MainActivity.kt            (onboarding flag from SettingsRepository)
app/src/main/java/com/iamonzon/dory/navigation/DoryNavGraph.kt (instantiate ViewModels, pass to screens)
app/src/main/java/com/iamonzon/dory/ui/dashboard/DashboardScreen.kt
app/src/main/java/com/iamonzon/dory/ui/dashboard/ItemEditScreen.kt
app/src/main/java/com/iamonzon/dory/ui/creation/ItemCreationScreen.kt
app/src/main/java/com/iamonzon/dory/ui/review/ReviewScreen.kt
app/src/main/java/com/iamonzon/dory/ui/review/ReviewSessionScreen.kt
app/src/main/java/com/iamonzon/dory/ui/profile/ProfileScreen.kt
app/src/main/java/com/iamonzon/dory/ui/profile/CategoryManagementScreen.kt
app/src/main/java/com/iamonzon/dory/ui/profile/ArchivedItemsScreen.kt
app/src/main/java/com/iamonzon/dory/ui/profile/AdvancedSettingsScreen.kt
app/src/main/java/com/iamonzon/dory/ui/onboarding/OnboardingScreen.kt
```

## Files to Create

```
app/src/main/java/com/iamonzon/dory/ui/dashboard/ItemEditViewModel.kt
```

## Files to Delete After Integration

```
app/src/main/java/com/iamonzon/dory/data/mock/MockData.kt
```

Remove MockData once all screens are wired to real ViewModels.

## Task Breakdown

### 1. Add lifecycle-runtime-compose Dependency

`collectAsStateWithLifecycle()` requires this dependency. Add to `app/build.gradle.kts`:

```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```

Or add to the version catalog in `gradle/libs.versions.toml` and reference it.

### 2. Create ItemEditViewModel

This ViewModel was not created in any previous stream. Create `ItemEditViewModel.kt`:

```kotlin
class ItemEditViewModel(
    private val itemId: Long,
    private val itemRepository: ItemRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    val item: StateFlow<Item?> = ...       // load item by ID
    val recentReviews: StateFlow<List<Review>> = ...  // last 3 reviews

    fun updateTitle(title: String) { ... }
    fun updateSource(source: String) { ... }
    fun updateNotes(notes: String) { ... }
    fun saveChanges() { ... }

    companion object {
        fun factory(itemId: Long, itemRepository: ItemRepository, reviewRepository: ReviewRepository): ViewModelProvider.Factory = ...
    }
}
```

### 3. Update AppContainer

Add ViewModel dependencies to `AppContainer.kt`:

```kotlin
class AppContainer(context: Context) {
    // ... existing fields ...

    // ViewModel factories — screens use these to create ViewModels
    val dashboardViewModelFactory get() = DashboardViewModel.factory(itemRepository)

    val reviewViewModelFactory: (Long) -> ViewModelProvider.Factory
        get() = { itemId -> ReviewViewModel.factory(itemId, itemRepository, reviewRepository, categoryRepository) }

    val reviewSessionViewModelFactory get() = ReviewSessionViewModel.factory(itemRepository)

    val creationViewModelFactory get() = CreationViewModel.factory(itemRepository, categoryRepository)

    val profileViewModelFactory get() = ProfileViewModel.factory(itemRepository, categoryRepository, settingsRepository)

    val categoryManagementViewModelFactory get() = CategoryManagementViewModel.factory(categoryRepository, itemRepository)

    val archivedItemsViewModelFactory get() = ArchivedItemsViewModel.factory(itemRepository)

    val advancedSettingsViewModelFactory get() = AdvancedSettingsViewModel.factory(settingsRepository)

    val itemEditViewModelFactory: (Long) -> ViewModelProvider.Factory
        get() = { itemId -> ItemEditViewModel.factory(itemId, itemRepository, reviewRepository) }
}
```

### 4. Update DoryApplication

Add notification channel creation and scheduler initialization:

```kotlin
class DoryApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.createChannels(this)

        // Schedule daily digest with saved notification time
        CoroutineScope(Dispatchers.IO).launch {
            val time = container.settingsRepository.getNotificationTime()
            DailyDigestScheduler.schedule(this@DoryApplication, time.hour, time.minute)
        }
    }
}
```

### 5. Update MainActivity — Onboarding Flag

Replace `IS_FIRST_LAUNCH = false` with a real check:

```kotlin
// In MainActivity, read from SettingsRepository:
val app = application as DoryApplication
val hasCompleted by app.container.settingsRepository
    .observeHasCompletedOnboarding()
    .collectAsStateWithLifecycle(initialValue = true)  // default true to avoid flash

val startDestination: Any = if (hasCompleted) Dashboard else Onboarding
```

### 6. Update DoryNavGraph

For each composable in the nav graph, obtain the ViewModel from the `AppContainer`:

```kotlin
composable<Dashboard> {
    val app = LocalContext.current.applicationContext as DoryApplication
    val viewModel: DashboardViewModel = viewModel(factory = app.container.dashboardViewModelFactory)
    DashboardScreen(
        viewModel = viewModel,
        onItemClick = { ... },
        onEditItem = { ... }
    )
}
```

**Recommended pattern:** Pass ViewModel as parameter to each screen. This makes screens testable (you can pass fake ViewModels in previews).

### 7. Wire Each Screen

For each screen, the changes follow this pattern:

1. Add a `viewModel` parameter to the composable
2. Collect the ViewModel's StateFlow using `collectAsStateWithLifecycle()`
3. Replace `MockData.xxx` references with ViewModel state
4. Wire user actions (button clicks, etc.) to ViewModel methods
5. Keep the `@Preview` working by providing a default/mock ViewModel or making preview use direct state

**DashboardScreen:**
- Replace `MockData.dashboardItems` with `viewModel.dashboardItems.collectAsStateWithLifecycle()`
- Wire archive menu item to `viewModel.archiveItem(id)`
- Wire delete menu item to `viewModel.deleteItem(id)`
- Observe one-shot events for toast messages

**ItemCreationScreen:**
- Replace local state (`title`, `source`, etc.) with `viewModel.uiState.collectAsStateWithLifecycle()`
- Replace `MockData.categories` with categories from uiState
- Wire Save button to `viewModel.saveItem()`
- Observe `savedSuccessfully` to trigger `onBackClick()`

**ReviewScreen:**
- Replace `MockData.itemById(itemId)` with `viewModel.item.collectAsStateWithLifecycle()`
- Replace `MockData.reviewsForItem(itemId)` with `viewModel.reviews.collectAsStateWithLifecycle()`
- Wire rating buttons to `viewModel.submitReview(rating, notes)`
- Observe `reviewSubmitted` to trigger `onBackClick()`

**ReviewSessionScreen:**
- Replace `MockData.dueItems` with `viewModel.dueItems.collectAsStateWithLifecycle()`

**ProfileScreen:**
- Replace `MockData.profileStats` with `viewModel.uiState.collectAsStateWithLifecycle()`
- Wire notification time to ViewModel

**CategoryManagementScreen:**
- Replace `MockData.categories` with `viewModel.uiState.collectAsStateWithLifecycle()`
- Wire create/rename/delete to ViewModel methods

**ArchivedItemsScreen:**
- Replace `MockData.archivedItems` with `viewModel.archivedItems.collectAsStateWithLifecycle()`
- Wire restore button to `viewModel.restoreItem(id)`

**AdvancedSettingsScreen:**
- Replace hardcoded defaults with `viewModel.uiState.collectAsStateWithLifecycle()`
- Wire slider to `viewModel.setDesiredRetention(value)`

**ItemEditScreen:**
- Wire to new `ItemEditViewModel`
- Replace `MockData.itemById(itemId)` with `viewModel.item.collectAsStateWithLifecycle()`
- Replace mock reviews with `viewModel.recentReviews.collectAsStateWithLifecycle()`
- Wire save button to `viewModel.saveChanges()`

### 8. Onboarding Flow

- In `OnboardingScreen`, when `onComplete` is triggered, call `settingsRepository.setHasCompletedOnboarding(true)`
- Pass `settingsRepository` (or a lambda) through `DoryNavGraph`

### 9. Notification Scheduling Integration

- In `ProfileViewModel.setNotificationTime()`, also call `DailyDigestScheduler.schedule(context, hour, minute)` — this requires the ViewModel to have access to the application context. Pass it via factory.

### 10. Delete MockData

Once all screens are wired:
- Delete `app/src/main/java/com/iamonzon/dory/data/mock/MockData.kt`
- Remove any remaining imports of `MockData` from composables

## Testing

### Manual Test Checklist
- [ ] App launches to onboarding on first install
- [ ] After onboarding, subsequent launches go to dashboard
- [ ] Create an item via Action Hub > Add > fill steps > Save
- [ ] Item appears on dashboard with correct urgency (Overdue since never reviewed)
- [ ] Tap item > Review screen shows item details, empty review history
- [ ] Submit review with "Good" > toast, navigate back
- [ ] Item urgency updates on dashboard (may change from Overdue to NotDue)
- [ ] Long-press item > context menu > Archive > item disappears, toast
- [ ] Profile > Archived Items > see archived item > Restore > returns to dashboard
- [ ] Profile > Categories > create, rename, delete category
- [ ] Profile > Advanced Settings > adjust retention slider > persists across restarts
- [ ] Review Session > shows only due/overdue items
- [ ] Data persists across app restarts
- [ ] Notification appears at configured time (advance device clock to test)

## Acceptance Criteria

1. All screens display real data from repositories (no MockData references remain)
2. All CRUD operations work (create, read, update, archive, delete, restore)
3. Review submission computes S/D via FSRS and updates urgency
4. Onboarding flow works correctly (first launch vs. subsequent)
5. Notification scheduling is initialized on app start
6. `MockData.kt` is deleted
7. `./gradlew assembleDebug` compiles successfully
8. All existing tests still pass
