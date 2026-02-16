package com.iamonzon.dory.data.db.entity

import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.Review
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class FsrsParametersJson(
    val w: List<Double>,
    val desiredRetention: Double
)

private val json = Json { ignoreUnknownKeys = true }

// --- Item ---

fun ItemEntity.toDomain(): Item = Item(
    id = id,
    title = title,
    source = source,
    categoryId = categoryId,
    notes = notes,
    createdAt = createdAt,
    isArchived = isArchived
)

fun Item.toEntity(): ItemEntity = ItemEntity(
    id = id,
    title = title,
    source = source,
    categoryId = categoryId,
    notes = notes,
    createdAt = createdAt,
    isArchived = isArchived
)

// --- Review ---

fun ReviewEntity.toDomain(): Review = Review(
    id = id,
    itemId = itemId,
    rating = Rating.fromValue(rating),
    notes = notes,
    reviewedAt = reviewedAt,
    stabilityAfter = stabilityAfter,
    difficultyAfter = difficultyAfter
)

fun Review.toEntity(): ReviewEntity = ReviewEntity(
    id = id,
    itemId = itemId,
    rating = rating.value,
    notes = notes,
    reviewedAt = reviewedAt,
    stabilityAfter = stabilityAfter,
    difficultyAfter = difficultyAfter
)

// --- Category ---

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    desiredRetention = desiredRetention,
    fsrsParameters = fsrsParametersJson?.toFsrsParameters()
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    desiredRetention = desiredRetention,
    fsrsParametersJson = fsrsParameters?.toJson()
)

// --- FsrsParameters JSON helpers ---

private fun String.toFsrsParameters(): FsrsParameters? =
    try {
        val parsed = json.decodeFromString<FsrsParametersJson>(this)
        FsrsParameters(
            w = parsed.w.toDoubleArray(),
            desiredRetention = parsed.desiredRetention
        )
    } catch (_: Exception) {
        null
    }

private fun FsrsParameters.toJson(): String {
    val dto = FsrsParametersJson(
        w = w.toList(),
        desiredRetention = desiredRetention
    )
    return json.encodeToString(FsrsParametersJson.serializer(), dto)
}
