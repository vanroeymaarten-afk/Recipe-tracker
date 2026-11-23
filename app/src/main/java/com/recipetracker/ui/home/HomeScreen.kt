package com.recipetracker.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipetracker.model.Recipe
import com.recipetracker.ui.components.EmptyState
import com.recipetracker.ui.components.RecipePhoto
import com.recipetracker.ui.components.ScorePill
import com.recipetracker.ui.shareFile
import com.recipetracker.util.Formatting
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenRecipe: (String) -> Unit,
    onNewRecipe: () -> Unit,
    onExportPdf: (List<String>) -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(enabled = state.selecting) { viewModel.clearSelection() }
    HomeContent(
        state = state,
        onQueryChange = viewModel::setQuery,
        onFilterChange = viewModel::setFilter,
        onSortChange = viewModel::setSort,
        onRecipeClick = { id -> if (state.selecting) viewModel.toggle(id) else onOpenRecipe(id) },
        onRecipeLongClick = { id -> if (state.selecting) viewModel.toggle(id) else viewModel.startSelectingWith(id) },
        onNewRecipe = onNewRecipe,
        onStartSelecting = viewModel::startSelecting,
        onClearSelection = viewModel::clearSelection,
        onSelectAll = viewModel::selectAllVisible,
        onExportPdf = {
            onExportPdf(state.orderedSelection)
            viewModel.clearSelection()
        },
        onShareRecipes = {
            val ids = state.orderedSelection
            scope.launch {
                val file = viewModel.exportRecipeFile(ids, context.cacheDir)
                context.shareFile(file, "application/zip", "Send recipes")
            }
        },
        onSettings = onSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onQueryChange: (String) -> Unit = {},
    onFilterChange: (HomeFilter) -> Unit = {},
    onSortChange: (SortOrder) -> Unit = {},
    onRecipeClick: (String) -> Unit = {},
    onRecipeLongClick: (String) -> Unit = {},
    onNewRecipe: () -> Unit = {},
    onStartSelecting: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onShareRecipes: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            if (state.selecting) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) { Icon(Icons.Filled.Close, contentDescription = "Stop selecting") }
                    },
                    title = { Text(if (state.selection.isEmpty()) "Select recipes" else "${state.selection.size} selected") },
                    actions = {
                        TextButton(onClick = onSelectAll) { Text("All") }
                        IconButton(onClick = onShareRecipes, enabled = state.selection.isNotEmpty()) {
                            Icon(Icons.Filled.Share, contentDescription = "Send as recipe file")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                )
            } else {
                TopAppBar(
                    title = { Text("Recipe Tracker", style = MaterialTheme.typography.headlineSmall) },
                    actions = {
                        if (state.totalCount > 0) {
                            SortMenu(state.sort, onSortChange)
                            IconButton(onClick = onStartSelecting) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export recipes to PDF")
                            }
                        }
                        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                    },
                )
            }
        },
        floatingActionButton = {
            if (state.selecting) {
                if (state.selection.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = onExportPdf,
                        icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                        text = { Text("Create PDF") },
                    )
                }
            } else if (state.totalCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onNewRecipe,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New recipe") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.totalCount > 0) {
                SearchField(state.query, onQueryChange)
                FilterRow(state, onFilterChange)
            }
            if (state.selecting) {
                Text(
                    "Tap the recipes you want. Create a PDF, or send them as a file someone can import into their app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            when {
                state.loading -> Unit
                state.totalCount == 0 -> EmptyState(
                    icon = Icons.Filled.MenuBook,
                    title = "Your kitchen notebook",
                    message = "Log every dish you cook: photos, how you made it, how it turned out and what to change next time.",
                    action = {
                        Button(onClick = onNewRecipe) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Add your first recipe")
                        }
                    },
                )
                state.recipes.isEmpty() -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "Nothing found",
                    message = "Try another word or filter.",
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            selecting = state.selecting,
                            selected = recipe.id in state.selection,
                            onClick = { onRecipeClick(recipe.id) },
                            onLongClick = { onRecipeLongClick(recipe.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search dishes, ingredients, tags…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun FilterRow(state: HomeUiState, onFilterChange: (HomeFilter) -> Unit) {
    val filters = buildList {
        add(HomeFilter.All)
        add(HomeFilter.Favorites)
        state.categories.forEach { add(HomeFilter.Category(it)) }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        items(filters, key = { it.toString() }) { f ->
            FilterChip(
                selected = state.filter == f,
                onClick = { onFilterChange(f) },
                label = {
                    Text(
                        when (f) {
                            HomeFilter.All -> "All"
                            HomeFilter.Favorites -> "Favourites"
                            is HomeFilter.Category -> f.name
                        },
                    )
                },
                leadingIcon = if (f == HomeFilter.Favorites) {
                    { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
        }
    }
}

@Composable
private fun SortMenu(current: SortOrder, onSortChange: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    onClick = { onSortChange(order); open = false },
                    trailingIcon = if (order == current) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCard(
    recipe: Recipe,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .clip(shape)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            RecipePhoto(
                recipe.coverPhoto?.file,
                Modifier.fillMaxWidth().aspectRatio(1f),
                contentDescription = recipe.title,
                iconSize = 48.dp,
            )
            ScorePill(recipe.score, Modifier.align(Alignment.BottomStart).padding(8.dp))
            if (recipe.isFavorite) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Favourite", tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                }
            }
            if (selecting) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val subtitle = buildList {
                if (recipe.iterations.size > 1) add("${recipe.iterations.size}× cooked")
                recipe.lastCookedOn?.let { add(Formatting.friendlyDate(it)) }
            }.joinToString(" · ")
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
