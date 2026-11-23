package com.recipetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.recipetracker.ui.AppNavigation
import com.recipetracker.ui.theme.RecipeTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeTrackerTheme {
                AppNavigation(appContainer)
            }
        }
    }
}
