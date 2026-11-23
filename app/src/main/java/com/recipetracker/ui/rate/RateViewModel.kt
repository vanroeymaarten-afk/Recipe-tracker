package com.recipetracker.ui.rate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipetracker.data.RecipeRepository
import com.recipetracker.model.Iteration
import com.recipetracker.model.Recipe
import kotlinx.coroutines.launch

class RateViewModel(private val repository: RecipeRepository, private val iterationId: String) : ViewModel() {
    var recipe by mutableStateOf<Recipe?>(null); private set
    var iteration by mutableStateOf<Iteration?>(null); private set
    var name by mutableStateOf("")
    var score by mutableStateOf<Float?>(null)
    var comment by mutableStateOf("")
    var saving by mutableStateOf(false); private set
    /** Name of the person who just saved; shows the thank-you card. */
    var thanked by mutableStateOf<String?>(null); private set
    var knownNames by mutableStateOf(emptyList<String>()); private set

    val canSave: Boolean get() = name.isNotBlank() && score != null && !saving

    init { reload() }

    private fun reload() = viewModelScope.launch {
        val r = repository.recipeForIteration(iterationId)
        recipe = r
        iteration = r?.iteration(iterationId)
        knownNames = repository.raterNames()
    }

    fun save() {
        val s = score ?: return
        if (!canSave) return
        saving = true
        viewModelScope.launch {
            repository.addRating(iterationId, name, s, comment)
            thanked = name.trim()
            saving = false
            reload()
        }
    }

    fun nextPerson() {
        name = ""
        score = null
        comment = ""
        thanked = null
    }
}
