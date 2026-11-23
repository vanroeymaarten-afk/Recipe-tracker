package com.recipetracker.ui.rate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recipetracker.model.Scores
import com.recipetracker.ui.components.LabeledStarInput
import com.recipetracker.ui.components.RecipePhoto
import com.recipetracker.ui.theme.KickerStyle
import com.recipetracker.util.Formatting

/**
 * Big and simple on purpose: hand the phone to whoever is at the table.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RateScreen(viewModel: RateViewModel, onDone: () -> Unit) {
    val vm = viewModel
    val recipe = vm.recipe
    val iteration = vm.iteration
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.Close, contentDescription = "Close") } },
                title = { Text("Pass the phone") },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (recipe == null || iteration == null) return@Column
            RecipePhoto(recipe.coverPhoto?.file, Modifier.size(120.dp).clip(CircleShape), recipe.title)
            Text(
                "ITERATION ${iteration.number} · ${Formatting.friendlyDate(iteration.cookedOn).uppercase()}",
                style = KickerStyle, color = MaterialTheme.colorScheme.primary,
            )
            val thanked = vm.thanked
            if (thanked != null) {
                Icon(Icons.Filled.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Text("Thanks, $thanked!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    "${Formatting.times(iteration.ratings.size, "guest rating")} so far" +
                        (iteration.guestAverage?.let { " · average ${Scores.format(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = vm::nextPerson, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Next person") }
                OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Done") }
            } else {
                Text("How was the ${recipe.title}?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                LabeledStarInput(vm.score, { vm.score = it }, starSize = 52.dp)
                OutlinedTextField(
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                val alreadyRated = iteration.ratings.map { it.raterName.lowercase() }.toSet()
                val names = vm.knownNames.filter { it.lowercase() !in alreadyRated }.take(8)
                if (names.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                        names.forEach { n -> FilterChip(selected = vm.name == n, onClick = { vm.name = n }, label = { Text(n) }) }
                    }
                }
                OutlinedTextField(
                    value = vm.comment,
                    onValueChange = { vm.comment = it },
                    label = { Text("Anything to add? (optional)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Button(onClick = vm::save, enabled = vm.canSave, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(if (vm.score == null) "Tap the stars first" else if (vm.name.isBlank()) "Fill in your name" else "Save my rating")
                }
                if (iteration.ratings.isNotEmpty()) {
                    Text(
                        "${Formatting.times(iteration.ratings.size, "person has", "people have")} rated this already",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
