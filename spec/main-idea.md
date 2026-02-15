# Dory - Main Idea

## Vision

Dory is an Android application designed to combat the forgetting curve. Users add concepts and topics they are learning, and the app signals when each item needs review through color coding and urgency-based sorting.

The core insight: by scheduling reviews based on the forgetting curve (next day, then a few days later, then increasingly longer intervals), the app naturally limits how many new items a user should introduce. If you add too many items at once, your review queue becomes unmanageable — this organic pressure scaffolds learning in a controlled, sustainable way.

## Item Model

Each item represents a concept or topic the user is studying. An item has:

| Field    | Description                                                        | Required |
|----------|--------------------------------------------------------------------|----------|
| Title    | The concept name (e.g., "VSA Pattern", "Binary Search")            | Yes      |
| Source   | Where to go study it — a URL, book reference, course module, etc.  | Yes      |
| Category | A single category the item belongs to (e.g., "Android", "Algorithms") | No (default: uncategorized) |
| Notes    | Persistent item-level reference notes                              | No       |

Items are not flashcards. There is no question/answer structure. An item points the user to *what* to review and *where* to review it.

Notes exist at two levels:
- **Item-level notes**: persistent reference notes set during creation and editable anytime
- **Review-level notes**: each review session captures its own notes (what the user learned/recalled that time), forming a timeline of learning

## Navigation

Three-tab bottom navigation:

| Position | Tab         | Screen                                       | Default |
|----------|-------------|----------------------------------------------|---------|
| Left     | Dashboard   | Flat urgency-sorted list of active items      | Yes     |
| Center   | Action Hub  | "Add new item" / "Start review session"       | No      |
| Right    | Profile     | Mastery overview, categories, archived items  | No      |

## Views

### 1. Dashboard (Default View)

The dashboard is the landing screen. It shows a **compact flat list** of all active (non-archived) items, **auto-sorted by review urgency** (most urgent first).

Each row displays:
- **Color indicator** — traffic light system with colorblind-safe shapes/icons:
  - **Red**: overdue (past its review date)
  - **Yellow**: due today
  - **Green**: not due yet (fresh or recently reviewed)
- **Title** of the item

Items are **not grouped by category**. The list is flat so the user immediately sees what needs attention across all topics.

**Interactions:**
- **Tap** an item → opens the review flow
- **Long-press** an item → opens edit/management mode (edit fields, archive, delete, edit last 3 reviews)

**First launch:** A 2-3 screen onboarding walkthrough is shown before the empty dashboard, explaining the forgetting curve concept and how Dory works.

### 2. Action Hub (Center Tab)

The center tab presents two options:
- **Add new item** — opens the item creation flow
- **Start review session** — shows a filtered list of due/overdue items to pick from

### 3. Item Creation

Two creation modes, both following a step-by-step flow (one field at a time):

**Simple mode:** Title → Source → Done

**Full mode:** Title → Source → Category (select existing or create new) → Notes → Done

New items are due for review immediately upon creation.

### 4. Review Screen

When a user taps an item (from the dashboard or the review session list):

1. The full item detail is displayed: title, source (plain text, URLs auto-linked), category, item-level notes, and a timeline of past review session notes
2. The user reviews the material externally (follows a link, reads notes, etc.)
3. The user can **edit item-level notes** and **add review-session notes**
4. The user **rates their recall**: Again, Hard, Good, or Easy (FSRS 4-point scale)
5. Save — a Review record is created; the algorithm recalculates when this item is next due

### 5. Profile

A **mastery overview** showing:
- Total number of active items
- Items mastered (definition to be determined during algorithm design)
- Items currently struggling
- Breakdown by category

Also provides access to:
- **Category management** (rename, delete, create)
- **Archived items** (view and restore)

## Review Mechanics

- After each review, the user rates recall using the **FSRS 4-point scale: Again, Hard, Good, Easy**
- The rating feeds the **FSRS-4.5** spaced repetition algorithm that adjusts the next review interval
- Better ratings push the next review further out; worse ratings bring it closer
- Review history is stored as individual records with computed stability and difficulty values
- The last 3 reviews are editable (notes and rating); editing a rating triggers recalculation of stability/difficulty forward
- See `spec/algorithm.md` for full FSRS-4.5 details, formulas, and parameters

## Color Signal System

| Color  | Meaning   | Condition                       | Accessibility       |
|--------|-----------|---------------------------------|----------------------|
| Red    | Overdue   | Past the scheduled review date  | Distinct icon/shape  |
| Yellow | Due today | Scheduled review date is today  | Distinct icon/shape  |
| Green  | Not due   | Next review date is in the future | Distinct icon/shape |

Urgency is **computed at display time** from raw review data, not stored in the database.

## Item Lifecycle

Items can exist in three states:
- **Active**: visible on the dashboard, included in review sessions
- **Archived**: hidden from dashboard and review queue, viewable/restorable from Profile
- **Deleted**: permanently removed with all associated review history

## Categories

Categories are first-class managed entities:
- Created during item creation or from the Profile screen
- Can be renamed or deleted from the Profile screen
- Deleting a category moves all its items to "uncategorized"
- Each item belongs to at most one category

## Technical Decisions

- **Storage**: Local-first using Room/SQLite, no backup/export in v1, data layer designed for future cloud sync
- **UI**: Jetpack Compose with Material Design 3, system dark/light theme
- **Accessibility**: Colorblind-safe urgency indicators (shapes/icons alongside colors)
- **Localization**: English only, all strings through Android resources (i18n-ready)
- **Min SDK**: 36 (Android 16) — personal use app
- **Offline**: Fully offline, zero network dependencies
- **Testing**: Unit tests for algorithm and data layer; manual UI testing
- **Notifications**: Daily digest — one notification per day summarizing due/overdue count
- **Architecture**: MVVM with Repository pattern, Jetpack Navigation Compose, manual DI. See `spec/architecture.md`
- **Algorithm**: FSRS-4.5, implemented from scratch in pure Kotlin. Configurable desired retention (global + per-category). See `spec/algorithm.md`
- **Mastery**: Stability > 90 days. **Struggling**: Stability < 3 days + recent "Again" rating
- **Computed state**: Next review date, urgency color, and sort order are derived at query/display time from raw data — not stored

## Related Specs

- `spec/use-cases-and-nfr.md` — Use cases (UC1-UC11), data model, and non-functional requirements
- `spec/algorithm.md` — FSRS-4.5 algorithm details, formulas, parameters, and configuration
- `spec/architecture.md` — MVVM architecture, package structure, data flow, and DI setup
- `spec/implementation-plan.md` — Six-phase build order with deliverables and verification criteria

## What This Spec Does NOT Cover

The following will be addressed in subsequent specs:
- UI component design, visual design, and animations
- Onboarding screen content and design
- Cloud sync strategy
- Notification scheduling implementation details
- Planned study scheduling (future feature)
