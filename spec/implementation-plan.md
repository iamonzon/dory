# Dory - Implementation Plan

## Build Order

Six phases, each producing a working increment that can be manually tested.

---

## Phase 1: Domain Models + FSRS Algorithm ✅

**Goal:** Pure Kotlin foundation with no Android dependencies. Fully testable in isolation.

**Deliverables:**
- [x] Domain models: `Item`, `Review`, `Category`, `Rating`, `ReviewUrgency`
- [x] `FsrsParameters` data class with FSRS-4.5 default values (w0-w16)
- [x] `Fsrs` class implementing all formulas:
  - Initial stability and difficulty
  - Difficulty update with mean reversion
  - Stability after recall
  - Stability after lapse
  - Retrievability (forgetting curve)
  - Interval calculation
- [x] Unit tests for every formula and edge case
- [x] Unit tests for full scheduling scenarios (simulate review sequences)

**Verification:** All unit tests pass. No Android code needed to run them.

---

## Phase 2: UI Shells with Mock Data ✅

**Goal:** All screens navigable with hardcoded mock data. Establishes the visual structure and navigation flow.

**Deliverables:**
- [x] Theme setup: Material 3 colors (including urgency red/yellow/green), typography, shapes, light/dark support
- [x] Colorblind-safe urgency indicators (distinct icons per urgency level)
- [x] Bottom navigation bar: Dashboard (left), Action Hub (center), Profile (right)
- [x] Navigation graph with all routes
- [x] Dashboard screen: flat list with mock items, urgency colors, tap/long-press interactions
- [x] Action Hub: dialog with "Add new item" / "Start review session"
- [x] Item Creation: step-by-step flow with "More options" on step 2
- [x] Review screen: item detail, notes, rating buttons (Again/Hard/Good/Easy)
- [x] Review Session screen: filtered list of mock due items
- [x] Item Edit screen: edit fields + last 3 reviews
- [x] Profile screen: mastery stats, notification time, links to sub-screens
- [x] Category Management screen: list with rename/delete/create
- [x] Archived Items screen: list with restore option
- [x] Advanced Settings screen: FSRS parameters, desired retention
- [x] Onboarding screens (2-3 screens, placeholder content)
- [x] Context menu on dashboard long-press: Edit, Archive, Delete
- [x] Toast messages for archive/delete actions

**Verification:** Can navigate through every screen. All UI elements visible with mock data. Light and dark themes work.

---

## Phase 3: Room Database + DAOs + Repositories ✅

**Goal:** Real persistence layer. Data survives app restarts.

**Deliverables:**
- [x] Room entities: `ItemEntity`, `ReviewEntity`, `CategoryEntity`
- [x] Type converters for `Instant` and any JSON fields
- [x] `DoryDatabase` with version 1 schema
- [x] `ItemDao`: insert, update, delete, get by ID, get all active, get archived
- [x] `ReviewDao`: insert, update, delete, get by item ID (ordered), get latest N by item ID
- [x] `CategoryDao`: insert, update, delete, get all, get by ID
- [x] `ItemRepository`: CRUD + urgency computation using Fsrs
- [x] `ReviewRepository`: CRUD + S/D computation on review submission
- [x] `CategoryRepository`: CRUD + cascade handling (delete/archive/uncategorize items)
- [x] `SettingsRepository`: DataStore-backed storage for global FSRS params, retention, notification time, onboarding flag
- [x] `AppContainer` in `DoryApplication`: wires database, Fsrs, repositories
- [x] Instrumented tests for DAOs (in-memory database)
- [x] Unit tests for repositories (verify entity↔model mapping, FSRS integration)

**Verification:** All instrumented and unit tests pass. Repositories correctly compute urgency and schedule reviews.

---

## Phase 4: Wire UI to Real Data — IN PROGRESS

**Goal:** Replace mock data with real repositories. The app becomes functional.

**Completed:**
- [x] `DashboardViewModel`: observe items from repository, expose sorted/colored list as StateFlow
- [x] `CreationViewModel`: handle creation flow, validate inputs, save via repository
- [x] `ReviewViewModel`: load item + review history, submit review with rating/notes, compute and store S/D
- [x] `ProfileViewModel`: compute mastery stats (mastered/struggling counts, category breakdown)

**Remaining:** All screen-to-ViewModel wiring, onboarding flag, and context menu actions. See [`docs/streams/stream-6-integration.md`](../docs/streams/stream-6-integration.md).

---

## Phase 5: Notifications — IN PROGRESS

**Goal:** Daily digest notification working.

**Completed:**
- [x] `DailyDigestWorker`: queries due/overdue counts, builds notification, skips if nothing due
- [x] `DailyDigestScheduler`: schedules periodic WorkManager task at user-configured time
- [x] Notification tap opens app to dashboard

**Remaining:** Notification channel init in `DoryApplication` and rescheduling on time change. See [`docs/streams/stream-6-integration.md`](../docs/streams/stream-6-integration.md).

---

## Phase 6: Polish + Edge Cases — IN PROGRESS

**Goal:** Handle all edge cases, finalize onboarding, clean up.

**Completed:**
- [x] Onboarding content: 2-3 screens with real copy about the forgetting curve and how Dory works
- [x] String resources audit: all user-facing strings in `strings.xml`
- [x] Accessibility pass: content descriptions, colorblind-safe indicators verified

**Remaining:** Edge-case handling, end-to-end testing, and release build config. Depends on Stream 6 integration completing first. See [`docs/streams/stream-6-integration.md`](../docs/streams/stream-6-integration.md).

---

## Working Increment at Each Phase

| Phase | What you can do after this phase |
|-------|----------------------------------|
| 1 | Run unit tests confirming FSRS math is correct |
| 2 | Navigate all screens, see the full UI with mock data |
| 3 | Verify data persistence, run instrumented tests |
| 4 | Use the app end-to-end: create, review, track progress |
| 5 | Receive daily review reminders |
| 6 | Ship-ready personal app |

## Dependencies Between Phases

```
Phase 1 (Models + FSRS) ──→ Phase 3 (Database) ──→ Phase 4 (Wire UI)
                                                         │
Phase 2 (UI Shells) ──────────────────────────────→ Phase 4 (Wire UI)
                                                         │
                                                    Phase 5 (Notifications)
                                                         │
                                                    Phase 6 (Polish)
```

Phases 1 and 2 can be built **in parallel** — they have no dependencies on each other.
