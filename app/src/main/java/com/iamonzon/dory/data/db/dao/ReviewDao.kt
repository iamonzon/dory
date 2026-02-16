package com.iamonzon.dory.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iamonzon.dory.data.db.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Insert
    suspend fun insert(review: ReviewEntity): Long

    @Update
    suspend fun update(review: ReviewEntity)

    @Delete
    suspend fun delete(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE item_id = :itemId ORDER BY reviewed_at ASC")
    fun observeByItemId(itemId: Long): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE item_id = :itemId ORDER BY reviewed_at DESC LIMIT 1")
    suspend fun getLatestReviewForItem(itemId: Long): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE item_id = :itemId ORDER BY reviewed_at DESC LIMIT :limit")
    suspend fun getLatestByItemId(itemId: Long, limit: Int): List<ReviewEntity>

    @Query("SELECT COUNT(*) FROM reviews WHERE item_id = :itemId")
    suspend fun getReviewCountForItem(itemId: Long): Int
}
