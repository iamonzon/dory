package com.iamonzon.dory.ui.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeItemDao: FakeItemDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeReviewDao: FakeReviewDao
    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeItemDao = FakeItemDao()
        fakeCategoryDao = FakeCategoryDao()
        fakeReviewDao = FakeReviewDao()
        fakeDataStore = FakeDataStore()
        settingsRepository = SettingsRepository(fakeDataStore)

        val itemRepository = ItemRepository(fakeItemDao, fakeReviewDao, fakeCategoryDao, settingsRepository)
        val categoryRepository = CategoryRepository(fakeCategoryDao, fakeItemDao)

        viewModel = ProfileViewModel(itemRepository, categoryRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Stats Computation ---

    @Test
    fun `totalItems counts all dashboard items`() = runTest {
        // 3 items with no reviews → all Overdue
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A"),
            itemEntity(2, "B"),
            itemEntity(3, "C")
        )
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.stats.totalItems)
    }

    @Test
    fun `masteredCount counts NotDue items`() = runTest {
        // Items 1,2 have recent reviews with high stability → NotDue (mastered)
        // Items 3,4 have no reviews → Overdue
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A"),
            itemEntity(2, "B"),
            itemEntity(3, "C"),
            itemEntity(4, "D")
        )
        fakeReviewDao.latestReviews[1] = recentReview(itemId = 1)
        fakeReviewDao.latestReviews[2] = recentReview(itemId = 2)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.stats.masteredCount)
    }

    @Test
    fun `strugglingCount counts Overdue items`() = runTest {
        // Items 1,2 have no reviews → Overdue (struggling)
        // Item 3 has a recent review → NotDue
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A"),
            itemEntity(2, "B"),
            itemEntity(3, "C")
        )
        fakeReviewDao.latestReviews[3] = recentReview(itemId = 3)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.stats.strugglingCount)
    }

    @Test
    fun `empty dashboard produces zero stats`() = runTest {
        advanceUntilIdle()

        val stats = viewModel.uiState.value.stats
        assertEquals(0, stats.totalItems)
        assertEquals(0, stats.masteredCount)
        assertEquals(0, stats.strugglingCount)
        assertEquals(emptyMap<String, Int>(), stats.byCategory)
    }

    // --- byCategory Grouping ---

    @Test
    fun `byCategory groups items by categoryName`() = runTest {
        val mathId = 10L
        val scienceId = 20L
        fakeCategoryDao.categoriesFlow.value = listOf(
            CategoryEntity(id = mathId, name = "Math"),
            CategoryEntity(id = scienceId, name = "Science")
        )
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A", categoryId = mathId),
            itemEntity(2, "B", categoryId = mathId),
            itemEntity(3, "C", categoryId = scienceId)
        )
        advanceUntilIdle()

        val byCategory = viewModel.uiState.value.stats.byCategory
        assertEquals(2, byCategory["Math"])
        assertEquals(1, byCategory["Science"])
    }

    @Test
    fun `byCategory uses Uncategorized for null categoryName`() = runTest {
        val artId = 30L
        fakeCategoryDao.categoriesFlow.value = listOf(
            CategoryEntity(id = artId, name = "Art")
        )
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A"), // no category
            itemEntity(2, "B"), // no category
            itemEntity(3, "C", categoryId = artId)
        )
        advanceUntilIdle()

        val byCategory = viewModel.uiState.value.stats.byCategory
        assertEquals(2, byCategory["Uncategorized"])
        assertEquals(1, byCategory["Art"])
    }

    // --- Notification Time ---

    @Test
    fun `default notification time is 9 00`() = runTest {
        advanceUntilIdle()

        assertEquals(9, viewModel.uiState.value.notificationHour)
        assertEquals(0, viewModel.uiState.value.notificationMinute)
    }

    @Test
    fun `setNotificationTime updates the state`() = runTest {
        advanceUntilIdle()

        viewModel.setNotificationTime(18, 45)
        advanceUntilIdle()

        assertEquals(18, viewModel.uiState.value.notificationHour)
        assertEquals(45, viewModel.uiState.value.notificationMinute)
    }

    // --- Reactive Updates ---

    @Test
    fun `stats update reactively when repository data changes`() = runTest {
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.stats.totalItems)

        // Add 2 items (both Overdue since no reviews)
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A"),
            itemEntity(2, "B")
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.stats.totalItems)
        assertEquals(0, viewModel.uiState.value.stats.masteredCount)
        assertEquals(2, viewModel.uiState.value.stats.strugglingCount)

        // Now add a 3rd item that is mastered
        fakeReviewDao.latestReviews[3] = recentReview(itemId = 3)
        fakeItemDao.activeItemsFlow.value = listOf(
            itemEntity(1, "A"),
            itemEntity(2, "B"),
            itemEntity(3, "C")
        )
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.stats.totalItems)
        assertEquals(1, viewModel.uiState.value.stats.masteredCount)
        assertEquals(2, viewModel.uiState.value.stats.strugglingCount)
    }

    // --- Helpers ---

    private fun itemEntity(
        id: Long,
        title: String,
        categoryId: Long? = null
    ): ItemEntity = ItemEntity(
        id = id,
        title = title,
        source = "src",
        categoryId = categoryId
    )

    /**
     * Creates a review with very high stability reviewed just now.
     * This ensures the item will be computed as NotDue (mastered).
     */
    private fun recentReview(itemId: Long): ReviewEntity = ReviewEntity(
        id = itemId * 100,
        itemId = itemId,
        rating = 3, // Good
        reviewedAt = Instant.now(),
        stabilityAfter = 10000.0,
        difficultyAfter = 5.0
    )

    // --- Fake implementations ---

    private class FakeItemDao : ItemDao {
        val activeItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())

        override fun observeAllActive(): Flow<List<ItemEntity>> = activeItemsFlow
        override suspend fun insert(item: ItemEntity): Long = 1L
        override suspend fun update(item: ItemEntity) = Unit
        override suspend fun delete(item: ItemEntity) = Unit
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun getById(id: Long): ItemEntity? = null
        override fun observeAllArchived(): Flow<List<ItemEntity>> = MutableStateFlow(emptyList())
        override fun observeById(id: Long): Flow<ItemEntity?> = MutableStateFlow(null)
        override suspend fun archive(id: Long) = Unit
        override suspend fun unarchive(id: Long) = Unit
        override suspend fun uncategorizeByCategory(categoryId: Long) = Unit
        override suspend fun archiveByCategory(categoryId: Long) = Unit
        override suspend fun deleteByCategoryId(categoryId: Long) = Unit
    }

    private class FakeCategoryDao : CategoryDao {
        val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

        override fun observeAll(): Flow<List<CategoryEntity>> = categoriesFlow
        override suspend fun getAll(): List<CategoryEntity> = categoriesFlow.value
        override suspend fun insert(category: CategoryEntity): Long = 1L
        override suspend fun update(category: CategoryEntity) = Unit
        override suspend fun delete(category: CategoryEntity) = Unit
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun getById(id: Long): CategoryEntity? = null
    }

    private class FakeReviewDao : ReviewDao {
        val latestReviews = mutableMapOf<Long, ReviewEntity>()

        override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? =
            latestReviews[itemId]

        override suspend fun insert(review: ReviewEntity): Long = 1L
        override suspend fun update(review: ReviewEntity) = Unit
        override suspend fun delete(review: ReviewEntity) = Unit
        override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> = emptyFlow()
        override suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity> = emptyList()
        override suspend fun getReviewCountForItem(itemId: Long): Int = 0
    }

    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            val newPrefs = transform(state.value)
            state.value = newPrefs
            return newPrefs
        }
    }
}
