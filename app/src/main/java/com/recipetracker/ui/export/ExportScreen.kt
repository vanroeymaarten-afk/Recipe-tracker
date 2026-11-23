package com.recipetracker.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recipetracker.model.Recipe
import com.recipetracker.pdf.PdfOptions
import com.recipetracker.ui.components.RecipePhoto
import com.recipetracker.ui.components.ScoreRow
import com.recipetracker.ui.components.SectionCard
import com.recipetracker.ui.openFile
import com.recipetracker.ui.shareFile
import com.recipetracker.util.Formatting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(viewModel: ExportViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.saveTo(context.contentResolver, uri)
    }
    LaunchedEffect(viewModel.savedMessage) {
        viewModel.savedMessage?.let { snackbar.showSnackbar(it); viewModel.savedMessage = null }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                title = { Text("Export to PDF") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ExportContent(
            recipes = viewModel.recipes,
            included = viewModel.included.toSet(),
            options = viewModel.options,
            status = viewModel.status,
            modifier = Modifier.padding(padding),
            onToggle = viewModel::toggle,
            onOptions = viewModel::updateOptions,
            onCreate = viewModel::create,
            onShare = { (viewModel.status as? ExportStatus.Done)?.let { context.shareFile(it.pdf.file, "application/pdf", "Share PDF") } },
            onOpen = { (viewModel.status as? ExportStatus.Done)?.let { context.openFile(it.pdf.file, "application/pdf") } },
            onSave = { (viewModel.status as? ExportStatus.Done)?.let { save.launch(it.pdf.file.name) } },
        )
    }
}

@Composable
fun ExportContent(
    recipes: List<Recipe>,
    included: Set<String>,
    options: PdfOptions,
    status: ExportStatus,
    modifier: Modifier = Modifier,
    onToggle: (String) -> Unit = {},
    onOptions: ((PdfOptions) -> PdfOptions) -> Unit = {},
    onCreate: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpen: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard(Formatting.times(included.size, "recipe") + " in the PDF", icon = Icons.Filled.RestaurantMenu) {
            recipes.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggle(r.id) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecipePhoto(r.coverPhoto?.file, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), iconSize = 22.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        ScoreRow(r.score, starSize = 12.dp)
                    }
                    Checkbox(checked = r.id in included, onCheckedChange = { onToggle(r.id) })
                }
            }
        }
        SectionCard("What to include", icon = Icons.Filled.Tune) {
            OptionSwitch("Cover page & contents", "A title page with photos and a table of contents", options.includeCover) { v -> onOptions { it.copy(includeCover = v) } }
            if (options.includeCover) {
                OutlinedTextField(
                    value = options.title,
                    onValueChange = { v -> onOptions { it.copy(title = v) } },
                    label = { Text("Title on the cover") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            OptionSwitch("Photos", "Main photo and a photo gallery", options.includePhotos) { v -> onOptions { it.copy(includePhotos = v) } }
            OptionSwitch("Ratings", "Scores and what everyone said", options.includeRatings) { v -> onOptions { it.copy(includeRatings = v) } }
            OptionSwitch("Notes & tips", "Notes, what changed and next-time tips", options.includeNotes) { v -> onOptions { it.copy(includeNotes = v) } }
            OptionSwitch("Iteration history", "A summary of every time it was cooked", options.includeHistory) { v -> onOptions { it.copy(includeHistory = v) } }
        }

        when (status) {
            ExportStatus.Working -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Laying out your cookbook…")
            }
            is ExportStatus.Done -> Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Your PDF is ready", style = MaterialTheme.typography.titleMedium)
                            Text("${status.pdf.file.name} · ${Formatting.times(status.pdf.pageCount, "page")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Open") }
                        FilledTonalButton(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Save, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save") }
                    }
                    Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share") }
                }
            }
            is ExportStatus.Failed -> Text("Export failed: ${status.message}", color = MaterialTheme.colorScheme.error)
            ExportStatus.Idle -> Unit
        }
        if (status !is ExportStatus.Done) {
            Button(
                onClick = onCreate,
                enabled = included.isNotEmpty() && status != ExportStatus.Working,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create PDF")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OptionSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
