package com.recipetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.SettingsStore
import com.recipetracker.data.backup.BackupManager
import com.recipetracker.model.Recipe
import com.recipetracker.util.Formatting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.File
import java.time.LocalDate

sealed interface HomeFilter {
    data object All : HomeFilter
    data object Favorites : HomeFilter
    data class Category(val name: String) : HomeFilter
}

enum class SortOrder(val label: String) {
    Recent("Recently cooked"),
    TopRated("Best rated"),
    MostCooked("Most cooked"),
    Alphabetical("A to Z"),
}

data class HomeUiState(
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val recipes: List<Recipe> = emptyList(),
    val query: String = "",
    val filter: HomeFilter = HomeFilter.All,
    val categories: List<String> = emptyList(),
    val sort: SortOrder = SortOrder.Recent,
    val selecting: Boolean = false,
    val selection: Set<String> = emptySet(),
    /** Selected recipe ids in the order they appear in the list. */
    val orderedSelection: List<String> = emptyList(),
)

private data class Selection(val active: Boolean, val ids: Set<String>)

class HomeViewModel(
    private val repository: RecipeRepository,
    private val backups: BackupManager,
    private val settings: SettingsStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<HomeFilter>(HomeFilter.All)
    private val sort = MutableStateFlow(SortOrder.Recent)
    private val selection = MutableStateFlow(Selection(false, emptySet()))

    val state: StateFlow<HomeUiState> =
        combine(repository.observeRecipes(), query, filter, sort, selection) { all, q, f, s, sel ->
            val sortedAll = sortRecipes(all, s)
            val categories = all.map { it.category.trim() }.filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
            val activeFilter = if (f is HomeFilter.Category && categories.none { it.equals(f.name, true) }) HomeFilter.All else f
            val visible = sortedAll
                .filter { r ->
                    when (activeFilter) {
                        HomeFilter.All -> true
                        HomeFilter.Favorites -> r.isFavorite
                        is HomeFilter.Category -> r.category.trim().equals(activeFilter.name, ignoreCase = true)
                    }
                }
                .filter { matches(it, q) }
            val existing = all.map { it.id }.toSet()
            val selectedIds = sel.ids.filter { it in existing }.toSet()
            HomeUiState(
                loading = false,
                totalCount = all.size,
                recipes = visible,
                query = q,
                filter = activeFilter,
                categories = categories,
                sort = s,
                selecting = sel.active,
                selection = selectedIds,
                orderedSelection = sortedAll.map { it.id }.filter { it in selectedIds },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: HomeFilter) { filter.value = value }
    fun setSort(value: SortOrder) { sort.value = value }

    fun startSelecting() { selection.value = Selection(true, emptySet()) }
    fun startSelectingWith(id: String) { selection.value = Selection(true, setOf(id)) }
    fun clearSelection() { selection.value = Selection(false, emptySet()) }

    fun toggle(id: String) = selection.update { s ->
        Selection(true, if (id in s.ids) s.ids - id else s.ids + id)
    }

    fun selectAllVisible() = selection.update { s ->
        val visible = state.value.recipes.map { it.id }.toSet()
        Selection(true, if (s.ids.containsAll(visible)) s.ids - visible else s.ids + visible)
    }

    /** Writes the selected recipes (with photos) into a file that can be sent to someone. */
    suspend fun exportRecipeFile(ids: List<String>, cacheDir: File): File {
        val name = if (ids.size == 1) repository.getRecipe(ids.first())?.title ?: "recipe" else "${ids.size}-recipes"
        val file = File(File(cacheDir, "shared"), Formatting.fileSlug(name) + ".recipes.zip")
        file.parentFile?.listFiles()?.forEach { it.delete() }
        backups.exportToFile(file, settings.userName.value, ids)
        return file
    }

    private fun matches(recipe: Recipe, query: String): Boolean {
        if (query.isBlank()) return true
        val words = query.trim().lowercase().split(Regex("\\s+"))
        val haystack = buildString {
            append(recipe.title).append(' ')
            append(recipe.category).append(' ')
            append(recipe.description).append(' ')
            append(recipe.tags.joinToString(" ")).append(' ')
            recipe.latest?.let { append(it.ingredients.joinToString(" ")).append(' ').append(it.cookedBy) }
        }.lowercase()
        return words.all { haystack.contains(it) }
    }

    private fun sortRecipes(list: List<Recipe>, order: SortOrder): List<Recipe> = when (order) {
        SortOrder.Recent -> list.sortedWith(
            compareByDescending<Recipe> { it.lastCookedOn ?: LocalDate.MIN }.thenByDescending { it.updatedAt },
        )
        SortOrder.TopRated -> list.sortedWith(
            compareByDescending<Recipe> { it.score ?: -1f }.thenBy { it.title.lowercase() },
        )
        SortOrder.MostCooked -> list.sortedWith(
            compareByDescending<Recipe> { it.iterations.size }.thenBy { it.title.lowercase() },
        )
        SortOrder.Alphabetical -> list.sortedBy { it.title.lowercase() }
    }
}
