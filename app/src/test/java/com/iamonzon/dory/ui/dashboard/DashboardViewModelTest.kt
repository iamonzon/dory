package com.iamonzon.dory.ui.dashboard

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
import kotlinx.coroutines.flow.first
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
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeItemDao: FakeItemDao
    private lateinit var itemRepository: ItemRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeItemDao = FakeItemDao()
        val fakeReviewDao = FakeReviewDao()
        val fakeCategoryDao = FakeCategoryDao()
        val fakeDataStore = FakePreferencesDataStore()
        val settingsRepository = SettingsRepository(fakeDataStore)
        itemRepository = ItemRepository(fakeItemDao, fakeReviewDao, fakeCategoryDao, settingsRepository)
        viewModel = DashboardViewModel(itemRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dashboardItems initial value is empty list`() = runTest {
        assertEquals(emptyList<DashboardItem>(), viewModel.dashboardItems.value)
    }

    @Test
    fun `dashboardItems emits items from repository`() = runTest {
        // Collect the StateFlow in backgroundScope to activate WhileSubscribed
        val values = mutableListOf<List<DashboardItem>>()
        backgroundScope.launch(testDispatcher) {
            viewModel.dashboardItems.collect { values.add(it) }
        }

        val item = ItemEntity(id = 1, title = "Item 1", source = "Source 1", createdAt = Instant.now())
        fakeItemDao.activeItemsFlow.value = listOf(item)

        assertTrue(values.any { it.size == 1 })
        val result = values.last { it.size == 1 }
        assertEquals("Item 1", result[0].item.title)
        assertEquals(ReviewUrgency.Overdue, result[0].urgency)
    }

    @Test
    fun `archiveItem calls repository archive`() = runTest {
        viewModel.archiveItem(42L)

        assertTrue(fakeItemDao.archivedIds.contains(42L))
    }

    @Test
    fun `archiveItem emits ItemArchived event`() = runTest {
        viewModel.archiveItem(42L)

        val event = viewModel.events.first()
        assertEquals(DashboardEvent.ItemArchived(42L), event)
    }

    @Test
    fun `deleteItem calls repository deleteById`() = runTest {
        viewModel.deleteItem(99L)

        assertTrue(fakeItemDao.deletedIds.contains(99L))
    }

    @Test
    fun `deleteItem emits ItemDeleted event`() = runTest {
        viewModel.deleteItem(99L)

        val event = viewModel.events.first()
        assertEquals(DashboardEvent.ItemDeleted(99L), event)
    }

    @Test
    fun `factory creates DashboardViewModel`() {
        val factory = DashboardViewModel.factory(itemRepository)
        val vm = factory.create(DashboardViewModel::class.java)
        assertTrue(vm is DashboardViewModel)
    }
}

// region Fakes

private class FakeItemDao : ItemDao {
    val activeItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())
    val archivedIds = mutableListOf<Long>()
    val deletedIds = mutableListOf<Long>()

    override suspend fun insert(item: ItemEntity): Long = item.id
    override suspend fun update(item: ItemEntity) {}
    override suspend fun delete(item: ItemEntity) {}
    override suspend fun deleteById(id: Long) { deletedIds.add(id) }
    override suspend fun getById(id: Long): ItemEntity? = null
    override fun observeAllActive(): Flow<List<ItemEntity>> = activeItemsFlow
    override fun observeAllArchived(): Flow<List<ItemEntity>> = flowOf(emptyList())
    override fun observeById(id: Long): Flow<ItemEntity?> = flowOf(null)
    override suspend fun archive(id: Long) { archivedIds.add(id) }
    override suspend fun unarchive(id: Long) {}
    override suspend fun uncategorizeByCategory(categoryId: Long) {}
    override suspend fun archiveByCategory(categoryId: Long) {}
    override suspend fun deleteByCategoryId(categoryId: Long) {}
}

private class FakeReviewDao : ReviewDao {
    override suspend fun insert(review: ReviewEntity): Long = 0
    override suspend fun update(review: ReviewEntity) {}
    override suspend fun delete(review: ReviewEntity) {}
    override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> = flowOf(emptyList())
    override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? = null
    override suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity> = emptyList()
    override suspend fun getReviewCountForItem(itemId: Long): Int = 0
}

private class FakeCategoryDao : CategoryDao {
    override suspend fun insert(category: CategoryEntity): Long = 0
    override suspend fun update(category: CategoryEntity) {}
    override suspend fun delete(category: CategoryEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun getById(id: Long): CategoryEntity? = null
    override fun observeAll(): Flow<List<CategoryEntity>> = MutableStateFlow(emptyList())
    override suspend fun getAll(): List<CategoryEntity> = emptyList()
}

private class FakePreferencesDataStore : DataStore<Preferences> {
    private val prefs = preferencesOf(
        doublePreferencesKey("desired_retention") to 0.9
    )

    override val data: Flow<Preferences> = MutableStateFlow(prefs)

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return transform(prefs)
    }
}

// endregion
