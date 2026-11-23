package com.recipetracker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.recipetracker.AppContainer
import com.recipetracker.ui.detail.RecipeDetailScreen
import com.recipetracker.ui.detail.RecipeDetailViewModel
import com.recipetracker.ui.edit.EditScreen
import com.recipetracker.ui.edit.EditViewModel
import com.recipetracker.ui.export.ExportScreen
import com.recipetracker.ui.export.ExportViewModel
import com.recipetracker.ui.home.HomeScreen
import com.recipetracker.ui.home.HomeViewModel
import com.recipetracker.ui.rate.RateScreen
import com.recipetracker.ui.rate.RateViewModel
import com.recipetracker.ui.settings.SettingsScreen
import com.recipetracker.ui.settings.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class DetailRoute(val recipeId: String)

/**
 * The editor. No ids: a brand-new recipe. recipeId + iterationId: edit that iteration.
 * recipeId + copyFromIterationId: cook it again, starting from that iteration.
 */
@Serializable
data class EditRoute(
    val recipeId: String? = null,
    val iterationId: String? = null,
    val copyFromIterationId: String? = null,
)

@Serializable
data class RateRoute(val iterationId: String)

/** Comma separated recipe ids. */
@Serializable
data class ExportRoute(val recipeIds: String)

@Serializable
object SettingsRoute

@Composable
fun AppNavigation(container: AppContainer, navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val vm = viewModel { HomeViewModel(container.repository, container.backups, container.settings) }
            HomeScreen(
                viewModel = vm,
                onOpenRecipe = { navController.navigate(DetailRoute(it)) },
                onNewRecipe = { navController.navigate(EditRoute()) },
                onExportPdf = { ids -> navController.navigate(ExportRoute(ids.joinToString(","))) },
                onSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<DetailRoute> { entry ->
            val route = entry.toRoute<DetailRoute>()
            val vm = viewModel { RecipeDetailViewModel(container.repository, route.recipeId) }
            RecipeDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEditIteration = { iterationId -> navController.navigate(EditRoute(route.recipeId, iterationId = iterationId)) },
                onCookAgain = { fromId -> navController.navigate(EditRoute(route.recipeId, copyFromIterationId = fromId)) },
                onRate = { iterationId -> navController.navigate(RateRoute(iterationId)) },
                onExportPdf = { navController.navigate(ExportRoute(route.recipeId)) },
            )
        }
        composable<EditRoute> { entry ->
            val route = entry.toRoute<EditRoute>()
            val vm = viewModel {
                EditViewModel(
                    repository = container.repository,
                    photos = container.photos,
                    settings = container.settings,
                    recipeId = route.recipeId,
                    iterationId = route.iterationId,
                    copyFromIterationId = route.copyFromIterationId,
                )
            }
            EditScreen(
                viewModel = vm,
                onClose = { navController.popBackStack() },
                onSaved = { recipeId ->
                    if (route.recipeId == null) {
                        navController.navigate(DetailRoute(recipeId)) {
                            popUpTo<HomeRoute>()
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<RateRoute> { entry ->
            val route = entry.toRoute<RateRoute>()
            val vm = viewModel { RateViewModel(container.repository, route.iterationId) }
            RateScreen(viewModel = vm, onDone = { navController.popBackStack() })
        }
        composable<ExportRoute> { entry ->
            val route = entry.toRoute<ExportRoute>()
            val vm = viewModel {
                ExportViewModel(container.repository, container.pdfExporter, route.recipeIds.split(",").filter { it.isNotBlank() })
            }
            ExportScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            val vm = viewModel { SettingsViewModel(container.settings, container.backups, container.repository) }
            SettingsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
