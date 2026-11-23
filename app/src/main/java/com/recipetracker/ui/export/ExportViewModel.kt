package com.recipetracker.ui.export

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipetracker.data.RecipeRepository
import com.recipetracker.model.Recipe
import com.recipetracker.pdf.ExportedPdf
import com.recipetracker.pdf.PdfExporter
import com.recipetracker.pdf.PdfOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object Working : ExportStatus
    data class Done(val pdf: ExportedPdf) : ExportStatus
    data class Failed(val message: String) : ExportStatus
}

class ExportViewModel(
    private val repository: RecipeRepository,
    private val exporter: PdfExporter,
    recipeIds: List<String>,
) : ViewModel() {
    var recipes by mutableStateOf(emptyList<Recipe>()); private set
    val included = mutableStateListOf<String>()
    var options by mutableStateOf(PdfOptions(includeCover = recipeIds.size > 1))
    var status by mutableStateOf<ExportStatus>(ExportStatus.Idle); private set
    var savedMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            recipes = repository.getRecipes(recipeIds)
            included.addAll(recipes.map { it.id })
        }
    }

    fun toggle(id: String) {
        if (id in included) included.remove(id) else included.add(id)
        status = ExportStatus.Idle
    }

    fun updateOptions(transform: (PdfOptions) -> PdfOptions) {
        options = transform(options)
        status = ExportStatus.Idle
    }

    fun create() {
        val chosen = recipes.filter { it.id in included }
        if (chosen.isEmpty()) return
        status = ExportStatus.Working
        viewModelScope.launch {
            status = try {
                ExportStatus.Done(exporter.export(chosen, options))
            } catch (e: Exception) {
                ExportStatus.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    fun saveTo(resolver: ContentResolver, uri: Uri) {
        val done = status as? ExportStatus.Done ?: return
        viewModelScope.launch {
            savedMessage = try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { out -> done.pdf.file.inputStream().use { it.copyTo(out) } }
                }
                "PDF saved"
            } catch (e: Exception) {
                "Couldn't save: ${e.message}"
            }
        }
    }
}
