package com.iamonzon.dory.ui.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.Review
import com.iamonzon.dory.data.repository.CategoryRepository
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.ReviewRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeItemDao: TestItemDao
    private lateinit var fakeReviewDao: TestReviewDao
    private lateinit var fakeCategoryDao: TestCategoryDao
    private lateinit var viewModel: ReviewViewModel

    private val testItemEntity = ItemEntity(
        id = 1L,
        title = "Test Item",
        source = "Test Source",
        categoryId = 10L,
        createdAt = Instant.now()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeItemDao = TestItemDao()
        fakeReviewDao = TestReviewDao()
        fakeCategoryDao = TestCategoryDao()
        val fakeDataStore = TestPreferencesDataStore()
        val settingsRepository = SettingsRepository(fakeDataStore)

        val itemRepository = ItemRepository(fakeItemDao, fakeReviewDao, fakeCategoryDao, settingsRepository)
        val reviewRepository = ReviewRepository(fakeReviewDao, fakeItemDao, fakeCategoryDao, settingsRepository)
        val categoryRepository = CategoryRepository(fakeCategoryDao, fakeItemDao)

        viewModel = ReviewViewModel(
            itemId = 1L,
            itemRepository = itemRepository,
            reviewRepository = reviewRepository,
            categoryRepository = categoryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `item initial value is null`() = runTest {
        assertNull(viewModel.item.value)
    }

    @Test
    fun `item emits from repository`() = runTest {
        val values = mutableListOf<Item?>()
        backgroundScope.launch(testDispatcher) {
            viewModel.item.collect { values.add(it) }
        }

        fakeItemDao.itemFlows[1L]!!.value = testItemEntity

        assertTrue(values.any { it != null })
        assertEquals("Test Item", values.last { it != null }!!.title)
    }

    @Test
    fun `reviews initial value is empty list`() = runTest {
        assertEquals(emptyList<Review>(), viewModel.reviews.value)
    }

    @Test
    fun `reviews emits from repository`() = runTest {
        val values = mutableListOf<List<Review>>()
        backgroundScope.launch(testDispatcher) {
            viewModel.reviews.collect { values.add(it) }
        }

        val reviewEntity = ReviewEntity(
            id = 1L,
            itemId = 1L,
            rating = Rating.Good.value,
            reviewedAt = Instant.now(),
            stabilityAfter = 3.0,
            difficultyAfter = 5.0
        )
        fakeReviewDao.reviewsByItemFlow[1L]!!.value = listOf(reviewEntity)

        assertTrue(values.any { it.isNotEmpty() })
        val result = values.last { it.isNotEmpty() }
        assertEquals(1, result.size)
        assertEquals(Rating.Good, result[0].rating)
    }

    @Test
    fun `category loads from item categoryId`() = runTest {
        val values = mutableListOf<Category?>()
        backgroundScope.launch(testDispatcher) {
            viewModel.category.collect { values.add(it) }
        }

        fakeCategoryDao.categoriesById[10L] = CategoryEntity(id = 10L, name = "Test Category")
        fakeItemDao.itemFlows[1L]!!.value = testItemEntity

        assertTrue(values.any { it != null })
        assertEquals("Test Category", values.last { it != null }!!.name)
    }

    @Test
    fun `category is null when item has no categoryId`() = runTest {
        val values = mutableListOf<Category?>()
        backgroundScope.launch(testDispatcher) {
            viewModel.category.collect { values.add(it) }
        }

        val itemWithoutCategory = testItemEntity.copy(categoryId = null)
        fakeItemDao.itemFlows[1L]!!.value = itemWithoutCategory

        // All values should be null (initial + after emission)
        assertTrue(values.all { it == null })
    }

    @Test
    fun `submitReview calls repository and emits reviewSubmitted`() = runTest {
        fakeItemDao.itemsById[1L] = testItemEntity

        var emitted = false
        backgroundScope.launch(testDispatcher) {
            viewModel.reviewSubmitted.collect { emitted = true }
        }

        viewModel.submitReview(Rating.Good, "Great!")

        assertTrue(fakeReviewDao.insertedReviews.isNotEmpty())
        val inserted = fakeReviewDao.insertedReviews.first()
        assertEquals(1L, inserted.itemId)
        assertEquals(Rating.Good.value, inserted.rating)
        assertEquals("Great!", inserted.notes)
        assertTrue(emitted)
    }

    @Test
    fun `submitReview with null notes`() = runTest {
        fakeItemDao.itemsById[1L] = testItemEntity

        var emitted = false
        backgroundScope.launch(testDispatcher) {
            viewModel.reviewSubmitted.collect { emitted = true }
        }

        viewModel.submitReview(Rating.Hard, null)

        val inserted = fakeReviewDao.insertedReviews.first()
        assertEquals(Rating.Hard.value, inserted.rating)
        assertNull(inserted.notes)
        assertTrue(emitted)
    }

    @Test
    fun `factory creates ReviewViewModel`() {
        val fakeDataStore = TestPreferencesDataStore()
        val settingsRepository = SettingsRepository(fakeDataStore)
        val itemRepository = ItemRepository(fakeItemDao, fakeReviewDao, fakeCategoryDao, settingsRepository)
        val reviewRepository = ReviewRepository(fakeReviewDao, fakeItemDao, fakeCategoryDao, settingsRepository)
        val categoryRepository = CategoryRepository(fakeCategoryDao, fakeItemDao)

        val factory = ReviewViewModel.factory(
            itemId = 1L,
            itemRepository = itemRepository,
            reviewRepository = reviewRepository,
            categoryRepository = categoryRepository
        )
        val vm = factory.create(ReviewViewModel::class.java)
        assertTrue(vm is ReviewViewModel)
    }

    // region Fakes

    private class TestItemDao : ItemDao {
        val itemFlows = mutableMapOf<Long, MutableStateFlow<ItemEntity?>>(
            1L to MutableStateFlow(null)
        )
        val itemsById = mutableMapOf<Long, ItemEntity>()
        val activeItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())

        override suspend fun insert(item: ItemEntity): Long = item.id
        override suspend fun update(item: ItemEntity) {}
        override suspend fun delete(item: ItemEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun getById(id: Long): ItemEntity? = itemsById[id]
        override fun observeAllActive(): Flow<List<ItemEntity>> = activeItemsFlow
        override fun observeAllArchived(): Flow<List<ItemEntity>> = flowOf(emptyList())
        override fun observeById(id: Long): Flow<ItemEntity?> =
            itemFlows.getOrPut(id) { MutableStateFlow(null) }
        override suspend fun archive(id: Long) {}
        override suspend fun unarchive(id: Long) {}
        override suspend fun uncategorizeByCategory(categoryId: Long) {}
        override suspend fun archiveByCategory(categoryId: Long) {}
        override suspend fun deleteByCategoryId(categoryId: Long) {}
    }

    private class TestReviewDao : ReviewDao {
        val reviewsByItemFlow = mutableMapOf<Long, MutableStateFlow<List<ReviewEntity>>>(
            1L to MutableStateFlow(emptyList())
        )
        val insertedReviews = mutableListOf<ReviewEntity>()

        override suspend fun insert(review: ReviewEntity): Long {
            insertedReviews.add(review)
            return (insertedReviews.size).toLong()
        }
        override suspend fun update(review: ReviewEntity) {}
        override suspend fun delete(review: ReviewEntity) {}
        override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> =
            reviewsByItemFlow.getOrPut(itemId) { MutableStateFlow(emptyList()) }
        override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? = null
        override suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity> = emptyList()
        override suspend fun getReviewCountForItem(itemId: Long): Int = 0
    }

    private class TestCategoryDao : CategoryDao {
        val categoriesById = mutableMapOf<Long, CategoryEntity>()

        override suspend fun insert(category: CategoryEntity): Long = category.id
        override suspend fun update(category: CategoryEntity) {}
        override suspend fun delete(category: CategoryEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun getById(id: Long): CategoryEntity? = categoriesById[id]
        override fun observeAll(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun getAll(): List<CategoryEntity> = emptyList()
    }

    private class TestPreferencesDataStore : DataStore<Preferences> {
        private val prefs = preferencesOf(
            doublePreferencesKey("desired_retention") to 0.9
        )

        override val data: Flow<Preferences> = MutableStateFlow(prefs)

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            return transform(prefs)
        }
    }

    // endregion
}
