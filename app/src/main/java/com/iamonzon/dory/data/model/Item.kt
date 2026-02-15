package com.iamonzon.dory.data.model

import java.time.Instant

data class Item(
    val id: Long = 0,
    val title: String,
    val source: String,
    val categoryId: Long? = null,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false
)
