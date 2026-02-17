# Parallel Work Streams — Orchestrator

## Overview

This directory contains self-contained task definitions for 6 work streams that parallelize Phases 4-6 of Dory. Streams 1-5 run in parallel in separate git worktrees. Stream 6 runs after merging 1-5.

## Setup & Launch Script

The script below is idempotent — re-running it skips already-existing worktrees and branches.

```bash
#!/usr/bin/env bash
set -euo pipefail

# --- Configuration ---
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORKTREE_DIR="${REPO_ROOT}/.worktrees"
BASE_BRANCH="main"
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export JAVA_HOME

STREAMS=(
    "stream/dashboard-review:stream-1-dashboard-review"
    "stream/creation-profile:stream-2-creation-profile"
    "stream/settings-support:stream-3-settings-support"
    "stream/notifications:stream-4-notifications"
    "stream/polish:stream-5-polish"
)

# --- Functions ---
ensure_on_main() {
    local current
    current=$(git -C "$REPO_ROOT" branch --show-current)
    if [[ "$current" != "$BASE_BRANCH" ]]; then
        echo "ERROR: Must be on '$BASE_BRANCH' branch (currently on '$current')"
        exit 1
    fi
}

ensure_clean() {
    if ! git -C "$REPO_ROOT" diff --quiet HEAD; then
        echo "ERROR: Working tree has uncommitted changes. Commit or stash first."
        exit 1
    fi
}

create_worktree() {
    local branch="$1"
    local stream_doc="$2"
    local wt_path="${WORKTREE_DIR}/${branch//\//-}"

    # Skip if worktree already exists
    if [[ -d "$wt_path" ]]; then
        echo "SKIP: Worktree already exists at $wt_path"
        return
    fi

    # Create branch if it doesn't exist
    if ! git -C "$REPO_ROOT" rev-parse --verify "$branch" &>/dev/null; then
        git -C "$REPO_ROOT" branch "$branch" "$BASE_BRANCH"
        echo "  Created branch: $branch"
    else
        echo "  Branch already exists: $branch"
    fi

    # Create worktree
    mkdir -p "$WORKTREE_DIR"
    git -C "$REPO_ROOT" worktree add "$wt_path" "$branch"
    echo "  Created worktree: $wt_path"
}

# --- Main ---
echo "=== Dory Parallel Work Streams Setup ==="
echo ""

ensure_on_main
ensure_clean

echo "Base commit: $(git -C "$REPO_ROOT" rev-parse --short HEAD)"
echo ""

for entry in "${STREAMS[@]}"; do
    IFS=':' read -r branch stream_doc <<< "$entry"
    echo "--- Setting up: $branch ---"
    create_worktree "$branch" "$stream_doc"
    echo ""
done

echo "=== Setup Complete ==="
echo ""
echo "Worktrees created in: $WORKTREE_DIR"
echo ""

# --- Print launch commands ---
echo "=== Launch Commands ==="
echo ""
echo "Run these commands to dispatch agents (each in a separate terminal):"
echo ""

for entry in "${STREAMS[@]}"; do
    IFS=':' read -r branch stream_doc <<< "$entry"
    wt_path="${WORKTREE_DIR}/${branch//\//-}"
    doc_path="${REPO_ROOT}/docs/streams/${stream_doc}.md"
    echo "# Stream: $branch"
    echo "cd \"$wt_path\" && claude --print \"Read the task definition at $doc_path and execute it fully. Create all specified files, implement all code, write all tests, and verify with ./gradlew test. Do not modify any existing files unless the task doc explicitly says to. Commit your work when done.\""
    echo ""
done

echo "=== Post-Completion ==="
echo ""
echo "After all streams complete:"
echo "  1. Review each branch: git log main..<branch>"
echo "  2. Merge each branch:  git merge <branch>"
echo "  3. Remove worktrees:   git worktree remove <path>"
echo "  4. Run Stream 6 on main (integration)"
```

## How to Use

### 1. Run the setup script

```bash
cd /Users/ivan/Personal/Projects/dory/android
bash docs/streams/README.md  # Won't work — copy the script block above to a file first
```

Or extract and run:

```bash
cd /Users/ivan/Personal/Projects/dory/android

# Create the launcher script
cat > /tmp/dory-streams-setup.sh << 'SCRIPT_EOF'
# (paste the script block above)
SCRIPT_EOF

bash /tmp/dory-streams-setup.sh
```

### 2. Launch agents

Run the printed `claude` commands, each in a separate terminal. Each agent:
- Works in its own worktree (isolated filesystem)
- Reads its task doc for instructions
- Creates only the specified files
- Runs tests to verify
- Commits its work

### 3. After all agents complete

```bash
cd /Users/ivan/Personal/Projects/dory/android

# Review each branch
for branch in stream/dashboard-review stream/creation-profile stream/settings-support stream/notifications stream/polish; do
    echo "=== $branch ==="
    git log --oneline main..$branch
    echo ""
done

# Merge each branch (one at a time, resolve any conflicts)
git checkout main
git merge stream/dashboard-review
git merge stream/creation-profile
git merge stream/settings-support
git merge stream/notifications
git merge stream/polish

# Clean up worktrees
git worktree list
git worktree remove .worktrees/stream-dashboard-review
git worktree remove .worktrees/stream-creation-profile
git worktree remove .worktrees/stream-settings-support
git worktree remove .worktrees/stream-notifications
git worktree remove .worktrees/stream-polish

# Prune stale worktree references
git worktree prune
```

### 4. Run Stream 6 (Integration)

After merging all 5 branches:

```bash
cd /Users/ivan/Personal/Projects/dory/android
claude --print "Read the task definition at docs/streams/stream-6-integration.md and execute it fully. Wire all ViewModels into screens, update AppContainer and DoryNavGraph, set up notifications, handle onboarding flow, delete MockData. Run ./gradlew assembleDebug to verify. Commit your work when done."
```

## Stream Summary

| Stream | Branch | Parallel | Description |
|--------|--------|----------|-------------|
| 1 | `stream/dashboard-review` | Yes | DashboardViewModel, ReviewViewModel, ReviewSessionViewModel |
| 2 | `stream/creation-profile` | Yes | CreationViewModel, ProfileViewModel |
| 3 | `stream/settings-support` | Yes | CategoryManagement, ArchivedItems, AdvancedSettings ViewModels |
| 4 | `stream/notifications` | Yes | NotificationChannels, DailyDigestWorker, DailyDigestScheduler |
| 5 | `stream/polish` | Yes | String extraction, onboarding content, accessibility |
| 6 | `stream/integration` | After 1-5 | Wire everything together, delete MockData |
