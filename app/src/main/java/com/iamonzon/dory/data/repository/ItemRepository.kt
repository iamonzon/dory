package com.iamonzon.dory.data.repository

import com.iamonzon.dory.algorithm.Fsrs
import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.toDomain
import com.iamonzon.dory.data.db.entity.toEntity
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.ReviewUrgency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

data class DashboardItem(
    val item: Item,
    val urgency: ReviewUrgency,
    val categoryName: String?
)

class ItemRepository(
    private val itemDao: ItemDao,
    private val reviewDao: ReviewDao,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository
) {

    fun observeAllActive(): Flow<List<Item>> =
        itemDao.observeAllActive().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeAllArchived(): Flow<List<Item>> =
        itemDao.observeAllArchived().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeById(id: Long): Flow<Item?> =
        itemDao.observeById(id).map { it?.toDomain() }

    suspend fun getById(id: Long): Item? =
        itemDao.getById(id)?.toDomain()

    suspend fun insert(item: Item): Long =
        itemDao.insert(item.toEntity())

    suspend fun update(item: Item) =
        itemDao.update(item.toEntity())

    suspend fun delete(item: Item) =
        itemDao.delete(item.toEntity())

    suspend fun deleteById(id: Long) =
        itemDao.deleteById(id)

    suspend fun archive(id: Long) =
        itemDao.archive(id)

    suspend fun unarchive(id: Long) =
        itemDao.unarchive(id)

    fun observeDashboardItems(): Flow<List<DashboardItem>> =
        combine(
            itemDao.observeAllActive(),
            categoryDao.observeAll(),
            settingsRepository.observeDesiredRetention()
        ) { items, categories, globalRetention ->
            val categoryMap = categories.associateBy { it.id }
            items.map { itemEntity ->
                val category = itemEntity.categoryId?.let { categoryMap[it] }
                val urgency = computeUrgency(
                    itemEntity.id,
                    category,
                    globalRetention
                )
                DashboardItem(
                    item = itemEntity.toDomain(),
                    urgency = urgency,
                    categoryName = category?.name
                )
            }.sortedWith(urgencyComparator)
        }

    fun observeDueItems(): Flow<List<DashboardItem>> =
        observeDashboardItems().map { items ->
            items.filter { it.urgency == ReviewUrgency.Overdue || it.urgency == ReviewUrgency.DueToday }
        }

    private suspend fun computeUrgency(
        itemId: Long,
        category: CategoryEntity?,
        globalRetention: Double
    ): ReviewUrgency {
        val latestReview = reviewDao.getLatestReviewForItem(itemId)
            ?: return ReviewUrgency.Overdue // Never reviewed

        val fsrsParams = if (category != null) {
            val categoryDomain = category.toDomain()
            categoryDomain.fsrsParameters ?: FsrsParameters.DEFAULT
        } else {
            FsrsParameters.DEFAULT
        }

        val desiredRetention = category?.desiredRetention ?: globalRetention

        val fsrs = Fsrs(fsrsParams)
        val nextInterval = fsrs.nextInterval(latestReview.stabilityAfter, desiredRetention)

        val daysSinceReview = ChronoUnit.DAYS.between(
            latestReview.reviewedAt,
            Instant.now()
        )

        return when {
            daysSinceReview > nextInterval -> ReviewUrgency.Overdue
            daysSinceReview >= nextInterval -> ReviewUrgency.DueToday
            else -> ReviewUrgency.NotDue
        }
    }

    companion object {
        private val urgencyComparator = compareBy<DashboardItem> { item ->
            when (item.urgency) {
                ReviewUrgency.Overdue -> 0
                ReviewUrgency.DueToday -> 1
                ReviewUrgency.NotDue -> 2
            }
        }
    }
}
