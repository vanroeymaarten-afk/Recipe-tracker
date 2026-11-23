package com.recipetracker.data.backup

import androidx.room.withTransaction
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.db.AppDatabase
import com.recipetracker.data.db.IterationEntity
import com.recipetracker.data.db.PhotoEntity
import com.recipetracker.data.db.RatingEntity
import com.recipetracker.data.db.RecipeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class RecipeArchive(
    val format: String = FORMAT,
    val version: Int = 1,
    val exportedAt: Long,
    val exportedBy: String,
    val recipes: List<RecipeEntity>,
    val iterations: List<IterationEntity>,
    val photos: List<PhotoEntity>,
    val ratings: List<RatingEntity>,
) {
    companion object {
        const val FORMAT = "recipe-tracker"
    }
}

data class ImportResult(val newRecipes: Int, val updatedRecipes: Int, val newIterations: Int, val newRatings: Int, val newPhotos: Int)

/**
 * Writes recipes (all, or a selection) with their photos to a .zip file, and merges such a
 * file back in. Importing never deletes anything: rows are matched by id, a newer edit wins,
 * and anything not yet on this phone is added. That makes the same file work as a backup
 * and as a way to send recipes to someone else who uses the app.
 */
class BackupManager(
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {
    private val dao = db.recipeDao()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

    suspend fun export(out: OutputStream, exportedBy: String, recipeIds: List<String>? = null): Int = withContext(Dispatchers.IO) {
        val all = if (recipeIds == null) dao.getAll() else dao.getByIds(recipeIds)
        val iterations = all.flatMap { it.iterations }
        val archive = RecipeArchive(
            exportedAt = System.currentTimeMillis(),
            exportedBy = exportedBy,
            recipes = all.map { it.recipe },
            iterations = iterations.map { it.iteration },
            photos = iterations.flatMap { it.photos },
            ratings = iterations.flatMap { it.ratings },
        )
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(json.encodeToString(archive).toByteArray())
            zip.closeEntry()
            for (photo in archive.photos) {
                val file = photos.file(photo.fileName)
                if (!file.exists()) continue
                zip.putNextEntry(ZipEntry("$PHOTO_DIR${photo.fileName}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        archive.recipes.size
    }

    suspend fun exportToFile(file: File, exportedBy: String, recipeIds: List<String>? = null): Int {
        file.parentFile?.mkdirs()
        return file.outputStream().use { export(it, exportedBy, recipeIds) }
    }

    suspend fun import(input: InputStream): ImportResult = withContext(Dispatchers.IO) {
        var archive: RecipeArchive? = null
        val copiedPhotos = mutableListOf<String>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                when {
                    name == DATA_ENTRY -> archive = json.decodeFromString<RecipeArchive>(zip.readBytes().decodeToString())
                    name.startsWith(PHOTO_DIR) && !entry.isDirectory -> {
                        val fileName = name.removePrefix(PHOTO_DIR)
                        // Only plain file names: never write outside the photos folder.
                        if (fileName.isNotEmpty() && !fileName.contains('/') && !fileName.contains('\\') && fileName != "..") {
                            val target = photos.file(fileName)
                            if (!target.exists()) {
                                target.outputStream().use { zip.copyTo(it) }
                                copiedPhotos += fileName
                            }
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val data = archive ?: throw IllegalArgumentException("This file is not a Recipe Tracker export.")
        require(data.format == RecipeArchive.FORMAT) { "This file is not a Recipe Tracker export." }

        var newRecipes = 0
        var updatedRecipes = 0
        var newIterations = 0
        var newRatings = 0
        var newPhotos = 0
        db.withTransaction {
            val recipeIds = mutableSetOf<String>()
            for (r in data.recipes) {
                val existing = dao.getRecipeEntity(r.id)
                when {
                    existing == null -> { dao.upsertRecipe(r); newRecipes++ }
                    r.updatedAt > existing.updatedAt -> { dao.upsertRecipe(r); updatedRecipes++ }
                }
                recipeIds += r.id
            }
            val iterationIds = mutableSetOf<String>()
            for (i in data.iterations) {
                if (i.recipeId !in recipeIds) continue
                val existing = dao.getIterationEntity(i.id)
                when {
                    existing == null -> { dao.upsertIteration(i); newIterations++ }
                    i.updatedAt > existing.updatedAt -> dao.upsertIteration(i)
                }
                iterationIds += i.id
            }
            val existingPhotoIds = dao.allPhotoIds().toSet()
            val photoRows = data.photos.filter {
                it.iterationId in iterationIds && it.id !in existingPhotoIds && photos.file(it.fileName).exists()
            }
            dao.insertPhotosIfAbsent(photoRows)
            newPhotos = photoRows.size

            val existingRatingIds = dao.allRatingIds().toSet()
            val ratingRows = data.ratings.filter { it.iterationId in iterationIds && it.id !in existingRatingIds }
            dao.insertRatingsIfAbsent(ratingRows)
            newRatings = ratingRows.size
        }
        // Photo files that came with the archive but ended up unused are removed again.
        val referenced = dao.allPhotoFileNames().toSet()
        copiedPhotos.filter { it !in referenced }.forEach { photos.delete(it) }
        ImportResult(newRecipes, updatedRecipes, newIterations, newRatings, newPhotos)
    }

    companion object {
        const val DATA_ENTRY = "recipes.json"
        const val PHOTO_DIR = "photos/"
    }
}
