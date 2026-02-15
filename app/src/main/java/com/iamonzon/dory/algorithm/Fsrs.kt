package com.iamonzon.dory.algorithm

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS-4.5 spaced repetition algorithm.
 *
 * Pure Kotlin implementation with no Android dependencies.
 * All methods are pure functions with no side effects.
 *
 * Reference: https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm
 */
class Fsrs(private val parameters: FsrsParameters = FsrsParameters.DEFAULT) {

    private val w get() = parameters.w

    // FSRS-4.5 constants
    private val decay = -0.5
    private val factor = 19.0 / 81.0 // ≈ 0.2346

    /**
     * Compute retrievability (probability of recall) after [elapsedDays] since last review,
     * given the current [stability].
     *
     * R(t, S) = (1 + FACTOR * t / S) ^ DECAY
     *
     * When t = S, R = 0.9 by construction.
     */
    fun retrievability(elapsedDays: Double, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        if (elapsedDays <= 0.0) return 1.0
        return (1.0 + factor * elapsedDays / stability).pow(decay)
    }

    /**
     * Compute the next review interval in days for a given [desiredRetention] and [stability].
     *
     * I(r, S) = (S / FACTOR) * (r^(1/DECAY) - 1)
     *
     * Minimum interval is 1 day.
     */
    fun nextInterval(stability: Double, desiredRetention: Double = parameters.desiredRetention): Int {
        if (stability <= 0.0) return 1
        val interval = (stability / factor) * (desiredRetention.pow(1.0 / decay) - 1.0)
        return max(1, interval.roundToInt())
    }

    /**
     * Initial stability for the first review of a new item.
     * S0(G) = w[G-1]
     */
    fun initialStability(rating: Rating): Double {
        return max(0.01, w[rating.value - 1])
    }

    /**
     * Initial difficulty for the first review of a new item.
     * D0(G) = w4 - exp(w5 * (G - 1)) + 1
     * Clamped to [1, 10].
     */
    fun initialDifficulty(rating: Rating): Double {
        val d = w[4] - exp(w[5] * (rating.value - 1)) + 1
        return d.clampDifficulty()
    }

    /**
     * Update difficulty after a subsequent review.
     *
     * 1. Grade-based change: ΔD = -w6 * (G - 3)
     * 2. Linear damping: D' = D + ΔD * (10 - D) / 9
     * 3. Mean reversion: D'' = w7 * D0(3) + (1 - w7) * D'
     *
     * Result clamped to [1, 10].
     */
    fun nextDifficulty(currentD: Double, rating: Rating): Double {
        val deltaD = -w[6] * (rating.value - 3)
        val dPrime = currentD + deltaD * (10.0 - currentD) / 9.0
        val d0Good = w[4] - exp(w[5] * (Rating.Good.value - 1)) + 1
        val dDoublePrime = w[7] * d0Good + (1.0 - w[7]) * dPrime
        return dDoublePrime.clampDifficulty()
    }

    /**
     * Stability after a successful recall (rating >= Hard).
     *
     * S'r(D, S, R, G) = S * (e^w8 * (11 - D) * S^(-w9) * (e^(w10*(1-R)) - 1) * hard_penalty * easy_bonus + 1)
     *
     * Constrained: S'r >= S (stability cannot decrease on successful recall).
     */
    fun stabilityAfterRecall(
        currentD: Double,
        currentS: Double,
        retrievability: Double,
        rating: Rating
    ): Double {
        val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) w[16] else 1.0

        val sinc = exp(w[8]) *
            (11.0 - currentD) *
            currentS.pow(-w[9]) *
            (exp(w[10] * (1.0 - retrievability)) - 1.0) *
            hardPenalty *
            easyBonus

        val newS = currentS * (sinc + 1.0)
        return max(currentS, newS) // S can only increase on success
    }

    /**
     * Stability after a lapse (rating = Again).
     *
     * S'f(D, S, R) = w11 * D^(-w12) * ((S + 1)^w13 - 1) * e^(w14 * (1 - R))
     *
     * Constrained: S'f < S and S'f >= 0.01.
     */
    fun stabilityAfterLapse(
        currentD: Double,
        currentS: Double,
        retrievability: Double
    ): Double {
        val newS = w[11] *
            currentD.pow(-w[12]) *
            ((currentS + 1.0).pow(w[13]) - 1.0) *
            exp(w[14] * (1.0 - retrievability))

        return max(0.01, min(newS, currentS)) // Must decrease, floor at 0.01
    }

    /**
     * Compute the next stability after a review.
     * Delegates to [stabilityAfterRecall] or [stabilityAfterLapse] based on rating.
     */
    fun nextStability(
        currentD: Double,
        currentS: Double,
        retrievability: Double,
        rating: Rating
    ): Double {
        return if (rating == Rating.Again) {
            stabilityAfterLapse(currentD, currentS, retrievability)
        } else {
            stabilityAfterRecall(currentD, currentS, retrievability, rating)
        }
    }

    /**
     * Process a full review and return the updated scheduling state.
     *
     * For the first review of an item ([currentState] is null), initial S and D are computed.
     * For subsequent reviews, S and D are updated based on elapsed time and rating.
     */
    fun review(
        currentState: SchedulingState?,
        elapsedDays: Double,
        rating: Rating,
        desiredRetention: Double = parameters.desiredRetention
    ): SchedulingState {
        if (currentState == null) {
            // First review
            val s = initialStability(rating)
            val d = initialDifficulty(rating)
            return SchedulingState(
                stability = s,
                difficulty = d,
                interval = nextInterval(s, desiredRetention)
            )
        }

        val r = retrievability(elapsedDays, currentState.stability)
        val newD = nextDifficulty(currentState.difficulty, rating)
        val newS = nextStability(currentState.difficulty, currentState.stability, r, rating)

        return SchedulingState(
            stability = newS,
            difficulty = newD,
            interval = nextInterval(newS, desiredRetention)
        )
    }

    private fun Double.clampDifficulty(): Double = max(1.0, min(10.0, this))
}

/**
 * The computed scheduling state after a review.
 */
data class SchedulingState(
    val stability: Double,
    val difficulty: Double,
    val interval: Int
)
