package com.iamonzon.dory.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FsrsTest {

    private lateinit var fsrs: Fsrs
    private val w = FsrsParameters.DEFAULT_WEIGHTS

    @Before
    fun setup() {
        fsrs = Fsrs()
    }

    // --- Retrievability ---

    @Test
    fun `retrievability at t=0 is 1_0`() {
        assertEquals(1.0, fsrs.retrievability(0.0, 10.0), 0.001)
    }

    @Test
    fun `retrievability at t=S is 0_9`() {
        val s = 10.0
        assertEquals(0.9, fsrs.retrievability(s, s), 0.001)
    }

    @Test
    fun `retrievability decreases over time`() {
        val s = 5.0
        val r1 = fsrs.retrievability(1.0, s)
        val r2 = fsrs.retrievability(3.0, s)
        val r3 = fsrs.retrievability(10.0, s)
        assertTrue("R should decrease over time", r1 > r2 && r2 > r3)
    }

    @Test
    fun `retrievability with zero stability returns 0`() {
        assertEquals(0.0, fsrs.retrievability(5.0, 0.0), 0.001)
    }

    // --- Interval ---

    @Test
    fun `interval minimum is 1 day`() {
        assertEquals(1, fsrs.nextInterval(0.01))
    }

    @Test
    fun `interval increases with stability`() {
        val i1 = fsrs.nextInterval(1.0)
        val i2 = fsrs.nextInterval(10.0)
        val i3 = fsrs.nextInterval(100.0)
        assertTrue("Interval should increase with stability", i1 < i2 && i2 < i3)
    }

    @Test
    fun `higher retention means shorter intervals`() {
        val s = 20.0
        val iLow = fsrs.nextInterval(s, 0.80)
        val iHigh = fsrs.nextInterval(s, 0.95)
        assertTrue("Higher retention should give shorter interval", iHigh < iLow)
    }

    @Test
    fun `interval at R=0_9 and S approximately equals S`() {
        // When desired retention is 0.9, the interval should be approximately equal to S
        // because R(S, S) = 0.9 by construction
        val s = 10.0
        val interval = fsrs.nextInterval(s, 0.9)
        // Allow some rounding tolerance
        assertTrue("Interval at R=0.9 should be close to S", interval in 9..11)
    }

    // --- Initial Stability ---

    @Test
    fun `initial stability matches first 4 parameters`() {
        assertEquals(w[0], fsrs.initialStability(Rating.Again), 0.001)
        assertEquals(w[1], fsrs.initialStability(Rating.Hard), 0.001)
        assertEquals(w[2], fsrs.initialStability(Rating.Good), 0.001)
        assertEquals(w[3], fsrs.initialStability(Rating.Easy), 0.001)
    }

    @Test
    fun `initial stability increases with better rating`() {
        val sAgain = fsrs.initialStability(Rating.Again)
        val sHard = fsrs.initialStability(Rating.Hard)
        val sGood = fsrs.initialStability(Rating.Good)
        val sEasy = fsrs.initialStability(Rating.Easy)
        assertTrue(sAgain < sHard)
        assertTrue(sHard < sGood)
        assertTrue(sGood < sEasy)
    }

    // --- Initial Difficulty ---

    @Test
    fun `initial difficulty decreases or stays equal with better rating`() {
        val dAgain = fsrs.initialDifficulty(Rating.Again)
        val dHard = fsrs.initialDifficulty(Rating.Hard)
        val dGood = fsrs.initialDifficulty(Rating.Good)
        val dEasy = fsrs.initialDifficulty(Rating.Easy)
        // With default params, Good and Easy both clamp to 1.0 (floor)
        assertTrue("Again should be >= Hard", dAgain >= dHard)
        assertTrue("Hard should be >= Good", dHard >= dGood)
        assertTrue("Good should be >= Easy", dGood >= dEasy)
        // Verify the non-clamped ones are strictly ordered
        assertTrue("Again should be strictly > Hard", dAgain > dHard)
    }

    @Test
    fun `initial difficulty is clamped between 1 and 10`() {
        for (rating in Rating.entries) {
            val d = fsrs.initialDifficulty(rating)
            assertTrue("D=$d should be >= 1", d >= 1.0)
            assertTrue("D=$d should be <= 10", d <= 10.0)
        }
    }

    // --- Difficulty Update ---

    @Test
    fun `difficulty increases when rating is Again`() {
        val currentD = 5.0
        val newD = fsrs.nextDifficulty(currentD, Rating.Again)
        assertTrue("Again should increase difficulty", newD > currentD)
    }

    @Test
    fun `difficulty decreases when rating is Easy`() {
        val currentD = 5.0
        val newD = fsrs.nextDifficulty(currentD, Rating.Easy)
        assertTrue("Easy should decrease difficulty", newD < currentD)
    }

    @Test
    fun `difficulty stays similar when rating is Good`() {
        // Good is the reference point (G=3), so ΔD = -w6*(3-3) = 0
        // Only mean reversion applies
        val currentD = 5.0
        val newD = fsrs.nextDifficulty(currentD, Rating.Good)
        // Should be close to current due to mean reversion toward D0(Good)
        assertTrue("Good should not change D dramatically", kotlin.math.abs(newD - currentD) < 1.0)
    }

    @Test
    fun `difficulty is always clamped between 1 and 10`() {
        // Start at extremes
        val dFromLow = fsrs.nextDifficulty(1.0, Rating.Easy)
        val dFromHigh = fsrs.nextDifficulty(10.0, Rating.Again)
        assertTrue(dFromLow >= 1.0)
        assertTrue(dFromHigh <= 10.0)
    }

    @Test
    fun `mean reversion prevents extreme drift`() {
        // Repeatedly rate Again from a moderate difficulty
        var d = 5.0
        repeat(100) {
            d = fsrs.nextDifficulty(d, Rating.Again)
        }
        // Mean reversion pulls toward D0(Good), preventing D from reaching 10
        // With default params, D converges around 7.6-7.8
        assertTrue("D should be clamped at 10", d <= 10.0)
        assertTrue("D should be elevated after many Again", d > 7.0)
        // Verify convergence: further Again ratings don't change D much
        val dNext = fsrs.nextDifficulty(d, Rating.Again)
        assertTrue("D should have converged", kotlin.math.abs(dNext - d) < 0.05)
    }

    // --- Stability After Recall ---

    @Test
    fun `stability after recall is greater than or equal to current stability`() {
        val d = 5.0
        val s = 10.0
        val r = 0.9
        for (rating in listOf(Rating.Hard, Rating.Good, Rating.Easy)) {
            val newS = fsrs.stabilityAfterRecall(d, s, r, rating)
            assertTrue("S should not decrease on recall ($rating)", newS >= s)
        }
    }

    @Test
    fun `easy gives bigger stability increase than good`() {
        val d = 5.0
        val s = 10.0
        val r = 0.9
        val sGood = fsrs.stabilityAfterRecall(d, s, r, Rating.Good)
        val sEasy = fsrs.stabilityAfterRecall(d, s, r, Rating.Easy)
        assertTrue("Easy should increase S more than Good", sEasy > sGood)
    }

    @Test
    fun `hard gives smaller stability increase than good`() {
        val d = 5.0
        val s = 10.0
        val r = 0.9
        val sHard = fsrs.stabilityAfterRecall(d, s, r, Rating.Hard)
        val sGood = fsrs.stabilityAfterRecall(d, s, r, Rating.Good)
        assertTrue("Hard should increase S less than Good", sGood > sHard)
    }

    @Test
    fun `lower retrievability gives bigger stability increase`() {
        // Reviewing when you've almost forgotten is worth more
        val d = 5.0
        val s = 10.0
        val sHighR = fsrs.stabilityAfterRecall(d, s, 0.95, Rating.Good)
        val sLowR = fsrs.stabilityAfterRecall(d, s, 0.5, Rating.Good)
        assertTrue("Lower R should give bigger S increase", sLowR > sHighR)
    }

    // --- Stability After Lapse ---

    @Test
    fun `stability after lapse is less than current stability`() {
        val d = 5.0
        val s = 10.0
        val r = 0.5
        val newS = fsrs.stabilityAfterLapse(d, s, r)
        assertTrue("S should decrease on lapse", newS < s)
    }

    @Test
    fun `stability after lapse has a floor of 0_01`() {
        val newS = fsrs.stabilityAfterLapse(10.0, 0.02, 0.1)
        assertTrue("S should not go below 0.01", newS >= 0.01)
    }

    @Test
    fun `higher difficulty means lower post-lapse stability`() {
        val s = 10.0
        val r = 0.5
        val sLowD = fsrs.stabilityAfterLapse(2.0, s, r)
        val sHighD = fsrs.stabilityAfterLapse(8.0, s, r)
        assertTrue("Higher D should give lower post-lapse S", sHighD < sLowD)
    }

    // --- nextStability delegates correctly ---

    @Test
    fun `nextStability delegates Again to stabilityAfterLapse`() {
        val d = 5.0
        val s = 10.0
        val r = 0.5
        val fromNext = fsrs.nextStability(d, s, r, Rating.Again)
        val fromLapse = fsrs.stabilityAfterLapse(d, s, r)
        assertEquals(fromLapse, fromNext, 0.0001)
    }

    @Test
    fun `nextStability delegates Good to stabilityAfterRecall`() {
        val d = 5.0
        val s = 10.0
        val r = 0.9
        val fromNext = fsrs.nextStability(d, s, r, Rating.Good)
        val fromRecall = fsrs.stabilityAfterRecall(d, s, r, Rating.Good)
        assertEquals(fromRecall, fromNext, 0.0001)
    }

    // --- Full Review Flow ---

    @Test
    fun `first review returns initial S and D`() {
        val state = fsrs.review(null, 0.0, Rating.Good)
        assertEquals(w[2], state.stability, 0.001) // w[2] = initial S for Good
        assertTrue(state.difficulty in 1.0..10.0)
        assertTrue(state.interval >= 1)
    }

    @Test
    fun `subsequent review updates S and D`() {
        val first = fsrs.review(null, 0.0, Rating.Good)
        val second = fsrs.review(first, 3.0, Rating.Good)
        assertTrue("S should increase on successful review", second.stability > first.stability)
    }

    @Test
    fun `again rating on subsequent review decreases stability`() {
        val first = fsrs.review(null, 0.0, Rating.Good)
        val second = fsrs.review(first, 3.0, Rating.Again)
        assertTrue("S should decrease on Again", second.stability < first.stability)
    }

    // --- Scheduling Scenarios ---

    @Test
    fun `consistent Good ratings lead to increasing intervals`() {
        var state: SchedulingState? = null
        val intervals = mutableListOf<Int>()

        // Simulate 5 reviews, each done at the scheduled interval, all rated Good
        repeat(5) {
            val elapsed = state?.interval?.toDouble() ?: 0.0
            state = fsrs.review(state, elapsed, Rating.Good)
            intervals.add(state!!.interval)
        }

        // Each interval should be >= the previous
        for (i in 1 until intervals.size) {
            assertTrue(
                "Interval ${intervals[i]} should be >= ${intervals[i - 1]}",
                intervals[i] >= intervals[i - 1]
            )
        }
    }

    @Test
    fun `again after multiple good reviews reduces interval significantly`() {
        var state: SchedulingState? = null

        // Build up stability with 4 Good reviews
        repeat(4) {
            val elapsed = state?.interval?.toDouble() ?: 0.0
            state = fsrs.review(state, elapsed, Rating.Good)
        }

        val stableInterval = state!!.interval

        // Now rate Again
        state = fsrs.review(state, state!!.interval.toDouble(), Rating.Again)
        assertTrue(
            "Interval after Again should be much less than before ($stableInterval)",
            state!!.interval < stableInterval
        )
    }

    @Test
    fun `easy ratings lead to longer intervals than good`() {
        val stateGood = fsrs.review(null, 0.0, Rating.Good)
        val stateEasy = fsrs.review(null, 0.0, Rating.Easy)
        assertTrue(
            "Easy first review should give longer interval than Good",
            stateEasy.interval >= stateGood.interval
        )
    }

    @Test
    fun `custom retention affects intervals`() {
        val fsrsHigh = Fsrs(FsrsParameters(desiredRetention = 0.95))
        val fsrsLow = Fsrs(FsrsParameters(desiredRetention = 0.80))

        val stateHigh = fsrsHigh.review(null, 0.0, Rating.Good)
        val stateLow = fsrsLow.review(null, 0.0, Rating.Good)

        assertTrue(
            "Higher retention should give shorter interval",
            stateHigh.interval <= stateLow.interval
        )
    }
}
