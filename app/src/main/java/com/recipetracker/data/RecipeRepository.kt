package com.recipetracker.data

import androidx.room.withTransaction
import com.recipetracker.data.db.AppDatabase
import com.recipetracker.data.db.IterationEntity
import com.recipetracker.data.db.IterationWithDetails
import com.recipetracker.data.db.PhotoEntity
import com.recipetracker.data.db.RatingEntity
import com.recipetracker.data.db.RecipeEntity
import com.recipetracker.data.db.RecipeWithIterations
import com.recipetracker.model.Iteration
import com.recipetracker.model.Photo
import com.recipetracker.model.Rating
import com.recipetracker.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class RecipeRepository(
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {
    private val dao = db.recipeDao()

    fun observeRecipes(): Flow<List<Recipe>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeRecipe(id: String): Flow<Recipe?> = dao.observe(id).map { it?.toModel() }

    suspend fun getRecipe(id: String): Recipe? = dao.get(id)?.toModel()

    /** Recipes in the same order as [ids]. */
    suspend fun getRecipes(ids: List<String>): List<Recipe> {
        val byId = dao.getByIds(ids).associate { it.recipe.id to it.toModel() }
        return ids.mapNotNull { byId[it] }
    }

    suspend fun getAllRecipes(): List<Recipe> = dao.getAll().map { it.toModel() }

    suspend fun recipeForIteration(iterationId: String): Recipe? =
        dao.recipeIdForIteration(iterationId)?.let { getRecipe(it) }

    suspend fun getRecipeEntity(id: String): RecipeEntity? = dao.getRecipeEntity(id)

    suspend fun getIterationEntity(id: String): IterationEntity? = dao.getIterationEntity(id)

    /** Saves the dish and one iteration of it, with exactly the given photos, in one go. */
    suspend fun save(recipe: RecipeEntity, iteration: IterationEntity, photos: List<PhotoEntity>) {
        db.withTransaction {
            dao.upsertRecipe(recipe)
            dao.upsertIteration(iteration)
            dao.deletePhotosExcept(iteration.id, photos.map { it.id })
            dao.upsertPhotos(photos)
        }
    }

    suspend fun addRating(iterationId: String, raterName: String, score: Float, comment: String) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            dao.upsertRating(
                RatingEntity(
                    id = newId(),
                    iterationId = iterationId,
                    raterName = raterName.trim(),
                    score = score,
                    comment = comment.trim(),
                    createdAt = now,
                ),
            )
            dao.recipeIdForIteration(iterationId)?.let { dao.touchRecipe(it, now) }
        }
    }

    suspend fun deleteRating(id: String) = dao.deleteRating(id)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite, System.currentTimeMillis())

    suspend fun deleteRecipe(id: String) {
        dao.deleteRecipe(id)
        cleanUpPhotos()
    }

    suspend fun deleteIteration(id: String) {
        val recipeId = dao.recipeIdForIteration(id)
        dao.deleteIteration(id)
        recipeId?.let { dao.touchRecipe(it, System.currentTimeMillis()) }
        cleanUpPhotos()
    }

    suspend fun cleanUpPhotos() {
        photos.deleteUnreferenced(dao.allPhotoFileNames().toSet())
    }

    suspend fun raterNames(): List<String> = dao.raterNames()

    suspend fun cookNames(): List<String> = dao.cookNames()

    suspend fun categories(): List<String> = dao.categories()

    private fun RecipeWithIterations.toModel(): Recipe {
        val sorted = iterations.sortedWith(compareBy({ it.iteration.cookedOn }, { it.iteration.createdAt }))
        return Recipe(
            id = recipe.id,
            title = recipe.title,
            description = recipe.description,
            category = recipe.category,
            tags = recipe.tags,
            servings = recipe.servings,
            prepMinutes = recipe.prepMinutes,
            cookMinutes = recipe.cookMinutes,
            source = recipe.source,
            isFavorite = recipe.isFavorite,
            createdBy = recipe.createdBy,
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt,
            iterations = sorted.mapIndexed { index, it -> it.toModel(index + 1) },
        )
    }

    private fun IterationWithDetails.toModel(number: Int) = Iteration(
        id = iteration.id,
        recipeId = iteration.recipeId,
        number = number,
        cookedOn = LocalDate.ofEpochDay(iteration.cookedOn),
        cookedBy = iteration.cookedBy,
        ingredients = iteration.ingredients,
        steps = iteration.steps,
        selfRating = iteration.selfRating,
        whatChanged = iteration.whatChanged,
        nextTime = iteration.nextTime,
        notes = iteration.notes,
        photos = photos.sortedWith(compareBy({ it.position }, { it.createdAt })).map {
            Photo(it.id, it.iterationId, it.fileName, this@RecipeRepository.photos.file(it.fileName), it.caption, it.position)
        },
        ratings = ratings.sortedBy { it.createdAt }.map {
            Rating(it.id, it.iterationId, it.raterName, it.score, it.comment, it.createdAt)
        },
        createdAt = iteration.createdAt,
        updatedAt = iteration.updatedAt,
    )

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
