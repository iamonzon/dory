package com.iamonzon.dory.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iamonzon.dory.data.db.DoryDatabase
import com.iamonzon.dory.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: DoryDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "Programming"))
        val category = categoryDao.getById(id)
        assertEquals("Programming", category?.name)
    }

    @Test
    fun observeAllSortedByName() = runTest {
        categoryDao.insert(CategoryEntity(name = "Zebra"))
        categoryDao.insert(CategoryEntity(name = "Apple"))
        categoryDao.insert(CategoryEntity(name = "Mango"))

        val categories = categoryDao.observeAll().first()
        assertEquals(3, categories.size)
        assertEquals("Apple", categories[0].name)
        assertEquals("Mango", categories[1].name)
        assertEquals("Zebra", categories[2].name)
    }

    @Test
    fun updateCategory() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "Old Name"))
        val category = categoryDao.getById(id)!!
        categoryDao.update(category.copy(name = "New Name"))
        assertEquals("New Name", categoryDao.getById(id)?.name)
    }

    @Test
    fun deleteCategory() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "ToDelete"))
        categoryDao.deleteById(id)
        assertNull(categoryDao.getById(id))
    }

    @Test
    fun fsrsJsonStorageAndRetrieval() = runTest {
        val fsrsJson = """{"w":[0.5,1.4,3.7,13.8,5.2,1.2,0.9,0.03,1.6,0.14,1.0,2.1,0.08,0.32,1.59,0.23,2.88],"desiredRetention":0.85}"""
        val id = categoryDao.insert(CategoryEntity(name = "Custom", fsrsParametersJson = fsrsJson))

        val retrieved = categoryDao.getById(id)
        assertEquals(fsrsJson, retrieved?.fsrsParametersJson)
    }

    @Test
    fun desiredRetentionStorageAndRetrieval() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "Custom", desiredRetention = 0.85))
        val category = categoryDao.getById(id)
        assertEquals(0.85, category?.desiredRetention ?: 0.0, 0.001)
    }

    @Test
    fun getAllReturnsAllCategories() = runTest {
        categoryDao.insert(CategoryEntity(name = "A"))
        categoryDao.insert(CategoryEntity(name = "B"))
        categoryDao.insert(CategoryEntity(name = "C"))

        val all = categoryDao.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun deleteNonExistentCategoryDoesNotThrow() = runTest {
        categoryDao.deleteById(999L)
        // Should not throw
    }
}
