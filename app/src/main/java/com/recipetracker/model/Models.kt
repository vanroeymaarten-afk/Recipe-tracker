package com.recipetracker.model

import java.io.File
import java.time.LocalDate

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val tags: List<String>,
    val servings: Int?,
    val prepMinutes: Int?,
    val cookMinutes: Int?,
    val source: String,
    val isFavorite: Boolean,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Oldest first. Iteration numbers follow this order. */
    val iterations: List<Iteration>,
) {
    val latest: Iteration? get() = iterations.lastOrNull()

    /** First photo of the newest iteration that has photos. */
    val coverPhoto: Photo?
        get() = iterations.asReversed().firstNotNullOfOrNull { it.photos.firstOrNull() }

    /** Score of the newest version of the dish. */
    val score: Float? get() = latest?.overallScore ?: iterations.lastOrNull { it.overallScore != null }?.overallScore

    val bestIteration: Iteration? get() = iterations.filter { it.overallScore != null }.maxByOrNull { it.overallScore!! }

    val lastCookedOn: LocalDate? get() = iterations.maxOfOrNull { it.cookedOn }

    fun iteration(id: String?): Iteration? = iterations.firstOrNull { it.id == id }
}

data class Iteration(
    val id: String,
    val recipeId: String,
    /** 1-based position in time: the first time it was cooked is iteration 1. */
    val number: Int,
    val cookedOn: LocalDate,
    val cookedBy: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val selfRating: Float?,
    val whatChanged: String,
    val nextTime: String,
    val notes: String,
    val photos: List<Photo>,
    val ratings: List<Rating>,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val guestAverage: Float? get() = Scores.average(ratings.map { it.score })

    /** Everyone's scores together: the cook's own plus every guest. */
    val overallScore: Float? get() = Scores.average(listOfNotNull(selfRating) + ratings.map { it.score })

    val ratingCount: Int get() = ratings.size + if (selfRating != null) 1 else 0
}

data class Photo(
    val id: String,
    val iterationId: String,
    val fileName: String,
    val file: File,
    val caption: String,
    val position: Int,
)

data class Rating(
    val id: String,
    val iterationId: String,
    val raterName: String,
    val score: Float,
    val comment: String,
    val createdAt: Long,
)

object Scores {
    fun average(values: List<Float>): Float? = if (values.isEmpty()) null else values.sum() / values.size

    /** Short words for a 0.5..5 score, shown next to the stars while rating. */
    fun label(score: Float?): String = when {
        score == null || score <= 0f -> "Tap the stars"
        score <= 0.5f -> "Inedible"
        score <= 1f -> "Bad"
        score <= 1.5f -> "Poor"
        score <= 2f -> "Meh"
        score <= 2.5f -> "Okay"
        score <= 3f -> "Decent"
        score <= 3.5f -> "Good"
        score <= 4f -> "Really good"
        score <= 4.5f -> "Excellent"
        else -> "Perfect!"
    }

    fun format(score: Float?): String = if (score == null) "–" else String.format(java.util.Locale.US, "%.1f", score)
}
