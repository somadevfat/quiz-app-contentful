package com.example.contentful_javasilver.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // Import Compose Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define colors using Color() composable for type safety
private val md_theme_light_primary = Color(0xFF7C5EAA)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFF1DEFF)
private val md_theme_light_onPrimaryContainer = Color(0xFF311962)
private val md_theme_light_secondary = Color(0xFF645A70)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFEBDDF7)
private val md_theme_light_onSecondaryContainer = Color(0xFF1F182A)
private val md_theme_light_tertiary = Color(0xFF7E525D)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFFFD9E1)
private val md_theme_light_onTertiaryContainer = Color(0xFF31101B)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_background = Color(0xFFFFFBFF)
private val md_theme_light_onBackground = Color(0xFF1D1B1E)
private val md_theme_light_surface = Color(0xFFFFFBFF)
private val md_theme_light_onSurface = Color(0xFF1D1B1E)
private val md_theme_light_surfaceVariant = Color(0xFFE8E0EB)
private val md_theme_light_onSurfaceVariant = Color(0xFF4A454E)
private val md_theme_light_outline = Color(0xFF7B757F)
private val md_theme_light_inverseOnSurface = Color(0xFFF5EFF4)
private val md_theme_light_inverseSurface = Color(0xFF323033)
private val md_theme_light_inversePrimary = Color(0xFFDEC1FF)
private val md_theme_light_surfaceTint = Color(0xFF7C5EAA)
private val md_theme_light_outlineVariant = Color(0xFFCCC4CF)
private val md_theme_light_scrim = Color(0xFF000000)
// Added Surface Container Colors for Light theme
private val md_theme_light_surfaceContainer = Color(0xFFF5EFF4) // Example, adjust as needed
private val md_theme_light_surfaceContainerHigh = Color(0xFFF1EFF4)
private val md_theme_light_surfaceContainerHighest = Color(0xFFE8E0EB)


private val md_theme_dark_primary = Color(0xFFDEC1FF)
private val md_theme_dark_onPrimary = Color(0xFF4A2A78)
private val md_theme_dark_primaryContainer = Color(0xFF61428F)
private val md_theme_dark_onPrimaryContainer = Color(0xFFF1DEFF)
private val md_theme_dark_secondary = Color(0xFFCEC1DA)
private val md_theme_dark_onSecondary = Color(0xFF352D40)
private val md_theme_dark_secondaryContainer = Color(0xFF4C4357)
private val md_theme_dark_onSecondaryContainer = Color(0xFFEBDDF7)
private val md_theme_dark_tertiary = Color(0xFFF0B8C5)
private val md_theme_dark_onTertiary = Color(0xFF4A2530)
private val md_theme_dark_tertiaryContainer = Color(0xFF633B46)
private val md_theme_dark_onTertiaryContainer = Color(0xFFFFD9E1)
private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_theme_dark_background = Color(0xFF141218) // Adjusted slightly from surface for potential difference
private val md_theme_dark_onBackground = Color(0xFFE7E0E8)
private val md_theme_dark_surface = Color(0xFF141218)
private val md_theme_dark_onSurface = Color(0xFFE7E0E8)
private val md_theme_dark_surfaceVariant = Color(0xFF4A454E)
private val md_theme_dark_onSurfaceVariant = Color(0xFFCCC4CF)
private val md_theme_dark_outline = Color(0xFF958E99)
private val md_theme_dark_inverseOnSurface = Color(0xFF1D1B1E) // Matches light onBackground
private val md_theme_dark_inverseSurface = Color(0xFFE7E0E8) // Matches light onSurface
private val md_theme_dark_inversePrimary = Color(0xFF7C5EAA)
private val md_theme_dark_surfaceTint = Color(0xFFDEC1FF)
private val md_theme_dark_outlineVariant = Color(0xFF4A454E)
private val md_theme_dark_scrim = Color(0xFF000000)
// Added Surface Container Colors for Dark theme
private val md_theme_dark_surfaceContainer = Color(0xFF211F26)
private val md_theme_dark_surfaceContainerHigh = Color(0xFF2C2930)
private val md_theme_dark_surfaceContainerHighest = Color(0xFF37343B)


private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
    // Add surface container colors
    surfaceContainerLowest = Color(0xFFFFFFFF), // Example value
    surfaceContainerLow = Color(0xFFF5EFF4),    // Example value (match High?)
    surfaceContainer = md_theme_light_surfaceContainer, // Example value
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest,
)


private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    // Add surface container colors
    surfaceContainerLowest = Color(0xFF0F0D13), // Example value
    surfaceContainerLow = Color(0xFF1D1B1E),    // Example value
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
)

// Helper function to find the Activity from a Context
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ContentfulJavasilverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // We don't have the user selection logic here yet, so disable it for now
    dynamicColor: Boolean = false, // Set to false to use defined colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Find activity safely using the helper function
            val activity = view.context.findActivity()
            if (activity != null) { // Check if activity is found
                val window = activity.window
                // Make status bar transparent for edge-to-edge
                window.statusBarColor = Color.Transparent.toArgb()
                // Set status bar icon colors based on theme
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
} 