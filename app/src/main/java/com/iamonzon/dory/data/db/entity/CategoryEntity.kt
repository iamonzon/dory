package com.iamonzon.dory.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "desired_retention")
    val desiredRetention: Double? = null,
    @ColumnInfo(name = "fsrs_parameters_json")
    val fsrsParametersJson: String? = null
)
