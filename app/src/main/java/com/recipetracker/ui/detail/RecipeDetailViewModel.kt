package com.recipetracker.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipetracker.data.RecipeRepository
import com.recipetracker.model.Recipe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    data class Loaded(val recipe: Recipe) : DetailState
}

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    val recipeId: String,
) : ViewModel() {
    /** The iteration being shown; null means the newest one. */
    var selectedIterationId by mutableStateOf<String?>(null)
        private set

    private var knownIterationIds: Set<String>? = null

    val state: StateFlow<DetailState> = repository.observeRecipe(recipeId)
        .onEach { recipe ->
            // When a new iteration was just added, jump to it.
            val ids = recipe?.iterations?.map { it.id }?.toSet() ?: emptySet()
            val previous = knownIterationIds
            if (previous != null && (ids - previous).isNotEmpty()) selectedIterationId = null
            if (selectedIterationId != null && selectedIterationId !in ids) selectedIterationId = null
            knownIterationIds = ids
        }
        .map { if (it == null) DetailState.Missing else DetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    fun selectIteration(id: String) { selectedIterationId = id }

    fun toggleFavorite(recipe: Recipe) = viewModelScope.launch { repository.setFavorite(recipe.id, !recipe.isFavorite) }

    fun deleteRecipe(onDone: () -> Unit) = viewModelScope.launch {
        repository.deleteRecipe(recipeId)
        onDone()
    }

    fun deleteIteration(id: String) = viewModelScope.launch {
        selectedIterationId = null
        repository.deleteIteration(id)
    }

    fun deleteRating(id: String) = viewModelScope.launch { repository.deleteRating(id) }
}
