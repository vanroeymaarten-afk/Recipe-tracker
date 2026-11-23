package com.recipetracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.backup.BackupManager
import com.recipetracker.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RepositoryAndBackupTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun iterationsAreNumberedByDateAndScoresAveraged() = runBlocking {
        val db = AppDatabase.inMemory(context)
        val photos = PhotoStorage(context)
        val repo = RecipeRepository(db, photos)
        SampleData.populate(repo, photos)

        val burger = repo.getRecipe("burger")!!
        assertEquals(listOf("b1", "b2", "b3"), burger.iterations.map { it.id })
        assertEquals(listOf(1, 2, 3), burger.iterations.map { it.number })
        val latest = burger.latest!!
        assertEquals(4, latest.photos.size)
        assertEquals("Double stack", latest.photos.first().caption)
        // self 4.5 + guests 5, 4.5, 4 -> 4.5
        assertEquals(4.5f, latest.overallScore!!, 0.001f)
        assertEquals(4.5f, latest.guestAverage!!, 0.001f)
        assertEquals("b3", burger.bestIteration!!.id)
        assertEquals(setOf("Pieter", "Sarah", "Mom"), repo.raterNames().toSet())

        // Deleting the recipe cascades to everything below it.
        repo.deleteIteration("b1")
        assertEquals(2, repo.getRecipe("burger")!!.iterations.size)
        assertEquals(1, repo.getRecipe("burger")!!.iterations.first().number)
        db.close()
    }

    @Test
    fun backupRoundTripMergesWithoutDuplicates() = runBlocking {
        val photos = PhotoStorage(context)
        val source = AppDatabase.inMemory(context)
        val sourceRepo = RecipeRepository(source, photos)
        SampleData.populate(sourceRepo, photos)
        val bytes = ByteArrayOutputStream().also { BackupManager(source, photos).export(it, "Maarten") }.toByteArray()

        // Import into an empty phone.
        val target = AppDatabase.inMemory(context)
        val targetRepo = RecipeRepository(target, photos)
        val backups = BackupManager(target, photos)
        val first = backups.import(ByteArrayInputStream(bytes))
        assertEquals(4, first.newRecipes)
        assertEquals(6, first.newIterations)
        assertEquals(5, first.newRatings)
        val imported = targetRepo.getRecipe("burger")!!
        assertEquals(4.5f, imported.latest!!.overallScore!!, 0.001f)
        assertTrue(imported.latest!!.photos.all { it.file.exists() })

        // Importing the same file again changes nothing.
        val second = backups.import(ByteArrayInputStream(bytes))
        assertEquals(0, second.newRecipes)
        assertEquals(0, second.newIterations)
        assertEquals(0, second.newRatings)
        assertEquals(4, targetRepo.getAllRecipes().size)

        // Sharing a selection only carries those recipes.
        val partial = ByteArrayOutputStream().also { BackupManager(source, photos).export(it, "Maarten", listOf("focaccia")) }.toByteArray()
        val third = AppDatabase.inMemory(context)
        BackupManager(third, photos).import(ByteArrayInputStream(partial))
        assertEquals(listOf("Rosemary focaccia"), RecipeRepository(third, photos).getAllRecipes().map { it.title })
    }
}
