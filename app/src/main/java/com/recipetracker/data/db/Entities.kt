package com.recipetracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * All ids are random UUID strings and every row carries timestamps and an author.
 * That keeps records unique across phones, so recipe files from two people can be
 * merged (see BackupManager) and a shared online database can be added later.
 */

/** The dish itself: what stays the same between the times you cook it. */
@Serializable
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val servings: Int? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val source: String = "",
    val isFavorite: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

/** One time the dish was cooked: exactly how it was made, how it went and what to change. */
@Serializable
@Entity(
    tableName = "iterations",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IterationEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    /** Day the dish was cooked, as LocalDate.toEpochDay(). */
    val cookedOn: Long,
    val cookedBy: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    /** The cook's own score, 0.5 to 5 in half steps, or null when not rated. */
    val selfRating: Float? = null,
    val whatChanged: String = "",
    val nextTime: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = IterationEntity::class,
            parentColumns = ["id"],
            childColumns = ["iterationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("iterationId")],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val iterationId: String,
    /** File name inside the app's private photos folder. */
    val fileName: String,
    val caption: String = "",
    val position: Int = 0,
    val createdAt: Long,
)

/** A score given by someone tasting the dish (the "pass the phone" ratings). */
@Serializable
@Entity(
    tableName = "ratings",
    foreignKeys = [
        ForeignKey(
            entity = IterationEntity::class,
            parentColumns = ["id"],
            childColumns = ["iterationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("iterationId")],
)
data class RatingEntity(
    @PrimaryKey val id: String,
    val iterationId: String,
    val raterName: String,
    val score: Float,
    val comment: String = "",
    val createdAt: Long,
)
