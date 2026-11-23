package com.recipetracker

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.recipetracker.data.PhotoStorage
import com.recipetracker.data.RecipeRepository
import com.recipetracker.data.SettingsStore
import com.recipetracker.data.backup.BackupManager
import com.recipetracker.data.db.AppDatabase
import com.recipetracker.pdf.PdfExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Creates the app's long-lived objects once. */
class AppContainer(context: Context) {
    val database = AppDatabase.create(context)
    val photos = PhotoStorage(context)
    val repository = RecipeRepository(database, photos)
    val settings = SettingsStore(context)
    val backups = BackupManager(database, photos)
    val pdfExporter = PdfExporter(context)
}

class RecipeTrackerApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { runCatching { container.repository.cleanUpPhotos() } }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).crossfade(true).build()
}

val Context.appContainer: AppContainer get() = (applicationContext as RecipeTrackerApp).container
