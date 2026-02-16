package com.iamonzon.dory.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iamonzon.dory.data.db.dao.CategoryDao
import com.iamonzon.dory.data.db.dao.ItemDao
import com.iamonzon.dory.data.db.dao.ReviewDao
import com.iamonzon.dory.data.db.entity.CategoryEntity
import com.iamonzon.dory.data.db.entity.ItemEntity
import com.iamonzon.dory.data.db.entity.ReviewEntity

@Database(
    entities = [ItemEntity::class, ReviewEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DoryDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun reviewDao(): ReviewDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        fun create(context: Context): DoryDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DoryDatabase::class.java,
                "dory.db"
            ).build()

        fun createInMemory(context: Context): DoryDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                DoryDatabase::class.java
            ).build()
    }
}
