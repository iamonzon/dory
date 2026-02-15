package com.iamonzon.dory.algorithm

/**
 * FSRS 4-point rating scale.
 * Maps directly to FSRS grade values (1-4).
 */
enum class Rating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4);

    companion object {
        fun fromValue(value: Int): Rating =
            entries.first { it.value == value }
    }
}
