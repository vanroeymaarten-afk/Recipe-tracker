package com.recipetracker.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class IterationWithDetails(
    @Embedded val iteration: IterationEntity,
    @Relation(parentColumn = "id", entityColumn = "iterationId")
    val photos: List<PhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "iterationId")
    val ratings: List<RatingEntity>,
)

data class RecipeWithIterations(
    @Embedded val recipe: RecipeEntity,
    @Relation(entity = IterationEntity::class, parentColumn = "id", entityColumn = "recipeId")
    val iterations: List<IterationWithDetails>,
)
