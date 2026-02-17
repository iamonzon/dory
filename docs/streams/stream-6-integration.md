# Stream 6: Integration — Wire Everything Together

## Branch
`stream/integration`

## Goal
Wire the ViewModels (created in Streams 1-3) into the screen composables, update `AppContainer` with ViewModel factories, update `DoryNavGraph` to instantiate ViewModels, set up notification initialization (Stream 4), and handle the onboarding flow.

## Prerequisite
**This stream runs AFTER Streams 1-5 are all merged to `main`.** It is the only stream that modifies shared files.

## Files to Modify

```
app/src/main/java/com/iamonzon/dory/AppContainer.kt
app/src/main/java/com/iamonzon/dory/DoryApplication.kt
app/src/main/java/com/iamonzon/dory/MainActivity.kt
app/src/main/java/com/iamonzon/dory/navigation/DoryNavGraph.kt
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

## Files That Can Be Deleted After Integration

```
app/src/main/java/com/iamonzon/dory/data/mock/MockData.kt
```

Remove MockData once all screens are wired to real ViewModels.

## Task Breakdown

### 1. Update AppContainer

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
}
```

### 2. Update DoryApplication

Add notification channel creation:

```kotlin
class DoryApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.createChannels(this)
        // Schedule daily digest with saved notification time
        // (launch coroutine to read from SettingsRepository)
    }
}
```

### 3. Update DoryNavGraph

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

Or alternatively, obtain ViewModels inside each screen composable. Choose whichever pattern is simpler — but be consistent.

**Recommended pattern:** Pass ViewModel as parameter to each screen. This makes screens testable (you can pass fake ViewModels in previews).

### 4. Wire Each Screen

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
- This screen needs an `ItemEditViewModel` — **or** it can reuse `ReviewViewModel` with item editing capabilities. Since no other stream created an `ItemEditViewModel`:
  - Create `ItemEditViewModel.kt` in this stream
  - It needs: load item by ID, load last 3 reviews, save item updates
  - Constructor: `ItemEditViewModel(itemId: Long, itemRepository: ItemRepository, reviewRepository: ReviewRepository)`
  - StateFlows: `item: StateFlow<Item?>`, `recentReviews: StateFlow<List<Review>>`
  - Methods: `updateTitle(String)`, `updateSource(String)`, `updateNotes(String)`, `saveChanges()`

### 5. Onboarding Flow

- In `MainActivity.kt`, check `settingsRepository.observeHasCompletedOnboarding()` to decide the start destination:
  ```kotlin
  val startDestination = if (hasCompletedOnboarding) Dashboard else Onboarding
  ```
- In the `Onboarding` composable in `DoryNavGraph`, call `settingsRepository.setHasCompletedOnboarding(true)` when onComplete is triggered

### 6. Notification Scheduling Integration

- In `DoryApplication.onCreate()`, launch a coroutine to read the saved notification time and call `DailyDigestScheduler.schedule(this, hour, minute)`
- In `ProfileViewModel.setNotificationTime()`, also call `DailyDigestScheduler.schedule(context, hour, minute)` — this requires the ViewModel to have access to the application context. Pass it via factory.

### 7. Delete MockData

Once all screens are wired:
- Delete `app/src/main/java/com/iamonzon/dory/data/mock/MockData.kt`
- Remove any remaining imports of `MockData` from composables

### 8. Add lifecycle dependency if needed

The `collectAsStateWithLifecycle()` extension requires:
```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```
Check if already present. If not, add it to `app/build.gradle.kts`.

## Testing

### Manual Test Checklist
- [ ] App launches to onboarding on first install
- [ ] After onboarding, subsequent launches go to dashboard
- [ ] Create an item via Action Hub → Add → fill steps → Save
- [ ] Item appears on dashboard with correct urgency (Overdue since never reviewed)
- [ ] Tap item → Review screen shows item details, empty review history
- [ ] Submit review with "Good" → toast, navigate back
- [ ] Item urgency updates on dashboard (may change from Overdue to NotDue)
- [ ] Long-press item → context menu → Archive → item disappears, toast
- [ ] Profile → Archived Items → see archived item → Restore → returns to dashboard
- [ ] Profile → Categories → create, rename, delete category
- [ ] Profile → Advanced Settings → adjust retention slider → persists across restarts
- [ ] Review Session → shows only due/overdue items
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
