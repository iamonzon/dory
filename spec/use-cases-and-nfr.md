# Dory - Use Cases & Non-Functional Requirements

## Navigation Structure

Three-tab bottom navigation:

| Position | Tab         | Screen                                      | Default |
|----------|-------------|---------------------------------------------|---------|
| Left     | Dashboard   | Flat urgency-sorted list of active items     | Yes     |
| Center   | Action Hub  | "Add new item" / "Start review session"      | No      |
| Right    | Profile     | Mastery overview, categories, archived items | No      |

### Dashboard Interactions

- **Tap** an item → opens the review flow for that item
- **Long-press** an item → shows a context menu with: **Edit**, **Archive**, **Delete**
  - Edit → navigates to the item edit screen
  - Archive → archives immediately, shows a toast ("Item archived")
  - Delete → deletes immediately (with confirmation dialog), shows a toast ("Item deleted")

### Action Hub

The center tab opens a **dialog** with two buttons:
- "Add new item" → navigates to the item creation flow
- "Start review session" → navigates to the review session list

## Use Cases

### UC1 - First Launch / Onboarding

**Actor:** New user
**Trigger:** First time opening the app (no items exist)
**Flow:**
1. App detects first launch (no items in database)
2. A brief 2-3 screen onboarding walkthrough is shown:
   - Screen 1: What Dory does (combat the forgetting curve)
   - Screen 2: How it works (add items, review when due, rate recall)
   - Screen 3: Get started (call-to-action to add first item)
3. User completes onboarding and lands on the empty dashboard
4. Onboarding is not shown again on subsequent launches

---

### UC2 - Add Item (Simple Mode)

**Actor:** User
**Trigger:** Center tab → "Add new item" (dialog) → creation flow
**Flow:**
1. Step 1: Enter title (required)
2. Step 2: Enter source (required). A **"More options"** button is visible.
3. User taps "Save" (not "More options")
4. Item is created with `createdAt = now`, no category, no notes
5. Item appears on the dashboard as due immediately (Yellow)

---

### UC3 - Add Item (Full Mode)

**Actor:** User
**Trigger:** Center tab → "Add new item" (dialog) → creation flow → "More options" on step 2
**Flow:**
1. Step 1: Enter title (required)
2. Step 2: Enter source (required). User taps **"More options"** instead of "Save".
3. Step 3: Select category — pick from existing categories or create a new one (optional, default: uncategorized)
4. Step 4: Enter notes — free-form item-level reference notes (optional)
5. Confirm and save
6. Item is created with all provided fields, `createdAt = now`
7. Item appears on dashboard as due immediately

**Note:** Simple and full mode are the same step-by-step flow. "More options" on step 2 expands to steps 3-4. Without it, the flow saves after step 2.

---

### UC4 - Review Item (from Dashboard)

**Actor:** User
**Trigger:** Tap an item on the dashboard
**Flow:**
1. Review screen opens showing:
   - Title
   - Source (plain text; URLs are auto-linked by the system and tappable)
   - Category (if set)
   - Item-level notes (persistent reference notes)
   - Review history: timeline of past review notes (most recent first)
2. User reviews the material externally (follows a link, consults a book, etc.)
3. User can edit or append to the item-level notes
4. User can add review-session notes (what they learned/recalled this time)
5. User rates their recall: **Again**, **Hard**, **Good**, or **Easy** (FSRS 4-point scale)
6. Save — a new Review record is created with the rating, session notes, computed stability, and computed difficulty
7. The FSRS-4.5 algorithm recalculates when this item is next due
8. User returns to the dashboard; the item's position and color update accordingly

---

### UC5 - Review Session (from Action Hub)

**Actor:** User
**Trigger:** Center tab → "Start review session"
**Precondition:** At least one item is due or overdue
**Flow:**
1. A filtered list is shown containing only due (Yellow) and overdue (Red) items, sorted by urgency
2. User picks an item from the list
3. Review screen opens (same as UC4, steps 1-7)
4. After saving, user returns to the filtered list
5. The just-reviewed item is removed from the list (it's now Green)
6. User can pick another item or leave the session

**Edge case:** If no items are due, the "Start review session" option shows a message indicating nothing is due for review.

---

### UC6 - Edit Item

**Actor:** User
**Trigger:** Long-press an item → context menu → "Edit"
**Flow:**
1. A dedicated edit screen opens for the item
2. User can modify:
   - Title
   - Source
   - Category
   - Item-level notes
3. User can also view and edit the **last 3 reviews**:
   - Edit the session notes from that review
   - Edit the recall rating (Again/Hard/Good/Easy)
   - Editing a rating triggers recalculation of stability/difficulty forward through subsequent reviews
4. Save changes

---

### UC7 - Archive Item

**Actor:** User
**Trigger:** Long-press an item → context menu → "Archive"
**Flow:**
1. Item's `isArchived` flag is set to true immediately
2. Item disappears from the dashboard
3. Item is excluded from the review session queue
4. A toast is shown: "Item archived"
5. Item remains in the database and is viewable from the Profile screen under archived items
6. User can restore (unarchive) the item from the Profile screen, which returns it to the dashboard

---

### UC8 - Delete Item

**Actor:** User
**Trigger:** Long-press an item → context menu → "Delete"
**Flow:**
1. A confirmation dialog is shown ("Delete [item title]? This cannot be undone.")
2. On confirm: the item and all its associated Review records are permanently deleted. A toast is shown: "Item deleted"
3. On cancel: no action taken

---

### UC9 - Manage Categories

**Actor:** User
**Trigger:** Profile screen → Categories section
**Flow:**
1. A list of all categories is shown, each with a count of items using it
2. User can:
   - **Rename** a category — updates the name; all items retain their association
   - **Delete** a category — a dialog is shown with three options:
     - "Delete all items" — permanently deletes the category and all its items
     - "Archive all items" — deletes the category, archives all its items
     - "Move to uncategorized" — deletes the category, items keep existing without a category
   - **Create** a new category (also possible during item creation in UC3)

---

### UC10 - View Mastery Profile

**Actor:** User
**Trigger:** Profile tab in bottom navigation
**Flow:**
1. Profile screen displays:
   - Total number of active items
   - Number of items mastered (FSRS stability > 90 days)
   - Number of items struggling (FSRS stability < 3 days + recent "Again" rating)
   - Breakdown by category (item count and mastery status per category)
2. Access to:
   - Category management (UC9)
   - Archived items list (with option to restore)

---

### UC11 - Daily Digest Notification

**Actor:** System
**Trigger:** Scheduled daily at a user-configured time (default: 9:00 AM, configurable in Profile)
**Flow:**
1. System checks how many items are due today and how many are overdue
2. If there are due or overdue items, a notification is shown:
   - Example: "You have 3 items due and 2 overdue. Time to review!"
3. If nothing is due, no notification is sent
4. Tapping the notification opens the app to the dashboard

---

## Data Model

### Item Table

| Field       | Type    | Required | Description                                      |
|-------------|---------|----------|--------------------------------------------------|
| id          | Long    | Auto     | Primary key, auto-generated                      |
| title       | String  | Yes      | The concept name                                 |
| source      | String  | Yes      | Where to study it (URL, book ref, etc.)          |
| categoryId  | Long?   | No       | FK to Category; null = uncategorized              |
| notes       | String? | No       | Persistent item-level reference notes             |
| createdAt   | Instant | Auto     | Timestamp of creation                            |
| isArchived  | Boolean | Auto     | Default: false                                   |

### Category Table

| Field             | Type     | Required | Description                                          |
|-------------------|----------|----------|------------------------------------------------------|
| id                | Long     | Auto     | Primary key, auto-generated                          |
| name              | String   | Yes      | Category name (unique)                               |
| desiredRetention  | Double?  | No       | Per-category retention target (null = use global default 0.9) |
| fsrsParameters    | String?  | No       | Per-category FSRS parameter overrides as JSON (null = use global defaults) |

### Review Table

| Field            | Type    | Required | Description                                          |
|------------------|---------|----------|------------------------------------------------------|
| id               | Long    | Auto     | Primary key, auto-generated                          |
| itemId           | Long    | Yes      | FK to Item                                           |
| rating           | Int     | Yes      | FSRS rating: 1=Again, 2=Hard, 3=Good, 4=Easy        |
| notes            | String? | No       | Per-session review notes                             |
| reviewedAt       | Instant | Auto     | Timestamp of the review                              |
| stabilityAfter   | Double  | Yes      | FSRS stability computed after this review            |
| difficultyAfter  | Double  | Yes      | FSRS difficulty computed after this review            |

### Relationships

- **Item → Category**: Many-to-one (optional). An item belongs to at most one category.
- **Item → Review**: One-to-many. An item has zero or more reviews.
- Deleting an Item cascades to delete all its Reviews.
- Deleting a Category: user chooses to delete items, archive items, or move to uncategorized (see UC9).

### Computed State (Not Stored)

The following are **derived at query time or in the presentation layer**, not stored in the database:

- **Next review date**: Computed by the spaced repetition algorithm from the item's review history
- **Urgency color**: Derived from comparing the computed next review date to today
- **Dashboard sort order**: Derived from urgency (most overdue first)
- **Items with no reviews**: Treated as due immediately based on `createdAt`

---

## Non-Functional Requirements

### Storage
- Local-only using Room/SQLite
- No cloud sync in v1
- No backup/export in v1
- Data layer designed so cloud sync can be added later (clean repository pattern)

### Offline
- Fully offline. Zero network dependencies.
- Source URLs that happen to be links open in the system browser (which may need network), but this is outside the app's responsibility.

### Performance
- Target scale: under 100 items
- No pagination required
- Full item list can be held in memory
- Simple queries with Room are sufficient

### Device Support
- Min SDK: 36 (Android 16)
- Personal use app — no broad device compatibility needed

### UI / Theme
- Jetpack Compose with Material Design 3
- Follow system dark/light mode
- Colorblind-safe urgency indicators: use shapes or icons alongside red/yellow/green colors (e.g., distinct icons per urgency level)

### Localization
- English only for v1
- All user-facing strings go through Android string resources (i18n-ready structure)

### Testing
- Unit tests for the spaced repetition algorithm (correctness of interval calculations)
- Unit tests for Room data layer (queries, cascading deletes, edge cases)
- UI testing: manual for v1

### Notifications
- Daily digest notification (one per day)
- Shows count of due + overdue items
- Silent if nothing is due
- Notification time: user-configurable in Profile (default: 9:00 AM)

### Architecture
- MVVM with Repository pattern, Jetpack Navigation Compose, manual DI
- Single `:app` module with clean package structure
- See `spec/architecture.md` for full details

### Schema Versioning
- Room database version starts at 1
- Schema changes in future versions use Room's migration API
- Destructive migration is acceptable for pre-release development; proper migrations required once the app is in active use

---

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| All items archived | Dashboard shows empty state (same as first launch, minus onboarding) |
| All items deleted | Dashboard shows empty state (same as first launch, minus onboarding) |
| Same-day multiple reviews of same item | Allowed. Each review is treated normally. Elapsed time is near-zero, so retrievability is ~1.0 and stability barely changes. The item is not re-shown as "due" after a same-day review. |
| No items due for review session | "Start review session" shows a message: "Nothing due for review right now." |
| Category deleted mid-edit | Not possible. User can only do one thing at a time (single-screen flows). |
| Item reviewed then immediately reviewed again | Same as same-day review. Allowed, minimal impact. |
| Onboarding completed but all items later deleted | Empty state shown, onboarding is NOT re-shown (flag persists in DataStore). |

---

## Related Specs

- `spec/main-idea.md` — Core vision, item model, navigation, views
- `spec/algorithm.md` — FSRS-4.5 algorithm details, formulas, parameters
- `spec/architecture.md` — MVVM architecture, package structure, data flow, DI

## What This Spec Does NOT Cover

The following are deferred:
- UI component design, visual design, and animations
- Onboarding screen content and design
- Cloud sync strategy
- Notification scheduling implementation details
- Planned study scheduling (future feature)
