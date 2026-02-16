package com.iamonzon.dory.data.mock

import com.iamonzon.dory.algorithm.FsrsParameters
import com.iamonzon.dory.algorithm.Rating
import com.iamonzon.dory.data.model.Category
import com.iamonzon.dory.data.model.Item
import com.iamonzon.dory.data.model.Review
import com.iamonzon.dory.data.model.ReviewUrgency
import java.time.Instant
import java.time.temporal.ChronoUnit

object MockData {

    val categories = listOf(
        Category(id = 1, name = "Programming"),
        Category(id = 2, name = "Design"),
        Category(id = 3, name = "Languages")
    )

    private val now = Instant.now()

    val items = listOf(
        // 3 overdue
        Item(id = 1, title = "Kotlin Coroutines", source = "kotlinlang.org", categoryId = 1, notes = "Focus on structured concurrency", createdAt = now.minus(30, ChronoUnit.DAYS)),
        Item(id = 2, title = "Gestalt Principles", source = "nngroup.com", categoryId = 2, createdAt = now.minus(25, ChronoUnit.DAYS)),
        Item(id = 3, title = "Spanish Subjunctive", source = "spanishdict.com", categoryId = 3, notes = "Irregular forms", createdAt = now.minus(20, ChronoUnit.DAYS)),
        // 3 due today
        Item(id = 4, title = "Compose State Management", source = "developer.android.com", categoryId = 1, createdAt = now.minus(14, ChronoUnit.DAYS)),
        Item(id = 5, title = "Color Theory Basics", source = "interaction-design.org", categoryId = 2, notes = "Complementary and analogous", createdAt = now.minus(10, ChronoUnit.DAYS)),
        Item(id = 6, title = "French Verb Conjugation", source = "lawlessfrench.com", categoryId = 3, createdAt = now.minus(7, ChronoUnit.DAYS)),
        // 2 not due
        Item(id = 7, title = "Room Database Patterns", source = "developer.android.com", categoryId = 1, createdAt = now.minus(2, ChronoUnit.DAYS)),
        Item(id = 8, title = "Typography in UI", source = "material.io", categoryId = 2, notes = "Scale and hierarchy", createdAt = now.minus(1, ChronoUnit.DAYS)),
        // 2 archived
        Item(id = 9, title = "Java Streams API", source = "docs.oracle.com", categoryId = 1, createdAt = now.minus(60, ChronoUnit.DAYS), isArchived = true),
        Item(id = 10, title = "CSS Grid Layout", source = "mdn.io", categoryId = 2, createdAt = now.minus(45, ChronoUnit.DAYS), isArchived = true)
    )

    val reviews = listOf(
        // Reviews for item 1 (Kotlin Coroutines) — overdue
        Review(id = 1, itemId = 1, rating = Rating.Good, notes = "Getting better", reviewedAt = now.minus(15, ChronoUnit.DAYS), stabilityAfter = 5.2, difficultyAfter = 4.8),
        Review(id = 2, itemId = 1, rating = Rating.Hard, reviewedAt = now.minus(10, ChronoUnit.DAYS), stabilityAfter = 3.1, difficultyAfter = 5.5),
        // Reviews for item 2 (Gestalt) — overdue
        Review(id = 3, itemId = 2, rating = Rating.Again, notes = "Need to revisit", reviewedAt = now.minus(12, ChronoUnit.DAYS), stabilityAfter = 0.5, difficultyAfter = 7.2),
        // Reviews for item 3 (Spanish) — overdue
        Review(id = 4, itemId = 3, rating = Rating.Good, reviewedAt = now.minus(8, ChronoUnit.DAYS), stabilityAfter = 4.0, difficultyAfter = 5.0),
        // Reviews for item 4 (Compose State) — due today
        Review(id = 5, itemId = 4, rating = Rating.Easy, notes = "Solid understanding", reviewedAt = now.minus(5, ChronoUnit.DAYS), stabilityAfter = 10.0, difficultyAfter = 3.2),
        // Reviews for item 5 (Color Theory) — due today
        Review(id = 6, itemId = 5, rating = Rating.Good, reviewedAt = now.minus(3, ChronoUnit.DAYS), stabilityAfter = 6.5, difficultyAfter = 4.1),
        // Reviews for item 7 (Room) — not due
        Review(id = 7, itemId = 7, rating = Rating.Good, reviewedAt = now.minus(1, ChronoUnit.DAYS), stabilityAfter = 8.0, difficultyAfter = 3.8),
    )

    fun reviewsForItem(itemId: Long): List<Review> =
        reviews.filter { it.itemId == itemId }

    private val urgencyMap = mapOf(
        1L to ReviewUrgency.Overdue,
        2L to ReviewUrgency.Overdue,
        3L to ReviewUrgency.Overdue,
        4L to ReviewUrgency.DueToday,
        5L to ReviewUrgency.DueToday,
        6L to ReviewUrgency.DueToday,
        7L to ReviewUrgency.NotDue,
        8L to ReviewUrgency.NotDue
    )

    data class DashboardItem(
        val item: Item,
        val urgency: ReviewUrgency,
        val categoryName: String?
    )

    val dashboardItems: List<DashboardItem> =
        items.filter { !it.isArchived }.map { item ->
            DashboardItem(
                item = item,
                urgency = urgencyMap[item.id] ?: ReviewUrgency.NotDue,
                categoryName = categories.find { it.id == item.categoryId }?.name
            )
        }

    val dueItems: List<DashboardItem> =
        dashboardItems.filter { it.urgency != ReviewUrgency.NotDue }

    val archivedItems: List<Item> =
        items.filter { it.isArchived }

    data class ProfileStats(
        val totalItems: Int,
        val masteredCount: Int,
        val strugglingCount: Int,
        val byCategory: Map<String, Int>
    )

    val profileStats = ProfileStats(
        totalItems = items.count { !it.isArchived },
        masteredCount = 2,
        strugglingCount = 3,
        byCategory = mapOf(
            "Programming" to 3,
            "Design" to 3,
            "Languages" to 2
        )
    )

    fun itemById(id: Long): Item? = items.find { it.id == id }

    val defaultFsrsParameters: FsrsParameters = FsrsParameters.DEFAULT
}
