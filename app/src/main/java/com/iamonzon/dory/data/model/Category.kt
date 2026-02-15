package com.iamonzon.dory.data.model

import com.iamonzon.dory.algorithm.FsrsParameters

data class Category(
    val id: Long = 0,
    val name: String,
    val desiredRetention: Double? = null,
    val fsrsParameters: FsrsParameters? = null
)
