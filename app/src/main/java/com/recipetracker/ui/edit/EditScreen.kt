package com.recipetracker.ui.edit

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recipetracker.ui.components.LabeledStarInput
import com.recipetracker.ui.components.RecipePhoto
import com.recipetracker.ui.components.SectionCard
import com.recipetracker.ui.theme.KickerStyle
import com.recipetracker.util.Formatting
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(viewModel: EditViewModel, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is EditEvent.Saved -> onSaved(e.recipeId)
                is EditEvent.Message -> scope.launch { snackbar.showSnackbar(e.text) }
            }
        }
    }
    val close = { if (viewModel.hasChanges) confirmDiscard = true else { viewModel.discard(); onClose() } }
    BackHandler(onBack = close)

    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        viewModel.addPhotos(uris)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        viewModel.onCameraResult(ok)
    }

    val title = when (viewModel.mode) {
        EditMode.NewRecipe -> "New recipe"
        EditMode.CookAgain -> "Iteration ${viewModel.iterationNumber}"
        EditMode.EditIteration -> "Edit iteration ${viewModel.iterationNumber}"
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = close) { Icon(Icons.Filled.Close, contentDescription = "Close") } },
                title = { Text(title) },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !viewModel.saving && !viewModel.loading) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (viewModel.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        EditForm(
            viewModel = viewModel,
            modifier = Modifier.padding(padding),
            onTakePhoto = {
                try {
                    takePicture.launch(viewModel.newCameraUri())
                } catch (e: ActivityNotFoundException) {
                    scope.launch { snackbar.showSnackbar("No camera app found") }
                }
            },
            onPickPhotos = {
                pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("What you entered here will be lost.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; viewModel.discard(); onClose() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditForm(
    viewModel: EditViewModel,
    modifier: Modifier = Modifier,
    onTakePhoto: () -> Unit = {},
    onPickPhotos: () -> Unit = {},
) {
    val vm = viewModel
    var showDetails by rememberSaveable { mutableStateOf(vm.mode == EditMode.NewRecipe) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        vm.reminder?.let { note ->
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp)) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("LAST TIME YOU SAID", style = KickerStyle, color = MaterialTheme.colorScheme.tertiary)
                        Text(note, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        // ---------------------------------------------------------------- photos
        SectionCard("Photos", icon = Icons.Filled.AddAPhoto) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { AddPhotoTile("Camera", Icons.Filled.AddAPhoto, onTakePhoto) }
                item { AddPhotoTile("Gallery", Icons.Filled.PhotoLibrary, onPickPhotos) }
                itemsIndexed(vm.photoList, key = { _, p -> p.id }) { index, photo ->
                    PhotoThumb(
                        photo = photo,
                        isCover = index == 0,
                        onMakeCover = { vm.makeCover(photo) },
                        onRemove = { vm.removePhoto(photo) },
                        onCaption = { vm.setCaption(photo, it) },
                    )
                }
                if (vm.importingPhotos > 0) {
                    item {
                        Box(Modifier.size(104.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp))
                        }
                    }
                }
            }
            if (vm.photoList.size > 1) {
                Spacer(Modifier.height(6.dp))
                Text("Tap a photo to make it the cover or add a caption.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------------------------------------------------------------- the dish
        SectionCard(
            "The dish",
            icon = Icons.Filled.RestaurantMenu,
            trailing = if (vm.mode != EditMode.NewRecipe) {
                {
                    IconButton(onClick = { showDetails = !showDetails }, modifier = Modifier.size(28.dp)) {
                        Icon(if (showDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = "Show recipe details")
                    }
                }
            } else null,
        ) {
            OutlinedTextField(
                value = vm.title,
                onValueChange = vm::updateTitle,
                label = { Text("Name of the dish") },
                placeholder = { Text("e.g. Smash burgers") },
                isError = vm.titleError,
                supportingText = if (vm.titleError) { { Text("A name is needed") } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(visible = showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedTextField(
                        value = vm.category,
                        onValueChange = { vm.category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        vm.categorySuggestions.take(12).forEach { c ->
                            SuggestionChip(
                                onClick = { vm.category = c },
                                label = { Text(c) },
                                border = if (vm.category.equals(c, true)) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            )
                        }
                    }
                    TagInput(vm)
                    OutlinedTextField(
                        value = vm.description,
                        onValueChange = { vm.description = it },
                        label = { Text("Short description") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Serves", vm.servings, { vm.servings = it }, Modifier.weight(1f))
                        NumberField("Prep min", vm.prepMinutes, { vm.prepMinutes = it }, Modifier.weight(1f))
                        NumberField("Cook min", vm.cookMinutes, { vm.cookMinutes = it }, Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = vm.source,
                        onValueChange = { vm.source = it },
                        label = { Text("Source / inspired by") },
                        placeholder = { Text("A cookbook, a website, grandma…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // ---------------------------------------------------------------- this time
        SectionCard("This time", icon = Icons.Filled.CalendarMonth) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Formatting.friendlyDate(vm.cookedOn))
                }
                OutlinedTextField(
                    value = vm.cookedBy,
                    onValueChange = { vm.cookedBy = it },
                    label = { Text("Cooked by") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.weight(1.3f),
                )
            }
            val suggestions = vm.cookSuggestions.filter { !it.equals(vm.cookedBy.trim(), true) }.take(4)
            if (suggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { name ->
                        SuggestionChip(onClick = { vm.cookedBy = name }, label = { Text(name) })
                        if (vm.cookedBy.isNotBlank()) {
                            SuggestionChip(onClick = { vm.cookedBy = "${vm.cookedBy.trim()} & $name" }, label = { Text("+ $name") })
                        }
                    }
                }
            }
            if (vm.mode != EditMode.NewRecipe && vm.iterationNumber > 1) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = vm.whatChanged,
                    onValueChange = { vm.whatChanged = it },
                    label = { Text("What did you change this time?") },
                    placeholder = { Text("e.g. Less salt, rested the dough overnight") },
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        }

        // ---------------------------------------------------------------- ingredients & steps
        SectionCard("Ingredients", icon = Icons.Filled.ShoppingBasket) {
            ListEditor(
                vm = vm,
                items = vm.ingredients,
                numbered = false,
                placeholder = "e.g. 200 g flour",
                addLabel = "Add ingredient",
                hint = "Tip: paste a whole list at once, every line becomes an ingredient. End a line with “:” for a heading like “Sauce:”.",
            )
        }
        SectionCard("Method", icon = Icons.Filled.FormatListNumbered) {
            ListEditor(
                vm = vm,
                items = vm.steps,
                numbered = true,
                placeholder = "Describe this step",
                addLabel = "Add step",
                hint = "Press Enter to start the next step.",
            )
        }

        // ---------------------------------------------------------------- verdict
        SectionCard("How was it?", icon = Icons.Filled.Star) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LabeledStarInput(vm.selfRating, { vm.selfRating = it })
            }
            if (vm.selfRating != null) {
                TextButton(onClick = { vm.selfRating = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Clear my rating") }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "This is your own score. Others can add theirs from the recipe page with “Let others rate”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionCard("Notes", icon = Icons.AutoMirrored.Filled.Notes) {
            OutlinedTextField(
                value = vm.notes,
                onValueChange = { vm.notes = it },
                placeholder = { Text("How did it go? Texture, taste, timing…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
        SectionCard("Next time", icon = Icons.Filled.Lightbulb, accent = MaterialTheme.colorScheme.tertiary) {
            OutlinedTextField(
                value = vm.nextTime,
                onValueChange = { vm.nextTime = it },
                placeholder = { Text("What would you change next time?") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text("You'll see this reminder when you cook it again.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(onClick = vm::save, enabled = !vm.saving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (vm.mode == EditMode.NewRecipe) "Save recipe" else "Save iteration ${vm.iterationNumber}")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.cookedOn.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.cookedOn = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun AddPhotoTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        Modifier.size(104.dp).clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PhotoThumb(
    photo: EditablePhoto,
    isCover: Boolean,
    onMakeCover: () -> Unit,
    onRemove: () -> Unit,
    onCaption: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var captionDialog by remember { mutableStateOf(false) }
    Box(Modifier.size(104.dp)) {
        RecipePhoto(photo.file, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable { menu = true })
        if (isCover) {
            Surface(
                color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            ) { Text("Cover", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp)) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (!isCover) DropdownMenuItem(text = { Text("Make cover photo") }, onClick = { menu = false; onMakeCover() })
            DropdownMenuItem(text = { Text(if (photo.caption.isBlank()) "Add caption" else "Edit caption") }, onClick = { menu = false; captionDialog = true })
            DropdownMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove() })
        }
    }
    if (captionDialog) {
        var text by remember { mutableStateOf(photo.caption) }
        AlertDialog(
            onDismissRequest = { captionDialog = false },
            title = { Text("Caption") },
            text = {
                OutlinedTextField(text, { text = it }, placeholder = { Text("e.g. The crust after 20 minutes") }, singleLine = true)
            },
            confirmButton = { TextButton(onClick = { onCaption(text); captionDialog = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { captionDialog = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagInput(vm: EditViewModel) {
    var text by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (it.endsWith(",") || it.endsWith("\n")) { vm.addTag(it); text = "" } else text = it
            },
            label = { Text("Tags") },
            placeholder = { Text("vegetarian, spicy, party…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.addTag(text); text = "" }),
            trailingIcon = {
                if (text.isNotBlank()) IconButton(onClick = { vm.addTag(text); text = "" }) { Icon(Icons.Filled.Add, contentDescription = "Add tag") }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (vm.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeTag(tag) },
                        label = { Text("#$tag") },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $tag", modifier = Modifier.size(16.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(4)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun ListEditor(
    vm: EditViewModel,
    items: MutableList<ListItem>,
    numbered: Boolean,
    placeholder: String,
    addLabel: String,
    hint: String,
) {
    var focusKey by remember { mutableStateOf<Long?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, entry ->
            androidx.compose.runtime.key(entry.key) {
                val requester = remember { FocusRequester() }
                LaunchedEffect(focusKey) {
                    if (focusKey == entry.key) {
                        requester.requestFocus()
                        focusKey = null
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (numbered) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    OutlinedTextField(
                        value = entry.text,
                        onValueChange = { value -> vm.updateItem(items, index, value)?.let { focusKey = it } },
                        placeholder = { Text(placeholder) },
                        singleLine = !numbered,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = if (numbered) ImeAction.Default else ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusKey = vm.addItem(items, index) }),
                        modifier = Modifier.weight(1f).focusRequester(requester),
                    )
                    if (numbered && items.size > 1) {
                        Column {
                            IconButton(onClick = { vm.moveItem(items, index, -1) }, enabled = index > 0, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move step up")
                            }
                            IconButton(onClick = { vm.moveItem(items, index, 1) }, enabled = index < items.lastIndex, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move step down")
                            }
                        }
                    }
                    IconButton(onClick = { vm.removeItem(items, index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        TextButton(onClick = { focusKey = vm.addItem(items) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(addLabel)
        }
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

