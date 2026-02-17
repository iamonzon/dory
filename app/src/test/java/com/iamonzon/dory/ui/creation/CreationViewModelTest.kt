package com.iamonzon.dory.ui.creation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity
import com.iamonzon.dory.data.model.Category
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeItemDao: FakeItemDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var viewModel: CreationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeItemDao = FakeItemDao()
        fakeCategoryDao = FakeCategoryDao()

        val fakeReviewDao = FakeReviewDao()
        val fakeSettingsRepository = SettingsRepository(FakeDataStore())

        val itemRepository = ItemRepository(fakeItemDao, fakeReviewDao, fakeCategoryDao, fakeSettingsRepository)
        val categoryRepository = CategoryRepository(fakeCategoryDao, fakeItemDao)

        viewModel = CreationViewModel(itemRepository, categoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Step Navigation ---

    @Test
    fun `initial step is TITLE`() = runTest {
        assertEquals(CreationStep.TITLE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `nextStep advances from TITLE to SOURCE`() = runTest {
        viewModel.nextStep()
        assertEquals(CreationStep.SOURCE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `nextStep advances from SOURCE to CATEGORY`() = runTest {
        viewModel.nextStep()
        viewModel.nextStep()
        assertEquals(CreationStep.CATEGORY, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `nextStep advances from CATEGORY to NOTES`() = runTest {
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        assertEquals(CreationStep.NOTES, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `nextStep is no-op on NOTES`() = runTest {
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        assertEquals(CreationStep.NOTES, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `previousStep is no-op on TITLE`() = runTest {
        viewModel.previousStep()
        assertEquals(CreationStep.TITLE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `previousStep goes from SOURCE to TITLE`() = runTest {
        viewModel.nextStep()
        viewModel.previousStep()
        assertEquals(CreationStep.TITLE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `previousStep goes from NOTES to CATEGORY`() = runTest {
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.previousStep()
        assertEquals(CreationStep.CATEGORY, viewModel.uiState.value.currentStep)
    }

    // --- Field Updates ---

    @Test
    fun `updateTitle reflects in uiState`() = runTest {
        viewModel.updateTitle("My Item")
        assertEquals("My Item", viewModel.uiState.value.title)
    }

    @Test
    fun `updateSource reflects in uiState`() = runTest {
        viewModel.updateSource("https://example.com")
        assertEquals("https://example.com", viewModel.uiState.value.source)
    }

    @Test
    fun `selectCategory reflects in uiState`() = runTest {
        viewModel.selectCategory(5L, "Science")
        assertEquals(5L, viewModel.uiState.value.selectedCategoryId)
        assertEquals("Science", viewModel.uiState.value.selectedCategoryName)
    }

    @Test
    fun `updateNotes reflects in uiState`() = runTest {
        viewModel.updateNotes("Some notes here")
        assertEquals("Some notes here", viewModel.uiState.value.notes)
    }

    // --- canAdvance ---

    @Test
    fun `canAdvance is false on TITLE when title is blank`() = runTest {
        assertFalse(viewModel.canAdvance())
    }

    @Test
    fun `canAdvance is false on TITLE when title is whitespace`() = runTest {
        viewModel.updateTitle("   ")
        assertFalse(viewModel.canAdvance())
    }

    @Test
    fun `canAdvance is true on TITLE when title is non-blank`() = runTest {
        viewModel.updateTitle("Some title")
        assertTrue(viewModel.canAdvance())
    }

    @Test
    fun `canAdvance is always true on SOURCE`() = runTest {
        viewModel.nextStep()
        assertTrue(viewModel.canAdvance())
    }

    @Test
    fun `canAdvance is always true on CATEGORY`() = runTest {
        viewModel.nextStep()
        viewModel.nextStep()
        assertTrue(viewModel.canAdvance())
    }

    @Test
    fun `canAdvance is always true on NOTES`() = runTest {
        viewModel.nextStep()
        viewModel.nextStep()
        viewModel.nextStep()
        assertTrue(viewModel.canAdvance())
    }

    // --- saveItem ---

    @Test
    fun `saveItem creates the correct Item and sets savedSuccessfully`() = runTest {
        viewModel.updateTitle("Learn Kotlin")
        viewModel.updateSource("https://kotlinlang.org")
        viewModel.selectCategory(2L, "Programming")
        viewModel.updateNotes("Focus on coroutines")

        viewModel.saveItem()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.savedSuccessfully)
        assertFalse(state.isSaving)

        val saved = fakeItemDao.lastInserted!!
        assertEquals("Learn Kotlin", saved.title)
        assertEquals("https://kotlinlang.org", saved.source)
        assertEquals(2L, saved.categoryId)
        assertEquals("Focus on coroutines", saved.notes)
    }

    @Test
    fun `saveItem with blank notes saves null notes`() = runTest {
        viewModel.updateTitle("Test")
        viewModel.updateSource("src")

        viewModel.saveItem()
        advanceUntilIdle()

        assertNull(fakeItemDao.lastInserted!!.notes)
    }

    @Test
    fun `saveItem without category saves null categoryId`() = runTest {
        viewModel.updateTitle("Test")
        viewModel.updateSource("src")

        viewModel.saveItem()
        advanceUntilIdle()

        assertNull(fakeItemDao.lastInserted!!.categoryId)
    }

    // --- Categories loaded from repository ---

    @Test
    fun `categories are loaded from repository`() = runTest {
        fakeCategoryDao.categoriesFlow.value = listOf(
            CategoryEntity(id = 1, name = "Math"),
            CategoryEntity(id = 2, name = "Science")
        )
        advanceUntilIdle()

        val names = viewModel.uiState.value.categories.map { it.name }
        assertEquals(listOf("Math", "Science"), names)
    }

    @Test
    fun `categories update when repository emits new data`() = runTest {
        advanceUntilIdle()
        assertEquals(emptyList<Category>(), viewModel.uiState.value.categories)

        fakeCategoryDao.categoriesFlow.value = listOf(
            CategoryEntity(id = 1, name = "History")
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.categories.size)
        assertEquals("History", viewModel.uiState.value.categories[0].name)
    }

    // --- Fake implementations ---

    private class FakeItemDao : ItemDao {
        var lastInserted: ItemEntity? = null

        override suspend fun insert(item: ItemEntity): Long {
            lastInserted = item
            return 1L
        }

        override suspend fun update(item: ItemEntity) = Unit
        override suspend fun delete(item: ItemEntity) = Unit
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun getById(id: Long): ItemEntity? = null
        override fun observeAllActive(): Flow<List<ItemEntity>> = MutableStateFlow(emptyList())
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
        override suspend fun insert(review: ReviewEntity): Long = 1L
        override suspend fun update(review: ReviewEntity) = Unit
        override suspend fun delete(review: ReviewEntity) = Unit
        override fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>> = emptyFlow()
        override suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity? = null
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
