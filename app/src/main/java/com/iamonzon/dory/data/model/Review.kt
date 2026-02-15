package com.iamonzon.dory.data.model

import com.iamonzon.dory.algorithm.Rating
import java.time.Instant

data class Review(
    val id: Long = 0,
    val itemId: Long,
    val rating: Rating,
    val notes: String? = null,
    val reviewedAt: Instant = Instant.now(),
    val stabilityAfter: Double,
    val difficultyAfter: Double
)
