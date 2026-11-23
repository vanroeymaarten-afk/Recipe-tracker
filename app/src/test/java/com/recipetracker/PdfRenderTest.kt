package com.recipetracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.db.AppDatabase
import com.recipetracker.pdf.PageSink
import com.recipetracker.pdf.PdfOptions
import com.recipetracker.pdf.RecipeBookRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Draws the PDF pages onto bitmaps (2x scale) so the layout can be checked as images. */
class BitmapSink(private val outDir: File, private val prefix: String) : PageSink {
    val pages = mutableListOf<File>()
    private var bitmap: Bitmap? = null
    private var number = 0

    override fun startPage(number: Int, width: Int, height: Int): Canvas {
        this.number = number
        val bmp = Bitmap.createBitmap(width * 2, height * 2, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        bitmap = bmp
        return Canvas(bmp).apply { scale(2f, 2f) }
    }

    override fun finishPage() {
        val f = File(outDir, "$prefix-page-%02d.png".format(number))
        f.outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
        pages += f
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfRenderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File(System.getProperty("screenshotDir") ?: "build/screenshots", "pdf").apply { mkdirs() }

    @Test
    fun rendersCookbook() = runBlocking {
        val db = AppDatabase.inMemory(context)
        val photos = PhotoStorage(context)
        val repo = RecipeRepository(db, photos)
        SampleData.populate(repo, photos)
        val recipes = repo.getRecipes(listOf("burger", "focaccia", "ramen", "tiramisu"))

        val sink = BitmapSink(outDir, "cookbook")
        val pages = RecipeBookRenderer(recipes, PdfOptions(title = "The Brothers' Kitchen"), LocalDate.of(2026, 11, 23)).render(sink)
        assertEquals(pages, sink.pages.size)
        assertTrue("expected cover + contents + recipes", pages >= 6)

        val single = BitmapSink(outDir, "single")
        RecipeBookRenderer(listOf(recipes[1]), PdfOptions(includeCover = false)).render(single)
        assertTrue(single.pages.size in 1..2)
    }
}
