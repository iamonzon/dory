# Stream 5: Polish — Strings, Onboarding, Accessibility

## Branch
`stream/polish`

## Goal
Extract all hardcoded strings to `strings.xml`, write real onboarding content, and add content descriptions for accessibility. This stream prepares the app for release-quality text and accessibility.

## Critical Constraint
**This is the one stream that DOES modify existing files** — specifically `strings.xml` and the screen composables to replace hardcoded strings with `stringResource()` calls. This is safe because no other stream (1-4) touches these files, and stream 6 runs after this is merged.

**However**, to minimize conflicts with Stream 6 (integration), follow this rule:
- DO NOT change function signatures, parameters, or ViewModel wiring in any composable
- ONLY change string literals to `stringResource()` calls and add `contentDescription` parameters
- DO NOT remove MockData references — Stream 6 handles that

## Files to Modify

```
app/src/main/res/values/strings.xml                          (add all string resources)
app/src/main/java/com/iamonzon/dory/ui/dashboard/DashboardScreen.kt   (string extraction)
app/src/main/java/com/iamonzon/dory/ui/dashboard/ItemEditScreen.kt    (string extraction)
app/src/main/java/com/iamonzon/dory/ui/creation/ItemCreationScreen.kt (string extraction)
app/src/main/java/com/iamonzon/dory/ui/review/ReviewScreen.kt         (string extraction)
app/src/main/java/com/iamonzon/dory/ui/review/ReviewSessionScreen.kt  (string extraction)
app/src/main/java/com/iamonzon/dory/ui/profile/ProfileScreen.kt       (string extraction)
app/src/main/java/com/iamonzon/dory/ui/profile/CategoryManagementScreen.kt (string extraction)
app/src/main/java/com/iamonzon/dory/ui/profile/ArchivedItemsScreen.kt (string extraction)
app/src/main/java/com/iamonzon/dory/ui/profile/AdvancedSettingsScreen.kt (string extraction)
app/src/main/java/com/iamonzon/dory/ui/onboarding/OnboardingScreen.kt (real content)
app/src/main/java/com/iamonzon/dory/ui/components/ItemCard.kt         (content descriptions)
app/src/main/java/com/iamonzon/dory/ui/components/UrgencyIndicator.kt (content descriptions)
```

## Task Breakdown

### 1. String Resources Extraction

Find all hardcoded user-facing strings in composables and extract them to `strings.xml`. Use `stringResource(R.string.xxx)` in the composable.

**Naming convention:** `screen_element_description`
- `dashboard_title` → "Dashboard"
- `dashboard_empty_message` → "No items yet. Tap the Action Hub to add one!"
- `dashboard_menu_edit` → "Edit"
- `dashboard_menu_archive` → "Archive"
- `dashboard_menu_delete` → "Delete"
- `review_title` → "Review"
- `review_history_title` → "Review History"
- `review_no_reviews` → "No reviews yet"
- `review_new_review_title` → "New Review"
- `review_notes_label` → "Notes (optional)"
- `review_rating_again` → "Again"
- `review_rating_hard` → "Hard"
- `review_rating_good` → "Good"
- `review_rating_easy` → "Easy"
- `creation_title` → "Add New Item"
- `creation_step_format` → "Step %1$d of %2$d"
- `creation_title_prompt` → "What do you want to remember?"
- `creation_title_label` → "Title"
- `creation_source_prompt` → "Where did you learn this?"
- `creation_source_label` → "Source (URL, book, etc.)"
- `creation_more_options` → "More options"
- `creation_category_prompt` → "Pick a category"
- `creation_category_label` → "Category"
- `creation_notes_prompt` → "Any notes to remember?"
- `creation_notes_label` → "Notes (optional)"
- `creation_back` → "Back"
- `creation_next` → "Next"
- `creation_save` → "Save"
- etc. for all screens

Use `plurals` for count-dependent strings:
- `review_session_count` → "%d items to review" (with proper plural forms)

Use string formatting for dynamic content:
- `toast_item_archived` → "Item archived"
- `toast_item_deleted` → "Item deleted"
- `toast_item_saved` → "Item saved!"
- `toast_reviewed_as` → "Reviewed as %1$s"
- `category_item_count` → "%d items"
- `profile_notification_time` → "%1$d:%2$02d AM"

### 2. Onboarding Real Content

Replace the placeholder onboarding pages with polished content about the forgetting curve and spaced repetition. The three pages should be:

**Page 1: "Welcome to Dory"**
- "You forget 70% of what you learn within 24 hours. Dory uses spaced repetition to beat the forgetting curve."

**Page 2: "Add What Matters"**
- "Save concepts, articles, and ideas you want to remember. Organize them by category to keep things tidy."

**Page 3: "Review at the Right Time"**
- "Dory uses the FSRS algorithm to schedule reviews at the optimal moment — right before you'd forget. Rate each review and watch your mastery grow."

### 3. Accessibility Content Descriptions

Add `contentDescription` to all `Icon` composables and meaningful images:
- Urgency indicators: "Overdue", "Due today", "Not due" (already partially done in `UrgencyIndicator.kt` — verify)
- Navigation icons: describe their purpose
- Action icons: "Edit item", "Delete item", "Archive item", "Add category", "Restore item"
- Rating buttons: ensure they have accessible labels
- Ensure all clickable cards have meaningful descriptions via `semantics { }`

### 4. Verify Colorblind-Safe Indicators

The urgency indicator already uses distinct icons per level. Verify:
- Overdue: uses a distinct icon shape + red
- DueToday: uses a distinct icon shape + yellow/amber
- NotDue: uses a distinct icon shape + green

If the icons are identical with only color differences, add distinct shapes.

## Existing Code Reference

Read these files to find all hardcoded strings:
- All files in `ui/dashboard/`, `ui/creation/`, `ui/review/`, `ui/profile/`, `ui/onboarding/`
- All files in `ui/components/`

Current `strings.xml` location: `app/src/main/res/values/strings.xml`

## Acceptance Criteria

1. No user-facing hardcoded strings remain in composable files (all extracted to `strings.xml`)
2. Onboarding has real, polished content about spaced repetition
3. All `Icon` composables have non-null `contentDescription`
4. String naming follows `screen_element_description` convention
5. `./gradlew assembleDebug` compiles successfully
6. Plurals used where counts vary
7. Function signatures in composables are NOT changed (no parameter additions/removals)
