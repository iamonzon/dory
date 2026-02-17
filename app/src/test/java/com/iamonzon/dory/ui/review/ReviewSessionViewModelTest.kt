package com.iamonzon.dory.ui.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.model.ReviewUrgency
import com.iamonzon.dory.data.repository.DashboardItem
import com.iamonzon.dory.data.repository.ItemRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewSessionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeItemDao: SessionTestItemDao
    private lateinit var itemRepository: ItemRepository
    private lateinit var viewModel: ReviewSessionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeItemDao = SessionTestItemDao()
        val fakeReviewDao = SessionTestReviewDao()
        val fakeCategoryDao = SessionTestCategoryDao()
        val fakeDataStore = SessionTestPreferencesDataStore()
        val settingsRepository = SettingsRepository(fakeDataStore)
        itemRepository = ItemRepository(fakeItemDao, fakeReviewDao, fakeCategoryDao, settingsRepository)
        viewModel = ReviewSessionViewModel(itemRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dueItems initial value is empty list`() = runTest {
        assertEquals(emptyList<DashboardItem>(), viewModel.dueItems.value)
    }

    @Test
    fun `dueItems emits due and overdue items`() = runTest {
        val values = mutableListOf<List<DashboardItem>>()
        backgroundScope.launch(testDispatcher) {
            viewModel.dueItems.collect { values.add(it) }
        }

        val item1 = ItemEntity(id = 1, title = "Overdue Item", source = "Source", createdAt = Instant.now())
        val item2 = ItemEntity(id = 2, title = "Another Item", source = "Source", createdAt = Instant.now())
        fakeItemDao.activeItemsFlow.value = listOf(item1, item2)

        assertTrue(values.any { it.size == 2 })
        val result = values.last { it.size == 2 }
        assertTrue(result.all { it.urgency == ReviewUrgency.Overdue })
    }

    @Test
    fun `dueItems updates reactively when items change`() = runTest {
        val values = mutableListOf<List<DashboardItem>>()
        backgroundScope.launch(testDispatcher) {
            viewModel.dueItems.collect { values.add(it) }
        }

        val item = ItemEntity(id = 1, title = "Item 1", source = "Source", createdAt = Instant.now())
        fakeItemDao.activeItemsFlow.value = listOf(item)
        assertTrue(values.any { it.size == 1 })

        fakeItemDao.activeItemsFlow.value = emptyList()
        assertTrue(values.last().isEmpty())
    }

    @Test
    fun `factory creates ReviewSessionViewModel`() {
        val factory = ReviewSessionViewModel.factory(itemRepository)
        val vm = factory.create(ReviewSessionViewModel::class.java)
        assertTrue(vm is ReviewSessionViewModel)
    }
}

// region Fakes

private class SessionTestItemDao : ItemDao {
    val activeItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())

    override suspend fun insert(item: ItemEntity): Long = item.id
    override suspend fun update(item: ItemEntity) {}
    override suspend fun delete(item: ItemEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun getById(id: Long): ItemEntity? = null
    override fun observeAllActive(): Flow<List<ItemEntity>> = activeItemsFlow
    override fun observeAllArchived(): Flow<List<ItemEntity>> = flowOf(emptyList())
    override fun observeById(id: Long): Flow<ItemEntity?> = flowOf(null)
    override suspend fun archive(id: Long) {}
    override suspend fun unarchive(id: Long) {}
    override suspend fun uncategorizeByCategory(categoryId: Long) {}
    override suspend fun archiveByCategory(categoryId: Long) {}
    override suspend fun deleteByCategoryId(categoryId: Long) {}
}

private class SessionTestReviewDao : ReviewDao {
    override suspend fun insert(review: ReviewEntity): Long = 0
    override suspend fun update(review: ReviewEntity) {}
    override suspend fun delete(review: ReviewEntity) {}
    override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> = flowOf(emptyList())
    override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? = null
    override suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity> = emptyList()
    override suspend fun getReviewCountForItem(itemId: Long): Int = 0
}

private class SessionTestCategoryDao : CategoryDao {
    override suspend fun insert(category: CategoryEntity): Long = 0
    override suspend fun update(category: CategoryEntity) {}
    override suspend fun delete(category: CategoryEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun getById(id: Long): CategoryEntity? = null
    override fun observeAll(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
    override suspend fun getAll(): List<CategoryEntity> = emptyList()
}

private class SessionTestPreferencesDataStore : DataStore<Preferences> {
    private val prefs = preferencesOf(
        doublePreferencesKey("desired_retention") to 0.9
    )

    override val data: Flow<Preferences> = MutableStateFlow(prefs)

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return transform(prefs)
    }
}

// endregion
