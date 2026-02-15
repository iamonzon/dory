# Dory - Architecture

## Overview

Dory follows the **MVVM** (Model-View-ViewModel) architecture pattern with a **Repository** layer, using **Jetpack Compose** for UI, **Room** for persistence, and **Jetpack Navigation Compose** for screen management. All code lives in a **single `:app` module** with a clean package structure.

Dependencies are provided via **manual dependency injection** through a composition root in the Application class.

## Package Structure

```
com.iamonzon.dory/
├── DoryApplication.kt              # Application class, composition root (DI container)
├── MainActivity.kt                 # Single Activity, hosts Compose content
│
├── navigation/
│   ├── DoryNavGraph.kt             # Navigation graph definition
│   ├── Routes.kt                   # Route constants / sealed class
│   └── BottomNavBar.kt             # Bottom navigation bar composable
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # Color definitions (including urgency colors)
│   │   ├── Theme.kt                # DoryTheme (light/dark, Material 3)
│   │   ├── Type.kt                 # Typography
│   │   └── Shape.kt                # Shapes (including colorblind-safe indicators)
│   │
│   ├── dashboard/
│   │   ├── DashboardScreen.kt      # Dashboard composable
│   │   └── DashboardViewModel.kt   # Dashboard state and logic
│   │
│   ├── actionhub/
│   │   └── ActionHubScreen.kt      # Action hub with Add / Review options
│   │
│   ├── creation/
│   │   ├── ItemCreationScreen.kt   # Step-by-step item creation flow
│   │   └── CreationViewModel.kt    # Creation state and validation
│   │
│   ├── review/
│   │   ├── ReviewScreen.kt         # Review screen composable
│   │   ├── ReviewSessionScreen.kt  # Filtered list of due items
│   │   └── ReviewViewModel.kt      # Review state, rating, notes
│   │
│   ├── profile/
│   │   ├── ProfileScreen.kt        # Mastery overview, notification settings, entry to sub-screens
│   │   ├── ProfileViewModel.kt     # Profile stats computation
│   │   ├── CategoryManagementScreen.kt  # Category CRUD
│   │   ├── ArchivedItemsScreen.kt  # Archived items list
│   │   └── AdvancedSettingsScreen.kt # FSRS parameters, per-category retention tuning
│   │
│   ├── onboarding/
│   │   └── OnboardingScreen.kt     # First-launch walkthrough
│   │
│   └── components/
│       └── (shared composables)    # Reusable UI components as needed
│
├── data/
│   ├── db/
│   │   ├── DoryDatabase.kt        # Room database definition
│   │   ├── ItemDao.kt             # DAO for Item table
│   │   ├── ReviewDao.kt           # DAO for Review table
│   │   ├── CategoryDao.kt         # DAO for Category table
│   │   └── entity/
│   │       ├── ItemEntity.kt      # Room entity for Item
│   │       ├── ReviewEntity.kt    # Room entity for Review
│   │       └── CategoryEntity.kt  # Room entity for Category
│   │
│   ├── model/
│   │   ├── Item.kt                # Domain model for Item
│   │   ├── Review.kt              # Domain model for Review
│   │   ├── Category.kt            # Domain model for Category
│   │   └── ReviewUrgency.kt       # Enum: Overdue, DueToday, NotDue
│   │
│   └── repository/
│       ├── ItemRepository.kt      # Item CRUD + urgency computation
│       ├── ReviewRepository.kt    # Review CRUD + history queries
│       ├── CategoryRepository.kt  # Category CRUD
│       └── SettingsRepository.kt  # FSRS params, retention, preferences
│
├── algorithm/
│   ├── Fsrs.kt                    # FSRS-4.5 core algorithm (pure Kotlin)
│   ├── FsrsParameters.kt         # Parameter data class with defaults
│   └── Rating.kt                  # Rating enum (Again, Hard, Good, Easy)
│
└── notifications/
    ├── DailyDigestScheduler.kt    # WorkManager scheduling for daily notification
    └── DailyDigestWorker.kt       # Worker that builds and shows the notification
```

## Layers

### UI Layer (Compose + ViewModels)

**Composables** are stateless functions that render UI from state provided by ViewModels.

**ViewModels** hold UI state as `StateFlow` and expose actions as methods. They depend on repositories (injected via constructor).

```
Screen (Composable)
  └── observes → ViewModel.uiState: StateFlow<UiState>
  └── calls   → ViewModel.onAction(action)
```

Each feature screen has its own ViewModel. ViewModels do not reference Android `Context` directly (except via `AndroidViewModel` if absolutely necessary for system services).

### Data Layer (Repositories + Room)

**Repositories** are the single source of truth for data. They:
- Expose data as `Flow` for reactive observation
- Map between Room entities and domain models
- Coordinate between DAOs and the FSRS algorithm for computed state (e.g., urgency, next review date)
- Handle business logic that spans multiple DAOs (e.g., deleting a category and updating items)

**Room DAOs** provide raw database access. They return `Flow<List<Entity>>` for observable queries and `suspend fun` for write operations.

**Room Entities** are data layer concerns — they mirror the database schema. Domain models are separate and used throughout the app.

### Algorithm Layer

The FSRS-4.5 implementation is a **pure Kotlin utility** with:
- No Android dependencies
- No database dependencies
- No side effects
- Fully deterministic given the same inputs

Repositories call into the algorithm when they need to compute intervals, urgency, or update stability/difficulty after a review.

### Notification Layer

Uses **WorkManager** to schedule a daily periodic task that:
1. Queries the database for due/overdue item counts
2. Shows a summary notification if items are due
3. Skips if nothing is due

## Data Flow

### Reading Data (Dashboard Example)

```
Room DB
  └── ItemDao.getAllActive(): Flow<List<ItemEntity>>
        └── ItemRepository.getDashboardItems(): Flow<List<DashboardItem>>
              │  (maps entities to domain models)
              │  (calls Fsrs to compute urgency per item)
              │  (sorts by urgency: overdue first, then due today, then not due)
              └── DashboardViewModel.uiState: StateFlow<DashboardState>
                    └── DashboardScreen (Compose)
```

`DashboardItem` is a presentation model containing: title, urgency color, urgency icon, and the item ID for navigation.

### Writing Data (Review Example)

```
ReviewScreen
  └── user taps "Save" with rating + notes
        └── ReviewViewModel.submitReview(itemId, rating, notes)
              └── ReviewRepository.addReview(itemId, rating, notes)
                    │  1. Get current S and D from latest review (or null if first review)
                    │  2. Compute retrievability from elapsed time
                    │  3. Call Fsrs to compute new S and D
                    │  4. Insert ReviewEntity with rating, notes, stabilityAfter, difficultyAfter
                    └── Room persists the review
                          └── Flow triggers → Dashboard auto-updates
```

## Dependency Injection

Manual DI via a simple container created in `DoryApplication`:

```kotlin
class DoryApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val database = DoryDatabase.create(context)
    private val fsrs = Fsrs()

    val itemRepository = ItemRepository(database.itemDao(), database.reviewDao(), fsrs)
    val reviewRepository = ReviewRepository(database.reviewDao(), fsrs)
    val categoryRepository = CategoryRepository(database.categoryDao())
    val settingsRepository = SettingsRepository(context)
}
```

ViewModels access the container through a factory:

```kotlin
class DashboardViewModel(
    private val itemRepository: ItemRepository
) : ViewModel() {
    // ...

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DoryApplication
                DashboardViewModel(app.container.itemRepository)
            }
        }
    }
}
```

## Navigation

Jetpack Navigation Compose with a `NavHost` and typed routes.

```kotlin
sealed class Route {
    data object Dashboard : Route()
    data object ActionHub : Route()
    data object Profile : Route()
    data object ItemCreation : Route()
    data class Review(val itemId: Long) : Route()
    data object ReviewSession : Route()
    data object CategoryManagement : Route()
    data object ArchivedItems : Route()
    data object AdvancedSettings : Route()
    data object Onboarding : Route()
}
```

Bottom navigation handles the three main tabs (Dashboard, Action Hub, Profile). Secondary screens (Review, Creation, Settings, etc.) are pushed onto the nav stack from their parent tabs.

## Settings Structure

Settings are split across two locations in the UI:

**Profile screen** (directly accessible):
- Notification time (default: 9:00 AM)
- Entry point to Advanced Settings
- Entry point to Category Management
- Entry point to Archived Items

**Advanced Settings screen** (under Profile):
- Global FSRS parameters (w0-w16)
- Global desired retention (default: 0.9)
- Per-category FSRS parameter and retention overrides

## Settings Storage

User preferences (FSRS global parameters, desired retention, notification time, onboarding-completed flag) are stored using **DataStore Preferences** (not Room). This keeps configuration separate from business data.

Per-category overrides (desired retention, FSRS parameters) are stored as fields on the Category entity in Room, since they're tightly coupled to category data.

## Error Handling

- Room operations are wrapped in `try/catch` at the repository level
- ViewModels expose error state as part of their `UiState` sealed class
- No network errors to handle (fully offline)
- Validation (e.g., empty title) is handled in ViewModels before calling repositories

## Testing Strategy

| Layer | Testing Approach |
|-------|-----------------|
| Algorithm (`Fsrs`) | Unit tests with JUnit. Pure functions, no mocking needed. |
| DAOs | Instrumented tests with in-memory Room database. |
| Repositories | Unit tests with fake/mock DAOs. Verify entity↔model mapping and FSRS integration. |
| ViewModels | Unit tests with fake repositories. Verify state transitions. |
| UI (Compose) | Manual testing for v1. |

## What This Spec Does NOT Cover

- Detailed screen layouts and component specifications
- Animation and transition design
- Cloud sync architecture (future)
- Notification channel configuration details
- ProGuard/R8 rules for release builds
- CI/CD pipeline
