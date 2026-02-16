package com.iamonzon.dory.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("item_id")]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    val rating: Int,
    val notes: String? = null,
    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: Instant = Instant.now(),
    @ColumnInfo(name = "stability_after")
    val stabilityAfter: Double,
    @ColumnInfo(name = "difficulty_after")
    val difficultyAfter: Double
)
