package com.recipetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFFA0461F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF77574A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCD),
    onSecondaryContainer = Color(0xFF2C160C),
    tertiary = Color(0xFF5F6A2A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3EBB0),
    onTertiaryContainer = Color(0xFF1B2000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),
    inverseSurface = Color(0xFF392E2B),
    inverseOnSurface = Color(0xFFFFEDE8),
    inversePrimary = Color(0xFFFFB596),
    surfaceDim = Color(0xFFE8D6D1),
    surfaceBright = Color(0xFFFFF8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainer = Color(0xFFFCEAE4),
    surfaceContainerHigh = Color(0xFFF6E4DE),
    surfaceContainerHighest = Color(0xFFF1DFD9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF7E2E0A),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFE7BDAD),
    onSecondary = Color(0xFF442A1F),
    secondaryContainer = Color(0xFF5D4034),
    onSecondaryContainer = Color(0xFFFFDBCD),
    tertiary = Color(0xFFC7CF8F),
    onTertiary = Color(0xFF303600),
    tertiaryContainer = Color(0xFF474E14),
    onTertiaryContainer = Color(0xFFE3EBB0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A110F),
    onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF1A110F),
    onSurface = Color(0xFFF1DFD9),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFFA08C86),
    outlineVariant = Color(0xFF53433E),
    inverseSurface = Color(0xFFF1DFD9),
    inverseOnSurface = Color(0xFF392E2B),
    inversePrimary = Color(0xFFA0461F),
    surfaceDim = Color(0xFF1A110F),
    surfaceBright = Color(0xFF423733),
    surfaceContainerLowest = Color(0xFF140C0A),
    surfaceContainerLow = Color(0xFF231917),
    surfaceContainer = Color(0xFF271D1B),
    surfaceContainerHigh = Color(0xFF322825),
    surfaceContainerHighest = Color(0xFF3D3230),
)

/** Star yellow used for ratings in both themes. */
val StarColor = Color(0xFFE3A21A)

private val base = Typography()
private val Serif = FontFamily.Serif

private val AppTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(letterSpacing = 0.8.sp),
)

/** Small uppercase label above sections ("INGREDIENTS", "METHOD"). */
val KickerStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)

@Composable
fun RecipeTrackerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
