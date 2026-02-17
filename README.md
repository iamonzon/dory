# Dory

Spaced repetition Android app using the FSRS-4.5 algorithm.

## What It Does

Dory helps you remember anything by scheduling reviews based on the forgetting curve — the scientifically-observed pattern where memories decay exponentially over time. Unlike flashcard apps, Dory is item-based: you track things you want to remember (articles, concepts, skills) and periodically review them with a 4-point rating (Again, Hard, Good, Easy). The FSRS algorithm computes optimal review intervals, and items are color-coded by urgency (red/yellow/green) so you always know what needs attention.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (type-safe routes via kotlinx.serialization) |
| Database | Room (SQLite) |
| Preferences | DataStore |
| Background work | WorkManager |
| Algorithm | FSRS-4.5 (pure Kotlin implementation) |
| DI | Manual (AppContainer) |
| Min SDK | 36 (Android 15) |
| Build | Gradle 9.0.1, Kotlin 2.0.21, KSP |

## Architecture

MVVM + Repository pattern in a single `:app` module.

```
UI (Compose Screens)
  ↓ observes StateFlow
ViewModels
  ↓ calls
Repositories (ItemRepository, ReviewRepository, CategoryRepository, SettingsRepository)
  ↓ reads/writes
Room Database + DataStore
```

The FSRS algorithm lives in `algorithm/` as pure Kotlin with no Android dependencies, making it independently testable.

## Project Structure

```
com.iamonzon.dory/
├── algorithm/          FSRS-4.5 engine (Fsrs, FsrsParameters, Rating)
├── data/
│   ├── db/             Room database, DAOs, entities, type converters
│   ├── model/          Domain models (Item, Review, Category, ReviewUrgency)
│   ├── repository/     Business logic (Item, Review, Category, Settings repos)
│   └── mock/           Mock data for development (to be removed)
├── navigation/         Route definitions and NavGraph
├── notifications/      Daily digest worker, scheduler, notification channels
├── ui/
│   ├── dashboard/      Dashboard + Item Edit screens
│   ├── creation/       Item creation flow
│   ├── review/         Review + Review Session screens
│   ├── profile/        Profile, Categories, Archived Items, Advanced Settings
│   ├── onboarding/     First-launch onboarding screens
│   ├── actionhub/      Action Hub dialog
│   ├── components/     Shared UI components (ItemCard, UrgencyIndicator, TopAppBar)
│   └── theme/          Material 3 theme (colors, typography)
├── AppContainer.kt     Manual DI container
├── DoryApplication.kt  Application class
└── MainActivity.kt     Single activity entry point
```

## Building

Requires JDK 21 (for Gradle daemon) with Java 11 target compatibility.

```bash
# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/jdk-21

# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

## Spec Docs

The `spec/` directory contains the full project specification:

| File | Contents |
|------|----------|
| `spec/README.md` | Index and quick reference |
| `spec/main-idea.md` | Vision, item model, navigation, review mechanics, color system |
| `spec/use-cases-and-nfr.md` | 11 use cases (UC1-UC11), data model, edge cases, NFRs |
| `spec/algorithm.md` | FSRS-4.5 formulas, parameters, rating scale, per-category config |
| `spec/architecture.md` | MVVM + Repository pattern, package structure, data flow, DI |
| `spec/implementation-plan.md` | 6-phase build order with progress tracking |

## Current Status

- **Phases 1-3** (domain models, FSRS algorithm, UI shells, Room database, repositories): Complete
- **Phases 4-6** (wire UI to real data, notifications, polish): In progress — ViewModels are created, but screens still use mock data. Integration (Stream 6) is the next step. See `docs/streams/stream-6-integration.md` for details.
