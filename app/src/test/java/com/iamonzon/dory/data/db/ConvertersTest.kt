package com.iamonzon.dory.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `Instant to Long round-trip`() {
        val instant = Instant.now()
        val epochMilli = converters.fromInstant(instant)
        val roundTripped = converters.toInstant(epochMilli)

        assertEquals(instant.toEpochMilli(), roundTripped?.toEpochMilli())
    }

    @Test
    fun `specific Instant to Long and back`() {
        val instant = Instant.parse("2025-01-15T10:30:00Z")
        val epochMilli = converters.fromInstant(instant)
        val roundTripped = converters.toInstant(epochMilli)

        assertEquals(instant, roundTripped)
    }

    @Test
    fun `null Instant to null Long`() {
        assertNull(converters.fromInstant(null))
    }

    @Test
    fun `null Long to null Instant`() {
        assertNull(converters.toInstant(null))
    }

    @Test
    fun `epoch zero round-trips correctly`() {
        val epoch = Instant.EPOCH
        val epochMilli = converters.fromInstant(epoch)
        val roundTripped = converters.toInstant(epochMilli)

        assertEquals(0L, epochMilli)
        assertEquals(epoch, roundTripped)
    }

    @Test
    fun `negative epoch millis round-trips correctly`() {
        val beforeEpoch = Instant.ofEpochMilli(-86400000L) // 1 day before epoch
        val epochMilli = converters.fromInstant(beforeEpoch)
        val roundTripped = converters.toInstant(epochMilli)

        assertEquals(-86400000L, epochMilli)
        assertEquals(beforeEpoch, roundTripped)
    }
}
