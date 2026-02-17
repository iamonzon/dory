package com.iamonzon.dory.ui.profile

import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.repository.CategoryDeleteStrategy
import com.iamonzon.dory.data.repository.CategoryRepository
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
class CategoryManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var fakeItemDao: FakeItemDao
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var itemRepository: ItemRepository
    private lateinit var viewModel: CategoryManagementViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCategoryDao = FakeCategoryDao()
        fakeItemDao = FakeItemDao()
        categoryRepository = CategoryRepository(fakeCategoryDao, fakeItemDao)
        itemRepository = ItemRepository(fakeItemDao, FakeReviewDao(), fakeCategoryDao, SettingsRepository(FakeDataStore()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `categories with counts are loaded correctly`() = runTest(testDispatcher) {
        fakeCategoryDao.categoriesFlow.value = listOf(
            CategoryEntity(id = 1, name = "Programming"),
            CategoryEntity(id = 2, name = "Design")
        )
        fakeItemDao.activeItemsFlow.value = listOf(
            ItemEntity(id = 1, title = "Item 1", source = "s", categoryId = 1, createdAt = Instant.EPOCH),
            ItemEntity(id = 2, title = "Item 2", source = "s", categoryId = 1, createdAt = Instant.EPOCH),
            ItemEntity(id = 3, title = "Item 3", source = "s", categoryId = 2, createdAt = Instant.EPOCH)
        )

        viewModel = CategoryManagementViewModel(categoryRepository, itemRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.categories.size)
        assertEquals("Programming", state.categories[0].category.name)
        assertEquals(2, state.categories[0].itemCount)
        assertEquals("Design", state.categories[1].category.name)
        assertEquals(1, state.categories[1].itemCount)
    }

    @Test
    fun `createCategory calls repository`() = runTest(testDispatcher) {
        viewModel = CategoryManagementViewModel(categoryRepository, itemRepository)
        advanceUntilIdle()

        viewModel.createCategory("Languages")
        advanceUntilIdle()

        assertEquals(1, fakeCategoryDao.insertedEntities.size)
        assertEquals("Languages", fakeCategoryDao.insertedEntities[0].name)
    }

    @Test
    fun `renameCategory calls repository`() = runTest(testDispatcher) {
        fakeCategoryDao.categoriesFlow.value = listOf(
            CategoryEntity(id = 1, name = "Old Name")
        )
        fakeCategoryDao.storedCategories[1L] = CategoryEntity(id = 1, name = "Old Name")

        viewModel = CategoryManagementViewModel(categoryRepository, itemRepository)
        advanceUntilIdle()

        viewModel.renameCategory(1L, "New Name")
        advanceUntilIdle()

        assertEquals("New Name", fakeCategoryDao.updatedEntities.last().name)
    }

    @Test
    fun `deleteCategory with UncategorizeItems calls repository correctly`() = runTest(testDispatcher) {
        viewModel = CategoryManagementViewModel(categoryRepository, itemRepository)
        advanceUntilIdle()

        viewModel.deleteCategory(1L, CategoryDeleteStrategy.UncategorizeItems)
        advanceUntilIdle()

        assertEquals(1L, fakeItemDao.uncategorizedCategoryIds.first())
        assertEquals(1L, fakeCategoryDao.deletedByIds.first())
    }

    @Test
    fun `deleteCategory with ArchiveItems calls repository correctly`() = runTest(testDispatcher) {
        viewModel = CategoryManagementViewModel(categoryRepository, itemRepository)
        advanceUntilIdle()

        viewModel.deleteCategory(2L, CategoryDeleteStrategy.ArchiveItems)
        advanceUntilIdle()

        assertEquals(2L, fakeItemDao.archivedByCategoryIds.first())
        assertEquals(2L, fakeCategoryDao.deletedByIds.first())
    }

    @Test
    fun `deleteCategory with DeleteItems calls repository correctly`() = runTest(testDispatcher) {
        viewModel = CategoryManagementViewModel(categoryRepository, itemRepository)
        advanceUntilIdle()

        viewModel.deleteCategory(3L, CategoryDeleteStrategy.DeleteItems)
        advanceUntilIdle()

        assertEquals(3L, fakeItemDao.deletedByCategoryIds.first())
        assertEquals(3L, fakeCategoryDao.deletedByIds.first())
    }
}

// --- Fake DAOs ---

private class FakeCategoryDao : CategoryDao {
    val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val insertedEntities = mutableListOf<CategoryEntity>()
    val updatedEntities = mutableListOf<CategoryEntity>()
    val deletedByIds = mutableListOf<Long>()
    val storedCategories = mutableMapOf<Long, CategoryEntity>()

    override fun observeAll(): Flow<List<CategoryEntity>> = categoriesFlow
    override suspend fun getAll(): List<CategoryEntity> = categoriesFlow.value
    override suspend fun getById(id: Long): CategoryEntity? = storedCategories[id]
    override suspend fun insert(category: CategoryEntity): Long {
        insertedEntities.add(category)
        return insertedEntities.size.toLong()
    }
    override suspend fun update(category: CategoryEntity) { updatedEntities.add(category) }
    override suspend fun delete(category: CategoryEntity) {}
    override suspend fun deleteById(id: Long) { deletedByIds.add(id) }
}

private class FakeItemDao : ItemDao {
    val activeItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())
    val archivedItemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())
    val uncategorizedCategoryIds = mutableListOf<Long>()
    val archivedByCategoryIds = mutableListOf<Long>()
    val deletedByCategoryIds = mutableListOf<Long>()
    val unarchivedIds = mutableListOf<Long>()

    override fun observeAllActive(): Flow<List<ItemEntity>> = activeItemsFlow
    override fun observeAllArchived(): Flow<List<ItemEntity>> = archivedItemsFlow
    override fun observeById(id: Long): Flow<ItemEntity?> = flowOf(null)
    override suspend fun getById(id: Long): ItemEntity? = null
    override suspend fun insert(item: ItemEntity): Long = 0
    override suspend fun update(item: ItemEntity) {}
    override suspend fun delete(item: ItemEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun archive(id: Long) {}
    override suspend fun unarchive(id: Long) { unarchivedIds.add(id) }
    override suspend fun uncategorizeByCategory(categoryId: Long) { uncategorizedCategoryIds.add(categoryId) }
    override suspend fun archiveByCategory(categoryId: Long) { archivedByCategoryIds.add(categoryId) }
    override suspend fun deleteByCategoryId(categoryId: Long) { deletedByCategoryIds.add(categoryId) }
}

private class FakeReviewDao : ReviewDao {
    override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> = flowOf(emptyList())
    override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? = null
    override suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity> = emptyList()
    override suspend fun getReviewCountForItem(itemId: Long): Int = 0
    override suspend fun insert(review: ReviewEntity): Long = 0
    override suspend fun update(review: ReviewEntity) {}
    override suspend fun delete(review: ReviewEntity) {}
}

