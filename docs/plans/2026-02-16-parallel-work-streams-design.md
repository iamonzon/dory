# Parallel Work Streams Design

## Context

Phases 1-3 of Dory are complete: domain models, FSRS algorithm, UI shells with mock data, Room database, DAOs, and repositories. The remaining work (Phases 4-6) can be parallelized across independent work streams running in separate git worktrees.

## Approach: Layer Isolation with Conflict-Free Merges

Each stream creates **only new files**. No stream modifies existing files. This guarantees zero merge conflicts when merging branches back to main.

A final integration stream (Stream 6) runs after streams 1-5 are merged and wires everything together by modifying the shared files: `AppContainer.kt`, `DoryNavGraph.kt`, and each screen composable.

## Stream Overview

| Stream | Branch | Scope | Parallel? |
|--------|--------|-------|-----------|
| 1 | `stream/dashboard-review` | DashboardViewModel, ReviewViewModel, ReviewSessionViewModel | Yes |
| 2 | `stream/creation-profile` | CreationViewModel, ProfileViewModel | Yes |
| 3 | `stream/settings-support` | CategoryManagementViewModel, ArchivedItemsViewModel, AdvancedSettingsViewModel | Yes |
| 4 | `stream/notifications` | NotificationChannels, DailyDigestWorker, DailyDigestScheduler | Yes |
| 5 | `stream/polish` | strings.xml, onboarding content, accessibility content descriptions | Yes |
| 6 | `stream/integration` | Wire ViewModels into screens, AppContainer, NavGraph | After 1-5 |

## Execution

- Each stream runs as a Claude Code agent in its own git worktree
- Streams 1-5 run in parallel with no dependencies between them
- Stream 6 runs sequentially after all 1-5 branches are merged to main
- Orchestrator script in `docs/streams/README.md` handles worktree creation

## Key Constraint

**No stream (1-5) may modify any existing file.** Each stream only creates new files in its designated locations. This is enforced by the stream prompts and verified at merge time.
