package com.iamonzon.dory.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FsrsParametersTest {

    @Test
    fun `default parameters has 17 weights`() {
        val params = FsrsParameters.DEFAULT
        assertEquals(17, params.w.size)
    }

    @Test
    fun `default desired retention is 0_9`() {
        assertEquals(0.9, FsrsParameters.DEFAULT.desiredRetention, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects fewer than 17 weights`() {
        FsrsParameters(w = doubleArrayOf(1.0, 2.0, 3.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects desired retention below 0_70`() {
        FsrsParameters(desiredRetention = 0.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects desired retention above 0_97`() {
        FsrsParameters(desiredRetention = 0.99)
    }

    @Test
    fun `equality works with same weights`() {
        val a = FsrsParameters.DEFAULT
        val b = FsrsParameters(w = FsrsParameters.DEFAULT_WEIGHTS.copyOf())
        assertEquals(a, b)
    }

    @Test
    fun `inequality works with different weights`() {
        val a = FsrsParameters.DEFAULT
        val modified = FsrsParameters.DEFAULT_WEIGHTS.copyOf().also { it[0] = 999.0 }
        val b = FsrsParameters(w = modified)
        assertNotEquals(a, b)
    }
}
