package com.iamonzon.dory.ui.profile

import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.repository.ItemRepository
import com.iamonzon.dory.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class ArchivedItemsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeItemDao: FakeArchivedItemDao
    private lateinit var itemRepository: ItemRepository
    private lateinit var viewModel: ArchivedItemsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeItemDao = FakeArchivedItemDao()
        itemRepository = ItemRepository(
            fakeItemDao,
            FakeArchivedReviewDao(),
            FakeArchivedCategoryDao(),
            SettingsRepository(FakeDataStore())
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `archived items are loaded from repository`() = runTest(testDispatcher) {
        fakeItemDao.archivedItemsFlow.value = listOf(
            ItemEntity(id = 1, title = "Archived 1", source = "s", isArchived = true, createdAt = Instant.EPOCH),
            ItemEntity(id = 2, title = "Archived 2", source = "s", isArchived = true, createdAt = Instant.EPOCH)
        )

        viewModel = ArchivedItemsViewModel(itemRepository)
        advanceUntilIdle()

        val items = viewModel.archivedItems.value
        assertEquals(2, items.size)
        assertEquals("Archived 1", items[0].title)
        assertEquals("Archived 2", items[1].title)
    }

    @Test
    fun `restoreItem calls unarchive on repository`() = runTest(testDispatcher) {
        viewModel = ArchivedItemsViewModel(itemRepository)
        advanceUntilIdle()

        viewModel.restoreItem(42L)
        advanceUntilIdle()

        assertEquals(1, fakeItemDao.unarchivedIds.size)
        assertEquals(42L, fakeItemDao.unarchivedIds[0])
    }
}

// --- Fake DAOs ---

private class FakeArchivedItemDao : ItemDao {
    val archivedItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())
    val unarchivedIds = mutableListOf<Long>()

    override fun observeAllActive(): Flow<List<ItemEntity>> = flowOf(emptyList())
    override fun observeAllArchived(): Flow<List<ItemEntity>> = archivedItemsFlow
    override fun observeById(id: Long): Flow<ItemEntity?> = flowOf(null)
    override suspend fun getById(id: Long): ItemEntity? = null
    override suspend fun insert(item: ItemEntity): Long = 0
    override suspend fun update(item: ItemEntity) {}
    override suspend fun delete(item: ItemEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun archive(id: Long) {}
    override suspend fun unarchive(id: Long) { unarchivedIds.add(id) }
    override suspend fun uncategorizeByCategory(categoryId: Long) {}
    override suspend fun archiveByCategory(categoryId: Long) {}
    override suspend fun deleteByCategoryId(categoryId: Long) {}
}

private class FakeArchivedCategoryDao : CategoryDao {
    override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun getAll(): List<CategoryEntity> = emptyList()
    override suspend fun getById(id: Long): CategoryEntity? = null
    override suspend fun insert(category: CategoryEntity): Long = 0
    override suspend fun update(category: CategoryEntity) {}
    override suspend fun delete(category: CategoryEntity) {}
    override suspend fun deleteById(id: Long) {}
}

private class FakeArchivedReviewDao : ReviewDao {
    override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> = flowOf(emptyList())
    override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? = null
    override suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity> = emptyList()
    override suspend fun getReviewCountForItem(itemId: Long): Int = 0
    override suspend fun insert(review: ReviewEntity): Long = 0
    override suspend fun update(review: ReviewEntity) {}
    override suspend fun delete(review: ReviewEntity) {}
}

