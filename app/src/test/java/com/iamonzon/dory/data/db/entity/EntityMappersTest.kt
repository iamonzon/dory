package com.iamonzon.dory.data.db.entity

import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.Review
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class EntityMappersTest {

    // --- Item round-trip ---

    @Test
    fun `Item domain to entity and back preserves all fields`() {
        val now = Instant.now()
        val item = Item(
            id = 42,
            title = "Test Title",
            source = "test.com",
            categoryId = 5,
            notes = "Some notes",
            createdAt = now,
            isArchived = true
        )

        val entity = item.toEntity()
        val roundTripped = entity.toDomain()

        assertEquals(item.id, roundTripped.id)
        assertEquals(item.title, roundTripped.title)
        assertEquals(item.source, roundTripped.source)
        assertEquals(item.categoryId, roundTripped.categoryId)
        assertEquals(item.notes, roundTripped.notes)
        assertEquals(item.createdAt, roundTripped.createdAt)
        assertEquals(item.isArchived, roundTripped.isArchived)
    }

    @Test
    fun `Item with null optional fields round-trips correctly`() {
        val item = Item(
            title = "Minimal",
            source = "s.com"
        )

        val roundTripped = item.toEntity().toDomain()

        assertNull(roundTripped.categoryId)
        assertNull(roundTripped.notes)
        assertEquals(false, roundTripped.isArchived)
    }

    // --- Review round-trip ---

    @Test
    fun `Review domain to entity and back preserves all fields`() {
        val now = Instant.now()
        val review = Review(
            id = 10,
            itemId = 42,
            rating = Rating.Good,
            notes = "Nice",
            reviewedAt = now,
            stabilityAfter = 5.5,
            difficultyAfter = 3.2
        )

        val roundTripped = review.toEntity().toDomain()

        assertEquals(review.id, roundTripped.id)
        assertEquals(review.itemId, roundTripped.itemId)
        assertEquals(review.rating, roundTripped.rating)
        assertEquals(review.notes, roundTripped.notes)
        assertEquals(review.reviewedAt, roundTripped.reviewedAt)
        assertEquals(review.stabilityAfter, roundTripped.stabilityAfter, 0.001)
        assertEquals(review.difficultyAfter, roundTripped.difficultyAfter, 0.001)
    }

    // --- Rating ↔ Int ---

    @Test
    fun `Rating to Int mapping is correct`() {
        assertEquals(1, Rating.Again.value)
        assertEquals(2, Rating.Hard.value)
        assertEquals(3, Rating.Good.value)
        assertEquals(4, Rating.Easy.value)
    }

    @Test
    fun `Int to Rating round-trip for all values`() {
        for (rating in Rating.entries) {
            val entity = ReviewEntity(
                itemId = 1,
                rating = rating.value,
                stabilityAfter = 1.0,
                difficultyAfter = 1.0
            )
            val domain = entity.toDomain()
            assertEquals(rating, domain.rating)
        }
    }

    // --- Category round-trip ---

    @Test
    fun `Category without FSRS params round-trips correctly`() {
        val category = Category(id = 1, name = "Test")
        val roundTripped = category.toEntity().toDomain()

        assertEquals(category.id, roundTripped.id)
        assertEquals(category.name, roundTripped.name)
        assertNull(roundTripped.desiredRetention)
        assertNull(roundTripped.fsrsParameters)
    }

    @Test
    fun `Category with FSRS params round-trips correctly`() {
        val params = FsrsParameters(
            w = doubleArrayOf(0.5, 1.4, 3.7, 13.8, 5.2, 1.2, 0.9, 0.03, 1.6, 0.14, 1.0, 2.1, 0.08, 0.32, 1.59, 0.23, 2.88),
            desiredRetention = 0.85
        )
        val category = Category(
            id = 2,
            name = "Custom",
            desiredRetention = 0.85,
            fsrsParameters = params
        )

        val roundTripped = category.toEntity().toDomain()

        assertEquals(category.name, roundTripped.name)
        assertEquals(category.desiredRetention, roundTripped.desiredRetention)
        assertArrayEquals(params.w, roundTripped.fsrsParameters!!.w, 0.001)
        assertEquals(params.desiredRetention, roundTripped.fsrsParameters!!.desiredRetention, 0.001)
    }

    @Test
    fun `Category with default FSRS params round-trips correctly`() {
        val category = Category(
            id = 3,
            name = "Default Params",
            fsrsParameters = FsrsParameters.DEFAULT
        )

        val roundTripped = category.toEntity().toDomain()

        assertArrayEquals(
            FsrsParameters.DEFAULT.w,
            roundTripped.fsrsParameters!!.w,
            0.0001
        )
        assertEquals(
            FsrsParameters.DEFAULT.desiredRetention,
            roundTripped.fsrsParameters!!.desiredRetention,
            0.0001
        )
    }

    // --- Corrupt JSON fallback ---

    @Test
    fun `Corrupt FSRS JSON returns null fsrsParameters`() {
        val entity = CategoryEntity(
            id = 1,
            name = "Corrupt",
            fsrsParametersJson = "not valid json"
        )

        val domain = entity.toDomain()

        assertEquals("Corrupt", domain.name)
        assertNull(domain.fsrsParameters)
    }

    @Test
    fun `Empty FSRS JSON returns null fsrsParameters`() {
        val entity = CategoryEntity(
            id = 1,
            name = "Empty",
            fsrsParametersJson = ""
        )

        val domain = entity.toDomain()
        assertNull(domain.fsrsParameters)
    }
}
