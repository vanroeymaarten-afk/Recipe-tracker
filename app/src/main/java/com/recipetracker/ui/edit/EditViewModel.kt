package com.recipetracker.ui.edit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.SettingsStore
import com.recipetracker.data.db.IterationEntity
import com.recipetracker.data.db.PhotoEntity
import com.recipetracker.data.db.RecipeEntity
import com.recipetracker.util.TextLists
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

data class ListItem(val key: Long, val text: String)

data class EditablePhoto(val id: String, val fileName: String, val file: File, val caption: String, val isNew: Boolean)

enum class EditMode { NewRecipe, CookAgain, EditIteration }

sealed interface EditEvent {
    data class Saved(val recipeId: String) : EditEvent
    data class Message(val text: String) : EditEvent
}

class EditViewModel(
    private val repository: RecipeRepository,
    private val photos: PhotoStorage,
    private val settings: SettingsStore,
    private val recipeId: String?,
    private val iterationId: String?,
    private val copyFromIterationId: String?,
) : ViewModel() {
    private val keys = AtomicLong(1)
    private fun item(text: String = "") = ListItem(keys.getAndIncrement(), text)

    val mode: EditMode = when {
        recipeId == null -> EditMode.NewRecipe
        iterationId != null -> EditMode.EditIteration
        else -> EditMode.CookAgain
    }

    var loading by mutableStateOf(true); private set
    var saving by mutableStateOf(false); private set
    var iterationNumber by mutableIntStateOf(1); private set
    var titleError by mutableStateOf(false); private set
    /** "Next time" note of the iteration this one starts from, shown as a reminder. */
    var reminder by mutableStateOf<String?>(null); private set
    var importingPhotos by mutableIntStateOf(0); private set

    // The dish
    var title by mutableStateOf("")
    var category by mutableStateOf("")
    val tags = mutableStateListOf<String>()
    var description by mutableStateOf("")
    var servings by mutableStateOf("")
    var prepMinutes by mutableStateOf("")
    var cookMinutes by mutableStateOf("")
    var source by mutableStateOf("")

    // This time
    var cookedOn by mutableStateOf(LocalDate.now())
    var cookedBy by mutableStateOf("")
    val ingredients = mutableStateListOf<ListItem>()
    val steps = mutableStateListOf<ListItem>()
    var selfRating by mutableStateOf<Float?>(null)
    var whatChanged by mutableStateOf("")
    var nextTime by mutableStateOf("")
    var notes by mutableStateOf("")
    val photoList = mutableStateListOf<EditablePhoto>()

    var categorySuggestions by mutableStateOf(DEFAULT_CATEGORIES); private set
    var cookSuggestions by mutableStateOf(emptyList<String>()); private set

    private var existingRecipe: RecipeEntity? = null
    private var existingIteration: IterationEntity? = null
    private var initialSnapshot: List<Any?> = emptyList()
    private var cameraTarget: File? = null

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val known = repository.categories()
            categorySuggestions = (known + DEFAULT_CATEGORIES).distinctBy { it.lowercase() }
            cookSuggestions = repository.cookNames()
            cookedBy = settings.userName.value
            if (recipeId != null) {
                val recipe = repository.getRecipe(recipeId)
                existingRecipe = repository.getRecipeEntity(recipeId)
                existingRecipe?.let { r ->
                    title = r.title; category = r.category; tags.addAll(r.tags); description = r.description
                    servings = r.servings?.toString() ?: ""; prepMinutes = r.prepMinutes?.toString() ?: ""
                    cookMinutes = r.cookMinutes?.toString() ?: ""; source = r.source
                }
                if (iterationId != null) {
                    val it = recipe?.iteration(iterationId)
                    existingIteration = repository.getIterationEntity(iterationId)
                    if (it != null) {
                        iterationNumber = it.number
                        cookedOn = it.cookedOn; cookedBy = it.cookedBy
                        ingredients.addAll(it.ingredients.map { s -> item(s) })
                        steps.addAll(it.steps.map { s -> item(s) })
                        selfRating = it.selfRating; whatChanged = it.whatChanged; nextTime = it.nextTime; notes = it.notes
                        photoList.addAll(it.photos.map { p -> EditablePhoto(p.id, p.fileName, p.file, p.caption, false) })
                    }
                } else {
                    iterationNumber = (recipe?.iterations?.size ?: 0) + 1
                    val base = recipe?.iteration(copyFromIterationId) ?: recipe?.latest
                    if (base != null) {
                        ingredients.addAll(base.ingredients.map { s -> item(s) })
                        steps.addAll(base.steps.map { s -> item(s) })
                        reminder = base.nextTime.takeIf { it.isNotBlank() }
                    }
                }
            }
            if (ingredients.isEmpty()) ingredients.add(item())
            if (steps.isEmpty()) steps.add(item())
            initialSnapshot = snapshot()
            loading = false
        }
    }

    val hasChanges: Boolean get() = !loading && snapshot() != initialSnapshot

    private fun snapshot(): List<Any?> = listOf(
        title, category, tags.toList(), description, servings, prepMinutes, cookMinutes, source,
        cookedOn, cookedBy, ingredients.map { it.text }, steps.map { it.text }, selfRating,
        whatChanged, nextTime, notes, photoList.map { it.id to it.caption },
    )

    fun updateTitle(value: String) { title = value; if (value.isNotBlank()) titleError = false }

    fun addTag(raw: String) {
        raw.split(',', '#').map { it.trim() }.filter { it.isNotEmpty() }.forEach { t ->
            if (tags.none { it.equals(t, true) }) tags.add(t)
        }
    }

    fun removeTag(tag: String) { tags.remove(tag) }

    // ---------------------------------------------------------------- list editing

    /**
     * Updates a row. A newline (Enter, or pasting several lines) splits the text into
     * more rows. Returns the key of the row that should get focus, if any.
     */
    fun updateItem(list: MutableList<ListItem>, index: Int, value: String): Long? {
        if (index !in list.indices) return null
        if (!value.contains('\n') && !value.contains('\r')) {
            list[index] = list[index].copy(text = value)
            return null
        }
        val lines = value.split('\n', '\r')
        val first = lines.first()
        val rest = lines.drop(1)
        val pasted = rest.map { TextLists.clean(it) }.filter { it.isNotEmpty() }
        list[index] = list[index].copy(text = if (pasted.isNotEmpty()) TextLists.clean(first) else first.trimEnd())
        val newItems = if (pasted.isEmpty()) listOf(item()) else pasted.map { item(it) }
        list.addAll(index + 1, newItems)
        return newItems.last().key
    }

    fun addItem(list: MutableList<ListItem>, afterIndex: Int = list.lastIndex): Long {
        val new = item()
        list.add((afterIndex + 1).coerceIn(0, list.size), new)
        return new.key
    }

    fun removeItem(list: MutableList<ListItem>, index: Int) {
        if (index !in list.indices) return
        list.removeAt(index)
        if (list.isEmpty()) list.add(item())
    }

    fun moveItem(list: MutableList<ListItem>, index: Int, delta: Int) {
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        val moved = list.removeAt(index)
        list.add(target, moved)
    }

    // ---------------------------------------------------------------- photos

    fun newCameraUri(): Uri {
        val (file, uri) = photos.newCameraTarget()
        cameraTarget = file
        return uri
    }

    fun onCameraResult(success: Boolean) {
        val file = cameraTarget ?: return
        cameraTarget = null
        if (!success) { file.delete(); return }
        viewModelScope.launch {
            importUris(listOf(Uri.fromFile(file)))
            file.delete()
        }
    }

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch { importUris(uris) }
    }

    private suspend fun importUris(uris: List<Uri>) {
        importingPhotos += uris.size
        for (uri in uris) {
            val name = photos.import(uri)
            importingPhotos -= 1
            if (name == null) {
                _events.send(EditEvent.Message("Couldn't read one of the pictures"))
            } else {
                photoList.add(EditablePhoto(RecipeRepository.newId(), name, photos.file(name), "", true))
            }
        }
    }

    fun removePhoto(photo: EditablePhoto) {
        photoList.remove(photo)
        if (photo.isNew) photos.delete(photo.fileName)
    }

    fun makeCover(photo: EditablePhoto) {
        if (photoList.remove(photo)) photoList.add(0, photo)
    }

    fun setCaption(photo: EditablePhoto, caption: String) {
        val i = photoList.indexOfFirst { it.id == photo.id }
        if (i >= 0) photoList[i] = photoList[i].copy(caption = caption)
    }

    /** Throws away photos added in this session that were never saved. */
    fun discard() {
        photoList.filter { it.isNew }.forEach { photos.delete(it.fileName) }
        photoList.removeAll { it.isNew }
    }

    // ---------------------------------------------------------------- save

    fun save() {
        if (saving || loading) return
        if (title.isBlank()) {
            titleError = true
            viewModelScope.launch { _events.send(EditEvent.Message("Give the dish a name first")) }
            return
        }
        if (importingPhotos > 0) {
            viewModelScope.launch { _events.send(EditEvent.Message("Wait a moment, photos are still loading")) }
            return
        }
        saving = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cook = cookedBy.trim()
            if (settings.userName.value.isBlank() && cook.isNotEmpty()) settings.setUserName(cook)
            val rid = existingRecipe?.id ?: recipeId ?: RecipeRepository.newId()
            val recipe = RecipeEntity(
                id = rid,
                title = title.trim(),
                description = description.trim(),
                category = category.trim(),
                tags = tags.toList(),
                servings = servings.trim().toIntOrNull(),
                prepMinutes = prepMinutes.trim().toIntOrNull(),
                cookMinutes = cookMinutes.trim().toIntOrNull(),
                source = source.trim(),
                isFavorite = existingRecipe?.isFavorite ?: false,
                createdBy = existingRecipe?.createdBy ?: settings.userName.value.ifBlank { cook },
                createdAt = existingRecipe?.createdAt ?: now,
                updatedAt = now,
            )
            val iid = existingIteration?.id ?: RecipeRepository.newId()
            val iteration = IterationEntity(
                id = iid,
                recipeId = rid,
                cookedOn = cookedOn.toEpochDay(),
                cookedBy = cook,
                ingredients = ingredients.map { it.text.trim() }.filter { it.isNotEmpty() },
                steps = steps.map { it.text.trim() }.filter { it.isNotEmpty() },
                selfRating = selfRating,
                whatChanged = whatChanged.trim(),
                nextTime = nextTime.trim(),
                notes = notes.trim(),
                createdAt = existingIteration?.createdAt ?: now,
                updatedAt = now,
            )
            val photoRows = photoList.mapIndexed { index, p ->
                PhotoEntity(id = p.id, iterationId = iid, fileName = p.fileName, caption = p.caption.trim(), position = index, createdAt = now)
            }
            try {
                repository.save(recipe, iteration, photoRows)
                repository.cleanUpPhotos()
                photoList.replaceAll { it.copy(isNew = false) }
                initialSnapshot = snapshot()
                _events.send(EditEvent.Saved(rid))
            } catch (e: Exception) {
                _events.send(EditEvent.Message("Saving failed: ${e.message}"))
            } finally {
                saving = false
            }
        }
    }

    override fun onCleared() {
        // Leaving without saving (e.g. system back after confirming) must not leave stray files.
        if (photoList.any { it.isNew }) discard()
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf("Breakfast", "Starter", "Main", "Side", "Soup", "Salad", "Dessert", "Baking", "Sauce", "Snack", "Drinks", "BBQ")
    }
}
