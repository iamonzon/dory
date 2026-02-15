# Dory - Spaced Repetition Algorithm

## Overview

Dory uses **FSRS-4.5** (Free Spaced Repetition Scheduler, version 4.5) as its spaced repetition algorithm. FSRS is based on the DSR (Difficulty, Stability, Retrievability) memory model and has been shown to outperform SM-2 and other traditional algorithms across large-scale benchmarks.

The implementation is written from scratch in pure Kotlin with no Android dependencies, making it fully unit-testable and independent of the app framework.

Reference: [The Algorithm (open-spaced-repetition wiki)](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm)

## Why FSRS-4.5

- Outperforms SM-2 in scheduling accuracy
- Configurable desired retention lets users make explicit tradeoffs between review load and retention rate
- Default parameters were optimized across hundreds of millions of reviews
- Per-item difficulty and stability tracking adapts to each concept
- Well-suited for applied pattern learning (DSA patterns, system design, AI concepts) where knowledge solidifies over many review cycles

## Rating Scale

FSRS's native 4-point scale:

| Rating | Label | Meaning |
|--------|-------|---------|
| 1 | Again | Completely forgot. Could not apply this pattern at all. |
| 2 | Hard | Recalled with significant difficulty. Shaky understanding. |
| 3 | Good | Recalled with some effort. Could apply with reference. |
| 4 | Easy | Recalled effortlessly. Could apply in a new problem right now. |

This replaces the 1-10 scale from the original spec. Fewer choices = faster reviews, and the scale maps directly to FSRS without a translation layer.

## Memory Model

FSRS tracks three variables per item:

### Difficulty (D)
- Range: 1.0 (easiest) to 10.0 (hardest)
- Represents the inherent complexity of the item
- Updated after each review based on the rating given
- Uses mean reversion to prevent extreme drift

### Stability (S)
- Unit: days
- The time interval after which retrievability drops to 90%
- Higher stability = slower forgetting = longer intervals between reviews
- Updated after each review (increases on successful recall, resets on lapse)

### Retrievability (R)
- Range: 0.0 to 1.0
- The current probability of successful recall
- Decays over time since the last review according to the forgetting curve
- Not stored — computed on demand from stability and elapsed time

## Core Formulas

### Constants

```
DECAY = -0.5
FACTOR = 19/81  (≈ 0.2346)
```

### Forgetting Curve (Retrievability)

```
R(t, S) = (1 + FACTOR * t / S) ^ DECAY
```

Where `t` = days since last review, `S` = current stability.

When `t = S`, `R = 0.9` (90%) by construction.

### Interval Calculation

Given a desired retention rate `r` and current stability `S`:

```
I(r, S) = (S / FACTOR) * (r^(1/DECAY) - 1)
```

The interval is rounded to the nearest integer (days). Minimum interval is 1 day.

### Initial Stability (First Review)

For an item's first-ever review, the initial stability depends on the rating:

```
S0(G) = w[G-1]
```

Where `G` is the rating (1-4) and `w[0]` through `w[3]` are the first four parameters.

With default parameters:
| Rating | Initial Stability (days) |
|--------|-------------------------|
| Again  | 0.4872                  |
| Hard   | 1.4003                  |
| Good   | 3.7145                  |
| Easy   | 13.8206                 |

### Initial Difficulty (First Review)

```
D0(G) = w4 - e^(w5 * (G - 1)) + 1
```

Clamped to range [1, 10].

### Difficulty Update (Subsequent Reviews)

Three steps applied after each review:

1. **Grade-based change**: `ΔD = -w6 * (G - 3)`
2. **Linear damping**: `D' = D + ΔD * (10 - D) / 9`
3. **Mean reversion**: `D'' = w7 * D0(3) + (1 - w7) * D'`

The mean reversion prevents difficulty from drifting to extremes over many reviews. Result is clamped to [1, 10].

### Stability After Successful Recall

When the user rates Hard, Good, or Easy (G >= 2):

```
S'r(D, S, R, G) = S * (e^(w8) * (11 - D) * S^(-w9) * (e^(w10 * (1 - R)) - 1) * hard_penalty * easy_bonus + 1)
```

Where:
- `hard_penalty = w15` if G = 2, else 1
- `easy_bonus = w16` if G = 4, else 1

The result is constrained so that `S'r >= S` (stability can only increase on successful recall).

### Stability After Lapse (Forgetting)

When the user rates Again (G = 1):

```
S'f(D, S, R) = w11 * D^(-w12) * ((S + 1)^w13 - 1) * e^(w14 * (1 - R))
```

The result is constrained so that `S'f < S` (stability always decreases on lapse) and `S'f >= 0.01`.

## Default Parameters

FSRS-4.5 uses 17 parameters (w0 through w16):

```
w = [0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031,
     1.6474, 0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755]
```

| Index | Parameter | Used In |
|-------|-----------|---------|
| w0    | Initial stability for Again | S0 |
| w1    | Initial stability for Hard | S0 |
| w2    | Initial stability for Good | S0 |
| w3    | Initial stability for Easy | S0 |
| w4    | Initial difficulty base | D0 |
| w5    | Initial difficulty scaling | D0 |
| w6    | Difficulty update rate | D update |
| w7    | Mean reversion weight | D update |
| w8    | Stability increase base | S'r |
| w9    | Stability-dependent factor | S'r |
| w10   | Retrievability-dependent factor | S'r |
| w11   | Post-lapse stability base | S'f |
| w12   | Difficulty factor for lapse | S'f |
| w13   | Stability factor for lapse | S'f |
| w14   | Retrievability factor for lapse | S'f |
| w15   | Hard penalty | S'r |
| w16   | Easy bonus | S'r |

## Configuration

### Global Defaults (Profile/Settings)

| Setting | Default | Range | Description |
|---------|---------|-------|-------------|
| Desired retention | 0.9 | 0.70 - 0.97 | Target probability of recall at review time |
| FSRS parameters | Published defaults | Per parameter | The 17 w-parameters |

### Per-Category Overrides

Each category can optionally override:
- **Desired retention** — e.g., 0.95 for DSA patterns, 0.85 for general concepts
- **FSRS parameters** — for fine-tuning specific subject areas

If a category does not have overrides, it inherits the global defaults. Uncategorized items always use global defaults.

## Mastery and Struggling Definitions

### Mastered

An item is considered **mastered** when:

```
Stability (S) > 90 days
```

This means the user would retain the item with 90% probability after 90 days without review. The item has been deeply learned.

### Struggling

An item is considered **struggling** when:

```
Stability (S) < 3 days AND last review rating was Again (1)
```

This captures items that the user repeatedly fails to retain — they forget quickly and have recently failed recall.

### Neither

Items that are neither mastered nor struggling are in the normal learning progression.

## Scheduling Behavior

### New Items (No Reviews)

- Treated as due immediately upon creation
- On the dashboard, they appear as Yellow (due today)
- The first review establishes initial S and D values

### After Each Review

1. Compute new Difficulty (D) from old D and rating
2. Compute new Stability (S) from old S, D, R, and rating
3. Compute next interval: `I(desired_retention, new_S)`
4. Next review date = today + interval

### Storing Computed State

After each review, the computed S and D values are stored on the Review record:

| Field on Review | Description |
|----------------|-------------|
| stabilityAfter | The stability value computed after this review |
| difficultyAfter | The difficulty value computed after this review |

This avoids recomputing from the full history every time. The current S and D for an item are simply the values from its most recent Review. If no reviews exist, the item has no S/D yet (first review will initialize them).

### Editing Past Reviews

When a user edits one of the last 3 reviews (changes the rating):
1. Recompute S and D forward from that review onward
2. All subsequent Review records have their S and D updated
3. The item's effective next review date changes accordingly

## Algorithm Interface

The FSRS implementation exposes a minimal API:

```kotlin
class Fsrs(private val parameters: FsrsParameters = FsrsParameters.DEFAULT) {

    fun initialStability(rating: Rating): Double
    fun initialDifficulty(rating: Rating): Double

    fun nextDifficulty(currentD: Double, rating: Rating): Double
    fun nextStability(currentD: Double, currentS: Double, retrievability: Double, rating: Rating): Double

    fun retrievability(elapsedDays: Double, stability: Double): Double
    fun nextInterval(desiredRetention: Double, stability: Double): Int
}
```

- `Rating` is an enum: `Again`, `Hard`, `Good`, `Easy`
- `FsrsParameters` holds w0-w16 and desired retention
- All methods are pure functions with no side effects

## Testing Strategy

Unit tests must cover:
- Each formula in isolation (initial S, initial D, D update, S after recall, S after lapse, retrievability, interval)
- Edge cases: minimum/maximum D clamping, S floor on lapse, S non-decrease on recall
- Full scheduling scenarios: simulate a sequence of reviews and verify intervals match expected behavior
- Parameter configuration: verify per-category overrides take effect
- Rating edit cascading: verify S/D are correctly recomputed when a past rating changes

## Same-Day Multiple Reviews

Multiple reviews of the same item in a single day are **allowed and handled naturally**:
- Elapsed time (t) since the last review is near zero (fraction of a day)
- Retrievability is ~1.0 (just reviewed moments ago)
- The stability increase factor `e^(w10 * (1 - R)) - 1` approaches 0, so stability barely changes
- The computed next interval still pushes the item to at least the next day
- The item is **not re-shown as due** after a same-day review

No special-case logic is needed — the FSRS formulas handle this gracefully.

## What This Spec Does NOT Cover

- Parameter optimization from review history (future feature)
- Fuzzing/jitter on intervals (adding slight randomness to prevent review clustering)
