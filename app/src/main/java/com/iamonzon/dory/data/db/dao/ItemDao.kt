package com.iamonzon.dory.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iamonzon.dory.data.db.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE is_archived = 0 ORDER BY created_at DESC")
    fun observeAllActive(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_archived = 1 ORDER BY created_at DESC")
    fun observeAllArchived(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<ItemEntity?>

    @Query("UPDATE items SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE items SET is_archived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long)

    @Query("UPDATE items SET category_id = NULL WHERE category_id = :categoryId")
    suspend fun uncategorizeByCategory(categoryId: Long)

    @Query("UPDATE items SET is_archived = 1 WHERE category_id = :categoryId")
    suspend fun archiveByCategory(categoryId: Long)

    @Query("DELETE FROM items WHERE category_id = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)
}
