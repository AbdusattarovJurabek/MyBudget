package uz.mybudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer,
    onPrimaryContainer = Indigo900,
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldDark,
    tertiary = Amber500,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Red500,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Red600,
    surface = Slate50,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    background = Color.White,
    onBackground = Slate900,
    outline = Slate200,
    outlineVariant = Slate100,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    inversePrimary = Indigo400,
)

private val DarkColors = darkColorScheme(
    primary = Indigo400,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Indigo900,
    onPrimaryContainer = IndigoContainer,
    secondary = Emerald400,
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = EmeraldContainer,
    tertiary = Amber500,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Red400,
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFECACA),
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    background = Slate950,
    onBackground = Slate50,
    outline = Slate700,
    outlineVariant = Slate800,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    inversePrimary = Indigo600,
)

@Composable
fun MyBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}