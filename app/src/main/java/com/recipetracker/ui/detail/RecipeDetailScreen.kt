package com.recipetracker.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipetracker.model.Iteration
import com.recipetracker.model.Recipe
import com.recipetracker.model.Scores
import com.recipetracker.ui.components.BulletLine
import com.recipetracker.ui.components.EmptyState
import com.recipetracker.ui.components.NumberedStep
import com.recipetracker.ui.components.RecipePhoto
import com.recipetracker.ui.components.ScoreRow
import com.recipetracker.ui.components.SectionCard
import com.recipetracker.ui.components.StarRatingDisplay
import com.recipetracker.ui.theme.KickerStyle
import com.recipetracker.util.Formatting
import com.recipetracker.util.TextLists

@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit,
    onEditIteration: (String) -> Unit,
    onCookAgain: (String) -> Unit,
    onRate: (String) -> Unit,
    onExportPdf: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        DetailState.Missing -> LaunchedEffect(Unit) { onBack() }
        is DetailState.Loaded -> {
            val recipe = s.recipe
            val iteration = recipe.iteration(viewModel.selectedIterationId) ?: recipe.latest
            RecipeDetailContent(
                recipe = recipe,
                iteration = iteration,
                onBack = onBack,
                onSelectIteration = viewModel::selectIteration,
                onToggleFavorite = { viewModel.toggleFavorite(recipe) },
                onEditIteration = onEditIteration,
                onCookAgain = onCookAgain,
                onRate = onRate,
                onExportPdf = onExportPdf,
                onDeleteRecipe = { viewModel.deleteRecipe(onBack) },
                onDeleteIteration = viewModel::deleteIteration,
                onDeleteRating = viewModel::deleteRating,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailContent(
    recipe: Recipe,
    iteration: Iteration?,
    onBack: () -> Unit = {},
    onSelectIteration: (String) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onEditIteration: (String) -> Unit = {},
    onCookAgain: (String) -> Unit = {},
    onRate: (String) -> Unit = {},
    onExportPdf: () -> Unit = {},
    onDeleteRecipe: () -> Unit = {},
    onDeleteIteration: (String) -> Unit = {},
    onDeleteRating: (String) -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteRecipe by remember { mutableStateOf(false) }
    var confirmDeleteIteration by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                title = { Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (recipe.isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (recipe.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onExportPdf) { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (iteration != null) {
                                DropdownMenuItem(
                                    text = { Text("Edit iteration ${iteration.number}") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                    onClick = { menuOpen = false; onEditIteration(iteration.id) },
                                )
                                if (recipe.iterations.size > 1) {
                                    DropdownMenuItem(
                                        text = { Text("Delete iteration ${iteration.number}") },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                        onClick = { menuOpen = false; confirmDeleteIteration = true },
                                    )
                                }
                            }
                            DropdownMenuItem(
                                text = { Text("Delete recipe") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menuOpen = false; confirmDeleteRecipe = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (iteration != null) {
                ExtendedFloatingActionButton(
                    onClick = { onCookAgain(iteration.id) },
                    icon = { Icon(Icons.Filled.Replay, contentDescription = null) },
                    text = { Text("Cook it again") },
                )
            }
        },
    ) { padding ->
        if (iteration == null) {
            EmptyState(Icons.Filled.Schedule, "No iterations", "This recipe has no cooking log yet.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
        ) {
            PhotoPager(iteration, recipe)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Header(recipe, iteration)
                if (recipe.iterations.size > 1) IterationPicker(recipe, iteration, onSelectIteration)
                RatingsCard(iteration, onRate = { onRate(iteration.id) }, onDeleteRating = onDeleteRating)
                if (iteration.whatChanged.isNotBlank()) {
                    NoteCard("What changed this time", iteration.whatChanged, Icons.Filled.AutoAwesome, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                }
                if (iteration.ingredients.any { it.isNotBlank() }) {
                    SectionCard("Ingredients", icon = Icons.Filled.ShoppingBasket) {
                        iteration.ingredients.filter { it.isNotBlank() }.forEach { line ->
                            BulletLine(line, TextLists.isSubheading(line))
                        }
                    }
                }
                if (iteration.steps.any { it.isNotBlank() }) {
                    SectionCard("Method", icon = Icons.Filled.FormatListNumbered) {
                        iteration.steps.filter { it.isNotBlank() }.forEachIndexed { i, step -> NumberedStep(i + 1, step) }
                    }
                }
                if (iteration.notes.isNotBlank()) {
                    NoteCard("Notes", iteration.notes, Icons.AutoMirrored.Filled.Notes, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceContainerLow)
                }
                if (iteration.nextTime.isNotBlank()) {
                    NoteCard("Next time", iteration.nextTime, Icons.Filled.Lightbulb, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                }
                if (recipe.iterations.count { it.overallScore != null } >= 2) ScoreTrend(recipe, iteration)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onEditIteration(iteration.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit iteration ${iteration.number}")
                    }
                }
            }
        }
    }

    if (confirmDeleteRecipe) {
        AlertDialog(
            onDismissRequest = { confirmDeleteRecipe = false },
            title = { Text("Delete “${recipe.title}”?") },
            text = { Text("All ${Formatting.times(recipe.iterations.size, "iteration")}, photos and ratings of this recipe are removed from this phone.") },
            confirmButton = { TextButton(onClick = { confirmDeleteRecipe = false; onDeleteRecipe() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDeleteRecipe = false }) { Text("Cancel") } },
        )
    }
    if (confirmDeleteIteration && iteration != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteIteration = false },
            title = { Text("Delete iteration ${iteration.number}?") },
            text = { Text("The log of ${Formatting.fullDate(iteration.cookedOn)} with its photos and ratings is removed. The other iterations stay.") },
            confirmButton = { TextButton(onClick = { confirmDeleteIteration = false; onDeleteIteration(iteration.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDeleteIteration = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PhotoPager(iteration: Iteration, recipe: Recipe) {
    val photos = iteration.photos
    if (photos.isEmpty()) {
        // Fall back to the cover of another iteration, dimmed less prominently.
        val cover = recipe.coverPhoto
        if (cover != null) {
            Box(Modifier.padding(16.dp)) {
                RecipePhoto(cover.file, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)), recipe.title)
                Surface(
                    color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White, shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                ) {
                    Text("No photos of this iteration", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        return
    }
    val pager = rememberPagerState(pageCount = { photos.size })
    Box(Modifier.padding(16.dp)) {
        HorizontalPager(state = pager, pageSpacing = 8.dp, key = { photos[it].id }) { page ->
            RecipePhoto(photos[page].file, Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(24.dp)), photos[page].caption.ifBlank { recipe.title })
        }
        if (photos.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(12.dp).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(photos.size) { i ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.45f)))
                }
            }
        }
        val caption = photos.getOrNull(pager.currentPage)?.caption
        if (!caption.isNullOrBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            ) {
                Text(caption, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(recipe: Recipe, iteration: Iteration) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val kicker = buildList {
            if (recipe.category.isNotBlank()) add(recipe.category.uppercase())
            add("ITERATION ${iteration.number} OF ${recipe.iterations.size}")
        }.joinToString("  ·  ")
        Text(kicker, style = KickerStyle, color = MaterialTheme.colorScheme.primary)
        Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
        if (recipe.description.isNotBlank()) {
            Text(recipe.description, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InfoChip(Icons.Filled.Schedule, Formatting.longDate(iteration.cookedOn))
            if (iteration.cookedBy.isNotBlank()) InfoChip(Icons.Filled.People, "by ${iteration.cookedBy}")
            recipe.servings?.let { InfoChip(Icons.Filled.Groups, "Serves $it") }
            recipe.prepMinutes?.let { InfoChip(Icons.Filled.Schedule, "Prep ${Formatting.minutes(it)}") }
            recipe.cookMinutes?.let { InfoChip(Icons.Filled.Schedule, "Cook ${Formatting.minutes(it)}") }
        }
        if (recipe.source.isNotBlank()) {
            Text("Source: ${recipe.source}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (recipe.tags.isNotEmpty()) {
            Text(recipe.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    AssistChip(
        onClick = {},
        label = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
    )
}

@Composable
private fun IterationPicker(recipe: Recipe, current: Iteration, onSelect: (String) -> Unit) {
    Column {
        Text("ITERATIONS", style = KickerStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 16.dp)) {
            items(recipe.iterations.asReversed(), key = { it.id }) { it ->
                FilterChip(
                    selected = it.id == current.id,
                    onClick = { onSelect(it.id) },
                    label = {
                        val score = it.overallScore?.let { s -> "  ★ ${Scores.format(s)}" } ?: ""
                        Text("#${it.number} · ${Formatting.friendlyDate(it.cookedOn)}$score")
                    },
                )
            }
        }
    }
}

@Composable
private fun RatingsCard(iteration: Iteration, onRate: () -> Unit, onDeleteRating: (String) -> Unit) {
    var ratingToDelete by remember { mutableStateOf<String?>(null) }
    SectionCard("Ratings", icon = Icons.Filled.Groups, container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (iteration.overallScore == null) "Not rated yet" else Scores.format(iteration.overallScore),
                    style = if (iteration.overallScore == null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                )
                if (iteration.overallScore != null) {
                    StarRatingDisplay(iteration.overallScore, starSize = 18.dp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${Scores.label(iteration.overallScore)} · ${Formatting.times(iteration.ratingCount, "rating")}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilledTonalButton(onClick = onRate) {
                Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Let others rate")
            }
        }
        if (iteration.selfRating != null || iteration.ratings.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        iteration.selfRating?.let {
            RatingLine(if (iteration.cookedBy.isBlank()) "The cook" else "${iteration.cookedBy} (cook)", it, "", null)
        }
        iteration.ratings.forEach { r ->
            RatingLine(r.raterName, r.score, r.comment, onRemove = { ratingToDelete = r.id })
        }
    }
    ratingToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { ratingToDelete = null },
            title = { Text("Remove this rating?") },
            confirmButton = { TextButton(onClick = { onDeleteRating(id); ratingToDelete = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { ratingToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RatingLine(name: String, score: Float, comment: String, onRemove: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
                ScoreRow(score, starSize = 13.dp)
            }
            if (comment.isNotBlank()) {
                Text("“$comment”", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove rating", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NoteCard(title: String, text: String, icon: ImageVector, accent: Color, container: Color) {
    SectionCard(title, icon = icon, accent = accent, container = container) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** How the score moved over the iterations: the point of iterating! */
@Composable
private fun ScoreTrend(recipe: Recipe, current: Iteration) {
    val points = recipe.iterations.filter { it.overallScore != null }
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    SectionCard("Score over the iterations", icon = Icons.Filled.TrendingUp) {
        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = 8.dp, horizontal = 6.dp)) {
            fun yFor(score: Float) = size.height - (score / 5f) * size.height
            listOf(1f, 2.5f, 5f).forEach { s ->
                drawLine(grid, Offset(0f, yFor(s)), Offset(size.width, yFor(s)), strokeWidth = 1f)
            }
            val step = if (points.size > 1) size.width / (points.size - 1) else 0f
            val path = Path()
            points.forEachIndexed { i, it ->
                val p = Offset(i * step, yFor(it.overallScore!!))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
            points.forEachIndexed { i, it ->
                val p = Offset(i * step, yFor(it.overallScore!!))
                drawCircle(line, radius = if (it.id == current.id) 7.dp.toPx() else 4.dp.toPx(), center = p)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#${points.first().number}: ${Scores.format(points.first().overallScore)}", style = MaterialTheme.typography.labelMedium)
            val best = recipe.bestIteration
            if (best != null) Text("Best: #${best.number} (${Scores.format(best.overallScore)})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("#${points.last().number}: ${Scores.format(points.last().overallScore)}", style = MaterialTheme.typography.labelMedium)
        }
    }
}
