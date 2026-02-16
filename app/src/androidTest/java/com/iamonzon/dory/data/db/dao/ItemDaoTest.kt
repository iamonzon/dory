package com.iamonzon.dory.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iamonzon.dory.data.db.DoryDatabase
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var database: DoryDatabase
    private lateinit var itemDao: ItemDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        itemDao = database.itemDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val item = ItemEntity(title = "Test Item", source = "test.com")
        val id = itemDao.insert(item)
        val retrieved = itemDao.getById(id)
        assertEquals("Test Item", retrieved?.title)
        assertEquals("test.com", retrieved?.source)
    }

    @Test
    fun observeAllActiveExcludesArchived() = runTest {
        itemDao.insert(ItemEntity(title = "Active", source = "a.com"))
        itemDao.insert(ItemEntity(title = "Archived", source = "b.com", isArchived = true))

        val active = itemDao.observeAllActive().first()
        assertEquals(1, active.size)
        assertEquals("Active", active[0].title)
    }

    @Test
    fun observeAllArchivedOnlyReturnsArchived() = runTest {
        itemDao.insert(ItemEntity(title = "Active", source = "a.com"))
        itemDao.insert(ItemEntity(title = "Archived", source = "b.com", isArchived = true))

        val archived = itemDao.observeAllArchived().first()
        assertEquals(1, archived.size)
        assertEquals("Archived", archived[0].title)
    }

    @Test
    fun archiveAndUnarchive() = runTest {
        val id = itemDao.insert(ItemEntity(title = "Item", source = "test.com"))
        itemDao.archive(id)
        assertEquals(true, itemDao.getById(id)?.isArchived)

        itemDao.unarchive(id)
        assertEquals(false, itemDao.getById(id)?.isArchived)
    }

    @Test
    fun uncategorizeByCategory() = runTest {
        val catId = categoryDao.insert(CategoryEntity(name = "Cat"))
        val id = itemDao.insert(ItemEntity(title = "Item", source = "s.com", categoryId = catId))
        itemDao.uncategorizeByCategory(catId)
        assertNull(itemDao.getById(id)?.categoryId)
    }

    @Test
    fun deleteByCategoryId() = runTest {
        val catId = categoryDao.insert(CategoryEntity(name = "Cat"))
        itemDao.insert(ItemEntity(title = "Item1", source = "s.com", categoryId = catId))
        itemDao.insert(ItemEntity(title = "Item2", source = "s.com", categoryId = catId))
        itemDao.insert(ItemEntity(title = "Other", source = "s.com"))

        itemDao.deleteByCategoryId(catId)

        val all = itemDao.observeAllActive().first()
        assertEquals(1, all.size)
        assertEquals("Other", all[0].title)
    }

    @Test
    fun deleteById() = runTest {
        val id = itemDao.insert(ItemEntity(title = "Item", source = "s.com"))
        itemDao.deleteById(id)
        assertNull(itemDao.getById(id))
    }

    @Test
    fun foreignKeySetNullOnCategoryDelete() = runTest {
        val catId = categoryDao.insert(CategoryEntity(name = "Cat"))
        val itemId = itemDao.insert(ItemEntity(title = "Item", source = "s.com", categoryId = catId))

        categoryDao.deleteById(catId)

        val item = itemDao.getById(itemId)
        assertNull(item?.categoryId)
    }

    @Test
    fun updateItem() = runTest {
        val id = itemDao.insert(ItemEntity(title = "Original", source = "s.com"))
        val item = itemDao.getById(id)!!
        itemDao.update(item.copy(title = "Updated"))
        assertEquals("Updated", itemDao.getById(id)?.title)
    }

    @Test
    fun observeAllActiveOrderedByCreatedAtDesc() = runTest {
        val earlier = java.time.Instant.now().minusSeconds(100)
        val later = java.time.Instant.now()
        itemDao.insert(ItemEntity(title = "First", source = "s.com", createdAt = earlier))
        itemDao.insert(ItemEntity(title = "Second", source = "s.com", createdAt = later))

        val items = itemDao.observeAllActive().first()
        assertEquals("Second", items[0].title)
        assertEquals("First", items[1].title)
    }

    @Test
    fun archiveByCategoryArchivesAllItems() = runTest {
        val catId = categoryDao.insert(CategoryEntity(name = "Cat"))
        itemDao.insert(ItemEntity(title = "Item1", source = "s.com", categoryId = catId))
        itemDao.insert(ItemEntity(title = "Item2", source = "s.com", categoryId = catId))

        itemDao.archiveByCategory(catId)

        val active = itemDao.observeAllActive().first()
        assertTrue(active.none { it.categoryId == catId })

        val archived = itemDao.observeAllArchived().first()
        assertEquals(2, archived.count { it.categoryId == catId })
    }
}
