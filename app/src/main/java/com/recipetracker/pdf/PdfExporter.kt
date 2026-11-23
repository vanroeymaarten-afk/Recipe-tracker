package com.recipetracker.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.recipetracker.model.Recipe
import com.recipetracker.util.Formatting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ExportedPdf(val file: File, val pageCount: Int)

class PdfExporter(private val context: Context) {
    suspend fun export(recipes: List<Recipe>, options: PdfOptions): ExportedPdf = withContext(Dispatchers.Default) {
        require(recipes.isNotEmpty()) { "Pick at least one recipe" }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val name = if (recipes.size == 1) recipes[0].title else options.title
        val file = File(dir, Formatting.fileSlug(name) + ".pdf")
        val document = PdfDocument()
        try {
            val pages = RecipeBookRenderer(recipes, options).render(PdfDocumentSink(document))
            file.outputStream().use { document.writeTo(it) }
            ExportedPdf(file, pages)
        } finally {
            document.close()
        }
    }
}
