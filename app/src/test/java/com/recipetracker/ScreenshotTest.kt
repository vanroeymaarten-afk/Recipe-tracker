package com.recipetracker

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.SettingsStore
import com.recipetracker.data.db.AppDatabase
import com.recipetracker.model.Recipe
import com.recipetracker.pdf.PdfOptions
import com.recipetracker.ui.detail.RecipeDetailContent
import com.recipetracker.ui.edit.EditForm
import com.recipetracker.ui.edit.EditViewModel
import com.recipetracker.ui.export.ExportContent
import com.recipetracker.ui.export.ExportStatus
import com.recipetracker.ui.home.HomeContent
import com.recipetracker.ui.home.HomeUiState
import com.recipetracker.ui.rate.RateScreen
import com.recipetracker.ui.rate.RateViewModel
import com.recipetracker.ui.theme.RecipeTrackerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w400dp-h1000dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File(System.getProperty("screenshotDir") ?: "build/screenshots", "app").apply { mkdirs() }
    private lateinit var repo: RecipeRepository
    private lateinit var photos: PhotoStorage
    private lateinit var recipes: List<Recipe>

    @Before
    fun setUp() = runBlocking {
        photos = PhotoStorage(context)
        // Photos are left out: images load asynchronously and would not show in a still capture.
        repo = RecipeRepository(AppDatabase.inMemory(context), photos)
        SampleData.populate(repo, photos, withPhotos = false)
        recipes = repo.getRecipes(listOf("burger", "focaccia", "tiramisu", "ramen"))
    }

    /** Lets Room's background threads finish and runs the main looper until [done]. */
    private fun idleUntil(done: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (!done() && System.currentTimeMillis() < end) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        check(done()) { "timed out" }
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent { RecipeTrackerTheme { Surface { content() } } }
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun home() = shot("1-home") {
        HomeContent(HomeUiState(loading = false, totalCount = 4, recipes = recipes, categories = listOf("BBQ", "Baking", "Dessert", "Soup")))
    }

    @Test fun homeSelecting() = shot("2-home-select") {
        HomeContent(HomeUiState(loading = false, totalCount = 4, recipes = recipes, selecting = true, selection = setOf("burger", "tiramisu")))
    }

    @Test fun homeEmpty() = shot("0-home-empty") { HomeContent(HomeUiState(loading = false)) }

    @Test fun detail() = shot("3-detail") { RecipeDetailContent(recipes[0], recipes[0].latest) }

    @Test fun edit() {
        val vm = EditViewModel(repo, photos, SettingsStore(context), "burger", null, "b3")
        idleUntil { !vm.loading }
        shot("4-cook-again") { EditForm(vm) }
    }

    @Test fun rate() {
        val vm = RateViewModel(repo, "b3")
        idleUntil { vm.iteration != null }
        vm.score = 4.5f
        vm.name = "Sarah"
        shot("5-rate") { RateScreen(vm, onDone = {}) }
    }

    @Test fun export() = shot("6-export") {
        ExportContent(recipes, recipes.map { it.id }.toSet(), PdfOptions(), ExportStatus.Idle)
    }
}
