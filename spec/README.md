# Dory — Spec Index

Dory is a spaced repetition Android app for reviewing learned concepts and patterns. Users add items (with a title, source, and optional category/notes), review them when due, rate recall, and the FSRS-4.5 algorithm schedules the next review. The dashboard signals urgency via color coding.

**Target:** Personal use (2 users), minSdk 36, fully offline.

## Specs

| File | Covers |
|------|--------|
| [main-idea.md](main-idea.md) | Vision, item model, navigation layout, views, review mechanics, color system, item lifecycle, categories, technical decisions |
| [use-cases-and-nfr.md](use-cases-and-nfr.md) | 11 use cases (UC1-UC11), data model (3 tables), edge cases, non-functional requirements |
| [algorithm.md](algorithm.md) | FSRS-4.5 formulas, 17 default parameters, rating scale, mastery/struggling definitions, per-category configuration |
| [architecture.md](architecture.md) | MVVM + Repository, package structure, data flow diagrams, manual DI, settings structure, navigation routes |
| [implementation-plan.md](implementation-plan.md) | 6-phase build order with checklists, verification criteria, dependency graph |

## Quick Reference

| Decision | Choice |
|----------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM, single `:app` module, manual DI |
| Navigation | Jetpack Navigation Compose |
| Database | Room/SQLite, local-only |
| Algorithm | FSRS-4.5, implemented from scratch (pure Kotlin, no dependencies) |
| Rating scale | 4-point: Again, Hard, Good, Easy |
| Desired retention | Default 0.9, configurable globally and per-category |
| Mastered | FSRS stability > 90 days |
| Struggling | FSRS stability < 3 days + recent "Again" |
| Theme | System light/dark, colorblind-safe urgency indicators |
| Notifications | Daily digest via WorkManager, user-configurable time |
| Testing | Unit tests for algorithm + data layer; manual UI testing |
| Backup/sync | None in v1 |

## Navigation Layout

```
Left: Dashboard (default)  |  Center: Action Hub (dialog)  |  Right: Profile
```

## Data Model (3 tables)

- **Item**: id, title, source, categoryId?, notes?, createdAt, isArchived
- **Category**: id, name, desiredRetention?, fsrsParameters?
- **Review**: id, itemId, rating, notes?, reviewedAt, stabilityAfter, difficultyAfter

Urgency, next review date, and sort order are computed at display time — not stored.

## Build Phases

1. Domain models + FSRS algorithm (pure Kotlin, unit tests)
2. UI shells with mock data (all screens, navigation, theme)
3. Room database + DAOs + repositories (persistence, instrumented tests)
4. Wire UI to real data (ViewModels, full functionality)
5. Notifications (daily digest)
6. Polish + edge cases (onboarding, accessibility, strings audit)

Phases 1 and 2 can run in parallel.
