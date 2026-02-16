package com.iamonzon.dory.data.repository

import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.entity.toDomain
import com.iamonzon.dory.data.db.entity.toEntity
import com.iamonzon.dory.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class CategoryDeleteStrategy {
    UncategorizeItems,
    ArchiveItems,
    DeleteItems
}

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao
) {

    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getAll(): List<Category> =
        categoryDao.getAll().map { it.toDomain() }

    suspend fun getById(id: Long): Category? =
        categoryDao.getById(id)?.toDomain()

    suspend fun insert(category: Category): Long =
        categoryDao.insert(category.toEntity())

    suspend fun update(category: Category) =
        categoryDao.update(category.toEntity())

    suspend fun rename(id: Long, newName: String) {
        val entity = categoryDao.getById(id) ?: return
        categoryDao.update(entity.copy(name = newName))
    }

    suspend fun delete(categoryId: Long, strategy: CategoryDeleteStrategy) {
        when (strategy) {
            CategoryDeleteStrategy.UncategorizeItems ->
                itemDao.uncategorizeByCategory(categoryId)
            CategoryDeleteStrategy.ArchiveItems ->
                itemDao.archiveByCategory(categoryId)
            CategoryDeleteStrategy.DeleteItems ->
                itemDao.deleteByCategoryId(categoryId)
        }
        categoryDao.deleteById(categoryId)
    }
}
