package com.iamonzon.dory.data.db

import androidx.room.TypeConverter
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? =
        instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? =
        epochMilli?.let { Instant.ofEpochMilli(it) }
}
