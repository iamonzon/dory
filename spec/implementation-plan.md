# Dory - Implementation Plan

## Build Order

Six phases, each producing a working increment that can be manually tested.

---

## Phase 1: Domain Models + FSRS Algorithm

**Goal:** Pure Kotlin foundation with no Android dependencies. Fully testable in isolation.

**Deliverables:**
- [ ] Domain models: `Item`, `Review`, `Category`, `Rating`, `ReviewUrgency`
- [ ] `FsrsParameters` data class with FSRS-4.5 default values (w0-w16)
- [ ] `Fsrs` class implementing all formulas:
  - Initial stability and difficulty
  - Difficulty update with mean reversion
  - Stability after recall
  - Stability after lapse
  - Retrievability (forgetting curve)
  - Interval calculation
- [ ] Unit tests for every formula and edge case
- [ ] Unit tests for full scheduling scenarios (simulate review sequences)

**Verification:** All unit tests pass. No Android code needed to run them.

---

## Phase 2: UI Shells with Mock Data

**Goal:** All screens navigable with hardcoded mock data. Establishes the visual structure and navigation flow.

**Deliverables:**
- [ ] Theme setup: Material 3 colors (including urgency red/yellow/green), typography, shapes, light/dark support
- [ ] Colorblind-safe urgency indicators (distinct icons per urgency level)
- [ ] Bottom navigation bar: Dashboard (left), Action Hub (center), Profile (right)
- [ ] Navigation graph with all routes
- [ ] Dashboard screen: flat list with mock items, urgency colors, tap/long-press interactions
- [ ] Action Hub: dialog with "Add new item" / "Start review session"
- [ ] Item Creation: step-by-step flow with "More options" on step 2
- [ ] Review screen: item detail, notes, rating buttons (Again/Hard/Good/Easy)
- [ ] Review Session screen: filtered list of mock due items
- [ ] Item Edit screen: edit fields + last 3 reviews
- [ ] Profile screen: mastery stats, notification time, links to sub-screens
- [ ] Category Management screen: list with rename/delete/create
- [ ] Archived Items screen: list with restore option
- [ ] Advanced Settings screen: FSRS parameters, desired retention
- [ ] Onboarding screens (2-3 screens, placeholder content)
- [ ] Context menu on dashboard long-press: Edit, Archive, Delete
- [ ] Toast messages for archive/delete actions

**Verification:** Can navigate through every screen. All UI elements visible with mock data. Light and dark themes work.

---

## Phase 3: Room Database + DAOs + Repositories

**Goal:** Real persistence layer. Data survives app restarts.

**Deliverables:**
- [ ] Room entities: `ItemEntity`, `ReviewEntity`, `CategoryEntity`
- [ ] Type converters for `Instant` and any JSON fields
- [ ] `DoryDatabase` with version 1 schema
- [ ] `ItemDao`: insert, update, delete, get by ID, get all active, get archived
- [ ] `ReviewDao`: insert, update, delete, get by item ID (ordered), get latest N by item ID
- [ ] `CategoryDao`: insert, update, delete, get all, get by ID
- [ ] `ItemRepository`: CRUD + urgency computation using Fsrs
- [ ] `ReviewRepository`: CRUD + S/D computation on review submission
- [ ] `CategoryRepository`: CRUD + cascade handling (delete/archive/uncategorize items)
- [ ] `SettingsRepository`: DataStore-backed storage for global FSRS params, retention, notification time, onboarding flag
- [ ] `AppContainer` in `DoryApplication`: wires database, Fsrs, repositories
- [ ] Instrumented tests for DAOs (in-memory database)
- [ ] Unit tests for repositories (verify entity↔model mapping, FSRS integration)

**Verification:** All instrumented and unit tests pass. Repositories correctly compute urgency and schedule reviews.

---

## Phase 4: Wire UI to Real Data

**Goal:** Replace mock data with real repositories. The app becomes functional.

**Deliverables:**
- [ ] `DashboardViewModel`: observe items from repository, expose sorted/colored list as StateFlow
- [ ] `CreationViewModel`: handle creation flow, validate inputs, save via repository
- [ ] `ReviewViewModel`: load item + review history, submit review with rating/notes, compute and store S/D
- [ ] `ProfileViewModel`: compute mastery stats (mastered/struggling counts, category breakdown)
- [ ] Wire Category Management to `CategoryRepository` (rename, delete with 3-option dialog, create)
- [ ] Wire Archived Items to repository (list, restore)
- [ ] Wire Advanced Settings to `SettingsRepository` (read/write FSRS params, retention)
- [ ] Wire notification time setting in Profile to `SettingsRepository`
- [ ] Wire onboarding flag: check on launch, set after completion
- [ ] Item Edit screen: load item + last 3 reviews, save changes, cascade S/D recomputation
- [ ] Dashboard context menu actions: archive (immediate + toast), delete (confirm + toast), edit (navigate)
- [ ] Review session: filtered list from repository, remove reviewed items
- [ ] Empty states: dashboard when no items, review session when nothing due

**Verification:** Can create items, review them, see urgency colors update, archive/delete, manage categories. Data persists across app restarts.

---

## Phase 5: Notifications

**Goal:** Daily digest notification working.

**Deliverables:**
- [ ] Notification channel setup in `DoryApplication`
- [ ] `DailyDigestWorker`: queries due/overdue counts, builds notification, skips if nothing due
- [ ] `DailyDigestScheduler`: schedules periodic WorkManager task at user-configured time
- [ ] Reschedule when notification time changes in Profile
- [ ] Notification tap opens app to dashboard

**Verification:** Notification appears at configured time with correct counts. Tapping opens dashboard.

---

## Phase 6: Polish + Edge Cases

**Goal:** Handle all edge cases, finalize onboarding, clean up.

**Deliverables:**
- [ ] Onboarding content: 2-3 screens with real copy about the forgetting curve and how Dory works
- [ ] All-items-archived/deleted empty state (not re-showing onboarding)
- [ ] Category deletion 3-option dialog: delete items / archive items / move to uncategorized
- [ ] Same-day review behavior verification
- [ ] String resources audit: all user-facing strings in `strings.xml`
- [ ] Accessibility pass: content descriptions, colorblind-safe indicators verified
- [ ] Manual testing of all 11 use cases end-to-end
- [ ] ProGuard/R8 rules for release build (if needed)

**Verification:** All use cases (UC1-UC11) work correctly. No hardcoded strings. Accessibility features functional.

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
