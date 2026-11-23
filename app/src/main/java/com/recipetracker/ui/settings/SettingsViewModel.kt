package com.recipetracker.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.SettingsStore
import com.recipetracker.data.backup.BackupManager
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsStore,
    private val backups: BackupManager,
    private val repository: RecipeRepository,
) : ViewModel() {
    var name by mutableStateOf(settings.userName.value)
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)

    fun saveName() { settings.setUserName(name) }

    fun exportTo(resolver: ContentResolver, uri: Uri) = run("Backup") {
        val count = resolver.openOutputStream(uri)?.use { backups.export(it, settings.userName.value) } ?: 0
        "Saved ${count} recipes with their photos"
    }

    fun importFrom(resolver: ContentResolver, uri: Uri) = run("Import") {
        val r = resolver.openInputStream(uri)?.use { backups.import(it) } ?: error("Couldn't open the file")
        buildString {
            append("Imported: ${r.newRecipes} new recipes")
            if (r.updatedRecipes > 0) append(", ${r.updatedRecipes} updated")
            append(", ${r.newIterations} iterations, ${r.newRatings} ratings, ${r.newPhotos} photos.")
        }
    }

    private fun run(what: String, block: suspend () -> String) {
        busy = true
        viewModelScope.launch {
            message = try { block() } catch (e: Exception) { "$what failed: ${e.message}" }
            repository.cleanUpPhotos()
            busy = false
        }
    }
}
