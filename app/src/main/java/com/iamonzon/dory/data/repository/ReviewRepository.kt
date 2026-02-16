package com.iamonzon.dory.data.repository

import com.iamonzon.dory.algorithm.Fsrs
import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.algorithm.SchedulingState
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.db.entity.toDomain
import com.iamonzon.dory.data.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

class ReviewRepository(
    private val reviewDao: ReviewDao,
    private val itemDao: ItemDao,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository
) {

    fun observeReviewsForItem(itemId: Long): Flow<List<Review>> =
        reviewDao.observeByItemId(itemId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getLatestReviewsForItem(itemId: Long, limit: Int = 10): List<Review> =
        reviewDao.getLatestByItemId(itemId, limit).map { it.toDomain() }

    suspend fun submitReview(itemId: Long, rating: Rating, notes: String? = null): Review {
        val item = itemDao.getById(itemId)
            ?: throw IllegalArgumentException("Item $itemId not found")

        // Resolve FSRS parameters: per-category → global default
        val fsrsParams = resolveFsrsParameters(item.categoryId)
        val fsrs = Fsrs(fsrsParams)
        val desiredRetention = resolveDesiredRetention(item.categoryId)

        // Get latest review to build current state
        val latestReview = reviewDao.getLatestReviewForItem(itemId)

        val currentState = latestReview?.let {
            SchedulingState(
                stability = it.stabilityAfter,
                difficulty = it.difficultyAfter,
                interval = 0 // not used for computation input
            )
        }

        val elapsedDays = if (latestReview != null) {
            ChronoUnit.DAYS.between(latestReview.reviewedAt, Instant.now()).toDouble()
                .coerceAtLeast(0.0)
        } else {
            0.0
        }

        val result = fsrs.review(currentState, elapsedDays, rating, desiredRetention)

        val reviewEntity = ReviewEntity(
            itemId = itemId,
            rating = rating.value,
            notes = notes,
            reviewedAt = Instant.now(),
            stabilityAfter = result.stability,
            difficultyAfter = result.difficulty
        )

        val id = reviewDao.insert(reviewEntity)
        return reviewEntity.copy(id = id).toDomain()
    }

    private suspend fun resolveFsrsParameters(categoryId: Long?): FsrsParameters {
        if (categoryId != null) {
            val category = categoryDao.getById(categoryId)
            if (category?.fsrsParametersJson != null) {
                val categoryDomain = category.toDomain()
                if (categoryDomain.fsrsParameters != null) {
                    return categoryDomain.fsrsParameters
                }
            }
        }
        return FsrsParameters.DEFAULT
    }

    private suspend fun resolveDesiredRetention(categoryId: Long?): Double {
        if (categoryId != null) {
            val category = categoryDao.getById(categoryId)
            if (category?.desiredRetention != null) {
                return category.desiredRetention
            }
        }
        return settingsRepository.getDesiredRetention()
    }
}
