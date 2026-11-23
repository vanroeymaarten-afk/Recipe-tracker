package com.recipetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes")
    fun observeAll(): Flow<List<RecipeWithIterations>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observe(id: String): Flow<RecipeWithIterations?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: String): RecipeWithIterations?

    @Transaction
    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<RecipeWithIterations>

    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<RecipeWithIterations>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeEntity(id: String): RecipeEntity?

    @Query("SELECT * FROM iterations WHERE id = :id")
    suspend fun getIterationEntity(id: String): IterationEntity?

    @Query("SELECT recipeId FROM iterations WHERE id = :iterationId")
    suspend fun recipeIdForIteration(iterationId: String): String?

    // Upsert updates rows in place. (REPLACE would delete the old row first, and the
    // ON DELETE CASCADE would then wipe its iterations, photos and ratings.)
    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Upsert
    suspend fun upsertIteration(iteration: IterationEntity)

    @Upsert
    suspend fun upsertPhotos(photos: List<PhotoEntity>)

    @Upsert
    suspend fun upsertRating(rating: RatingEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotosIfAbsent(photos: List<PhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRatingsIfAbsent(ratings: List<RatingEntity>)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: String)

    @Query("DELETE FROM iterations WHERE id = :id")
    suspend fun deleteIteration(id: String)

    @Query("DELETE FROM photos WHERE iterationId = :iterationId AND id NOT IN (:keepIds)")
    suspend fun deletePhotosExcept(iterationId: String, keepIds: List<String>)

    @Query("DELETE FROM ratings WHERE id = :id")
    suspend fun deleteRating(id: String)

    @Query("UPDATE recipes SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long)

    @Query("UPDATE recipes SET updatedAt = :now WHERE id = :id")
    suspend fun touchRecipe(id: String, now: Long)

    @Query("SELECT fileName FROM photos")
    suspend fun allPhotoFileNames(): List<String>

    @Query("SELECT id FROM photos")
    suspend fun allPhotoIds(): List<String>

    @Query("SELECT id FROM ratings")
    suspend fun allRatingIds(): List<String>

    @Query("SELECT raterName FROM ratings GROUP BY raterName ORDER BY MAX(createdAt) DESC")
    suspend fun raterNames(): List<String>

    @Query("SELECT category FROM recipes WHERE category != '' GROUP BY category ORDER BY COUNT(*) DESC")
    suspend fun categories(): List<String>

    @Query("SELECT cookedBy FROM iterations WHERE cookedBy != '' GROUP BY cookedBy ORDER BY MAX(createdAt) DESC")
    suspend fun cookNames(): List<String>
}
