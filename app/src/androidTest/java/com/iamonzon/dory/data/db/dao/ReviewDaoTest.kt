package com.iamonzon.dory.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iamonzon.dory.data.db.DoryDatabase
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class ReviewDaoTest {

    private lateinit var database: DoryDatabase
    private lateinit var reviewDao: ReviewDao
    private lateinit var itemDao: ItemDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reviewDao = database.reviewDao()
        itemDao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createItem(): Long =
        itemDao.insert(ItemEntity(title = "Test Item", source = "test.com"))

    @Test
    fun insertAndGetLatest() = runTest {
        val itemId = createItem()
        val review = ReviewEntity(
            itemId = itemId,
            rating = 3,
            notes = "Good",
            stabilityAfter = 5.0,
            difficultyAfter = 4.0
        )
        reviewDao.insert(review)

        val latest = reviewDao.getLatestReviewForItem(itemId)
        assertEquals(3, latest?.rating)
        assertEquals("Good", latest?.notes)
    }

    @Test
    fun latestReturnsNewest() = runTest {
        val itemId = createItem()
        val now = Instant.now()
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 1, reviewedAt = now.minus(2, ChronoUnit.DAYS), stabilityAfter = 1.0, difficultyAfter = 6.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 3, reviewedAt = now.minus(1, ChronoUnit.DAYS), stabilityAfter = 3.0, difficultyAfter = 5.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 4, reviewedAt = now, stabilityAfter = 8.0, difficultyAfter = 3.0))

        val latest = reviewDao.getLatestReviewForItem(itemId)
        assertEquals(4, latest?.rating)
        assertEquals(8.0, latest?.stabilityAfter ?: 0.0, 0.001)
    }

    @Test
    fun observeByItemIdReturnsChronologicalOrder() = runTest {
        val itemId = createItem()
        val now = Instant.now()
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 3, reviewedAt = now.minus(2, ChronoUnit.DAYS), stabilityAfter = 1.0, difficultyAfter = 5.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 1, reviewedAt = now.minus(1, ChronoUnit.DAYS), stabilityAfter = 0.5, difficultyAfter = 7.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 4, reviewedAt = now, stabilityAfter = 8.0, difficultyAfter = 3.0))

        val reviews = reviewDao.observeByItemId(itemId).first()
        assertEquals(3, reviews.size)
        assertEquals(3, reviews[0].rating) // oldest first
        assertEquals(1, reviews[1].rating)
        assertEquals(4, reviews[2].rating) // newest last
    }

    @Test
    fun cascadeDeleteRemovesReviews() = runTest {
        val itemId = createItem()
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 3, stabilityAfter = 5.0, difficultyAfter = 4.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 2, stabilityAfter = 3.0, difficultyAfter = 5.0))

        itemDao.deleteById(itemId)

        val reviews = reviewDao.observeByItemId(itemId).first()
        assertEquals(0, reviews.size)
    }

    @Test
    fun reviewCount() = runTest {
        val itemId = createItem()
        assertEquals(0, reviewDao.getReviewCountForItem(itemId))

        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 3, stabilityAfter = 5.0, difficultyAfter = 4.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 2, stabilityAfter = 3.0, difficultyAfter = 5.0))

        assertEquals(2, reviewDao.getReviewCountForItem(itemId))
    }

    @Test
    fun getLatestByItemIdReturnsTopN() = runTest {
        val itemId = createItem()
        val now = Instant.now()
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 1, reviewedAt = now.minus(3, ChronoUnit.DAYS), stabilityAfter = 0.5, difficultyAfter = 7.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 3, reviewedAt = now.minus(2, ChronoUnit.DAYS), stabilityAfter = 3.0, difficultyAfter = 5.0))
        reviewDao.insert(ReviewEntity(itemId = itemId, rating = 4, reviewedAt = now.minus(1, ChronoUnit.DAYS), stabilityAfter = 8.0, difficultyAfter = 3.0))

        val latest2 = reviewDao.getLatestByItemId(itemId, 2)
        assertEquals(2, latest2.size)
        assertEquals(4, latest2[0].rating) // most recent first
        assertEquals(3, latest2[1].rating)
    }

    @Test
    fun getLatestReviewForNonExistentItemReturnsNull() = runTest {
        assertNull(reviewDao.getLatestReviewForItem(999L))
    }
}
